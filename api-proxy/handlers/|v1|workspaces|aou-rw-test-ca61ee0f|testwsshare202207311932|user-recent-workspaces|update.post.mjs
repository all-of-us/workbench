const matchReq = req =>
  req.url.pathname === '/v1/workspaces/aou-rw-test-ca61ee0f/testwsshare202207311932/user-recent-workspaces/update'
  && req.url.search === ''
  && req.method === 'POST'

const body = JSON.stringify(
[
  {
    workspace: {
      id: 'testwsshare202207311932',
      terraName: 'testwsshare202207311932',
      etag: '"1"',
      name: 'test-ws-share-202207311932',
      displayName: 'test-ws-share-202207311932',
      namespace: 'aou-rw-test-ca61ee0f',
      cdrVersionId: '3',
      creator: 'puppeteer-tester-7@fake-research-aou.org',
      billingAccountName: 'billingAccounts/013713-75CFF6-1751E5',
      googleBucketName: null,
      accessTierShortName: 'registered',
      researchPurpose: {
        additionalNotes: null,
        approved: null,
        ancestry: false,
        anticipatedFindings: 'What, if anything, is broken.',
        commercialPurpose: false,
        controlSet: false,
        diseaseFocusedResearch: false,
        diseaseOfFocus: '',
        drugDevelopment: false,
        educational: true,
        intendedStudy: 'Does this work?',
        scientificApproach: 'Automated regression testing.',
        methodsDevelopment: false,
        otherPopulationDetails: '',
        otherPurpose: false,
        otherPurposeDetails: '',
        ethics: false,
        populationDetails: [],
        populationHealth: false,
        reasonForAllOfUs: '',
        reviewRequested: false,
        socialBehavioral: false,
        timeRequested: null,
        timeReviewed: null,
        disseminateResearchFindingList: [ 'OTHER' ],
        otherDisseminateResearchFindings: 'Team test reports.',
        researchOutcomeList: [ 'NONE_APPLY' ],
        needsReviewPrompt: false
      },
      billingStatus: 'ACTIVE',
      creationTime: 1659295983000,
      lastModifiedTime: 1659295983000,
      published: false,
      adminLocked: false,
      adminLockedReason: null,
      googleProject: 'terra-vpc-sc-dev-99b6969b'
    },
    accessLevel: 'OWNER'
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
