const assert = require('assert').strict
const fs = require('fs')
const https = require('https')

const export_ = o => Object.keys(o).forEach(k => { module.exports[k] = o[k] })

const mapValues =
  f => obj => Object.keys(obj).reduce((acc, k) => ({...acc, [k]: f(obj[k])}), {})
export_({mapValues})

const toPairs = xs => xs.reduce((acc, x) => {
  if (acc.length === 0) { acc.push([]) }
  const last = acc[acc.length - 1]
  if (last.length < 2) { last.push(x) } else { acc.push([x]) }
  return acc
}, [])
export_({toPairs})

function* range(start, end) {
  for (let i = start; i < end; ++i) {
    yield i;
  }
}
export_({range})

const id = x => x
export_({id})

const noop = () => {}
export_({noop})

const Right = x => ({
  map: f => Right(f(x)),
  chain: f => f(x),
  fold: (f, g) => g(x),
})
export_({Right})

const Left = x => ({
  map: f => Left(x),
  chain: f => x,
  fold: (f, g) => f(x),
})
export_({Left})

const Either = () => { throw 'use Left or Right' }
Either.fromNullable = x => (x ?? Right(x)) || Left(x)
export_({Either})

const Task = fork => ({
  fork,
  forkOut: () => fork(console.error, console.log),
  toPromise: () => new Promise((resolve, reject) => fork(reject, resolve)),
  map: f => Task((rej, res) => fork(e => rej(e), x => res(f(x)))),
  chain: f => Task((rej, res) => fork(e => rej(e), x => f(x).fork(rej, res))),
  fold: (f, g) => Task((rej, res) => fork(e => res(f(e)), x => res(g(x)))),
  ap: vt => Task((rej, res) => fork(rej, f => vt.fork(rej, v => res(f(v))))),
})
Task.of = x => Task((rej, res) => res(x))
Task.rejected = e => Task((rej, res) => rej(e))
Task.fromEither = e => e.fold(Task.rejected, Task.of)
export_({Task})

const delay = ms => Task((rej, res) => setTimeout(res, ms))
export_({delay})
const delayP = ms => new Promise(resolve => setTimeout(resolve, ms))
export_({delayP})

const traverse = (of, l, f) => {
  const acc = xs => x => [...xs, x]
  return l.reduce((xs_, x_) => xs_.map(acc).ap(f(x_)), of([]))
}
export_({traverse})

const promiseEvent = (eventName, x) =>
  new Promise(resolve => x.on(eventName, (...args) => resolve(...args)))
export_({promiseEvent})

const readFile = (path, options) =>
  Task((rej, res) => fs.readFile(path, options, (err, data) => err ? rej(err) : res(data)))
export_({readFile})

const parseJson = s => {
  try { return Right(JSON.parse(s)) } catch (e) { e.string = s; return Left(e) }
}
export_({parseJson})

const httpReq = (url, options) => Task((rej, res) => {
  const req = https.request(url, options, response => {
    if (options.rejectNotOkay && (response.statusCode < 200 || response.statusCode >= 300)) {
      return rej(new Error(`${url} responded with status ${response.statusCode}`))
    }
    res(response)
  })
  if (options.body) { req.write(options.body) }
  req.end()
})
export_({httpReq})

const readStream = s => Task((rej, res) => {
  const chunks = []
  s.on('data', chunk => chunks.push(chunk))
  s.on('end', () => res(Buffer.concat(chunks)))
})
export_({readStream})

const denseDateTime = () =>
  (new Date()).toISOString().slice(0, 'XXXX-XX-XXTXX:XX'.length).replace(/[-T:]/g, '')
export_({denseDateTime})

// Currently, we don't mock Leo requests so the setCookie request always fails. This hack dismisses the error modal
// and should be removed when we mock Leo requests.
const dismissLeoAuthErrorModal = (page) => {
  return page.waitForXPath("//div[@role='dialog']//div[@role='button'][contains(., 'OK')]").then(b => b.click());
}
export_({dismissLeoAuthErrorModal})
