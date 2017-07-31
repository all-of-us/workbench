#!/bin/bash -e

#The git secrets command will default to placing it in the .git hooks,
# we need to provide it a different directory to make them in and then
# move them to the proper directory.
{
  git --git-dir ../.git --work-tree .. config --remove-section secrets 2>/dev/null
} || {
  :
}
git --git-dir ../.git --work-tree .. secrets --add 'private_key'
git --git-dir ../.git --work-tree .. secrets --add --allowed --literal "'private_key'"
git --git-dir ../.git --work-tree .. secrets --add 'private_key_id'
git --git-dir ../.git --work-tree .. secrets --add --allowed --literal "'private_key_id'"
