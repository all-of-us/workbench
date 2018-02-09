#!/usr/bin/env bash
set -euo pipefail
IFS=$'\n\t'

{ which git-secrets; } || {
  echo 'git-secrets required and not found. See https://github.com/all-of-us/workbench#git-secrets for installation instructions.'
i
  echo
  exit 1
}

{
  git config --remove-section secrets 2>/dev/null
} || {
  :
}
git secrets --add '"private_key":'
git secrets --add --allowed 'git secrets --add *'
