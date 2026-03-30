import type { PageSignalPayload, DetectedSignal, AnalysisResult } from '../types';
import { SIGNAL_WEIGHTS, THRESHOLDS, VQA_DIFFICULTY, SUSPICIOUS_WEBGL_RENDERERS } from './constants';
import { stddev } from './utils';

/**
 * 프론트엔드에서 받은 rawData를 분석하여 시그널을 감지하고 점수를 계산한다.
 */
export function analyzeRawData(payload: PageSignalPayload): AnalysisResult {
  const detected: DetectedSignal[] = [];
  const { rawData, fingerprint } = payload;

  // === fingerprint 기반 ===
  if (fingerprint) {
    // Hard: webdriver
    if (fingerprint.webdriver) {
      detected.push({
        ...SIGNAL_WEIGHTS.webdriver_detected,
        name: 'webdriver_detected',
        value: true,
      });
    }

    // Medium: 비정상 WebGL 렌더러
    const renderer = (fingerprint.webglRenderer ?? '').toLowerCase();
    const isSuspicious = SUSPICIOUS_WEBGL_RENDERERS.some((s) => renderer.includes(s));
    if (isSuspicious || !fingerprint.webglRenderer) {
      detected.push({
        ...SIGNAL_WEIGHTS.abnormal_webgl,
        name: 'abnormal_webgl',
        value: { renderer: fingerprint.webglRenderer, vendor: fingerprint.webglVendor },
      });
    }

    // Soft: 낮은 confidence
    if (fingerprint.confidence < THRESHOLDS.FINGERPRINT_CONFIDENCE_MIN) {
      detected.push({
        ...SIGNAL_WEIGHTS.low_fingerprint_confidence,
        name: 'low_fingerprint_confidence',
        value: fingerprint.confidence,
      });
    }
  }

  // === 클릭 행동 ===
  const clicks = rawData.clicks ?? [];
  if (clicks.length >= 2) {
    // Medium: 버튼 정중앙 클릭 비율
    const buttonClicks = clicks.filter((c) => c.centerDistance !== null);
    if (buttonClicks.length >= 2) {
      const centerClicks = buttonClicks.filter(
        (c) => c.centerDistance! < THRESHOLDS.BUTTON_CENTER_DISTANCE_PX,
      );
      const centerRatio = centerClicks.length / buttonClicks.length;
      if (centerRatio >= THRESHOLDS.BUTTON_CENTER_RATIO) {
        detected.push({
          ...SIGNAL_WEIGHTS.click_button_center,
          name: 'click_button_center',
          value: { centerRatio, buttonClickCount: buttonClicks.length },
        });
      }
    }

    // 클릭 간격
    const intervals: number[] = [];
    for (let i = 1; i < clicks.length; i++) {
      intervals.push(clicks[i].timestamp - clicks[i - 1].timestamp);
    }

    // Hard: 비현실적 클릭 속도
    const inhumanClicks = intervals.filter((ms) => ms < THRESHOLDS.INHUMAN_CLICK_INTERVAL_MS);
    if (inhumanClicks.length > 0) {
      detected.push({
        ...SIGNAL_WEIGHTS.click_speed_inhuman,
        name: 'click_speed_inhuman',
        value: { inhumanCount: inhumanClicks.length, minInterval: Math.min(...inhumanClicks) },
      });
    }

    // Medium: 클릭 간격 일정성
    if (intervals.length >= 2) {
      const intervalStddev = stddev(intervals);
      if (intervalStddev < THRESHOLDS.CLICK_INTERVAL_STDDEV_MS) {
        detected.push({
          ...SIGNAL_WEIGHTS.click_interval_uniform,
          name: 'click_interval_uniform',
          value: { stddev: intervalStddev },
        });
      }
    }
  }

  // === 마우스 ===
  const movements = rawData.mouseMovements ?? [];

  if (rawData.hasUntrustedEvent) {
    detected.push({
      ...SIGNAL_WEIGHTS.untrusted_event,
      name: 'untrusted_event',
      value: true,
    });
  }

  if (movements.length === 0 && payload.page !== 'popup-detail') {
    detected.push({
      ...SIGNAL_WEIGHTS.zero_mouse_touch_events,
      name: 'zero_mouse_touch_events',
      value: { mouseEvents: 0 },
    });
  }

  // === 타이밍 ===
  if (rawData.loadToFirstClickMs !== null && rawData.loadToFirstClickMs !== undefined
    && rawData.loadToFirstClickMs < THRESHOLDS.FAST_CLICK_MS
    && payload.page !== 'popup-detail') {
    detected.push({
      ...SIGNAL_WEIGHTS.fast_load_to_click,
      name: 'fast_load_to_click',
      value: { loadToClickMs: rawData.loadToFirstClickMs },
    });
  }

  if (rawData.tabFocusedDuringClicks === false) {
    detected.push({
      ...SIGNAL_WEIGHTS.tab_not_focused,
      name: 'tab_not_focused',
      value: true,
    });
  }

  // === 허니팟 ===
  if (rawData.triggered) {
    detected.push({
      ...SIGNAL_WEIGHTS.honeypot_triggered,
      name: 'honeypot_triggered',
      value: { fieldValue: rawData.fieldValue },
    });
  }

  // === 환경 ===
  if (rawData.timezone && rawData.timezone !== 'Asia/Seoul') {
    detected.push({
      ...SIGNAL_WEIGHTS.timezone_language_mismatch,
      name: 'timezone_language_mismatch',
      value: { timezone: rawData.timezone, language: rawData.language },
    });
  }

  if (rawData.language) {
    const langPrefix = rawData.language.split('-')[0].toLowerCase();
    if (langPrefix !== 'ko' && langPrefix !== 'en') {
      detected.push({
        ...SIGNAL_WEIGHTS.non_korean_language,
        name: 'non_korean_language',
        value: { language: rawData.language },
      });
    }
  }

  // === 점수 합산 ===
  const score = detected.reduce((sum, s) => sum + s.weight, 0);

  // === VQA 난이도 ===
  let vqaDifficulty: 'easy' | 'medium' | 'hard';
  if (score <= VQA_DIFFICULTY.EASY_MAX) {
    vqaDifficulty = 'easy';
  } else if (score <= VQA_DIFFICULTY.MEDIUM_MAX) {
    vqaDifficulty = 'medium';
  } else {
    vqaDifficulty = 'hard';
  }

  // === 드로우 신청 페이지 최종 판정 ===
  let drawResult: 'pass' | 'fail' | undefined;
  if (payload.page === 'draw-application') {
    drawResult = score > VQA_DIFFICULTY.MEDIUM_MAX ? 'fail' : 'pass';
  }

  return { score, detectedSignals: detected, vqaDifficulty, drawResult };
}
