package com.t1.popcon.queue.service;

import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.queue.client.VqaClient;
import com.t1.popcon.queue.common.config.QueueProperties;
import com.t1.popcon.queue.common.redis.*;
import com.t1.popcon.queue.dto.response.VqaStartResponse;
import com.t1.popcon.queue.dto.response.VqaSubmitResult;
import com.t1.popcon.queue.dto.vqa.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class VqaService {

    private final VqaClient vqaClient;
    private final AntiMacroScoreRepository antiMacroScoreRepository;
    private final QueueActiveRepository activeRepository;
    private final QueueBlockRepository blockRepository;
    private final QueueCleanupRepository cleanupRepository;
    private final QueueProperties queueProperties;
    private final QueueTokenResolver tokenResolver;
    private final StringRedisTemplate redisTemplate;
    private final RedissonClient redissonClient;

    private static final String VQA_SESSION_KEY_PREFIX = "vqa:session:";
    private static final String VQA_USER_SESSION_KEY_PREFIX = "vqa:session:user:";
    private static final String VQA_GLOBAL_ATTEMPTS_PREFIX = "vqa:attempts:global:";
    private static final String VQA_LOCK_PREFIX = "lock:vqa:submit:";
    private static final long VQA_SESSION_TTL_SECONDS = 600; // 10분
    private static final long VQA_GLOBAL_LOCKOUT_TTL_SECONDS = 1800; // 30분
    private static final int MAX_ATTEMPTS = 3;

    /**
     * 보안 퀴즈 시작 (원샷 방식)
     * - 중복 호출 방지 및 세션 재사용 로직 포함
     */
    public VqaStartResponse start(String queueToken) {
        QueueTokenResolver.TokenInfo tokenInfo = tokenResolver.resolve(queueToken);
        long userId = tokenInfo.userId();
        String phaseType = tokenInfo.phaseType();
        long phaseId = tokenInfo.phaseId();

        // 사용자가 현재 대기열 ACTIVE 상태이며 만료되지 않았는지 검증
        long now = Instant.now().toEpochMilli();
        activeRepository.getActiveExpireAt(phaseType, phaseId, userId)
                .filter(expireAt -> expireAt > now)
                .orElseThrow(() -> new CustomException(ErrorCode.QUEUE_NOT_ACTIVE));

        // 1. 이미 통과했는지 확인 (중복 발급 방지)
        Map<Object, Object> userHash = activeRepository.getUserHash(phaseType, phaseId, userId);
        if (userHash.containsKey(QueueRedisKeys.FIELD_QUIZ_PASSED_TOKEN)) {
            log.warn("[VQA] 이미 퀴즈를 통과한 사용자 - userId={}", userId);
            throw new CustomException(ErrorCode.QUIZ_ALREADY_PASSED);
        }

        // 2. 글로벌 시도 횟수 체크 (최종 실패 여부)
        String globalKey = getGlobalAttemptsKey(userId, phaseType, phaseId);
        String attemptsStr = redisTemplate.opsForValue().get(globalKey);
        int globalAttempts = (attemptsStr != null) ? Integer.parseInt(attemptsStr) : 0;

        if (globalAttempts >= MAX_ATTEMPTS) {
            log.warn("[VQA] 글로벌 시도 횟수 초과 차단 - userId={}, attempts={}", userId, globalAttempts);
            throw new CustomException(ErrorCode.QUIZ_ATTEMPTS_EXCEEDED);
        }

        // 3. 기존 진행 중인 세션이 있는지 확인 (세션 재사용)
        String userSessionKey = getUserSessionKey(userId, phaseType, phaseId);
        String existingVqaSessionId = redisTemplate.opsForValue().get(userSessionKey);

        if (existingVqaSessionId != null) {
            String sessionData = redisTemplate.opsForValue().get(VQA_SESSION_KEY_PREFIX + existingVqaSessionId);
            if (sessionData != null) {
                log.info("[VQA] 기존 세션 재사용 - userId={}, vqaSessionId={}", userId, existingVqaSessionId);
                VqaNextQuestionResponse nextQuestion = getNextQuestion(existingVqaSessionId);
                return VqaStartResponse.session(existingVqaSessionId, nextQuestion);
            }
        }

        int totalScore = antiMacroScoreRepository.getTotalScore(userId);
        log.info("[VQA] 퀴즈 시작 시도 - userId={}, score={}, globalAttempts={}", userId, totalScore, globalAttempts);

        // 4. 레벨 0 (0~20점): 면제
        if (totalScore <= 20) {
            String quizPassedToken = generateAndSaveQuizPassedToken(tokenInfo);
            log.info("[VQA] 퀴즈 면제 처리 - userId={}", userId);
            return VqaStartResponse.exempt(quizPassedToken);
        }

        // 5. 레벨 1 이상: VQA 서버 세션 시작
        VqaSessionStartResponse vqaResponse = vqaClient.startSession(VqaSessionStartRequest.empty());
        Long pythonSessionId = vqaResponse.sessionId();

        VqaNextQuestionResponse firstQuestion = vqaClient.getNextQuestion(pythonSessionId, totalScore);

        if (Boolean.TRUE.equals(firstQuestion.isExempt())) {
            String quizPassedToken = generateAndSaveQuizPassedToken(tokenInfo);
            log.info("[VQA] 퀴즈 면제 처리 (VQA 서버 판단) - userId={}", userId);
            return VqaStartResponse.exempt(quizPassedToken);
        }

        String vqaSessionId = UUID.randomUUID().toString();
        saveVqaSession(vqaSessionId, pythonSessionId, tokenInfo, globalAttempts, totalScore);
        
        redisTemplate.opsForValue().set(userSessionKey, vqaSessionId, Duration.ofSeconds(VQA_SESSION_TTL_SECONDS));

        log.info("[VQA] 퀴즈 세션 생성 완료 - userId={}, vqaSessionId={}", userId, vqaSessionId);
        return VqaStartResponse.session(vqaSessionId, firstQuestion);
    }

    /** 다음 문제 정보 조회 */
    public VqaNextQuestionResponse getNextQuestion(String vqaSessionId) {
        String sessionData = getSessionDataOrThrow(vqaSessionId);
        Long pythonSessionId = parsePythonSessionId(sessionData);
        int score = parseScore(sessionData);

        return vqaClient.getNextQuestion(pythonSessionId, score);
    }

    /** 답변 제출 (분산 락을 통한 원자성 보장) */
    public VqaSubmitResult submit(String vqaSessionId, Long videoId, Long questionId, String answer, Double time) {
        RLock lock = redissonClient.getLock(VQA_LOCK_PREFIX + vqaSessionId);
        
        try {
            // 5초 대기, 10초 점유
            if (!lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                throw new CustomException(ErrorCode.ERROR_SYSTEM, "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.");
            }

            // 1. 락 획득 후 최신 세션 데이터 조회
            String sessionData = getSessionDataOrThrow(vqaSessionId);
            String[] parts = sessionData.split(":");
            
            String phaseType = parts[0];
            long phaseId = Long.parseLong(parts[1]);
            long userId = Long.parseLong(parts[2]);
            long pythonSessionId = Long.parseLong(parts[3]);
            int score = Integer.parseInt(parts[5]);

            // 2. VQA 서버에 답변 제출
            VqaSubmitResponse response = vqaClient.submitAnswer(new VqaSubmitRequest(
                pythonSessionId, videoId, questionId, answer, time
            ));

            // 3. 통과 시
            if (Boolean.TRUE.equals(response.isCorrect())) {
                QueueTokenResolver.TokenInfo tokenInfo = new QueueTokenResolver.TokenInfo(phaseType, phaseId, userId);
                String quizPassedToken = generateAndSaveQuizPassedToken(tokenInfo);
                
                clearVqaSession(vqaSessionId, userId, phaseType, phaseId);
                
                log.info("[VQA] 퀴즈 통과 - userId={}", userId);
                return VqaSubmitResult.success(response.similarityScore(), quizPassedToken);
            }

            // 4. 실패 시 - 글로벌 시도 횟수 원자적 증가
            String globalKey = getGlobalAttemptsKey(userId, phaseType, phaseId);
            Long nextAttemptsLong = redisTemplate.opsForValue().increment(globalKey);
            int nextAttempts = (nextAttemptsLong != null) ? nextAttemptsLong.intValue() : 0;
            
            if (nextAttempts == 1) {
                redisTemplate.expire(globalKey, Duration.ofSeconds(VQA_GLOBAL_LOCKOUT_TTL_SECONDS));
            }

            if (nextAttempts >= MAX_ATTEMPTS) {
                clearVqaSession(vqaSessionId, userId, phaseType, phaseId);
                blockRepository.block(phaseType, phaseId, userId, queueProperties.getBlockTtlSeconds());
                cleanupRepository.cleanupUserData(phaseType, phaseId, userId, null);

                log.warn("[VQA] 퀴즈 최종 실패 - userId={}", userId);
                return VqaSubmitResult.fail(response.similarityScore(), 0);
            }

            saveVqaSession(vqaSessionId, pythonSessionId, new QueueTokenResolver.TokenInfo(phaseType, phaseId, userId), nextAttempts, score);
            return VqaSubmitResult.fail(response.similarityScore(), MAX_ATTEMPTS - nextAttempts);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CustomException(ErrorCode.ERROR_SYSTEM, "인증 처리 중 오류가 발생했습니다.");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private String getGlobalAttemptsKey(long userId, String phaseType, long phaseId) {
        return VQA_GLOBAL_ATTEMPTS_PREFIX + String.format("%d:%s:%d", userId, phaseType, phaseId);
    }

    private String getUserSessionKey(long userId, String phaseType, long phaseId) {
        return VQA_USER_SESSION_KEY_PREFIX + String.format("%d:%s:%d", userId, phaseType, phaseId);
    }

    private void clearVqaSession(String vqaSessionId, long userId, String phaseType, long phaseId) {
        redisTemplate.delete(VQA_SESSION_KEY_PREFIX + vqaSessionId);
        redisTemplate.delete(getUserSessionKey(userId, phaseType, phaseId));
    }

    private String getSessionDataOrThrow(String vqaSessionId) {
        String data = redisTemplate.opsForValue().get(VQA_SESSION_KEY_PREFIX + vqaSessionId);
        if (data == null) {
            throw new CustomException(ErrorCode.VQA_SESSION_EXPIRED);
        }
        return data;
    }

    private Long parsePythonSessionId(String sessionData) {
        try {
            return Long.parseLong(sessionData.split(":")[3]);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.VQA_SESSION_EXPIRED, "세션 데이터 파싱 오류");
        }
    }

    private int parseScore(String sessionData) {
        try {
            return Integer.parseInt(sessionData.split(":")[5]);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.VQA_SESSION_EXPIRED, "점수 파싱 오류");
        }
    }

    private void saveVqaSession(String vqaSessionId, Long pythonSessionId, QueueTokenResolver.TokenInfo info, int attempts, int score) {
        String key = VQA_SESSION_KEY_PREFIX + vqaSessionId;
        String value = String.format("%s:%d:%d:%d:%d:%d", 
            info.phaseType(), info.phaseId(), info.userId(), pythonSessionId, attempts, score);
        redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(VQA_SESSION_TTL_SECONDS));
    }

    private String generateAndSaveQuizPassedToken(QueueTokenResolver.TokenInfo tokenInfo) {
        String token = UUID.randomUUID().toString();
        long now = Instant.now().toEpochMilli();
        long expireAt = activeRepository.getActiveExpireAt(tokenInfo.phaseType(), tokenInfo.phaseId(), tokenInfo.userId())
                .filter(e -> e > now)
                .orElseThrow(() -> new CustomException(ErrorCode.QUEUE_NOT_ACTIVE));

        long remainSeconds = (expireAt - now) / 1000;
        activeRepository.saveQuizPassedToken(token, tokenInfo.phaseType(), tokenInfo.phaseId(), tokenInfo.userId(), remainSeconds);
        activeRepository.saveQuizPassedTokenToUserHash(tokenInfo.phaseType(), tokenInfo.phaseId(), tokenInfo.userId(), token);
        return token;
    }
}
