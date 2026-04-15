import { redis } from '../config/redis';

const SCORE_TTL = 1800; // 30분
const KEY_PREFIX = 'score:';

/** 신청 페이지: 첫 제출 점수로 고정 (HSETNX) */
const APPLICATION_PAGES = new Set(['draw-application', 'dutch-auction-application']);

/**
 * identityKey 결정: visitorId를 기본으로, userId가 있으면 userId 우선
 * 단, 상세 페이지에서 둘 다 오면 visitorId 기반 점수를 userId로 병합
 */
export function resolveIdentityKey(visitorId?: string, userId?: string): string | undefined {
  return userId || visitorId || undefined;
}

/**
 * 페이지별 점수 업데이트 (Redis Hash)
 * - 신청 페이지: HSETNX로 첫 값 고정 (재제출 시 무시)
 * - 그 외: max-merge로 더 높은 값만 유지 (재방문으로 씻기 방지)
 */
export async function updatePageScore(
  identityKey: string,
  page: string,
  score: number,
): Promise<void> {
  const key = `${KEY_PREFIX}${identityKey}`;

  console.log(`[updatePageScore] key=${key} page=${page} incoming=${score}`);

  if (APPLICATION_PAGES.has(page)) {
    const res = await redis.hsetnx(key, page, score.toString());
    console.log(`[updatePageScore] HSETNX result=${res} (1=written, 0=already exists)`);
  } else {
    const existingStr = await redis.hget(key, page);
    const existing = existingStr !== null ? Number(existingStr) : -Infinity;
    const willWrite = score > existing;
    console.log(
      `[updatePageScore] existing=${existingStr === null ? 'null' : existing} willWrite=${willWrite}`,
    );
    if (willWrite) {
      await redis.hset(key, page, score.toString());
      console.log(`[updatePageScore] HSET ${key} ${page} ${score}`);
    }
  }

  await redis.expire(key, SCORE_TTL);
}

/** 3페이지 합산 총점 조회 */
export async function getTotalScore(identityKey: string): Promise<number> {
  const key = `${KEY_PREFIX}${identityKey}`;
  const scores = await redis.hgetall(key);

  return Object.values(scores).reduce((sum, val) => sum + Number(val), 0);
}

/**
 * visitorId → userId 점수 병합 (상세 페이지에서 둘 다 올 때)
 * - field별 max-merge: visitor와 user 중 더 높은 점수를 user에 유지
 * - 공격자가 먼저 낮은 점수를 심어 이후 고위험 signal을 막는 고착 우회를 차단
 */
export async function mergeScores(visitorId: string, userId: string): Promise<void> {
  const visitorKey = `${KEY_PREFIX}${visitorId}`;
  const userKey = `${KEY_PREFIX}${userId}`;

  console.log(`[merge] start visitorKey=${visitorKey} userKey=${userKey}`);

  const visitorScores = await redis.hgetall(visitorKey);
  console.log(`[merge] visitorScores=`, visitorScores);

  if (Object.keys(visitorScores).length === 0) {
    console.log(`[merge] skip: visitor hash empty`);
    return;
  }

  const userScoresBefore = await redis.hgetall(userKey);
  console.log(`[merge] userScores(before)=`, userScoresBefore);

  for (const [page, scoreStr] of Object.entries(visitorScores)) {
    const incoming = Number(scoreStr);
    const existingStr = await redis.hget(userKey, page);
    const existing = existingStr !== null ? Number(existingStr) : -Infinity;
    const willWrite = incoming > existing;
    console.log(
      `[merge] field=${page} incoming=${incoming} existing=${existingStr === null ? 'null' : existing} willWrite=${willWrite}`,
    );
    if (willWrite) {
      await redis.hset(userKey, page, incoming.toString());
      console.log(`[merge] HSET ${userKey} ${page} ${incoming}`);
    }
  }
  await redis.expire(userKey, SCORE_TTL);

  const userScoresAfter = await redis.hgetall(userKey);
  console.log(`[merge] userScores(after)=`, userScoresAfter);
  console.log(`[merge] done`);
}
