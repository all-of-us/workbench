const matchReq = req =>
  req.url.pathname === '/v1/workspaces/aou-rw-test-ca61ee0f/apps'
  && req.url.search === ''
  && req.method === 'GET'

const body = JSON.stringify(
{
  message: 'Workspace not found: aou-rw-test-ca61ee0f',
  statusCode: 404,
  errorClassName: 'org.pmiops.workbench.exceptions.NotFoundException',
  errorCode: null,
  errorUniqueId: '7e9bd131-e875-43cd-8d65-1b567bfc94af',
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
