const matchReq = req =>
  req.url.pathname === '/v1/workspaces/aou-rw-test-53ff4756/mohstest'
  && req.method === 'GET'

const body = JSON.stringify(
  {
    workspace:{
      namespace:"aou-rw-local1-a312fa3d",
      name:"TestSpace",
      displayName:"TestSpace",
      id:"testspace",
      terraName:"testspace",
      etag:"\"10\"",
      cdrVersionId:"3",
      creator:"evrii-local@fake-research-aou.org",
      billingAccountName:"billingAccounts/013713-75CFF6-1751E5",
      googleBucketName:"fc-secure-8e743ccb-acf1-475e-9a5c-50409865a47b",
      accessTierShortName:"registered",
      researchPurpose:{
        additionalNotes:null,
        approved:null,
        ancestry:false,
        anticipatedFindings:"sadasddfgfdgty",
        commercialPurpose:true,
        controlSet:false,
        diseaseFocusedResearch:false,
        diseaseOfFocus:"",
        drugDevelopment:false,
        educational:true,
        intendedStudy:"assatryrtygfgdfg",
        scientificApproach:"saddsaefeft",
        methodsDevelopment:false,
        otherPopulationDetails:"",
        otherPurpose:false,
        otherPurposeDetails:"",
        ethics:false,
        populationDetails:[],
        populationHealth:false,
        reasonForAllOfUs:"",
        reviewRequested:false,
        socialBehavioral:false,
        timeRequested:null,
        timeReviewed:null,
        disseminateResearchFindingList:["SOCIAL_MEDIA"],
        otherDisseminateResearchFindings:"",
        researchOutcomeList:["PROMOTE_HEALTHY_LIVING"],
        needsReviewPrompt:false
      },
      billingStatus:"ACTIVE",
      creationTime:1657565804000,
      lastModifiedBy:null,
      lastModifiedTime:1659050858000,
      published:false,
      adminLocked:false,
      adminLockedReason:null,
      googleProject:"terra-vpc-sc-dev-fc90ffc7",
      initialCredits: {
        exhausted: false,
        expirationBypassed: true,
        expirationEpochMillis: 3,
        extensionEpochMillis: null
      }
    },
    accessLevel:"OWNER"
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
