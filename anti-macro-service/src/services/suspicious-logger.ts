import type { ResultSetHeader } from 'mysql2';
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
 * 시그널 로그 DB 저장 (fire-and-forget)
 * 모든 요청 저장. insert된 row id를 Promise로 resolve (S3 업로드 체이닝용).
 * 실패 시 resolve(null) — 호출자는 null이면 후속 처리 skip.
 */
export function saveSignalLog(params: LogParams): Promise<number | null> {
  return insertLog(params).catch((err) => {
    console.error('[db] 시그널 로그 저장 실패:', err.message);
    return null;
  });
}

async function insertLog(params: LogParams): Promise<number | null> {
  const sql = `
    INSERT INTO macro_detection_log
      (visitor_id, user_id, identity_key, page, page_score, total_score,
       vqa_difficulty, draw_result, detected_signals, raw_summary,
       ip_address, user_agent)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
  `;

  const [result] = await pool.execute<ResultSetHeader>(sql, [
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

  console.log(`[db] 시그널 로그 저장: id=${result.insertId} page=${params.page} score=${params.pageScore} total=${params.totalScore}`);
  return result.insertId;
}
