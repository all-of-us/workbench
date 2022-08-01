const assert = require('assert').strict

const export_ = o => Object.keys(o).forEach(k => { module.exports[k] = o[k] })

const projectName = 'all-of-us-workbench-test'
export_({projectName})

const usernames = ['puppeteer-tester-7@fake-research-aou.org']
export_({usernames})

const urlRoot = () => {
  assert(
    process.env.UI_HOSTNAME,
    'UI_HOSTNAME not defined. Try: export UI_HOSTNAME=all-of-us-workbench-test.appspot.com'
  )
  return `https://${process.env.UI_HOSTNAME}`
}

export_({urlRoot})

