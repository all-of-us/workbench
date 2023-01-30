#!/usr/bin/env sh

# Webpack cannot analyze require statements if they are indirect and we don't need the protection
# since we know `require` is present.

set -v

node patches/patch.mjs '__require[(]"react"[)]' "() => 'require(\"react\")'" "$@"

# Webpack warns if any indirect requires are present even if they are not used.
# There are two offending usages.

node patches/patch.mjs '[?] require ' "() => '? () => null '" "$@"
node patches/patch.mjs 'return require.apply' "() => 'return (() => null)'" "$@"
