// Run with: cd firestore-tests && npm install && npm test   (needs Java + the Firebase CLI emulator download)
import test, { before, after } from "node:test";
import { readFileSync } from "node:fs";
import { initializeTestEnvironment, assertFails, assertSucceeds } from "@firebase/rules-unit-testing";
import { doc, getDoc, setDoc, deleteDoc, collection, getDocs, query, where, serverTimestamp } from "firebase/firestore";

let env;
before(async () => {
  env = await initializeTestEnvironment({
    projectId: "demo-orbit",
    firestore: { rules: readFileSync("../firestore.rules", "utf8") },
  });
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await setDoc(doc(db, "groups/g1"), { name: "Trip", ownerUid: "alice", memberUids: ["alice", "bob"] });
    await setDoc(doc(db, "groups/g1/members/alice"), { displayName: "Alice", role: "owner" });
    await setDoc(doc(db, "groups/g1/events/e1"), { uid: "alice", type: "EXIT" });
    await setDoc(doc(db, "groups/g1/private/invite"), { code: "ABC234" });
    await setDoc(doc(db, "inviteCodes/ABC234"), { groupId: "g1" });
    await setDoc(doc(db, "users/alice/devices/tok1"), { updatedAt: new Date() });
  });
});
after(() => env.cleanup());

const as = (uid) => env.authenticatedContext(uid).firestore();

test("members can read their group, members and events", async () => {
  await assertSucceeds(getDoc(doc(as("bob"), "groups/g1")));
  await assertSucceeds(getDoc(doc(as("bob"), "groups/g1/members/alice")));
  await assertSucceeds(getDoc(doc(as("bob"), "groups/g1/events/e1")));
});
test("non-members cannot read group, members or events", async () => {
  await assertFails(getDoc(doc(as("mallory"), "groups/g1")));
  await assertFails(getDoc(doc(as("mallory"), "groups/g1/members/alice")));
  await assertFails(getDoc(doc(as("mallory"), "groups/g1/events/e1")));
});
test("unauthenticated users can read nothing", async () => {
  await assertFails(getDoc(doc(env.unauthenticatedContext().firestore(), "groups/g1")));
});
test("group list query is limited to the caller's own groups", async () => {
  await assertSucceeds(getDocs(query(collection(as("bob"), "groups"), where("memberUids", "array-contains", "bob"))));
  await assertFails(getDocs(collection(as("bob"), "groups")));
});
test("clients cannot write groups, members, events or invite codes (even owners)", async () => {
  await assertFails(setDoc(doc(as("alice"), "groups/g1"), { name: "Hijack" }, { merge: true }));
  await assertFails(setDoc(doc(as("bob"), "groups/g1/members/bob"), { role: "owner" }));
  await assertFails(setDoc(doc(as("bob"), "groups/g1/events/fake"), { uid: "alice", type: "EXIT" }));
  await assertFails(getDoc(doc(as("bob"), "inviteCodes/ABC234")));
  await assertFails(getDoc(doc(as("alice"), "groups/g1/private/invite")));
  await assertFails(setDoc(doc(as("bob"), "inviteCodes/ZZZ999"), { groupId: "g1" }));
});
test("a user can register only their own push token, with only the allowed field", async () => {
  await assertSucceeds(setDoc(doc(as("bob"), "users/bob/devices/tok2"), { updatedAt: serverTimestamp() }));
  await assertFails(setDoc(doc(as("bob"), "users/bob/devices/tok3"), { updatedAt: serverTimestamp(), extra: 1 }));
  await assertFails(setDoc(doc(as("bob"), "users/alice/devices/tok9"), { updatedAt: serverTimestamp() }));
  await assertFails(getDoc(doc(as("bob"), "users/alice/devices/tok1")));
  await assertSucceeds(deleteDoc(doc(as("bob"), "users/bob/devices/tok2")));
});
