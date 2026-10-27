# Publishing to GitHub

```bash
cd geofence-groups
git init -b main                       # skip if already a repo
git status --short                     # review: no google-services.json, local.properties, keystores, node_modules
git add .
git commit -m "Orbit: group geofencing app (Android, Firebase)"

# create an EMPTY repo on github.com (no README/license), then:
git remote add origin https://github.com/<your-username>/orbit.git
git push -u origin main
```
Optional: `gh repo create orbit --public --source=. --remote=origin --push` (GitHub CLI, after `gh auth login`).

Pre-push checklist
- [ ] `git ls-files | grep -Ei "google-services.json$|local.properties|\.jks|keystore|service-account|adminsdk|\.env"` prints nothing except `google-services.json.example`
- [ ] No Maps/Firebase keys in screenshots (blur the console)
- [ ] Add a LICENSE if you want one; pick the real package name before any store release
- [ ] After the first successful device run, add screenshots and update `docs/TEST_REPORT.md`
