import { S3Client, PutObjectCommand } from '@aws-sdk/client-s3';
import { gzip } from 'zlib';
import { promisify } from 'util';
import { env } from '../config/env';
import type { AnalysisResult, PageSignalPayload, RawData } from '../types';

const gzipAsync = promisify(gzip);

const enabled = Boolean(env.S3_BUCKET && env.S3_ACCESS_KEY && env.S3_SECRET_KEY);

const s3 = enabled
  ? new S3Client({
      region: env.S3_REGION,
      credentials: {
        accessKeyId: env.S3_ACCESS_KEY!,
        secretAccessKey: env.S3_SECRET_KEY!,
      },
      ...(env.S3_ENDPOINT && env.S3_ENDPOINT.trim().length > 0
        ? { endpoint: env.S3_ENDPOINT, forcePathStyle: true }
        : {}),
    })
  : null;

if (enabled) {
  console.log(
    `[S3] 업로더 초기화 완료 - region=${env.S3_REGION} bucket=${env.S3_BUCKET}` +
      (env.S3_ENDPOINT ? ` endpoint=${env.S3_ENDPOINT}` : ' endpoint=(AWS default)'),
  );
} else {
  console.warn(
    '[S3] 환경변수 미설정 (S3_BUCKET/S3_ACCESS_KEY/S3_SECRET_KEY) — raw 시그널 업로드 비활성화',
  );
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
 * - 절대 throw하지 않음 — 라우트/DB 흐름에 영향 없음
 */
export function uploadRawSignal(params: UploadParams): void {
  if (!enabled || !s3) {
    console.log(`[S3] skip (비활성) - dbId=${params.dbId} page=${params.payload.page}`);
    return;
  }

  try {
    putObject(params).catch((err) => {
      console.error(
        `[S3] 업로드 실패 - dbId=${params.dbId} page=${params.payload.page} ` +
          `error=${err?.name ?? 'Error'} message=${err?.message ?? err}`,
        err?.stack,
      );
    });
  } catch (err: any) {
    console.error(
      `[S3] 업로드 진입 실패 - dbId=${params.dbId} page=${params.payload.page} ` +
        `error=${err?.name ?? 'Error'} message=${err?.message ?? err}`,
      err?.stack,
    );
  }
}

async function putObject({ dbId, payload, analysis }: UploadParams): Promise<void> {
  const startedAt = Date.now();
  const now = new Date();
  const key = buildKey(dbId, now);

  console.log(
    `[S3] 업로드 시작 - dbId=${dbId} page=${payload.page} key=${key} ` +
      `clicks=${payload.rawData.clicks?.length ?? 0} movements=${payload.rawData.mouseMovements?.length ?? 0}`,
  );

  const body = {
    dbId,
    page: payload.page,
    savedAt: now.toISOString(),
    pageScore: analysis.score,
    detectedSignals: analysis.detectedSignals,
    rawData: sanitizeRawData(payload.rawData),
  };

  const json = JSON.stringify(body);
  const compressed = await gzipAsync(Buffer.from(json, 'utf-8'));
  console.log(
    `[S3] gzip 완료 - dbId=${dbId} key=${key} ` +
      `rawBytes=${json.length} gzippedBytes=${compressed.length} ` +
      `ratio=${(compressed.length / Math.max(json.length, 1)).toFixed(3)}`,
  );

  await s3!.send(
    new PutObjectCommand({
      Bucket: env.S3_BUCKET!,
      Key: key,
      Body: compressed,
      ContentType: 'application/json',
      ContentEncoding: 'gzip',
    }),
  );

  console.log(
    `[S3] 업로드 완료 - dbId=${dbId} page=${payload.page} key=${key} ` +
      `bytes=${compressed.length} elapsedMs=${Date.now() - startedAt}`,
  );
}
