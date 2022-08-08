const matchReq = req =>
  req.url.pathname === '/v2/workspaces/aou-rw-test-ca61ee0f/testwsshare202207311932/resources'
  && req.url.search === '?resourceTypesToFetch=COHORT%2CCOHORT_REVIEW%2CCONCEPT_SET%2CDATASET'
  && req.method === 'OPTIONS'

const body = ``

const headers = {
  'access-control-allow-credentials': 'true',
  'access-control-allow-origin': '*',
  'access-control-allow-methods': 'GET, HEAD, POST, PUT, DELETE, PATCH, TRACE, OPTIONS',
  'access-control-allow-headers': 'Origin, X-Requested-With, Content-Type, Accept, Authorization',
  allow: 'GET, HEAD, POST, PUT, DELETE, TRACE, OPTIONS, PATCH',
  'content-type': 'text/html'
}

const handleReq = (req, res) => {
  if (!matchReq(req)) { return }
  Object.keys(headers).forEach(h => res.setHeader(h, headers[h]))
  res.status(200).mwrite(body).mend()
}
export default handleReq
