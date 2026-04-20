import type { AnalysisResult, PageType } from '../types';

const WEBHOOK_URL =
  'https://discord.com/api/webhooks/1495698795793481853/-ULRYXhBa2459Ec6y2JawfLoLCu0yPhK-FHA_MwKZfpc3YL6eaZdcXNJW2WALEEREdNf';

type NotifyParams = {
  dbId: number | null;
  page: PageType;
  maskedUserId: string;
  maskedVisitorId: string;
  pageScore: number;
  totalScore: number;
  result: AnalysisResult;
  ipAddress?: string;
  userAgent?: string;
};

/** pageScore > 0일 때 디스코드 알람 전송 (fire-and-forget) */
export function notifyDiscord(params: NotifyParams): void {
  post(params).catch((err) => {
    console.error('[discord] 알람 전송 실패:', err?.message ?? err);
  });
}

async function post(params: NotifyParams): Promise<void> {
  const {
    dbId, page, maskedUserId, maskedVisitorId,
    pageScore, totalScore, result, ipAddress, userAgent,
  } = params;

  const signalLines = result.detectedSignals
    .map((s) => `• \`${s.name}\` (${s.tier}, +${s.weight})`)
    .join('\n') || '(none)';

  const color =
    result.vqaLevel === 4 ? 0xE74C3C :
    result.vqaLevel === 3 ? 0xE67E22 :
    result.vqaLevel === 2 ? 0xF1C40F :
    0x95A5A6;

  const fields: Array<{ name: string; value: string; inline?: boolean }> = [
    { name: 'page', value: page, inline: true },
    { name: 'pageScore', value: String(pageScore), inline: true },
    { name: 'totalScore', value: String(totalScore), inline: true },
    { name: 'vqaLevel', value: String(result.vqaLevel), inline: true },
    { name: 'user', value: maskedUserId, inline: true },
    { name: 'visitor', value: maskedVisitorId, inline: true },
  ];
  if (result.drawResult) {
    fields.push({ name: 'drawResult', value: result.drawResult, inline: true });
  }
  if (dbId !== null) {
    fields.push({ name: 'dbId', value: String(dbId), inline: true });
  }
  if (ipAddress) {
    fields.push({ name: 'ip', value: ipAddress, inline: false });
  }
  if (userAgent) {
    fields.push({ name: 'userAgent', value: userAgent.slice(0, 300), inline: false });
  }
  fields.push({ name: 'signals', value: signalLines.slice(0, 1000), inline: false });

  const body = {
    username: 'anti-macro',
    embeds: [
      {
        title: '의심 시그널 감지',
        color,
        fields,
        timestamp: new Date().toISOString(),
      },
    ],
  };

  const res = await fetch(WEBHOOK_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });

  if (!res.ok) {
    const text = await res.text().catch(() => '');
    throw new Error(`status=${res.status} body=${text.slice(0, 200)}`);
  }
}
