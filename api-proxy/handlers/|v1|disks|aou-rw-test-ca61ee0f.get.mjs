const matchReq = req =>
  req.url.pathname === '/v1/disks/aou-rw-test-ca61ee0f'
  && req.url.search === ''
  && req.method === 'GET'

const body = JSON.stringify(
{
  message: 'Active PD with prefix all-of-us-pd-4451 not found in workspace aou-rw-test-ca61ee0f',
  statusCode: 404,
  errorClassName: 'org.pmiops.workbench.exceptions.NotFoundException',
  errorCode: null,
  errorUniqueId: '584d7b0f-6097-42d4-9160-1be80eab911b',
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
