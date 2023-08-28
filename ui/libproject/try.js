const REPO_BASE_URL = 'https://github.com/all-of-us/workbench';

function checking(text) {
  console.log('in here');
  console.log(text)
  console.log(text.replace(/\(#([0-9]+)\)/g, `([#$1|${REPO_BASE_URL}/pull/$1])`));
}
