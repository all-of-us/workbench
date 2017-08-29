#!/usr/bin/env bash
set -euo pipefail
IFS=$'\n\t'

{ which git-secrets; } || {
  echo 'git-secrets required and not found. See README for installation instructions.'
  echo
  exit 1
}

{
  git config --remove-section secrets 2>/dev/null
} || {
  :
}
git secrets --add 'private_key'
git secrets --add 'private_key_id'
git secrets --add --allowed --literal "git secrets --add 'private_key'"
git secrets --add --allowed --literal "git secrets --add 'private_key_id'"
