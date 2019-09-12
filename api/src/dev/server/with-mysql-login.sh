#!/bin/bash
# Generates a temporary "my.cnf" MySQL client configuration file. This is useful
# in situations where we want an interactive shell for MySQL interactions, e.g.
# for mysqlbinlog, without requiring the developer to re-enter the user/pass.
# See https://dev.mysql.com/doc/refman/8.0/en/option-file-options.html

# This script is designed to work within docker run --rm, where /tmp is
# generally not a mounted volume and is assumed not to persist beyond a single
# docker invocation.
tmp_dir="$(mktemp -d)"
tmp="${tmp_dir}/my.cnf"

user="${1}"
password="${2}"
cat > "${tmp}" << EOF
[client]
user = ${user}
password = ${password}
host = 127.0.0.1
port = 3307
EOF

echo "${tmp_dir}"
