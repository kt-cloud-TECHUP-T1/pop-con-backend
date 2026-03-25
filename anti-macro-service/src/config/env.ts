export const env = {
  PORT: Number(process.env.PORT) || 8084,

  REDIS_HOST: process.env.REDIS_HOST || 'localhost',
  REDIS_PORT: Number(process.env.REDIS_PORT) || 6379,
  REDIS_PASSWORD: process.env.REDIS_PASSWORD || 'testpw',

  DB_HOST: process.env.DB_HOST || 'localhost',
  DB_PORT: Number(process.env.DB_PORT) || 3306,
  DB_NAME: process.env.DB_NAME || 'popcon',
  DB_USERNAME: process.env.DB_USERNAME || 'test',
  DB_PASSWORD: process.env.DB_PASSWORD || 'testpw',
} as const;
