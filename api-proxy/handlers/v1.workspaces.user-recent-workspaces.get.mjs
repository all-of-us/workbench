const matchReq = req =>
  req.url.pathname === '/v1/workspaces/user-recent-workspaces'
  && req.url.search === ''
  && req.method === 'GET'

const body = JSON.stringify(
[
  {
    workspace: {
      id: 'mohstest',
      terraName: 'mohstest',
      etag: '"4"',
      name: 'Mohs Test',
      displayName: 'Mohs Test',
      namespace: 'aou-rw-test-53ff4756',
      cdrVersionId: '3',
      creator: 'dmohs@fake-research-aou.org',
      billingAccountName: 'billingAccounts/00293C-5DEA2D-6887E7',
      googleBucketName: null,
      accessTierShortName: 'registered',
      researchPurpose: {
        additionalNotes: null,
        approved: null,
        ancestry: false,
        anticipatedFindings: 'None.',
        commercialPurpose: false,
        controlSet: false,
        diseaseFocusedResearch: false,
        diseaseOfFocus: '',
        drugDevelopment: false,
        educational: false,
        intendedStudy: 'None.',
        scientificApproach: 'None.',
        methodsDevelopment: false,
        otherPopulationDetails: '',
        otherPurpose: true,
        otherPurposeDetails: 'Testing and development.',
        ethics: false,
        populationDetails: [],
        populationHealth: false,
        reasonForAllOfUs: '',
        reviewRequested: false,
        socialBehavioral: false,
        timeRequested: null,
        timeReviewed: null,
        disseminateResearchFindingList: [ 'OTHER' ],
        otherDisseminateResearchFindings: 'I will share my findings with the AoU engineering \nteam.',
        researchOutcomeList: [ 'NONE_APPLY' ],
        needsReviewPrompt: false
      },
      billingStatus: 'ACTIVE',
      creationTime: 1607460491000,
      lastModifiedTime: 1649424756000,
      published: false,
      adminLocked: false,
      adminLockedReason: null,
      googleProject: 'aou-rw-test-53ff4756'
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
