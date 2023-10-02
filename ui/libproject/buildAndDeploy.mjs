import {spawn} from "child_process";

function build_and_deploy(arg) {
  try {
    spawn("which gsutil", (err, stdout, stderr) => {
      if (err) {
        console.error("Is this an error: ", err);
      }
      console.log(stdout.toString('utf8'));
      console.log("Stderr: ", stderr.toString('utf8'));
    });
  } catch (e) {
    console.log("Error while running command ", e);
  }
}

build_and_deploy(process.argv)