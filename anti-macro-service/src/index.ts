import dotenv from 'dotenv';
import path from 'path';

// 부모 디렉토리의 .env 로드 (로컬 개발용)
dotenv.config({ path: path.resolve(__dirname, '../../.env') });

import express from 'express';
import cors from 'cors';
import zlib from 'zlib';
import signalsRouter from './routes/signals';
import { env } from './config/env';
import { redis, waitForRedis } from './config/redis';
import { testConnection, pool } from './config/database';
import type { Server } from 'http';

const app = express();

app.use(cors());

// gzip 요청 본문 해제 후 JSON 파싱
const MAX_BODY_BYTES = 1024 * 1024; // 1MB
app.use((req, res, next) => {
  if (req.headers['content-encoding'] !== 'gzip') return next();
  if (req.method === 'GET' || req.method === 'HEAD') return next();

  const chunks: Buffer[] = [];
  let received = 0;
  req.on('data', (chunk: Buffer) => {
    received += chunk.length;
    if (received > MAX_BODY_BYTES * 2) {
      req.destroy();
      if (!res.headersSent) {
        res.status(413).json({ error: 'payload too large' });
      }
      return;
    }
    chunks.push(chunk);
  });
  req.on('end', () => {
    if (res.headersSent) return;
    zlib.gunzip(Buffer.concat(chunks), (err, decoded) => {
      if (res.headersSent) return;
      if (err) return res.status(400).json({ error: 'invalid gzip body' });
      if (decoded.length > MAX_BODY_BYTES) {
        return res.status(413).json({ error: 'payload too large' });
      }
      try {
        const contentType = req.headers['content-type'] || '';
        if (contentType.includes('application/json')) {
          req.body = JSON.parse(decoded.toString('utf8'));
        } else {
          req.body = decoded;
        }
        delete req.headers['content-encoding'];
        next();
      } catch {
        res.status(400).json({ error: 'invalid JSON' });
      }
    });
  });
  req.on('error', next);
});

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
    // Redis 연결 대기
    await waitForRedis();
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
