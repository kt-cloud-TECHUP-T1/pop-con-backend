import { Router, Request, Response } from 'express';
import type { AntiMacroSubmission } from '../types';
import { analyzeRawData } from '../analysis/analyze';
import { resolveIdentityKey, updatePageScore, getTotalScore, mergeScores } from '../services/score-aggregator';
import { saveIfSuspicious } from '../services/suspicious-logger';

const router = Router();

/** ID 마스킹 (앞 4자리만 노출) */
function maskId(id?: string): string {
  if (!id) return 'N/A';
  if (id.length <= 4) return '****';
  return id.slice(0, 4) + '****';
}

// POST /signals - raw 데이터 수신 + 분석
router.post('/signals', async (req: Request, res: Response) => {
  try {
    const body: AntiMacroSubmission = req.body;

    // 요청 검증
    if (!body?.payload?.page || !body?.payload?.rawData) {
      return res.json({ received: true, vqaDifficulty: 'easy' });
    }

    const { payload } = body;

    // 1. 시그널 분석
    const result = analyzeRawData(payload);

    // 2. identity 결정 + 점수 집계
    const identityKey = resolveIdentityKey(body.visitorId, body.userId);

    if (identityKey) {
      await updatePageScore(identityKey, payload.page, result.score);

      // 상세 페이지에서 visitorId + userId 둘 다 오면 점수 병합
      if (body.visitorId && body.userId) {
        await mergeScores(body.visitorId, body.userId);
      }
    }

    const totalScore = identityKey ? await getTotalScore(identityKey) : result.score;

    // 3. 로그 출력 (ID 마스킹)
    console.log(`[anti-macro] page=${payload.page} score=${result.score} total=${totalScore} vqa=${result.vqaDifficulty}`, {
      signals: result.detectedSignals.map((s) => `${s.name}(${s.weight})`),
      visitorId: maskId(body.visitorId),
      userId: maskId(body.userId),
    });

    // 4. 의심 로그 DB 저장 (score > 0이면 저장, fire-and-forget)
    if (identityKey) {
      saveIfSuspicious({
        visitorId: body.visitorId,
        userId: body.userId,
        identityKey,
        page: payload.page,
        pageScore: result.score,
        totalScore,
        result,
        ipAddress: req.ip || req.socket?.remoteAddress,
        userAgent: req.headers['user-agent'],
        rawSummary: {
          clicks: payload.rawData.clicks?.length ?? 0,
          movements: payload.rawData.movements?.length ?? 0,
          hasUntrustedEvent: payload.rawData.hasUntrustedEvent ?? false,
          loadToFirstClickMs: payload.rawData.loadToFirstClickMs ?? null,
        },
      });
    }

    // 5. 응답
    res.json({
      received: true,
      vqaDifficulty: result.vqaDifficulty,
      drawResult: result.drawResult,
    });
  } catch (err) {
    console.error('[anti-macro] 시그널 처리 에러:', err);
    res.json({ received: true, vqaDifficulty: 'easy' });
  }
});

export default router;
