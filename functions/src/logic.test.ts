import test from "node:test";
import assert from "node:assert/strict";
import { decideTransition, distanceM, recipientsOf, countStates, Fence } from "./logic";

const fence: Fence = { lat: 12.9716, lng: 77.5946, radiusM: 200, updatedAtMs: 1_000_000 };
// ~0.001 deg lat ≈ 111 m
const at = (dLat: number) => ({ lat: fence.lat + dLat, lng: fence.lng });

test("distance: 0.001 deg latitude is about 111 m", () => {
  const d = distanceM(fence.lat, fence.lng, fence.lat + 0.001, fence.lng);
  assert.ok(d > 105 && d < 117, String(d));
});

test("EXIT after being inside notifies", () => {
  const r = decideTransition({ prev: true, type: "EXIT", fence, pos: at(0.004), clientTimeMs: 2_000_000 });
  assert.deepEqual(r, { kind: "notify" });
});

test("re-ENTER after being outside notifies", () => {
  const r = decideTransition({ prev: false, type: "ENTER", fence, pos: at(0.0005), clientTimeMs: 2_000_000 });
  assert.deepEqual(r, { kind: "notify" });
});

test("duplicate EXIT is ignored (dedupe on state)", () => {
  const r = decideTransition({ prev: false, type: "EXIT", fence, pos: at(0.004), clientTimeMs: 2_000_000 });
  assert.deepEqual(r, { kind: "ignore", reason: "no-change" });
});

test("first ENTER with unknown state is a silent baseline", () => {
  const r = decideTransition({ prev: null, type: "ENTER", fence, pos: at(0.0005), clientTimeMs: 2_000_000 });
  assert.deepEqual(r, { kind: "baseline" });
});

test("first EXIT with unknown state notifies", () => {
  const r = decideTransition({ prev: null, type: "EXIT", fence, pos: at(0.004), clientTimeMs: 2_000_000 });
  assert.deepEqual(r, { kind: "notify" });
});

test("event created before the geofence was reconfigured is ignored as stale", () => {
  const r = decideTransition({ prev: true, type: "EXIT", fence, pos: at(0.004), clientTimeMs: 900_000 });
  assert.deepEqual(r, { kind: "ignore", reason: "stale" });
});

test("EXIT claimed from deep inside the fence is rejected", () => {
  const r = decideTransition({ prev: true, type: "EXIT", fence, pos: at(0.0001), clientTimeMs: 2_000_000 });
  assert.equal(r.kind, "reject");
});

test("ENTER claimed from far outside the fence is rejected", () => {
  const r = decideTransition({ prev: false, type: "ENTER", fence, pos: at(0.02), clientTimeMs: 2_000_000 });
  assert.equal(r.kind, "reject");
});

test("EXIT within GPS tolerance of the edge is accepted", () => {
  // ~ 190 m from center, claims EXIT: within tolerance (100 m) of the 200 m radius
  const r = decideTransition({ prev: true, type: "EXIT", fence, pos: at(0.0017), clientTimeMs: 2_000_000 });
  assert.deepEqual(r, { kind: "notify" });
});

test("missing location skips plausibility but still dedupes", () => {
  assert.deepEqual(decideTransition({ prev: true, type: "EXIT", fence, pos: null, clientTimeMs: null }), { kind: "notify" });
  assert.deepEqual(decideTransition({ prev: false, type: "EXIT", fence, pos: null, clientTimeMs: null }),
    { kind: "ignore", reason: "no-change" });
});

test("recipients never include the actor", () => {
  assert.deepEqual(recipientsOf(["a", "b", "c"], "b"), ["a", "c"]);
  assert.deepEqual(recipientsOf(["a"], "a"), []);
});

test("pulse counts treat missing/null as unknown", () => {
  assert.deepEqual(countStates([true, false, null, undefined, true]), { inside: 2, outside: 1, unknown: 2 });
});
