#!/usr/bin/env sh

# Webpack cannot analyze require statements if they are indirect and we don't need the protection
# since we know `require` is present.

set -v

vi -e -s -c '%s/__require("react")/require("react")' -c wq $@
