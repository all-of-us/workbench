const fs = require('fs');
const moment = require('moment');

const filterBeforeDays = process.argv.slice(2)[0] || 99999999;

const startDate = moment().subtract(filterBeforeDays, 'days');

fs.readFile('circleci_tests.json', 'utf-8', (err, data) => {
  if (err) {
    console.error(err);
    return;
  }

  const failures = {};
  const failureMessages = {};

  const tests = JSON.parse(data)
    .filter(test => test.result === 'failure')
    .filter(test => moment(test.job_start_time) > startDate);

  tests.forEach(test => {
    const key = `${test.job_name}:${test.name}`;
    if (key in failures) {
      failures[key] = failures[key] + 1
    } else {
      failures[key] = 1
    }

    const lines = test.message.split("\n");
    const msg = lines[0] + lines[lines.length - 1];
    if (msg in failureMessages) {
      failureMessages[msg] = failureMessages[msg] + 1
    } else {
      failureMessages[msg] = 1
    }
  });

  for (const [key, count] of Object.entries(failures).sort(([,a],[,b]) => b-a)) {
    const [jobName, name] = key.split(":");
    console.log(`${jobName}, ${name}, ${count}`);
  }
});
