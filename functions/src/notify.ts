// FCM delivery with bounded retry. The sender is injected so behaviour is unit-testable.

export type SendResult = { ok: boolean; code?: string };
export type Sender = (tokens: string[]) => Promise<SendResult[]>;

const RETRYABLE = new Set([
  "messaging/internal-error",
  "messaging/server-unavailable",
  "messaging/message-rate-exceeded",
  "messaging/device-message-rate-exceeded",
  "messaging/unknown-error",
]);
const DEAD = new Set(["messaging/registration-token-not-registered", "messaging/invalid-registration-token"]);

export type DeliveryReport = { delivered: number; deadTokens: string[]; failedTokens: string[] };

export async function sendWithRetry(
  tokens: string[], send: Sender, attempts = 3, sleep: (ms: number) => Promise<void> = (ms) => new Promise((r) => setTimeout(r, ms)),
): Promise<DeliveryReport> {
  let pending = [...tokens];
  const dead: string[] = [];
  let delivered = 0;
  for (let attempt = 1; attempt <= attempts && pending.length > 0; attempt++) {
    const results = await send(pending);
    const retry: string[] = [];
    results.forEach((r, i) => {
      if (r.ok) delivered++;
      else if (r.code && DEAD.has(r.code)) dead.push(pending[i]);
      else if (!r.code || RETRYABLE.has(r.code)) retry.push(pending[i]);
      // any other error code is treated as permanent for that token
    });
    pending = retry;
    if (pending.length > 0 && attempt < attempts) await sleep(500 * attempt);
  }
  return { delivered, deadTokens: dead, failedTokens: pending };
}
