function validate_options1(arg) {
  'use strict';
  if (arg && arg.project && arg.version) {
    return true
  }

  // console.log('hey there');
  return false;
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