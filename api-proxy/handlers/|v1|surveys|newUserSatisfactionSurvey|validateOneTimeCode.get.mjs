const matchReq = req =>
  req.url.pathname === '/v1/surveys/newUserSatisfactionSurvey/validateOneTimeCode/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee'
  && req.url.search === ''
  && req.method === 'GET'

const body = JSON.stringify(true)

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
