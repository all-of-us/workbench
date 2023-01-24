#!/usr/bin/env sh

# The react-virtualized library contains a garbage import. Esbuild is strict about these things,
# Webpack is not.

# http://redsymbol.net/articles/unofficial-bash-strict-mode/
set -euo pipefail
set -v

WD="$(dirname "$(dirname "$0")")"
cd "${WD:="$PWD"}"
ZF="$PWD"/.repo/.yarn/cache/react-virtualized-npm-9.22.3-0fff3cbf64-5e3b566592.zip

TMPDIR="$(mktemp -d)"
cd "$TMPDIR"

BADFILE=node_modules/react-virtualized/dist/es/WindowScroller/utils/onScroll.js

unzip "$ZF" $BADFILE

vi -e -c '%s/\(import { bpfrpt_proptype_WindowScroller\)/\/\/ \1' -c wq $BADFILE

zip -f "$ZF" $BADFILE

rm $BADFILE
rmdir -p "$(dirname $BADFILE)"
cd -
rmdir "$TMPDIR"
