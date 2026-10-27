#!/usr/bin/env bash
# Captures real screenshots from a connected device into docs/screenshots/.
# Usage: scripts/capture-screenshots.sh home   (then navigate on the phone, run again with another name)
set -euo pipefail
name="${1:?usage: $0 <name>}"
mkdir -p "$(dirname "$0")/../docs/screenshots"
adb exec-out screencap -p > "$(dirname "$0")/../docs/screenshots/${name}.png"
echo "saved docs/screenshots/${name}.png"
