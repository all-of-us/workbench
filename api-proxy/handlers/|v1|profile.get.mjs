const matchReq = req =>
  req.url.pathname === '/v1/profile'
  && req.url.search === ''
  && req.method === 'GET'

const body = JSON.stringify(
{
  userId: 165,
  username: 'dmohs@fake-research-aou.org',
  creationNonce: null,
  contactEmail: 'dmohs@broadinstitute.org',
  firstSignInTime: 1531763736000,
  accessTierShortNames: [ 'controlled', 'registered' ],
  tierEligibilities: [
    { accessTierShortName: 'registered', eligible: true, eraRequired: false },
    { accessTierShortName: 'controlled', eligible: true, eraRequired: false }
  ],
  degrees: [],
  givenName: 'David',
  familyName: 'Mohs',
  professionalUrl: null,
  authorities: [ 'DEVELOPER' ],
  pageVisits: [
    { page: 'moodle', firstVisit: 1607460051000 },
    { page: 'workspace', firstVisit: 1544206681000 },
    { page: 'notebook', firstVisit: 1544206701000 },
    { page: 'homepage', firstVisit: 1544206607000 }
  ],
  demographicSurveyCompletionTime: 1634150436000,
  disabled: false,
  areaOfResearch: 'asdf',
  verifiedInstitutionalAffiliation: {
    institutionShortName: 'Broad',
    institutionDisplayName: 'Broad Institute',
    institutionRequestAccessUrl: 'www.google.com',
    institutionalRoleEnum: 'PROJECT_PERSONNEL',
    institutionalRoleOtherText: null
  },
  demographicSurvey: {
    race: [],
    ethnicity: null,
    identifiesAsLgbtq: null,
    lgbtqIdentity: null,
    genderIdentityList: [],
    sexAtBirth: [],
    yearOfBirth: 0,
    education: null,
    disability: null
  },
  eraCommonsLinkedNihUsername: 'mohswashere',
  eraCommonsLinkExpireTime: 1610052034000,
  duccSignedVersion: 4,
  duccSignedInitials: 'DM',
  duccCompletionTimeEpochMillis: 1650380045000,
  address: {
    streetAddress1: '230 Forbush Mountain Dr',
    streetAddress2: null,
    zipCode: '27514',
    city: 'Chapel Hill',
    state: 'NC',
    country: 'USA'
  },
  freeTierUsage: 1.7914730000000008,
  freeTierDollarQuota: 300,
  latestTermsOfServiceVersion: 1,
  latestTermsOfServiceTime: 1650401853000,
  rasLinkUsername: null,
  accessModules: {
    modules: [
      {
        moduleName: 'ERA_COMMONS',
        completionEpochMillis: 1607460042000,
        expirationEpochMillis: null,
        bypassEpochMillis: null
      },
      {
        moduleName: 'TWO_FACTOR_AUTH',
        completionEpochMillis: 1556314341000,
        expirationEpochMillis: null,
        bypassEpochMillis: null
      },
      {
        moduleName: 'RAS_LINK_LOGIN_GOV',
        completionEpochMillis: null,
        expirationEpochMillis: null,
        bypassEpochMillis: 1643312974000
      },
      {
        moduleName: 'COMPLIANCE_TRAINING',
        completionEpochMillis: 1607460121000,
        expirationEpochMillis: 1922820121000,
        bypassEpochMillis: null
      },
      {
        moduleName: 'DATA_USER_CODE_OF_CONDUCT',
        completionEpochMillis: 1650380045000,
        expirationEpochMillis: 1965740045000,
        bypassEpochMillis: null
      },
      {
        moduleName: 'PROFILE_CONFIRMATION',
        completionEpochMillis: 1656614778000,
        expirationEpochMillis: 1971974778000,
        bypassEpochMillis: null
      },
      {
        moduleName: 'PUBLICATION_CONFIRMATION',
        completionEpochMillis: 1607460186000,
        expirationEpochMillis: 1922820186000,
        bypassEpochMillis: null
      },
      {
        moduleName: 'CT_COMPLIANCE_TRAINING',
        completionEpochMillis: null,
        expirationEpochMillis: null,
        bypassEpochMillis: 1634684187000
      }
    ],
    anyModuleHasExpired: false
  },
  demographicSurveyV2: {
    completionTime: 1656367904,
    ethnicCategories: [ 'AI_AN_ALASKA_NATIVE', 'AI_AN' ],
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
    sexAtBirth: 'PREFER_NOT_TO_ANSWER',
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
    education: 'PREFER_NOT_TO_ANSWER',
    disadvantaged: 'PREFER_NOT_TO_ANSWER',
    surveyComments: 'asdfg'
  },
  newUserSatisfactionSurveyEligibility: true,
  newUserSatisfactionSurveyEligibilityEndTime: "2023-01-10T14:38:10Z"
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
