import { onCall, HttpsError, CallableRequest } from "firebase-functions/v2/https";
import { setGlobalOptions } from "firebase-functions/v2";
import { initializeApp } from "firebase-admin/app";
import { getFirestore, FieldValue, DocumentReference } from "firebase-admin/firestore";
import { getAuth } from "firebase-admin/auth";
import { getMessaging } from "firebase-admin/messaging";
import { decideTransition, recipientsOf, countStates } from "./logic";
import { sendWithRetry, SendResult } from "./notify";

initializeApp();
setGlobalOptions({ region: "us-central1", maxInstances: 10 });
const db = getFirestore();

const CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
const MAX_MEMBERS = 50;
const MIN_RADIUS = 100;
const MAX_RADIUS = 50000;

function requireUid(req: CallableRequest): string {
  if (!req.auth) throw new HttpsError("unauthenticated", "Sign in required.");
  return req.auth.uid;
}
function str(v: unknown, field: string, max: number): string {
  const s = typeof v === "string" ? v.trim() : "";
  if (!s || s.length > max) throw new HttpsError("invalid-argument", `Invalid ${field}.`);
  return s;
}
function num(v: unknown, field: string, min: number, max: number): number {
  if (typeof v !== "number" || !isFinite(v) || v < min || v > max)
    throw new HttpsError("invalid-argument", `Invalid ${field}.`);
  return v;
}
function genCode(): string {
  let c = "";
  for (let i = 0; i < 6; i++) c += CODE_ALPHABET[Math.floor(Math.random() * CODE_ALPHABET.length)];
  return c;
}
async function displayNameOf(uid: string): Promise<string> {
  const u = await getAuth().getUser(uid);
  return u.displayName || u.email?.split("@")[0] || "Member";
}

type LastEvent = { lastEventName: string | null; lastEventType: string | null; lastEventAt: unknown };
const noEvent: LastEvent = { lastEventName: null, lastEventType: null, lastEventAt: null };
/** Denormalised summary shown on the home screen and Group Pulse. Holds counts only, never coordinates. */
function pulse(states: (boolean | null | undefined)[], last: LastEvent) {
  return { ...countStates(states), ...last };
}
function lastOf(group: FirebaseFirestore.DocumentData): LastEvent {
  const p = group.pulse ?? {};
  return { lastEventName: p.lastEventName ?? null, lastEventType: p.lastEventType ?? null, lastEventAt: p.lastEventAt ?? null };
}

type Device = { token: string; ref: DocumentReference };
async function devicesOf(uids: string[]): Promise<Device[]> {
  const snaps = await Promise.all(uids.map((u) => db.collection("users").doc(u).collection("devices").get()));
  return snaps.flatMap((s) => s.docs.map((d) => ({ token: d.id, ref: d.ref })));
}

/** Sends in chunks of 500, retries transient failures, and prunes tokens FCM reports as invalid. */
async function push(devices: Device[], build: (tokens: string[]) => object) {
  const byToken = new Map(devices.map((d) => [d.token, d.ref]));
  const sender = async (tokens: string[]): Promise<SendResult[]> => {
    const out: SendResult[] = [];
    for (let i = 0; i < tokens.length; i += 500) {
      const chunk = tokens.slice(i, i + 500);
      try {
        const res = await getMessaging().sendEachForMulticast(build(chunk) as any);
        res.responses.forEach((r) => out.push({ ok: r.success, code: r.error?.code }));
      } catch {
        chunk.forEach(() => out.push({ ok: false })); // whole-batch failure: retryable
      }
    }
    return out;
  };
  const report = await sendWithRetry(devices.map((d) => d.token), sender);
  await Promise.all(report.deadTokens.map((t) => byToken.get(t)?.delete()));
  return report;
}

export const createGroup = onCall(async (req) => {
  const uid = requireUid(req);
  const name = str(req.data?.name, "group name", 60);
  const displayName = await displayNameOf(uid);
  const groupRef = db.collection("groups").doc();
  for (let attempt = 0; attempt < 10; attempt++) {
    const code = genCode();
    try {
      await db.runTransaction(async (tx) => {
        const codeRef = db.collection("inviteCodes").doc(code);
        if ((await tx.get(codeRef)).exists) throw new Error("collision");
        tx.set(codeRef, { groupId: groupRef.id });
        tx.set(groupRef, {
          name, ownerUid: uid, memberUids: [uid],
          geofence: null, pulse: pulse([null], noEvent), createdAt: FieldValue.serverTimestamp(),
        });
        tx.set(groupRef.collection("private").doc("invite"), { code });
        tx.set(groupRef.collection("members").doc(uid), {
          displayName, role: "owner", joinedAt: FieldValue.serverTimestamp(),
          insideGeofence: null, lastTransitionAt: null,
        });
      });
      return { groupId: groupRef.id, inviteCode: code };
    } catch (e) {
      if ((e as Error).message !== "collision") throw e;
    }
  }
  throw new HttpsError("internal", "Could not allocate an invite code. Try again.");
});

export const getInviteCode = onCall(async (req) => {
  const uid = requireUid(req);
  const groupId = str(req.data?.groupId, "groupId", 40);
  const groupRef = db.collection("groups").doc(groupId);
  const [gSnap, mSnap] = await Promise.all([
    groupRef.get(), groupRef.collection("members").doc(uid).get(),
  ]);
  if (!gSnap.exists || !mSnap.exists || !((gSnap.data()!.memberUids ?? []) as string[]).includes(uid))
    throw new HttpsError("permission-denied", "Not a member of this group.");
  if (!["owner", "admin"].includes(mSnap.data()!.role))
    throw new HttpsError("permission-denied", "Only the owner or an admin can view the invite code.");
  const inviteSnap = await groupRef.collection("private").doc("invite").get();
  const code = inviteSnap.data()?.code;
  if (typeof code !== "string" || !code) throw new HttpsError("not-found", "Invite code unavailable.");
  return { inviteCode: code };
});

export const joinGroup = onCall(async (req) => {
  const uid = requireUid(req);
  const code = str(req.data?.code, "invite code", 12).toUpperCase();
  const displayName = await displayNameOf(uid);
  return db.runTransaction(async (tx) => {
    const codeSnap = await tx.get(db.collection("inviteCodes").doc(code));
    if (!codeSnap.exists) throw new HttpsError("not-found", "Invalid invite code.");
    const groupRef = db.collection("groups").doc(codeSnap.data()!.groupId);
    const [gSnap, members] = await Promise.all([tx.get(groupRef), tx.get(groupRef.collection("members"))]);
    if (!gSnap.exists) throw new HttpsError("not-found", "Group no longer exists.");
    const uids: string[] = gSnap.data()!.memberUids ?? [];
    if (uids.includes(uid)) return { groupId: groupRef.id, alreadyMember: true };
    if (uids.length >= MAX_MEMBERS) throw new HttpsError("resource-exhausted", "Group is full.");
    const states = [...members.docs.map((m) => m.data().insideGeofence as boolean | null), null];
    tx.update(groupRef, { memberUids: FieldValue.arrayUnion(uid), pulse: pulse(states, lastOf(gSnap.data()!)) });
    tx.set(groupRef.collection("members").doc(uid), {
      displayName, role: "member", joinedAt: FieldValue.serverTimestamp(),
      insideGeofence: null, lastTransitionAt: null,
    });
    return { groupId: groupRef.id, alreadyMember: false };
  });
});

export const leaveGroup = onCall(async (req) => {
  const uid = requireUid(req);
  const groupId = str(req.data?.groupId, "groupId", 40);
  const groupRef = db.collection("groups").doc(groupId);
  await db.runTransaction(async (tx) => {
    const [gSnap, members] = await Promise.all([tx.get(groupRef), tx.get(groupRef.collection("members"))]);
    if (!gSnap.exists || !((gSnap.data()!.memberUids ?? []) as string[]).includes(uid))
      throw new HttpsError("permission-denied", "Not a member of this group.");
    if (gSnap.data()!.ownerUid === uid)
      throw new HttpsError("failed-precondition", "The owner can't leave the group.");
    const states = members.docs.filter((m) => m.id !== uid).map((m) => m.data().insideGeofence as boolean | null);
    tx.update(groupRef, { memberUids: FieldValue.arrayRemove(uid), pulse: pulse(states, lastOf(gSnap.data()!)) });
    tx.delete(groupRef.collection("members").doc(uid));
  });
  return { ok: true };
});

export const setGeofence = onCall(async (req) => {
  const uid = requireUid(req);
  const groupId = str(req.data?.groupId, "groupId", 40);
  const lat = num(req.data?.lat, "latitude", -90, 90);
  const lng = num(req.data?.lng, "longitude", -180, 180);
  const radiusM = num(req.data?.radiusM, "radius", MIN_RADIUS, MAX_RADIUS);

  const groupRef = db.collection("groups").doc(groupId);
  const [gSnap, mSnap] = await Promise.all([groupRef.get(), groupRef.collection("members").doc(uid).get()]);
  if (!gSnap.exists || !mSnap.exists) throw new HttpsError("permission-denied", "Not a member of this group.");
  const role = mSnap.data()!.role;
  if (role !== "owner" && role !== "admin")
    throw new HttpsError("permission-denied", "Only the owner or an admin can change the geofence.");

  const members = await groupRef.collection("members").get();
  const batch = db.batch();
  batch.update(groupRef, {
    geofence: { lat, lng, radiusM, updatedBy: uid, updatedAt: FieldValue.serverTimestamp() },
    pulse: pulse(members.docs.map(() => null), lastOf(gSnap.data()!)),
  });
  // New geofence => everyone's inside/outside state is unknown until their device reports it.
  members.docs.forEach((m) => batch.update(m.ref, { insideGeofence: null }));
  await batch.commit();

  const devices = await devicesOf((gSnap.data()!.memberUids ?? []) as string[]);
  await push(devices, (tokens) => ({
    tokens, data: { type: "SYNC", groupId }, android: { priority: "high" },
  })).catch(() => undefined);
  return { ok: true };
});

export const setMemberRole = onCall(async (req) => {
  const uid = requireUid(req);
  const groupId = str(req.data?.groupId, "groupId", 40);
  const target = str(req.data?.targetUid, "targetUid", 128);
  const role = req.data?.role;
  if (role !== "admin" && role !== "member") throw new HttpsError("invalid-argument", "Invalid role.");
  const groupRef = db.collection("groups").doc(groupId);
  const gSnap = await groupRef.get();
  if (!gSnap.exists || gSnap.data()!.ownerUid !== uid)
    throw new HttpsError("permission-denied", "Only the group owner can change roles.");
  if (target === uid) throw new HttpsError("invalid-argument", "Cannot change the owner's role.");
  const tRef = groupRef.collection("members").doc(target);
  if (!(await tRef.get()).exists) throw new HttpsError("not-found", "Member not found.");
  await tRef.update({ role });
  return { ok: true };
});

export const reportGeofenceEvent = onCall(async (req) => {
  const uid = requireUid(req);
  const groupId = str(req.data?.groupId, "groupId", 40);
  const type = req.data?.type;
  if (type !== "ENTER" && type !== "EXIT") throw new HttpsError("invalid-argument", "Invalid type.");
  const hasLoc = typeof req.data?.lat === "number" && typeof req.data?.lng === "number";
  const pos = hasLoc
    ? { lat: num(req.data.lat, "lat", -90, 90), lng: num(req.data.lng, "lng", -180, 180) }
    : null;
  const clientTime = typeof req.data?.clientTime === "number" ? (req.data.clientTime as number) : null;
  const inside = type === "ENTER";

  const groupRef = db.collection("groups").doc(groupId);
  const memberRef = groupRef.collection("members").doc(uid);

  const out = await db.runTransaction(async (tx) => {
    const [gSnap, mSnap, members] = await Promise.all([
      tx.get(groupRef), tx.get(memberRef), tx.get(groupRef.collection("members")),
    ]);
    if (!gSnap.exists || !mSnap.exists) throw new HttpsError("permission-denied", "Not a member of this group.");
    const g = gSnap.data()!;
    if (!((g.memberUids ?? []) as string[]).includes(uid))
      throw new HttpsError("permission-denied", "Not a member of this group.");
    const fence = g.geofence;
    if (!fence) throw new HttpsError("failed-precondition", "Group has no geofence.");

    const decision = decideTransition({
      prev: (mSnap.data()!.insideGeofence ?? null) as boolean | null,
      type, pos, clientTimeMs: clientTime,
      fence: { lat: fence.lat, lng: fence.lng, radiusM: fence.radiusM, updatedAtMs: fence.updatedAt?.toMillis?.() ?? null },
    });
    if (decision.kind === "reject") throw new HttpsError("failed-precondition", decision.message);
    if (decision.kind === "ignore") return { notify: false as const, reason: decision.reason };

    tx.update(memberRef, { insideGeofence: inside, lastTransitionAt: FieldValue.serverTimestamp() });
    const states = members.docs.map((m) => (m.id === uid ? inside : (m.data().insideGeofence as boolean | null)));
    const displayName: string = mSnap.data()!.displayName;

    if (decision.kind === "baseline") {
      tx.update(groupRef, { pulse: pulse(states, lastOf(g)) });
      return { notify: false as const, reason: "baseline" };
    }
    // Events deliberately omit coordinates: the history says who and when, never where.
    const evRef = groupRef.collection("events").doc();
    tx.set(evRef, { uid, displayName, type, serverTime: FieldValue.serverTimestamp() });
    tx.update(groupRef, {
      pulse: pulse(states, { lastEventName: displayName, lastEventType: type, lastEventAt: FieldValue.serverTimestamp() }),
    });
    return { notify: true as const, evRef, groupName: g.name as string, displayName, memberUids: g.memberUids as string[] };
  });

  if (!out.notify) return { recorded: false, reason: out.reason };

  const devices = await devicesOf(recipientsOf(out.memberUids, uid)); // actor never notified
  const verb = inside ? "entered" : "exited";
  const report = await push(devices, (tokens) => ({
    tokens,
    notification: { title: out.groupName, body: `${out.displayName} ${verb} the boundary` },
    data: { type: "MEMBER_EVENT", groupId, actorUid: uid, eventType: type },
    android: { priority: "high", notification: { channelId: "member_events" } },
  }));
  // The event is already durable; record delivery outcome so failures are visible, and never re-throw
  // (a client retry would be deduped as "no-change" and could not resend anyway).
  await out.evRef.update({ delivered: report.delivered, undelivered: report.failedTokens.length }).catch(() => undefined);
  return { recorded: true, delivered: report.delivered, undelivered: report.failedTokens.length };
});
