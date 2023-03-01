#!/usr/bin/env sh

# The react-virtualized library contains a garbage import. Esbuild is strict about these things,
# Webpack is not.

# http://redsymbol.net/articles/unofficial-bash-strict-mode/
set -euo pipefail
set -v

WD="$PWD"

TMPDIR="$(mktemp -d)"
cd "$TMPDIR"

ZF="$WD"/.repo/.yarn/cache/react-virtualized-npm-9.22.3-0fff3cbf64-5e3b566592.zip
BADFILE=node_modules/react-virtualized/dist/es/WindowScroller/utils/onScroll.js

unzip "$ZF" $BADFILE

node "$WD"/patches/patch.mjs '(import [{] bpfrpt_proptype_WindowScroller)' \
  "s => '// '+s" "$BADFILE"

zip -f "$ZF" $BADFILE

rm $BADFILE
rmdir -p "$(dirname $BADFILE)"
cd -
rmdir "$TMPDIR"
