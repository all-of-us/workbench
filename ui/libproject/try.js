const REPO_BASE_URL = 'https://github.com/all-of-us/workbench';

function checking(text) {
  return text.replace(/\(#([0-9]+)\)/g, `([#$1|${REPO_BASE_URL}/pull/$1])`);
}
