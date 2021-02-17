const fetch = require('node-fetch');
const Bottleneck = require('bottleneck');
const queryString = require('query-string');
const fs = require('fs');

const CIRCLE_API_TOKEN = process.env.CIRCLE_TOKEN;

const limiter = new Bottleneck({
  minTime: 100
});

const fetchCircleCI = (url, queryParams) => {
  return limiter.wrap(fetch)(queryString.stringifyUrl({url: url, query: queryParams}), {headers: {'Circle-Token': CIRCLE_API_TOKEN}});
};

async function getPipelines() {
  return await (await fetchAllItems('https://circleci.com/api/v2/project/github/all-of-us/workbench/pipeline', {branch: 'master'}));
}

async function getWorkflows(pipelineId) {
  return await (await fetchAllItems(`https://circleci.com/api/v2/pipeline/${pipelineId}/workflow`, {}));
}

async function getJobs(workflowId) {
  return await (await fetchAllItems(`https://circleci.com/api/v2/workflow/${workflowId}/job`, {}));
}

async function getTestsWithJobMetadata(job) {
  const tests = await getTests(job.job_number);

  if (!tests) {
    console.log(job);
    return [];
  }

  return tests.map(test => { return {...test, job_number: job.job_number, job_name: job.name, job_status: job.status, job_start_time: job.started_at}});
}

async function getTests(jobNumber) {
  return await fetchAllItems(`https://circleci.com/api/v2/project/github/all-of-us/workbench/${jobNumber}/tests`, {});
}

async function fetchAllItems(url, queryParams) {
  var resp = await (await fetchCircleCI(url, queryParams)).json();
  const items = resp.items;

  while (!!resp.next_page_token) {
    resp = await (await fetchCircleCI(url, {...queryParams, 'page-token': resp.next_page_token})).json();
    items.push(...resp.items);
  }

  return items;
}

async function asyncFlatMap (arr, asyncFn) {
  return Promise.all(flatten(await asyncMap(arr, asyncFn)))
}

function asyncMap (arr, asyncFn) {
  return Promise.all(arr.map(asyncFn))
}

function flatMap (arr, fn) {
  return flatten(arr.map(fn))
}

function flatten (arr) {
  return [].concat(...arr)
}

(async () => {
  const pipelines = await getPipelines();
  const workflows = await asyncFlatMap(pipelines, pipeline => getWorkflows(pipeline.id));
  const jobs = (await asyncFlatMap(workflows, workflow => getJobs(workflow.id))).filter(job => job.status !== 'blocked');
  const tests = await asyncFlatMap(jobs, job => getTestsWithJobMetadata(job));


  fs.writeFile('circleci_tests.json', JSON.stringify(tests), (err) => { if (err) { console.log(err) }});

  console.log(`Pipelines: ${pipelines.length}`);
  console.log(`Workflows: ${workflows.length}`);
  console.log(`Jobs: ${jobs.length}`);
  console.log(`Tests: ${tests.length}`);

})();

