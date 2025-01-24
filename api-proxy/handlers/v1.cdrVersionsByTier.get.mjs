const matchReq = req =>
  req.url.pathname === '/v1/cdrVersionsByTier'
  && req.url.search === ''
  && req.method === 'GET'

const body = JSON.stringify(
{
  tiers: [
    {
      accessTierShortName: 'controlled',
      accessTierDisplayName: 'Controlled Tier',
      defaultCdrVersionId: '5',
      defaultCdrVersionCreationTime: 1598400591000,
      versions: [
        {
          cdrVersionId: '5',
          name: 'Synthetic Dataset in the Controlled Tier',
          accessTierShortName: 'controlled',
          archivalStatus: 'LIVE',
          hasWgsData: true,
          hasFitbitData: true,
          hasCopeSurveyData: true,
          creationTime: 1598400591000
        }
      ]
    },
    {
      accessTierShortName: 'registered',
      accessTierDisplayName: 'Registered Tier',
      defaultCdrVersionId: '3',
      defaultCdrVersionCreationTime: 1569801600000,
      versions: [
        {
          cdrVersionId: '4',
          name: '[Removed] Synthetic Dataset v3 with Microarray',
          accessTierShortName: 'registered',
          archivalStatus: 'ARCHIVED',
          hasWgsData: false,
          hasFitbitData: true,
          hasCopeSurveyData: true,
          creationTime: 1598400591000
        },
        {
          cdrVersionId: '6',
          name: '[Removed] Synthetic Dataset v3 with WGS',
          accessTierShortName: 'registered',
          archivalStatus: 'ARCHIVED',
          hasWgsData: false,
          hasFitbitData: false,
          hasCopeSurveyData: false,
          creationTime: 1598400591000
        },
        {
          cdrVersionId: '2',
          name: 'Synthetic Dataset v2',
          accessTierShortName: 'registered',
          archivalStatus: 'LIVE',
          hasWgsData: false,
          hasFitbitData: true,
          hasCopeSurveyData: true,
          creationTime: 1569801600000
        },
        {
          cdrVersionId: '3',
          name: 'Synthetic Dataset v3',
          accessTierShortName: 'registered',
          archivalStatus: 'LIVE',
          hasWgsData: false,
          hasFitbitData: true,
          hasCopeSurveyData: true,
          creationTime: 1569801600000
        },
        {
          cdrVersionId: '1',
          name: '[Removed] Synthetic Dataset v1',
          accessTierShortName: 'registered',
          archivalStatus: 'ARCHIVED',
          hasWgsData: false,
          hasFitbitData: false,
          hasCopeSurveyData: false,
          creationTime: 1528243200000
        }
      ]
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
