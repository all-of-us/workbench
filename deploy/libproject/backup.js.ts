// async function backup(arg) {
//   console.log('in the method');
//   try {
//     console.log("in try");
//     const { stdout, stderr } = await exec('gsutil cp gs://all-of-us-workbench-test-credentials/.npmrc ..');
//     stdout.on('data', function(data) {
//       console.log(data);
//     });
//     stderr.on('data', function(data) {
//       console.log(data);
//     });
//     console.log('all done')
//   } catch (e) {
//     console.log("Error while running command ", e);
//   }
// }
//
