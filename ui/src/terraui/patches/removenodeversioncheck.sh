#!/usr/bin/env sh

# Remove node version check.

set -v

vi -e -s -c '%s/- .\/.hooks\/plugin-warning-logger.js/' -c wq $@
