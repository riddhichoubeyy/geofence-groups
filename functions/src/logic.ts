// Pure decision logic for geofence events. No Firebase imports so it can be unit-tested.

export const PLAUSIBILITY_SLACK_M = 300; // max tolerance for GPS error
/** Tolerance scales with the fence so small fences are still checked: a flat 300 m would make EXIT unfalsifiable at r<=300. */
export const toleranceM = (radiusM: number) => Math.min(PLAUSIBILITY_SLACK_M, radiusM * 0.5);
export const STALE_EVENT_GRACE_MS = 5000;

export type Fence = { lat: number; lng: number; radiusM: number; updatedAtMs?: number | null };
export type Transition = "ENTER" | "EXIT";

export type Decision =
  | { kind: "ignore"; reason: "stale" | "no-change" }
  | { kind: "baseline" } // first known state after a (re)configure when the member is inside: record, don't notify
  | { kind: "notify" }
  | { kind: "reject"; message: string };

export function distanceM(aLat: number, aLng: number, bLat: number, bLng: number): number {
  const R = 6371000, rad = Math.PI / 180;
  const dLat = (bLat - aLat) * rad, dLng = (bLng - aLng) * rad;
  const h = Math.sin(dLat / 2) ** 2 + Math.cos(aLat * rad) * Math.cos(bLat * rad) * Math.sin(dLng / 2) ** 2;
  return 2 * R * Math.asin(Math.sqrt(h));
}

export function decideTransition(i: {
  prev: boolean | null;
  type: Transition;
  fence: Fence;
  pos: { lat: number; lng: number } | null;
  clientTimeMs: number | null;
}): Decision {
  const inside = i.type === "ENTER";
  if (i.clientTimeMs && i.fence.updatedAtMs && i.clientTimeMs < i.fence.updatedAtMs - STALE_EVENT_GRACE_MS)
    return { kind: "ignore", reason: "stale" };
  if (i.pos) {
    const d = distanceM(i.pos.lat, i.pos.lng, i.fence.lat, i.fence.lng);
    const tol = toleranceM(i.fence.radiusM);
    if (inside && d > i.fence.radiusM + tol)
      return { kind: "reject", message: "Position not inside geofence." };
    if (!inside && d < i.fence.radiusM - tol)
      return { kind: "reject", message: "Position not outside geofence." };
  }
  if (i.prev === inside) return { kind: "ignore", reason: "no-change" };
  if (i.prev === null && inside) return { kind: "baseline" };
  return { kind: "notify" };
}

export function recipientsOf(memberUids: string[], actorUid: string): string[] {
  return memberUids.filter((u) => u !== actorUid);
}

export type PulseCounts = { inside: number; outside: number; unknown: number };
export function countStates(states: (boolean | null | undefined)[]): PulseCounts {
  let inside = 0, outside = 0, unknown = 0;
  for (const s of states) {
    if (s === true) inside++;
    else if (s === false) outside++;
    else unknown++;
  }
  return { inside, outside, unknown };
}
