#!/bin/bash -e

#The git secrets command will default to placing it in the .git hooks,
# we need to provide it a different directory to make them in and then
# move them to the proper directory.
{
  git config --remove-section secrets 2>/dev/null
} || {
  :
}
git secrets --add 'private_key'
git secrets --add 'private_key_id'
git secrets --add --allowed --literal "git secrets --add 'private_key'"
git secrets --add --allowed --literal "git secrets --add 'private_key_id'"
