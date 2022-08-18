const matchReq = req =>
  req.url.pathname === '/v1/workspaces/user-recent-resources'
  && req.url.search === ''
  && req.method === 'GET'

const body = JSON.stringify(
[
  {
    workspaceId: 39345,
    workspaceNamespace: 'aou-rw-test-53ff4756',
    workspaceFirecloudName: 'mohstest',
    workspaceBillingStatus: 'ACTIVE',
    cdrVersionId: '3',
    accessTierShortName: 'registered',
    permission: 'OWNER',
    cohort: null,
    cohortReview: null,
    notebook: {
      name: 'SnakeLang.ipynb',
      path: 'gs://fc-secure-3c9c3712-c42e-4e7f-9ec3-34afdd711880/notebooks/',
      lastModifiedTime: null,
      sizeInBytes: null
    },
    conceptSet: null,
    dataSet: null,
    lastModifiedEpochMillis: 1653597052000,
    adminLocked: false
  }
]
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
  res.status(200).mwrite(body).mend()
}
export default handleReq
