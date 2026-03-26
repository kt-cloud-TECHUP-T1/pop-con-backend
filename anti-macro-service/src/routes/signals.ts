import { Router, Request, Response } from 'express';
import type { AntiMacroSubmission } from '../types';
import { analyzeRawData } from '../analysis/analyze';
import { resolveIdentityKey, updatePageScore, getTotalScore, mergeScores } from '../services/score-aggregator';
import { saveIfSuspicious } from '../services/suspicious-logger';

const router = Router();

// POST /api/signals - raw 데이터 수신 + 분석
router.post('/signals', async (req: Request, res: Response) => {
  const body: AntiMacroSubmission = req.body;
  const { payload } = body;

  // 1. 시그널 분석
  const result = analyzeRawData(payload);

  // 3. identity 결정 + 점수 집계
  const identityKey = resolveIdentityKey(body.visitorId, body.userId);
  await updatePageScore(identityKey, payload.page, result.score);

  // 상세 페이지에서 visitorId + userId 둘 다 오면 점수 병합
  if (body.visitorId && body.userId) {
    await mergeScores(body.visitorId, body.userId);
  }

  const totalScore = await getTotalScore(identityKey);

  // 4. 로그 출력
  console.log(`[anti-macro] page=${payload.page} score=${result.score} total=${totalScore} vqa=${result.vqaDifficulty}`, {
    signals: result.detectedSignals.map((s) => `${s.name}(${s.weight})`),
    visitorId: body.visitorId,
    userId: body.userId,
    identityKey,
  });

  // 5. 의심 로그 DB 저장 (score > 0이면 저장, fire-and-forget)
  saveIfSuspicious({
    visitorId: body.visitorId,
    userId: body.userId,
    identityKey,
    page: payload.page,
    pageScore: result.score,
    totalScore,
    result,
    ipAddress: req.ip || req.socket.remoteAddress,
    userAgent: req.headers['user-agent'],
    rawSummary: {
      clicks: payload.rawData.clicks?.length ?? 0,
      movements: payload.rawData.movements?.length ?? 0,
      hasUntrustedEvent: payload.rawData.hasUntrustedEvent ?? false,
      loadToFirstClickMs: payload.rawData.loadToFirstClickMs ?? null,
    },
  });

  // 6. 응답
  res.json({
    received: true,
    vqaDifficulty: result.vqaDifficulty,
    drawResult: result.drawResult,
  });
});

export default router;
