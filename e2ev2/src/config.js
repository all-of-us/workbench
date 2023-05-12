const assert = require('assert').strict

const export_ = o => Object.keys(o).forEach(k => { module.exports[k] = o[k] })

const projectName = 'all-of-us-workbench-test'
export_({projectName})

const usernames = ['puppeteer-tester-8@fake-research-aou.org']
export_({usernames})

const urlRoot = () => {
  assert(
    process.env.UI_URL_ROOT,
    'UI_URL_ROOT not defined. Try: export UI_URL_ROOT=https://all-of-us-workbench-test.appspot.com'
  )
  return process.env.UI_URL_ROOT
}

export_({urlRoot})

