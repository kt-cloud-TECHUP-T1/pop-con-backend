package com.t1.popcon.queue.service;

import com.t1.popcon.queue.client.VqaClient;
import com.t1.popcon.queue.common.redis.AntiMacroScoreRepository;
import com.t1.popcon.queue.common.redis.QueueActiveRepository;
import com.t1.popcon.queue.dto.response.VqaStartResponse;
import com.t1.popcon.queue.dto.response.VqaSubmitResult;
import com.t1.popcon.queue.dto.vqa.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VqaService {

    private final VqaClient vqaClient;
    private final AntiMacroScoreRepository antiMacroScoreRepository;
    private final QueueActiveRepository activeRepository;
    private final QueueTokenResolver tokenResolver;
    private final StringRedisTemplate redisTemplate;

    private static final String VQA_SESSION_KEY_PREFIX = "vqa:session:";
    private static final long VQA_SESSION_TTL_SECONDS = 600; // 10분
    private static final int MAX_ATTEMPTS = 3;

    /**
     * 보안 퀴즈 시작 (원샷 방식)
     * - 레벨 0 (면제 대상) 판단 후 퀴즈 세션 초기화
     */
    public VqaStartResponse start(String queueToken) {
        QueueTokenResolver.TokenInfo tokenInfo = tokenResolver.resolve(queueToken);
        long userId = tokenInfo.userId();

        int totalScore = antiMacroScoreRepository.getTotalScore(userId);
        log.info("[VQA] 퀴즈 시작 시도 - userId={}, score={}", userId, totalScore);

        // 1. 레벨 0 (0~20점): 면제
        if (totalScore <= 20) {
            String quizPassedToken = generateAndSaveQuizPassedToken(tokenInfo);
            log.info("[VQA] 퀴즈 면제 처리 - userId={}", userId);
            return VqaStartResponse.exempt(quizPassedToken);
        }

        // 2. 레벨 1 이상: VQA 서버 세션 시작
        VqaSessionStartResponse vqaResponse = vqaClient.startSession(VqaSessionStartRequest.empty());
        Long pythonSessionId = vqaResponse.sessionId();

        // 즉시 첫 번째 문제 조회 (점수 전달하여 VQA 서버가 레벨 결정)
        VqaNextQuestionResponse firstQuestion = vqaClient.getNextQuestion(pythonSessionId, totalScore);

        // VQA 서버에서 점수를 보고 면제 결정한 경우
        if (Boolean.TRUE.equals(firstQuestion.isExempt())) {
            String quizPassedToken = generateAndSaveQuizPassedToken(tokenInfo);
            log.info("[VQA] 퀴즈 면제 처리 (VQA 서버 판단) - userId={}", userId);
            return VqaStartResponse.exempt(quizPassedToken);
        }

        // 우리 플랫폼용 세션 UUID 생성 및 Redis 저장 (점수 포함)
        String vqaSessionId = UUID.randomUUID().toString();
        saveVqaSession(vqaSessionId, pythonSessionId, tokenInfo, 0, totalScore);

        log.info("[VQA] 퀴즈 세션 생성 완료 - userId={}, vqaSessionId={}", userId, vqaSessionId);

        return VqaStartResponse.session(vqaSessionId, firstQuestion);
    }

    /** 다음 문제 정보 조회 (재시도 시 사용) */
    public VqaNextQuestionResponse getNextQuestion(String vqaSessionId) {
        String sessionData = getSessionDataOrThrow(vqaSessionId);
        Long pythonSessionId = parsePythonSessionId(sessionData);
        int score = parseScore(sessionData);

        return vqaClient.getNextQuestion(pythonSessionId, score);
    }

    /** 답변 제출 (3회 시도 제한 로직 포함) */
    public VqaSubmitResult submit(String vqaSessionId, Long videoId, Long questionId, String answer, Double time) {
        String sessionData = getSessionDataOrThrow(vqaSessionId);
        String[] parts = sessionData.split(":");
        
        String phaseType = parts[0];
        long phaseId = Long.parseLong(parts[1]);
        long userId = Long.parseLong(parts[2]);
        long pythonSessionId = Long.parseLong(parts[3]);
        int currentAttempts = Integer.parseInt(parts[4]);
        int score = Integer.parseInt(parts[5]);

        // VQA 서버에 답변 제출
        VqaSubmitResponse response = vqaClient.submitAnswer(new VqaSubmitRequest(
            pythonSessionId, videoId, questionId, answer, time
        ));

        // 1. 통과 시
        if (Boolean.TRUE.equals(response.isCorrect())) {
            QueueTokenResolver.TokenInfo tokenInfo = new QueueTokenResolver.TokenInfo(phaseType, phaseId, userId);
            String quizPassedToken = generateAndSaveQuizPassedToken(tokenInfo);
            
            redisTemplate.delete(VQA_SESSION_KEY_PREFIX + vqaSessionId);
            log.info("[VQA] 퀴즈 통과 - userId={}, attempts={}", userId, currentAttempts + 1);
            return VqaSubmitResult.success(response.similarityScore(), quizPassedToken);
        }

        // 2. 실패 시 - 시도 횟수 증가
        int nextAttempts = currentAttempts + 1;
        int remainAttempts = MAX_ATTEMPTS - nextAttempts;

        if (nextAttempts >= MAX_ATTEMPTS) {
            // 3회 실패 - 세션 파기
            redisTemplate.delete(VQA_SESSION_KEY_PREFIX + vqaSessionId);
            log.warn("[VQA] 퀴즈 최종 실패 (3회 초과) - userId={}", userId);
            return VqaSubmitResult.fail(response.similarityScore(), 0);
        }

        // 시도 횟수 업데이트하여 Redis 저장
        saveVqaSession(vqaSessionId, pythonSessionId, new QueueTokenResolver.TokenInfo(phaseType, phaseId, userId), nextAttempts, score);
        log.info("[VQA] 퀴즈 오답 - userId={}, remain={}", userId, remainAttempts);
        
        return VqaSubmitResult.fail(response.similarityScore(), remainAttempts);
    }

    private String getSessionDataOrThrow(String vqaSessionId) {
        String data = redisTemplate.opsForValue().get(VQA_SESSION_KEY_PREFIX + vqaSessionId);
        if (data == null) {
            throw new RuntimeException("유효하지 않거나 만료된 VQA 세션입니다.");
        }
        return data;
    }

    private Long parsePythonSessionId(String sessionData) {
        return Long.parseLong(sessionData.split(":")[3]);
    }

    private int parseScore(String sessionData) {
        return Integer.parseInt(sessionData.split(":")[5]);
    }

    private void saveVqaSession(String vqaSessionId, Long pythonSessionId, QueueTokenResolver.TokenInfo info, int attempts, int score) {
        String key = VQA_SESSION_KEY_PREFIX + vqaSessionId;
        String value = String.format("%s:%d:%d:%d:%d:%d", 
            info.phaseType(), info.phaseId(), info.userId(), pythonSessionId, attempts, score);
        redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(VQA_SESSION_TTL_SECONDS));
    }

    private String generateAndSaveQuizPassedToken(QueueTokenResolver.TokenInfo tokenInfo) {
        String token = UUID.randomUUID().toString();
        activeRepository.saveQuizPassedToken(token, tokenInfo.phaseType(), tokenInfo.phaseId(), tokenInfo.userId(), 900);
        activeRepository.saveQuizPassedTokenToUserHash(tokenInfo.phaseType(), tokenInfo.phaseId(), tokenInfo.userId(), token);
        return token;
    }
}
