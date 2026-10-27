# Test report

Honest status as of this commit. "Passed" means a command ran and produced the output shown; everything else is untested.

## Follow-up changes and checks (2026-10-25)

- Added a bootstrap `gradlew` / `gradlew.bat` launcher for the pinned Gradle 8.9 distribution because the repository previously had only `gradle-wrapper.properties`. The launcher downloads Gradle on first use; it still needs network access and does not install the Android SDK.
- Hardened invite-code privacy: new codes are stored under `groups/{groupId}/private/invite`, Firestore rules deny client access, and a new `getInviteCode` callable allows only group owners/admins to fetch them. The group document no longer contains the code. If an older backend was already deployed, existing group documents need the migration described in `README.md`.
- Re-ran the 18 dependency-free logic/notification tests after the changes by transpiling those pure TypeScript modules with the globally installed TypeScript compiler and running `node --test`: **18 passed, 0 failed**. TypeScript syntax transpilation and Android XML parsing also passed.
- **Not verified after these edits:** full Cloud Functions type-check/build, Firestore emulator rules tests, Android Gradle compile/lint, Firebase deployment, or device behaviour. `npm ci --offline` could not install dependencies because the npm registry packages were not cached; this environment cannot resolve external hosts.

## Latest revalidation in the current sandbox (2026-10-25)

- The 18 pure TypeScript logic/notification tests were re-transpiled from source with the globally available TypeScript transpiler and run with `node --test`: **18 passed, 0 failed**. This checks the isolated decision/retry modules, not Firebase integration.
- Every Android XML resource and manifest parsed successfully; repository JSON files parsed successfully; `gradlew` and the screenshot script passed shell syntax checks.
- A full `npm ci` attempt timed out while reaching the package registry and left an incomplete `node_modules` tree. Consequently, `npm test` could not perform the full type-check in this sandbox (missing package/type declarations). Generated `node_modules` and `lib` artifacts are not included in the delivered ZIP. Re-run `npm ci && npm test` on a network-connected machine with Node 20.
- Android build/lint, Firestore emulator rules tests, Firebase deployment, and real-device testing remain unverified for the reasons below.

## Environment used for automated checks
Linux sandbox, Node 22, OpenJDK 21 (JRE only), Kotlin compiler 2.0.21. **No Android SDK, no Gradle, no Firebase emulator** —
the sandbox could not reach `dl.google.com`, `maven.google.com`, `services.gradle.org`, `repo.maven.apache.org` or `storage.googleapis.com`
(all returned no response).

## Verified (evidence)
| Check | Command | Result |
|---|---|---|
| Cloud Functions TypeScript compile | `cd functions && npm run build` | passed, no errors |
| Functions unit tests (18): transition decisions, dedupe, stale-event, plausibility, actor exclusion, pulse counts, FCM retry/prune | `cd functions && npm test` | **18 passed, 0 failed** |
| Android domain logic (22 tests: monitoring state, presence/staleness, pulse headline, time formatting) | compiled with `kotlinc 2.0.21` and executed with a small local JUnit-compatible shim runner | **22 passed, 0 failed** |
| XML resources well-formed | Python `minidom` parse of every `res/` and manifest file | passed |
| Secret scan | regex scan for API keys / private keys / client secrets | none found; no `google-services.json` or `local.properties` present |

Notes: the Kotlin tests are real JUnit4 files; I ran them through a shim because JUnit/Gradle weren't downloadable. Treat that as strong
evidence for the logic, not a substitute for `./gradlew testDebugUnitTest`.
A real bug was found and fixed by these tests: the original ±300 m plausibility tolerance made EXIT unfalsifiable for fences ≤ 300 m; tolerance now scales with radius.

## Not verified — needs your machine
| Area | How |
|---|---|
| **Android compile** (all Compose/UI/Firebase/Play Services code, ~25 files) | open in Android Studio / `./gradlew :app:assembleDebug`. Expect possible first-sync compile errors; none have been seen because it hasn't been compiled. |
| Android lint, JUnit run via Gradle | `./gradlew :app:lintDebug :app:testDebugUnitTest` |
| Firestore rules | `cd firestore-tests && npm install && npm test` (tests are written, **never run**) |
| Cloud Functions against Firestore/FCM (transactions, `createGroup`/`joinGroup`/`leaveGroup`/`setGeofence`, real push) | deploy to a Firebase project, or run the emulators |
| Auth flows, group create/join with two accounts | two devices/accounts |
| Real geofence EXIT/ENTER on hardware, background delivery, app killed, reboot, airplane-mode retry | physical phone, walking or a mock-location app |
| Notification reaches B and not A; multi-device actor exclusion | two devices + A on a second device |
| Invalid-token pruning in production | sign out/in, uninstall/reinstall, check `users/{uid}/devices` |
| Map style, Compose layout/spacing, accessibility | on device; screenshots pending |

## Manual end-to-end checklist (fill in with date/device)
- [ ] A signs up, creates group, sees invite code
- [ ] B signs up, joins with code; group shows 2 members
- [ ] A sets boundary; B's phone receives silent sync and registers geofence (Settings → monitoring "active")
- [ ] A exits → B gets "A exited the boundary"; A gets nothing; timeline shows the exit on both
- [ ] A re-enters → B notified; timeline shows return; Group Pulse updates
- [ ] Duplicate: toggle location off/on near the edge → no duplicate notification for the same state
- [ ] B force-stopped / screen off → notification still arrives
- [ ] Revoke "Allow all the time" → banner flips to "Location permission needed"
- [ ] Non-member cannot read the group (rules test or console check)
