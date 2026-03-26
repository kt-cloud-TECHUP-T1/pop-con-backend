import Redis from 'ioredis';
import { env } from './env';

export const redis = new Redis({
  host: env.REDIS_HOST,
  port: env.REDIS_PORT,
  password: env.REDIS_PASSWORD,
  maxRetriesPerRequest: 3,
  enableOfflineQueue: false,
  connectTimeout: 5000,
  retryStrategy(times) {
    if (times > 5) return null; // 5회 초과 시 재시도 중단
    return Math.min(times * 200, 2000);
  },
});

redis.on('connect', () => console.log('[redis] 연결 성공'));
redis.on('error', (err) => console.error('[redis] 연결 에러:', err.message));

/** Redis 연결 완료 대기 */
export function waitForRedis(): Promise<void> {
  if (redis.status === 'ready') return Promise.resolve();
  return new Promise((resolve, reject) => {
    redis.once('ready', resolve);
    redis.once('error', reject);
  });
}
