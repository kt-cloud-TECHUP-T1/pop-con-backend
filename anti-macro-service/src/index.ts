import express from 'express';
import cors from 'cors';
import signalsRouter from './routes/signals';
import { env } from './config/env';
import { redis } from './config/redis';
import { testConnection, pool } from './config/database';
import type { Server } from 'http';

const app = express();

app.use(cors());
app.use(express.json({ limit: '1mb' }));

// 라우트
app.use('/anti-macro', signalsRouter);

// 헬스체크
app.get('/health', (_req, res) => {
  res.json({ status: 'ok', service: 'anti-macro' });
});

let server: Server;

// 서버 시작
async function start() {
  try {
    // Redis 연결 확인
    await redis.ping();
    console.log('[redis] ping 성공');

    // MySQL 연결 확인
    await testConnection();

    server = app.listen(env.PORT, () => {
      console.log(`[anti-macro] 서버 시작: http://localhost:${env.PORT}`);
    });
  } catch (err) {
    console.error('[anti-macro] 시작 실패:', err);
    process.exit(1);
  }
}

// graceful shutdown
async function shutdown() {
  console.log('[anti-macro] 종료 중...');

  // 새 요청 수신 중단
  if (server) {
    server.close();
  }

  try {
    await redis.quit();
    await pool.end();
    console.log('[anti-macro] 정상 종료 완료');
  } catch (err) {
    console.error('[anti-macro] 종료 중 에러:', err);
  }

  process.exit(0);
}

process.on('SIGTERM', shutdown);
process.on('SIGINT', shutdown);

start();
