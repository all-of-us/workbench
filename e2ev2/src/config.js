const assert = require('assert').strict

const export_ = o => Object.keys(o).forEach(k => { module.exports[k] = o[k] })

const projectName = 'all-of-us-workbench-test'
export_({projectName})

const usernames = ['puppeteer-tester-7@fake-research-aou.org']
export_({usernames})

const urlRoot = () => `https://${process.env.UI_HOSTNAME}`

export_({urlRoot})

