import express from 'express';
import cors from 'cors';
import signalsRouter from './routes/signals';
import { env } from './config/env';
import { redis } from './config/redis';
import { testConnection } from './config/database';

const app = express();

app.use(cors());
app.use(express.json({ limit: '1mb' }));

// 라우트
app.use('/api', signalsRouter);

// 헬스체크
app.get('/health', (_req, res) => {
  res.json({ status: 'ok', service: 'anti-macro' });
});

// 서버 시작
async function start() {
  try {
    // Redis 연결 확인
    await redis.ping();
    console.log('[redis] ping 성공');

    // MySQL 연결 확인
    await testConnection();

    app.listen(env.PORT, () => {
      console.log(`[anti-macro] 서버 시작: http://localhost:${env.PORT}`);
    });
  } catch (err) {
    console.error('[anti-macro] 시작 실패:', err);
    process.exit(1);
  }
}

// graceful shutdown
process.on('SIGTERM', async () => {
  console.log('[anti-macro] 종료 중...');
  redis.quit();
  process.exit(0);
});

start();
