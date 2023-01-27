#!/usr/bin/env sh

# Remove node version check.

set -v

node patches/patch.mjs '- [.][/][.]hooks[/]plugin-warning-logger[.]js' "() => ''" "$@"
