#!/usr/bin/env sh

# Esbuild doesn't support the es5 target for certain things, so switch to a newer target.

set -v

node patches/patch.mjs '"target": "es5"' "() => '\"target\": \"es2022\"'" "$@"
