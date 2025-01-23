const matchReq = req =>
  req.url.pathname === '/v1/runtimes/aou-rw-test-ca61ee0f'
  && req.url.search === ''
  && req.method === 'GET'

const body = JSON.stringify(
{
  message: null,
  statusCode: 404,
  errorClassName: 'org.pmiops.workbench.exceptions.NotFoundException',
  errorCode: null,
  errorUniqueId: 'c0dc1e8c-0718-4305-8e70-8cd37e5901f6',
  parameters: null
}
)

const headers = {
  'access-control-allow-credentials': 'true',
  'access-control-allow-origin': '*',
  'access-control-allow-methods': 'GET, HEAD, POST, PUT, DELETE, PATCH, TRACE, OPTIONS',
  'access-control-allow-headers': 'Origin, X-Requested-With, Content-Type, Accept, Authorization',
  'content-type': 'application/json'
}

const handleReq = (req, res) => {
  if (!matchReq(req)) { return }
  Object.keys(headers).forEach(h => res.setHeader(h, headers[h]))
  res.status(404).mwrite(body).mend()
}
export default handleReq
