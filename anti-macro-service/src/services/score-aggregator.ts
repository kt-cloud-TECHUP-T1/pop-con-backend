import { redis } from '../config/redis';

const SCORE_TTL = 1800; // 30분
const KEY_PREFIX = 'score:';

/**
 * identityKey 결정: visitorId를 기본으로, userId가 있으면 userId 우선
 * 단, 상세 페이지에서 둘 다 오면 visitorId 기반 점수를 userId로 병합
 */
export function resolveIdentityKey(visitorId?: string, userId?: string): string {
  return userId || visitorId || 'unknown';
}

/** 페이지별 점수 업데이트 (Redis Hash) */
export async function updatePageScore(
  identityKey: string,
  page: string,
  score: number,
): Promise<void> {
  const key = `${KEY_PREFIX}${identityKey}`;
  await redis.hset(key, page, score.toString());
  await redis.expire(key, SCORE_TTL);
}

/** 3페이지 합산 총점 조회 */
export async function getTotalScore(identityKey: string): Promise<number> {
  const key = `${KEY_PREFIX}${identityKey}`;
  const scores = await redis.hgetall(key);

  return Object.values(scores).reduce((sum, val) => sum + Number(val), 0);
}

/** visitorId → userId 점수 병합 (상세 페이지에서 둘 다 올 때) */
export async function mergeScores(visitorId: string, userId: string): Promise<void> {
  const visitorKey = `${KEY_PREFIX}${visitorId}`;
  const userKey = `${KEY_PREFIX}${userId}`;

  const visitorScores = await redis.hgetall(visitorKey);
  if (Object.keys(visitorScores).length === 0) return;

  // visitorId의 점수를 userId로 복사 (기존 userId 점수가 있으면 덮어쓰지 않음)
  for (const [page, score] of Object.entries(visitorScores)) {
    await redis.hsetnx(userKey, page, score);
  }
  await redis.expire(userKey, SCORE_TTL);
}
