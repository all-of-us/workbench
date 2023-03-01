const matchReq = req =>
  req.url.pathname === '/v1/config'
  && req.url.search === ''
  && req.method === 'GET'

const body = JSON.stringify(
{
  gsuiteDomain: 'fake-research-aou.org',
  projectId: 'all-of-us-workbench-test',
  firecloudURL: 'https://firecloud-orchestration.dsde-dev.broadinstitute.org',
  publicApiKeyForErrorReports: 'AIzaSyDPoX4Eg7-_FVKi7JFzEKaJpZ4IMRLaER4',
  shibbolethUiBaseUrl: 'https://broad-shibboleth-prod.appspot.com/dev',
  enableComplianceTraining: true,
  complianceTrainingHost: 'aoudev.nnlm.gov',
  enableEraCommons: true,
  unsafeAllowSelfBypass: true,
  defaultFreeCreditsDollarLimit: 300,
  enableEventDateModifier: false,
  enableResearchReviewPrompt: false,
  enableRasLoginGovLinking: true,
  enforceRasLoginGovLinking: true,
  enableGenomicExtraction: true,
  enableGpu: true,
  enablePersistentDisk: true,
  enableUniversalSearch: true,
  enableMultiReview: true,
  enableDrugWildcardSearch: false,
  rasHost: 'https://stsstg.nih.gov',
  accessRenewalLookback: 330,
  complianceTrainingRenewalLookback: 30,
  rasClientId: 'e5c5d714-d597-48c8-b564-a249d729d0c9',
  rasLogoutUrl: 'https://authtest.nih.gov/siteminderagent/smlogoutredirector.asp?TARGET=',
  runtimeImages: [],
  freeTierBillingAccountId: '013713-75CFF6-1751E5',
  accessModules: [
    {
      name: 'ERA_COMMONS',
      expirable: false,
      bypassable: true,
      requiredForRTAccess: false,
      requiredForCTAccess: false
    },
    {
      name: 'TWO_FACTOR_AUTH',
      expirable: false,
      bypassable: true,
      requiredForRTAccess: true,
      requiredForCTAccess: true
    },
    {
      name: 'RAS_LINK_LOGIN_GOV',
      expirable: false,
      bypassable: true,
      requiredForRTAccess: true,
      requiredForCTAccess: true
    },
    {
      name: 'COMPLIANCE_TRAINING',
      expirable: true,
      bypassable: true,
      requiredForRTAccess: true,
      requiredForCTAccess: true
    },
    {
      name: 'DATA_USER_CODE_OF_CONDUCT',
      expirable: true,
      bypassable: true,
      requiredForRTAccess: true,
      requiredForCTAccess: true
    },
    {
      name: 'PROFILE_CONFIRMATION',
      expirable: true,
      bypassable: false,
      requiredForRTAccess: true,
      requiredForCTAccess: true
    },
    {
      name: 'PUBLICATION_CONFIRMATION',
      expirable: true,
      bypassable: false,
      requiredForRTAccess: true,
      requiredForCTAccess: true
    },
    {
      name: 'CT_COMPLIANCE_TRAINING',
      expirable: true,
      bypassable: true,
      requiredForRTAccess: false,
      requiredForCTAccess: true
    }
  ],
  currentDuccVersions: [ 3, 4, 5 ],
  enableUpdatedDemographicSurvey: true
}
)

const headers = {
  'access-control-allow-credentials': 'true',
  'access-control-allow-origin': '*',
  'access-control-allow-methods': 'GET, HEAD, POST, PUT, DELETE, PATCH, TRACE, OPTIONS',
  'access-control-allow-headers': 'Origin, X-Requested-With, Content-Type, Accept, Authorization',
  'content-type': 'application/json',
  server: 'Google Frontend'
}

const handleReq = (req, res) => {
  if (!matchReq(req)) { return }
  Object.keys(headers).forEach(h => res.setHeader(h, headers[h]))
  res.status(200).mwrite(body).mend()
}
export default handleReq
