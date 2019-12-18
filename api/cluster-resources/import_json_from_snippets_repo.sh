#!/bin/bash

if [ -z "$SNIPPETS_REPO_DIR" ]
then
    >&2 echo "Must set SNIPPETS_REPO_DIR to the location of the workbench-snippets repository"
    exit 1
fi


snippet_filename_prefixes=("py_gcs" "py_sql" "py_dataset" "r_gcs" "r_sql" "r_dataset")

for name in "${snippet_filename_prefixes[@]}"; do
  jq '.' "${SNIPPETS_REPO_DIR}/build/${name}_snippets_menu_config.json" > \
    "$(git rev-parse --show-toplevel)/api/cluster-resources/$(echo ${name} | tr '_' '-')-snippets-menu.json";
done
