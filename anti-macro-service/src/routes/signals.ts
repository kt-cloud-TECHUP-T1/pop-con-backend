import { Router, Request, Response } from 'express';
import type { AntiMacroSubmission } from '../types';
import { analyzeRawData } from '../analysis/analyze';
import { resolveIdentityKey, updatePageScore, getTotalScore, mergeScores } from '../services/score-aggregator';
import { saveIfSuspicious } from '../services/suspicious-logger';
import { uploadRawSignal } from '../services/raw-signal-uploader';

const router = Router();

/** ID 마스킹 (앞 4자리만 노출) */
function maskId(id?: string): string {
  if (!id) return 'N/A';
  if (id.length <= 4) return '****';
  return id.slice(0, 4) + '****';
}

/** Cloudflare 뒤에서 실제 클라이언트 IP 추출 */
function getClientIp(req: Request): string | undefined {
  const cfIp = req.headers['cf-connecting-ip'];
  if (typeof cfIp === 'string' && cfIp.length > 0) return cfIp;

  const xff = req.headers['x-forwarded-for'];
  if (typeof xff === 'string' && xff.length > 0) {
    return xff.split(',')[0].trim();
  }

  return req.ip || req.socket?.remoteAddress;
}

// POST /signals - raw 데이터 수신 + 분석
router.post('/signals', async (req: Request, res: Response) => {
  try {
    const body: AntiMacroSubmission = req.body;

    // 요청 검증
    if (!body?.payload?.page || !body?.payload?.rawData) {
      return res.json({ received: true, vqaLevel: 1 });
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
    console.log(
      `[anti-macro] user=${maskId(body.userId)} visitor=${maskId(body.visitorId)} page=${payload.page} ` +
      `pageScore=${result.score} totalScore=${totalScore} vqaLevel=${result.vqaLevel}`,
      {
        signals: result.detectedSignals.map((s) => `${s.name}(${s.weight})`),
      }
    );

    // 4. 시그널 로그 DB 저장 → S3 raw 데이터 업로드 (fire-and-forget)
    if (identityKey) {
      saveIfSuspicious({
        visitorId: body.visitorId,
        userId: body.userId,
        identityKey,
        page: payload.page,
        pageScore: result.score,
        totalScore,
        result,
        ipAddress: getClientIp(req),
        userAgent: req.headers['user-agent'],
        rawSummary: {
          clicks: payload.rawData.clicks?.length ?? 0,
          movements: payload.rawData.mouseMovements?.length ?? 0,
          hasUntrustedEvent: payload.rawData.hasUntrustedEvent ?? false,
          loadToFirstClickMs: payload.rawData.loadToFirstClickMs ?? null,
        },
      }).then((dbId) => {
        if (dbId) uploadRawSignal({ dbId, payload, analysis: result });
      });
    }

    // 5. 응답
    res.json({
      received: true,
      vqaLevel: result.vqaLevel,
      drawResult: result.drawResult,
    });
  } catch (err) {
    console.error('[anti-macro] 시그널 처리 에러:', err);
    res.json({ received: true, vqaLevel: 1 });
  }
});

export default router;
