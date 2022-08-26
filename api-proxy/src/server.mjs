import * as http from 'http'
import createLogger from './logging.mjs'
import {handleReq} from './reqhandler.mjs'

const nl = '\n'

const parseUrl = (protocol, req) => {
  const {host} = req.headers
  if (!host) {
    throw Err('no-host', 'missing Host header', {status: 400})
  }
  if (host.startsWith(protocol+'://')) {
    return new URL(req.url, host)
  } else {
    return new URL(req.url, protocol+'://'+host)
  }
}


export const createReqHandler = handleReq => {
  let requestIndex = 0
  const {log} = createLogger()
  return async (req, res) => {
    req.startTimeMs = Date.now()
    req.index = requestIndex++
    res.index = req.index
    req.log = (...formatArgs) => {
      const [f, ...args] = formatArgs
      log('#%d '+f, req.index, ...args)
    }
    res.log = req.log
    const protocol = req.socket.encrypted ? 'https' : 'http'
    req.url = parseUrl(protocol, req)
    req.log('req %s %s %s',
      req.socket.remoteAddress, req.method, req.url
    )
    try {
      await handleReq(req, res)
      if (!res.writableEnded) {
        throw new Error('response left open on req '+req.index)
      }
    } catch (err) {
      res.err = err
      if (!err.status) {
        console.error(err)
      }
      if (!res.headersSent) {
        res.status(err.status || 500).mwrite(err.message+nl)
      } else {
        res.mwrite(`\nERROR{{${err.toString()}}}\n`)
      }
      res.mend()
    }
    res.log('res %d %sms %s',
      res.statusCode, Date.now() - req.startTimeMs, res.err ? res.err.message : ''
    )
  }
}

class MIncomingMessage extends http.IncomingMessage {}

class MServerResponse extends http.ServerResponse {
  status(code) { this.statusCode = code; return this }
  header(h, v) { this.setHeader(h, v); return this }
  type(type) { return this.header('content-type', `${type}; charset=UTF-8`) }
  typeText() { return this.type('text/plain') }
  typeHtml() { return this.type('text/html') }
  typeJs() { return this.type('text/javascript') }
  mwrite(s) { this.write(s); return this }
  mend() {
    if (this.writableEnded) { throw new Error('response already ended') }
    this.end()
    return this
  }
  httpsRedirect() { return res.status(301).header('location', req.url.href).mend() }
  notFound() { return res.status(404).typeText().mwrite('Path not found.\n').mend() }
  methodNotAllowed(allowedMethods) {
    return res.status(405).header('Allow', allowedMethods(', ')).typeText()
      .mwrite(req.method+' method not supported at this path.\n').mend()
  }
}

export const create = () => {
  const server = http.createServer({
    IncomingMessage: MIncomingMessage, ServerResponse: MServerResponse})
  server.on('listening', () =>
    console.log(`server listening on port ${server.address().port}.`))
  server.on('close', () => console.log('server closed.'))
  return server
}

export const startServer = async port => {
  const server = create()
  server.listen(port)
  server.on('request', createReqHandler(async (req, res) => {
    return handleReq(req, res)
  }))
}
