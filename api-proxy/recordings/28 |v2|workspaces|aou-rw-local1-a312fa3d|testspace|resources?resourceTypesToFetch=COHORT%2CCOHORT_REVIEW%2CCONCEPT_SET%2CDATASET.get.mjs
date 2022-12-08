const matchReq = req =>
  req.url.pathname === '/v2/workspaces/aou-rw-local1-a312fa3d/testspace/resources'
  && req.url.search === '?resourceTypesToFetch=COHORT%2CCOHORT_REVIEW%2CCONCEPT_SET%2CDATASET'
  && req.method === 'GET'

const body = JSON.stringify(
{
  message: null,
  statusCode: 500,
  errorClassName: 'org.pmiops.workbench.exceptions.ServerErrorException',
  errorCode: null,
  errorUniqueId: 'f7b2a3fa-a95e-446a-9d89-6370149eafb5',
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
  res.status(500).mwrite(body).mend()
}
export default handleReq
