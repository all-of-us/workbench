#!/usr/bin/env sh

# Puppeteer does not build after upgrading the deployment script to Node 18.
# Reasons for this are unclear, and we don't care about puppeteer.

# Remove the declaration in package.json that requires it:
# "workspaces": [
#   "integration-tests"
# ],

JSON="$PWD"/.repo/package.json
TMPJSON=tmp.json

grep -v 'integration-tests' "$JSON" > "$TMPJSON"
mv "$TMPJSON" "$JSON"
