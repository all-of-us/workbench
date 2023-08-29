function validate_options1(arg) {
  'use strict';
  console.log(arg.project);
  if (isEmpty(arg) || isEmpty(arg.project)) {// || isEmpty(arg.version)) {
    return false;
  }
  return true;
}

function  isEmpty(value) {
  if (value !== 'undefined' && value !== null) {
    return false;
  }
  return true;
}
// const { spawn } = require("child_process");
// const listening = spawn('gcloud', ['app', 'deploy']);
//
// listening.stdout.on("data", data => {
//   console.log(`stdout: ${data}`);
// });
//
// listening.stderr.on("data", data => {
//   console.log(`stderr: ${data}`);
// });
//
// listening.on('error', (error) => {
//   console.log(`error: ${error.message}`);
// });