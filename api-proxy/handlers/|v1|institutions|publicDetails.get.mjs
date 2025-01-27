const matchReq = req =>
  req.url.pathname === '/v1/institutions/publicDetails'
  && req.url.search === ''
  && req.method === 'GET'

const body = JSON.stringify(
{
  institutions: [
    {
      shortName: 'VUMC',
      displayName: 'Vanderbilt University Medical Center',
      organizationTypeEnum: 'HEALTH_CENTER_NON_PROFIT',
      registeredTierMembershipRequirement: 'DOMAINS',
      requestAccessUrl: null
    },
    {
      shortName: 'Broad',
      displayName: 'Broad Institute',
      organizationTypeEnum: 'ACADEMIC_RESEARCH_INSTITUTION',
      registeredTierMembershipRequirement: 'DOMAINS',
      requestAccessUrl: 'www.google.com'
    },
    {
      shortName: 'Verily',
      displayName: 'Verily LLC',
      organizationTypeEnum: 'INDUSTRY',
      registeredTierMembershipRequirement: 'DOMAINS',
      requestAccessUrl: null
    },
    {
      shortName: 'NIH',
      displayName: 'National Institute of Health',
      organizationTypeEnum: 'ACADEMIC_RESEARCH_INSTITUTION',
      registeredTierMembershipRequirement: 'DOMAINS',
      requestAccessUrl: null
    },
    {
      shortName: 'Wondros',
      displayName: 'Wondros',
      organizationTypeEnum: 'INDUSTRY',
      registeredTierMembershipRequirement: 'DOMAINS',
      requestAccessUrl: null
    },
    {
      shortName: 'Columbia',
      displayName: 'Columbia University',
      organizationTypeEnum: 'ACADEMIC_RESEARCH_INSTITUTION',
      registeredTierMembershipRequirement: 'DOMAINS',
      requestAccessUrl: null
    },
    {
      shortName: 'FSU',
      displayName: 'Florida State University',
      organizationTypeEnum: 'ACADEMIC_RESEARCH_INSTITUTION',
      registeredTierMembershipRequirement: 'DOMAINS',
      requestAccessUrl: null
    },
    {
      shortName: 'Yale',
      displayName: 'Yale University',
      organizationTypeEnum: 'OTHER',
      registeredTierMembershipRequirement: 'DOMAINS',
      requestAccessUrl: null
    },
    {
      shortName: 'UNC',
      displayName: ' University of North Carolina',
      organizationTypeEnum: 'ACADEMIC_RESEARCH_INSTITUTION',
      registeredTierMembershipRequirement: 'DOMAINS',
      requestAccessUrl: ''
    },
    {
      shortName: 'Pennsylvania',
      displayName: 'University of Pennsylvania',
      organizationTypeEnum: 'ACADEMIC_RESEARCH_INSTITUTION',
      registeredTierMembershipRequirement: 'DOMAINS',
      requestAccessUrl: null
    },
    {
      shortName: 'Harvard',
      displayName: 'Harvard University',
      organizationTypeEnum: 'ACADEMIC_RESEARCH_INSTITUTION',
      registeredTierMembershipRequirement: 'DOMAINS',
      requestAccessUrl: null
    },
    {
      shortName: 'Washington',
      displayName: 'University of Washington',
      organizationTypeEnum: 'ACADEMIC_RESEARCH_INSTITUTION',
      registeredTierMembershipRequirement: 'DOMAINS',
      requestAccessUrl: null
    },
    {
      shortName: 'UCDenver',
      displayName: 'University of Colorado - Denver',
      organizationTypeEnum: 'ACADEMIC_RESEARCH_INSTITUTION',
      registeredTierMembershipRequirement: 'DOMAINS',
      requestAccessUrl: null
    },
    {
      shortName: 'UMassMed',
      displayName: 'University of Massachusetts Medical School',
      organizationTypeEnum: 'ACADEMIC_RESEARCH_INSTITUTION',
      registeredTierMembershipRequirement: 'DOMAINS',
      requestAccessUrl: null
    },
    {
      shortName: 'BWH',
      displayName: "Brigham and Women's Hospital",
      organizationTypeEnum: 'ACADEMIC_RESEARCH_INSTITUTION',
      registeredTierMembershipRequirement: 'DOMAINS',
      requestAccessUrl: null
    },
    {
      shortName: 'Google',
      displayName: 'Google',
      organizationTypeEnum: 'INDUSTRY',
      registeredTierMembershipRequirement: 'ADDRESSES',
      requestAccessUrl: null
    },
    {
      shortName: 'Google2',
      displayName: 'Google Joel Test 2',
      organizationTypeEnum: 'INDUSTRY',
      registeredTierMembershipRequirement: 'ADDRESSES',
      requestAccessUrl: ''
    },
    {
      shortName: 'Dummyinstitution1321',
      displayName: 'Dummy institution for JoelT',
      organizationTypeEnum: 'OTHER',
      registeredTierMembershipRequirement: 'ADDRESSES',
      requestAccessUrl: 'google.com'
    },
    {
      shortName: 'Dummymaster451',
      displayName: 'Dummy master',
      organizationTypeEnum: 'ACADEMIC_RESEARCH_INSTITUTION',
      registeredTierMembershipRequirement: 'DOMAINS',
      requestAccessUrl: null
    },
    {
      shortName: 'ZSpace1891',
      displayName: 'Z Space time',
      organizationTypeEnum: 'HEALTH_CENTER_NON_PROFIT',
      registeredTierMembershipRequirement: 'DOMAINS',
      requestAccessUrl: ''
    },
    {
      shortName: 'AouOps',
      displayName: 'Aou Ops with the magic shortName for testing',
      organizationTypeEnum: 'OTHER',
      registeredTierMembershipRequirement: 'DOMAINS',
      requestAccessUrl: null
    },
    {
      shortName: 'RW-61072251',
      displayName: 'RW-6107',
      organizationTypeEnum: 'EDUCATIONAL_INSTITUTION',
      registeredTierMembershipRequirement: 'ADDRESSES',
      requestAccessUrl: null
    },
    {
      shortName: 'Admin_ui_testing_14161',
      displayName: 'Admin testing',
      organizationTypeEnum: 'OTHER',
      registeredTierMembershipRequirement: 'ADDRESSES',
      requestAccessUrl: ''
    },
    {
      shortName: 'UniversityofNorthCarolina2591',
      displayName: 'University of North Carolina',
      organizationTypeEnum: 'EDUCATIONAL_INSTITUTION',
      registeredTierMembershipRequirement: 'DOMAINS',
      requestAccessUrl: null
    },
    {
      shortName: 'Institutiontesting1201',
      displayName: 'Institution testing',
      organizationTypeEnum: 'INDUSTRY',
      registeredTierMembershipRequirement: 'DOMAINS',
      requestAccessUrl: null
    },
    {
      shortName: 'Anewinstitution4061',
      displayName: 'A new institution',
      organizationTypeEnum: 'INDUSTRY',
      registeredTierMembershipRequirement: null,
      requestAccessUrl: null
    },
    {
      shortName: 'thisoneisfine2311',
      displayName: 'this one is fine',
      organizationTypeEnum: 'INDUSTRY',
      registeredTierMembershipRequirement: 'DOMAINS',
      requestAccessUrl: null
    },
    {
      shortName: 'LocalTestSep221111',
      displayName: 'Local Test Sep 22',
      organizationTypeEnum: 'HEALTH_CENTER_NON_PROFIT',
      registeredTierMembershipRequirement: 'DOMAINS',
      requestAccessUrl: null
    },
    {
      shortName: 'MoodleDevelopmentTeam1861',
      displayName: 'Moodle Development Team',
      organizationTypeEnum: 'OTHER',
      registeredTierMembershipRequirement: 'DOMAINS',
      requestAccessUrl: null
    },
    {
      shortName: 'period1test111431',
      displayName: 'period1test.1.1',
      organizationTypeEnum: 'ACADEMIC_RESEARCH_INSTITUTION',
      registeredTierMembershipRequirement: 'DOMAINS',
      requestAccessUrl: null
    },
    {
      shortName: 'NewTest2911',
      displayName: 'New Test',
      organizationTypeEnum: 'EDUCATIONAL_INSTITUTION',
      registeredTierMembershipRequirement: 'DOMAINS',
      requestAccessUrl: null
    },
    {
      shortName: 'Institutiontestdataaccess941',
      displayName: 'Institution test data access',
      organizationTypeEnum: 'OTHER',
      registeredTierMembershipRequirement: 'ADDRESSES',
      requestAccessUrl: null
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
