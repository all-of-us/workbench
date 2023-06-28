const matchReq = req =>
  req.url.pathname === '/v1/google-account'
  && req.url.search === ''
  && req.method === 'POST'

const body = JSON.stringify(
{
  userId: 4476,
  username: 'test@fake-research-aou.org',
  creationNonce: '5271399413494123422',
  contactEmail: 'fake@broadinstitute.org',
  firstSignInTime: null,
  accessTierShortNames: [],
  tierEligibilities: [
    { accessTierShortName: 'registered', eligible: true, eraRequired: false },
    { accessTierShortName: 'controlled', eligible: true, eraRequired: false }
  ],
  degrees: [],
  givenName: 'Test',
  familyName: 'User',
  professionalUrl: null,
  authorities: [],
  pageVisits: [],
  demographicSurveyCompletionTime: null,
  disabled: false,
  areaOfResearch: 'Testing the system.',
  verifiedInstitutionalAffiliation: {
    institutionShortName: 'Broad',
    institutionDisplayName: 'Broad Institute',
    institutionRequestAccessUrl: 'www.google.com',
    institutionalRoleEnum: 'PROJECT_PERSONNEL',
    institutionalRoleOtherText: null
  },
  demographicSurvey: {
    race: null,
    ethnicity: null,
    identifiesAsLgbtq: null,
    lgbtqIdentity: null,
    genderIdentityList: [],
    sexAtBirth: null,
    yearOfBirth: 0,
    education: null,
    disability: null
  },
  eraCommonsLinkedNihUsername: null,
  eraCommonsLinkExpireTime: null,
  duccSignedVersion: null,
  duccSignedInitials: null,
  duccCompletionTimeEpochMillis: null,
  address: {
    streetAddress1: '123 Anytown Rd',
    streetAddress2: '',
    zipCode: '12345',
    city: 'Anytown',
    state: 'TN',
    country: 'United States of America'
  },
  freeTierUsage: null,
  freeTierDollarQuota: 300,
  latestTermsOfServiceVersion: 1,
  latestTermsOfServiceTime: 1666285094027,
  rasLinkUsername: null,
  accessModules: {
    modules: [
      {
        moduleName: 'PROFILE_CONFIRMATION',
        completionEpochMillis: 1666285093765,
        expirationEpochMillis: 1981645093765,
        bypassEpochMillis: null
      },
      {
        moduleName: 'PUBLICATION_CONFIRMATION',
        completionEpochMillis: 1666285093765,
        expirationEpochMillis: 1981645093765,
        bypassEpochMillis: null
      }
    ],
    anyModuleHasExpired: false
  },
  demographicSurveyV2: {
    completionTime: 1666285093.808,
    ethnicCategories: [ 'PREFER_NOT_TO_ANSWER' ],
    ethnicityAiAnOtherText: null,
    ethnicityAsianOtherText: null,
    ethnicityBlackOtherText: null,
    ethnicityHispanicOtherText: null,
    ethnicityMeNaOtherText: null,
    ethnicityNhPiOtherText: null,
    ethnicityWhiteOtherText: null,
    ethnicityOtherText: null,
    genderIdentities: [ 'PREFER_NOT_TO_ANSWER' ],
    genderOtherText: null,
    sexualOrientations: [ 'PREFER_NOT_TO_ANSWER' ],
    orientationOtherText: null,
    sexAtBirth: 'FEMALE',
    sexAtBirthOtherText: null,
    yearOfBirth: null,
    yearOfBirthPreferNot: true,
    disabilityHearing: 'PREFER_NOT_TO_ANSWER',
    disabilitySeeing: 'PREFER_NOT_TO_ANSWER',
    disabilityConcentrating: 'PREFER_NOT_TO_ANSWER',
    disabilityWalking: 'PREFER_NOT_TO_ANSWER',
    disabilityDressing: 'PREFER_NOT_TO_ANSWER',
    disabilityErrands: 'PREFER_NOT_TO_ANSWER',
    disabilityOtherText: null,
    education: 'NO_EDUCATION',
    disadvantaged: 'PREFER_NOT_TO_ANSWER',
    surveyComments: null
  }
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
