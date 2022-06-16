const nu = require('util')

const mapValues =
  f => obj => Object.keys(obj).reduce((acc, k) => ({...acc, [k]: f(obj[k])}), {})
module.exports.mapValues = mapValues

const toPairs = xs => xs.reduce((acc, x) => {
  if (acc.length === 0) { acc.push([]) }
  const last = acc[acc.length - 1]
  if (last.length < 2) { last.push(x) } else { acc.push([x]) }
  return acc
}, [])
module.exports.toPairs = toPairs

function* range(start, end) {
  for (let i = start; i < end; ++i) {
    yield i;
  }
}
module.exports.range = range

const id = x => x
module.exports.id = id

const noop = () => {}
module.exports.noop = noop

const Right = x => ({
  map: f => Right(f(x)),
  chain: f => f(x),
  fold: (f, g) => g(x),
  [nu.inspect.custom]: (depth, options, inspect) =>
    `${options.stylize('Right', 'special')}<${nu.inspect(x, options)}>`,
})
module.exports.Right = Right

const Left = x => ({
  map: f => Left(x),
  chain: f => x,
  fold: (f, g) => f(x),
  [nu.inspect.custom]: (depth, options, inspect) =>
    `${options.stylize('Left', 'special')}<${nu.inspect(x, options)}>`,
})
module.exports.Left = Left

const Either = () => { throw 'use Left or Right' }
Either.fromNullable = x => (x ?? Right(x)) || Left(x)
module.exports.Either = Either

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
module.exports.Task = Task

const delay = ms => Task((rej, res) => setTimeout(res, ms))
module.exports.delay = delay

const traverse = (of, l, f) => {
  const acc = xs => x => [...xs, x]
  return l.reduce((xs_, x_) => xs_.map(acc).ap(f(x_)), of([]))
}
module.exports.traverse = traverse

