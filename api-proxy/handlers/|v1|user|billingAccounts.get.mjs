const matchReq = req =>
  req.url.pathname === '/v1/user/billingAccounts'
  && req.url.search === ''
  && req.method === 'GET'

const body = JSON.stringify(
{
  billingAccounts: [
    {
      freeTier: true,
      name: 'billingAccounts/013713-75CFF6-1751E5',
      displayName: 'Use All of Us initial credits',
      isOpen: true
    }
  ]
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
  res.status(200).mwrite(body).mend()
}
export default handleReq
