function requireEnv(key: string): string {
  const value = process.env[key];
  if (!value) {
    throw new Error(`환경변수 ${key}가 설정되지 않았습니다.`);
  }
  return value;
}

export const env = {
  PORT: Number(process.env.PORT) || 8084,

  REDIS_HOST: process.env.REDIS_HOST || 'localhost',
  REDIS_PORT: Number(process.env.REDIS_PORT) || 6379,
  REDIS_PASSWORD: requireEnv('REDIS_PASSWORD'),

  DB_HOST: process.env.DB_HOST || 'localhost',
  DB_PORT: Number(process.env.DB_PORT) || 3306,
  DB_NAME: process.env.DB_NAME || 'popcon',
  DB_USERNAME: requireEnv('DB_USERNAME'),
  DB_PASSWORD: requireEnv('DB_PASSWORD'),
} as const;
