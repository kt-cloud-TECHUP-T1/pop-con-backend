import { redis } from '../config/redis';

const NONCE_TTL = 300; // 5분
const KEY_PREFIX = 'nonce:';

type NonceData = {
  nonce: string;
  challenge: string;
  expiresAt: number;
};

/** nonce + challenge 발급 → Redis 저장 */
export async function createNonce(): Promise<NonceData> {
  const nonce = crypto.randomUUID();
  const challenge = crypto.randomUUID();
  const expiresAt = Date.now() + NONCE_TTL * 1000;

  await redis.set(
    `${KEY_PREFIX}${nonce}`,
    JSON.stringify({ challenge, createdAt: Date.now() }),
    'EX',
    NONCE_TTL,
  );

  return { nonce, challenge, expiresAt };
}

/** nonce 검증 + 1회용 삭제. 유효하면 true, 아니면 false */
export async function validateAndConsumeNonce(nonce: string): Promise<boolean> {
  const key = `${KEY_PREFIX}${nonce}`;
  const data = await redis.get(key);

  if (!data) return false; // 만료됐거나 이미 사용됨

  await redis.del(key); // 1회용 삭제
  return true;
}
