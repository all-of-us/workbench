#!/bin/bash

if [ -z "$SNIPPETS_REPO_DIR" ]
then
    >&2 echo "Must set SNIPPETS_REPO_DIR to the location of the workbench-snippets repository"
    exit 1
fi

# drop trailing slash if exists
SNIPPETS_REPO_DIR=${SNIPPETS_REPO_DIR%/}

snippet_filename_prefixes=("py_gcs" "py_sql" "py_dataset" "r_gcs" "r_sql" "r_dataset")

for prefix in "${snippet_filename_prefixes[@]}"; do
  source="${SNIPPETS_REPO_DIR}/build/${prefix}_snippets_menu_config.json"
  dest="$(git rev-parse --show-toplevel)/api/cluster-resources/$(echo ${prefix} | tr '_' '-')-snippets-menu.json"
  jq '.' $source > $dest
  echo "Copied pretty printed $source into $dest"
done
