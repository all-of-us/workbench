const assert = require('assert').strict

const export_ = o => Object.keys(o).forEach(k => { module.exports[k] = o[k] })

const projectName = 'all-of-us-workbench-test'
export_({projectName})

const usernames = ['puppeteer-tester-6@fake-research-aou.org']
export_({usernames})

const urlRoot = () => {
  assert(process.env.SHORT_HASH, 'SHORT_HASH is not defined')
  `https://pr-${process.env.SHORT_HASH}-dot-${projectName}.appspot.com`
}
export_({urlRoot})

