import * as fs from 'fs'
const fsp = fs.promises
import * as https from 'https'
import * as stream from 'stream'
import * as nu from 'util'
import * as zlib from 'zlib'

const omit = ks => obj =>
  Object.keys(obj).reduce((acc, k) => ks.includes(k) ? acc : ({...acc, [k]: obj[k]}), {})

const promiseEvent = eventName => x => new Promise(resolve => {
  x.on(eventName, a => resolve(a))
})

const quoteSingles = x => x.replace(/[']/g, '\\\'')
const quoteBackquotes = x => x.replace(/[`]/g, '\\`')
const formatObj = x => nu.inspect(x, {depth: Infinity, maxArrayLength: 10e3, breakLength: 100})

const jsonParse = s => {
  try { return [null, JSON.parse(s)] }
  catch (e) { return [e, null] }
}

const formatResBody = buf => {
  const s = buf.toString()
  const [e, obj] = jsonParse(s)
  return e ? [null, s] : [obj, null]
}

const formatHeaders = headers =>
  formatObj(
    omit(`alt-svc cache-control connection content-encoding content-length date
    server strict-transport-security transfer-encoding vary x-cloud-trace-context
    `.split(/\s+/))(headers)
  )

const record = async (req, res, resBody) => {
  const rdir = 'recordings'
  // URLs are related to each other first by path then by method, so the path should come first
  // so related URLs are sorted together alphabetically.
  const fname =
    req.index.toString().padStart(2, '0')+' '
    req.url.pathname.replace(/[/]/g, '|')+req.url.search+'.'+req.method.toLowerCase()
    +'.mjs'
  await fsp.mkdir(rdir, {recursive: true})
  const [rbObj, rbString] = formatResBody(resBody)
  const escapedBody = rbObj
    ? `JSON.stringify(\n${formatObj(rbObj)}\n)`
    : '`'+quoteBackquotes(rbString)+'`'
  const fileContents = `const matchReq = req =>
  req.url.pathname === '${quoteSingles(req.url.pathname)}'
  && req.url.search === '${quoteSingles(req.url.search)}'
  && req.method === '${req.method}'

const body = ${escapedBody}

const headers = ${formatHeaders(res.headers)}

const handleReq = (req, res) => {
  if (!matchReq(req)) { return }
  Object.keys(headers).forEach(h => res.setHeader(h, headers[h]))
  res.status(${res.statusCode}).mwrite(body).mend()
}
export default handleReq
`
  const fpath = rdir+'/'+fname
  await fsp.writeFile(fpath, fileContents)
  return fpath
}

const createSniffStream = () => new stream.Transform({
  construct(callback) {
    this.chunks = []
    callback()
  },
  transform(chunk, encoding, callback) {
    this.chunks.push(chunk)
    callback(null, chunk)
  }
})

const getMode = () => {
  const pmStr = process.env.PROXY_MODE
  return pmStr === 'record-only' ? {record: true, replay: false}
    : pmStr === 'replay-only' ? {record: false, replay: true}
    : {record: true, replay: true}
}

export const handleReq = async (req, res) => {
  if (getMode().replay) {
    const hdir = 'handlers'
    const hpaths = await fsp.readdir(hdir)
    for (let hp of hpaths.filter(p => p.endsWith('.mjs'))) {
      await import('../'+hdir+'/'+encodeURIComponent(hp)+'?'+Date.now())
        .then(m => m.default(req, res))
      if (res.writableEnded) {
        res.log('response via handler: '+hp)
        return
      }
    }
  }
  if (!getMode().record) {
    res.status(500).typeText().mwrite('Handler not found in replay-only mode.').mend()
    return
  }
  res.log('no handler responded. forwarding...')
  const targetUrl = new URL(req.url)
  targetUrl.hostname = 'api-dot-all-of-us-workbench-test.appspot.com'
  targetUrl.port = ''
  targetUrl.protocol = 'https:'
  const chunks = []
  const sniff = new stream.Transform({
    transform(chunk, encoding, callback) {
      chunks.push(chunk)
      callback(null, chunk)
    }
  })
  const treq = https.request(targetUrl,
    {method: req.method, headers: {...req.headers, host: targetUrl.hostname}})
  req.pipe(treq)
  return new Promise(resolve => {
    treq.on('response', tres => {
      res.status(tres.statusCode)
      Object.keys(tres.headers).forEach(h => res.setHeader(h, tres.headers[h]))
      const ss = createSniffStream()
      tres.headers['content-encoding'] === 'gzip'
        ? tres.pipe(zlib.createGunzip()).pipe(ss).pipe(zlib.createGzip()).pipe(res)
        : tres.pipe(ss).pipe(res)
      res.on('finish', async () => {
        const rpath = await record(req, tres, Buffer.concat(ss.chunks))
        res.log('response recorded: '+rpath)
        resolve()
      })
    })
  })
}
