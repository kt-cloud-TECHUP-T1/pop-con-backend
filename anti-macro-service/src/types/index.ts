// === 프론트엔드에서 보내는 raw 데이터 타입 ===

export type PointEvent = {
  x: number;
  y: number;
  timestamp: number;
  isTrusted: boolean;
};

export type ClickEvent = {
  x: number;
  y: number;
  timestamp: number;
  isTrusted: boolean;
  targetSelector: string;
  centerDistance: number | null;
};

export type BrowserFingerprint = {
  visitorId: string;
  confidence: number;
  webdriver: boolean;
  webglRenderer: string | null;
  webglVendor: string | null;
  userAgent: string;
  platform: string;
  language: string;
  languages: readonly string[];
  timezone: string;
  screenResolution: { width: number; height: number };
  colorDepth: number;
  hardwareConcurrency: number;
  deviceMemory?: number;
  components: Record<string, unknown>;
};

export type PageType = 'login' | 'popup-detail' | 'draw-application';

export type RawData = {
  // fingerprint 관련 (flat)
  visitorId?: string;
  confidence?: number;
  webdriver?: boolean;
  webglRenderer?: string;
  webglVendor?: string;
  userAgent?: string;
  platform?: string;
  language?: string;
  languages?: string[];
  timezone?: string;
  screenResolution?: { width: number; height: number };
  colorDepth?: number;
  hardwareConcurrency?: number;
  deviceMemory?: number;
  components?: Record<string, unknown>;
  // 클릭/마우스
  clicks?: ClickEvent[];
  movements?: PointEvent[];
  hasUntrustedEvent?: boolean;
  // 허니팟 (flat)
  triggered?: boolean;
  fieldValue?: string | null;
  // 타이밍 (flat)
  pageLoadTimestamp?: number;
  firstInteractionTimestamp?: number | null;
  loadToFirstClickMs?: number | null;
  tabFocusedDuringClicks?: boolean;
};

export type PageSignalPayload = {
  page: PageType;
  collectedAt: number;
  fingerprint?: BrowserFingerprint;
  rawData: RawData;
};

export type AntiMacroSubmission = {
  nonce: string;
  timestamp: number;
  payload: PageSignalPayload;
  signature: string;
  visitorId?: string;
  userId?: string;
};

// === 분석 결과 타입 ===

export type SignalTier = 'hard' | 'medium' | 'soft';

export type DetectedSignal = {
  name: string;
  tier: SignalTier;
  weight: number;
  value: unknown;
};

export type AnalysisResult = {
  score: number;
  detectedSignals: DetectedSignal[];
  vqaDifficulty: 'easy' | 'medium' | 'hard';
  drawResult?: 'pass' | 'fail';
};
