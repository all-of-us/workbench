#!/bin/bash

if [ -z "$1" ]
then
    >&2 echo "Missing argument - directory of the workbench-snippets repository"
    exit 1
fi

# drop trailing slash if exists
SNIPPETS_REPO_DIR=${1%/}

snippet_filename_prefixes=("py_gcs" "py_sql" "py_dataset" "r_gcs" "r_sql" "r_dataset", "py_cromwell")

for prefix in "${snippet_filename_prefixes[@]}"; do
  source="${SNIPPETS_REPO_DIR}/build/${prefix}_snippets_menu_config.json"
  dest="$(git rev-parse --show-toplevel)/api/snippets-menu/$(echo ${prefix} | tr '_' '-')-snippets-menu.json"
  jq -S '.' $source > $dest
  echo "Copied pretty printed $source into $dest"
done
