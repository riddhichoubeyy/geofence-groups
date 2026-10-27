import test from "node:test";
import assert from "node:assert/strict";
import { sendWithRetry, Sender } from "./notify";

const noSleep = async () => {};

test("all tokens delivered on first attempt", async () => {
  const send: Sender = async (t) => t.map(() => ({ ok: true }));
  const r = await sendWithRetry(["a", "b"], send, 3, noSleep);
  assert.deepEqual(r, { delivered: 2, deadTokens: [], failedTokens: [] });
});

test("invalid tokens are reported for pruning and not retried", async () => {
  let calls = 0;
  const send: Sender = async (t) => { calls++; return t.map((x) => x === "bad" ? { ok: false, code: "messaging/registration-token-not-registered" } : { ok: true }); };
  const r = await sendWithRetry(["a", "bad"], send, 3, noSleep);
  assert.equal(calls, 1);
  assert.deepEqual(r.deadTokens, ["bad"]);
  assert.equal(r.delivered, 1);
});

test("transient failures are retried only for failed tokens, then succeed", async () => {
  const seen: string[][] = [];
  let n = 0;
  const send: Sender = async (t) => { seen.push([...t]); n++; return t.map((x) => x === "b" && n === 1 ? { ok: false, code: "messaging/server-unavailable" } : { ok: true }); };
  const r = await sendWithRetry(["a", "b"], send, 3, noSleep);
  assert.deepEqual(seen, [["a", "b"], ["b"]]);
  assert.deepEqual(r, { delivered: 2, deadTokens: [], failedTokens: [] });
});

test("persistent transient failure gives up after the attempt limit and reports it", async () => {
  let calls = 0;
  const send: Sender = async (t) => { calls++; return t.map(() => ({ ok: false, code: "messaging/internal-error" })); };
  const r = await sendWithRetry(["a"], send, 3, noSleep);
  assert.equal(calls, 3);
  assert.deepEqual(r.failedTokens, ["a"]);
  assert.equal(r.delivered, 0);
});

test("permanent non-token errors are not retried", async () => {
  let calls = 0;
  const send: Sender = async (t) => { calls++; return t.map(() => ({ ok: false, code: "messaging/invalid-argument" })); };
  const r = await sendWithRetry(["a"], send, 3, noSleep);
  assert.equal(calls, 1);
  assert.deepEqual(r.failedTokens, []);
});
