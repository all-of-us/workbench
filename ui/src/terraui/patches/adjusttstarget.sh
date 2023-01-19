# Esbuild doesn't support the es5 target for certain things, so switch to a newer target.

set -v

vim -e -s -c '%s/"target": "es5"/"target": "es2022"' -c wq $@
