#!/usr/bin/env sh

# Webpack cannot analyze require statements if they are indirect and we don't need the protection
# since we know `require` is present.

set -v

vi -e -s -c '%s/__require("react")/require("react")' -c x $@

# Webpack warns if any indirect requires are present even if they are not used.
# There are two offending usages.

vi -e -s -c '%s/? require /? () => null ' -c x $@
vi -e -s -c '%s/return require.apply/return (() => null)' -c x $@
