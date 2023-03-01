#!/usr/bin/env sh

# The SVG plugin in Terra UI exports the SVG as ReactComponent, whereas the plugin we're using
# makes them the default export.

set -v

node patches/patch.mjs '[{]\s*ReactComponent\s+as\s+(\S+)\s*[}]' "(m, s) => s" "$@"
