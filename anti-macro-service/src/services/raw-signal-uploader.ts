import { S3Client, PutObjectCommand } from '@aws-sdk/client-s3';
import { gzip } from 'zlib';
import { promisify } from 'util';
import { env } from '../config/env';
import type { AnalysisResult, PageSignalPayload, RawData } from '../types';

const gzipAsync = promisify(gzip);

const enabled = Boolean(env.AWS_REGION && env.S3_RAW_SIGNAL_BUCKET);

const s3 = enabled
  ? new S3Client({ region: env.AWS_REGION })
  : null;

if (!enabled) {
  console.warn('[s3-raw-signal] AWS_REGION 또는 S3_RAW_SIGNAL_BUCKET 미설정 — 업로드 비활성화');
}

type UploadParams = {
  dbId: number;
  payload: PageSignalPayload;
  analysis: AnalysisResult;
};

/** rawData에서 PII(visitorId, userAgent)를 제거 */
function sanitizeRawData(raw: RawData): Record<string, unknown> {
  const { visitorId, userAgent, ...rest } = raw;
  return rest;
}

function buildKey(dbId: number, now: Date): string {
  const yyyy = now.getUTCFullYear();
  const mm = String(now.getUTCMonth() + 1).padStart(2, '0');
  const dd = String(now.getUTCDate()).padStart(2, '0');
  const hh = String(now.getUTCHours()).padStart(2, '0');
  return `signals/${yyyy}/${mm}/${dd}/${hh}/${dbId}.json.gz`;
}

/**
 * Raw 시그널을 S3에 업로드 (fire-and-forget)
 * - PII 제거: userId/ip/userAgent/visitorId 전부 제외
 * - 주체 추적이 필요해지면 visitorId 해시 추가 (현재는 요청 단위 학습만 지원)
 */
export function uploadRawSignal(params: UploadParams): void {
  if (!enabled || !s3) return;

  putObject(params).catch((err) => {
    console.error('[s3-raw-signal] 업로드 실패:', err?.message || err);
  });
}

async function putObject({ dbId, payload, analysis }: UploadParams): Promise<void> {
  const now = new Date();
  const body = {
    dbId,
    page: payload.page,
    savedAt: now.toISOString(),
    pageScore: analysis.score,
    detectedSignals: analysis.detectedSignals,
    rawData: sanitizeRawData(payload.rawData),
  };

  const compressed = await gzipAsync(Buffer.from(JSON.stringify(body), 'utf-8'));

  await s3!.send(
    new PutObjectCommand({
      Bucket: env.S3_RAW_SIGNAL_BUCKET!,
      Key: buildKey(dbId, now),
      Body: compressed,
      ContentType: 'application/json',
      ContentEncoding: 'gzip',
    }),
  );
}
