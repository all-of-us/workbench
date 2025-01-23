const matchReq = req => req.method === 'OPTIONS'

const headers = {
  'access-control-allow-credentials': 'true',
  'access-control-allow-origin': '*',
  'access-control-allow-methods': 'GET, HEAD, POST, PUT, DELETE, PATCH, TRACE, OPTIONS',
  'access-control-allow-headers': 'Origin, X-Requested-With, Content-Type, Accept, Authorization',
  vary: 'Origin, Access-Control-Request-Method, Access-Control-Request-Headers',
  allow: 'GET, HEAD, POST, PUT, DELETE, TRACE, OPTIONS, PATCH',
  'content-type': 'text/html',
  server: 'Google Frontend'
}

const handleReq = (req, res) => {
  if (!matchReq(req)) { return }
  Object.keys(headers).forEach(h => res.setHeader(h, headers[h]))
  res.status(200).mend()
}
export default handleReq
