import type { SignalTier } from '../types';

// --- 시그널 가중치 ---
export const SIGNAL_WEIGHTS: Record<string, { tier: SignalTier; weight: number }> = {
  // Hard (30-50)
  webdriver_detected: { tier: 'hard', weight: 50 },
  honeypot_triggered: { tier: 'hard', weight: 40 },
  click_speed_inhuman: { tier: 'hard', weight: 40 },
  zero_mouse_touch_events: { tier: 'hard', weight: 30 },
  untrusted_event: { tier: 'hard', weight: 50 },

  // Medium (10-20)
  click_button_center: { tier: 'medium', weight: 20 },
  click_interval_uniform: { tier: 'medium', weight: 15 },
  abnormal_webgl: { tier: 'medium', weight: 15 },
  ua_touch_mismatch: { tier: 'medium', weight: 15 },
  timezone_language_mismatch: { tier: 'medium', weight: 10 },

  // Soft (3-7)
  fast_load_to_click: { tier: 'soft', weight: 5 },
  tab_not_focused: { tier: 'soft', weight: 5 },
  low_fingerprint_confidence: { tier: 'soft', weight: 5 },
  non_korean_language: { tier: 'soft', weight: 3 },
};

// --- 임계값 ---
export const THRESHOLDS = {
  FAST_CLICK_MS: 200,
  INHUMAN_CLICK_INTERVAL_MS: 50,
  CLICK_INTERVAL_STDDEV_MS: 20,
  BUTTON_CENTER_DISTANCE_PX: 1,
  BUTTON_CENTER_RATIO: 0.8,
  FINGERPRINT_CONFIDENCE_MIN: 0.3,
} as const;

// --- VQA 레벨 기준 ---
export const VQA_LEVEL = {
  LEVEL_1_MAX: 20,
  LEVEL_2_MAX: 50,
  LEVEL_3_MAX: 80,
} as const;

// --- 의심 WebGL 렌더러 ---
export const SUSPICIOUS_WEBGL_RENDERERS = [
  'swiftshader',
  'llvmpipe',
  'mesa',
  'virtualbox',
];
