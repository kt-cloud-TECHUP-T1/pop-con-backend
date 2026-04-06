import { pool } from '../config/database';
import type { AnalysisResult } from '../types';

type LogParams = {
  visitorId?: string;
  userId?: string;
  identityKey: string;
  page: string;
  pageScore: number;
  totalScore: number;
  result: AnalysisResult;
  ipAddress?: string;
  userAgent?: string;
  rawSummary?: Record<string, unknown>;
};

/**
 * 의심 로그 DB 저장 (fire-and-forget)
 * score > 0이면 저장 (시그널이 하나라도 감지된 경우)
 */
export function saveIfSuspicious(params: LogParams): void {
  if (params.pageScore <= 0) return; // 정상이면 저장 안 함

  // fire-and-forget: await 안 함
  insertLog(params).catch((err) => {
    console.error('[db] 의심 로그 저장 실패:', err.message);
  });
}

async function insertLog(params: LogParams): Promise<void> {
  const sql = `
    INSERT INTO macro_detection_log
      (visitor_id, user_id, identity_key, page, page_score, total_score,
       vqa_difficulty, draw_result, detected_signals, raw_summary,
       ip_address, user_agent)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
  `;

  await pool.execute(sql, [
    params.visitorId || null,
    params.userId || null,
    params.identityKey,
    params.page,
    params.pageScore,
    params.totalScore,
    params.result.vqaLevel,
    params.result.drawResult || null,
    JSON.stringify(params.result.detectedSignals),
    params.rawSummary ? JSON.stringify(params.rawSummary) : null,
    params.ipAddress || null,
    params.userAgent || null,
  ]);

  console.log(`[db] 의심 로그 저장: page=${params.page} score=${params.pageScore} total=${params.totalScore}`);
}
