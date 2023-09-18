import {
  Degree,
  Education,
  Ethnicity,
  GenderIdentity,
  InstitutionalRole,
  OrganizationType,
  Race,
  SexAtBirth,
} from 'generated/fetch';

export const AccountCreationOptions = {
  degree: [
    { label: 'Ph.D.', value: Degree.PHD },
    { label: 'M.D.', value: Degree.MD },
    { label: 'J.D.', value: Degree.JD },
    { label: 'Ed.D.', value: Degree.EDD },
    { label: 'M.S.N.', value: Degree.MSN },
    { label: 'M.S.', value: Degree.MS },
    { label: 'M.A.', value: Degree.MA },
    { label: 'M.B.A.', value: Degree.MBA },
    { label: 'M.E.', value: Degree.ME },
    { label: 'M.S.W.', value: Degree.MSW },
    { label: 'M.P.H.', value: Degree.MPH },
    { label: 'B.A.', value: Degree.BA },
    { label: 'B.S.', value: Degree.BS },
    { label: 'B.S.N.', value: Degree.BSN },
  ],
  institutionalRoleOptions: [
    {
      label: 'Undergraduate (Bachelor level) student',
      value: InstitutionalRole.UNDERGRADUATE,
    },
    {
      label:
        'Graduate trainee (Current student in a Masters, PhD, or Medical school training program)',
      value: InstitutionalRole.TRAINEE,
    },
    {
      label:
        'Research fellow (a post-doctoral fellow or medical resident in training)',
      value: InstitutionalRole.FELLOW,
    },
    {
      label: 'Early career tenure-track researcher',
      value: InstitutionalRole.EARLY_CAREER,
    },
    {
      label: 'Mid-career tenured researcher',
      value: InstitutionalRole.MID_CAREER,
    },
    {
      label: 'Late career tenured researcher',
      value: InstitutionalRole.LATE_CAREER,
    },
    {
      label:
        'Project Personnel (eg: Research Assistant, Data Analyst, Project Manager, Research Coordinator, or other roles)',
      value: InstitutionalRole.PROJECT_PERSONNEL,
    },
    {
      label: 'Research Assistant (pre-doctoral)',
      value: InstitutionalRole.PRE_DOCTORAL,
    },
    {
      label: 'Research associate (post-doctoral; early/mid career)',
      value: InstitutionalRole.POST_DOCTORAL,
    },
    {
      label: 'Senior Researcher (PI/Team Lead, senior scientist)',
      value: InstitutionalRole.SENIOR_RESEARCHER,
    },
    { label: 'Teacher/Instructor/Professor', value: InstitutionalRole.TEACHER },
    { label: 'Student', value: InstitutionalRole.STUDENT },
    { label: 'Administrator', value: InstitutionalRole.ADMIN },
    { label: 'Other (free text)', value: InstitutionalRole.OTHER },
  ],
  institutionalRolesByOrganizationType: [
    {
      type: OrganizationType.ACADEMIC_RESEARCH_INSTITUTION,
      roles: [
        InstitutionalRole.UNDERGRADUATE,
        InstitutionalRole.TRAINEE,
        InstitutionalRole.FELLOW,
        InstitutionalRole.EARLY_CAREER,
        InstitutionalRole.MID_CAREER,
        InstitutionalRole.LATE_CAREER,
        InstitutionalRole.PROJECT_PERSONNEL,
        InstitutionalRole.OTHER,
      ],
    },
    {
      type: OrganizationType.INDUSTRY,
      // identical to the HEALTHCENTERNONPROFIT roles
      roles: [
        InstitutionalRole.PRE_DOCTORAL,
        InstitutionalRole.POST_DOCTORAL,
        InstitutionalRole.SENIOR_RESEARCHER,
        InstitutionalRole.PROJECT_PERSONNEL,
        InstitutionalRole.OTHER,
      ],
    },
    {
      type: OrganizationType.HEALTH_CENTER_NON_PROFIT,
      // identical to the INDUSTRY roles
      roles: [
        InstitutionalRole.PRE_DOCTORAL,
        InstitutionalRole.POST_DOCTORAL,
        InstitutionalRole.SENIOR_RESEARCHER,
        InstitutionalRole.PROJECT_PERSONNEL,
        InstitutionalRole.OTHER,
      ],
    },
    {
      type: OrganizationType.EDUCATIONAL_INSTITUTION,
      roles: [
        InstitutionalRole.TEACHER,
        InstitutionalRole.STUDENT,
        InstitutionalRole.ADMIN,
        InstitutionalRole.PROJECT_PERSONNEL,
        InstitutionalRole.OTHER,
      ],
    },
    {
      type: OrganizationType.OTHER,
      // display all roles for OTHER
      roles: Object.keys(InstitutionalRole).map((k) => InstitutionalRole[k]),
    },
  ],
  race: [
    { label: 'American Indian or Alaska Native (AI/AN)', value: Race.AIAN },
    { label: 'Black or African American', value: Race.AA },
    { label: 'Asian', value: Race.ASIAN },
    { label: 'Native Hawaiian or Other Pacific Islander', value: Race.NHOPI },
    { label: 'White', value: Race.WHITE },
    { label: 'None of these describe me', value: Race.NONE },
    { label: 'Prefer not to answer', value: Race.PREFER_NO_ANSWER },
  ],
  ethnicity: [
    { label: 'Hispanic or Latino', value: Ethnicity.HISPANIC },
    { label: 'Not Hispanic or Latino', value: Ethnicity.NOT_HISPANIC },
    { label: ' Prefer not to answer', value: Ethnicity.PREFER_NO_ANSWER },
  ],
  genderIdentity: [
    { label: 'Man', value: GenderIdentity.MAN },
    { label: 'Non-Binary', value: GenderIdentity.NON_BINARY },
    { label: 'Transgender', value: GenderIdentity.TRANSGENDER },
    { label: 'Woman', value: GenderIdentity.WOMAN },
    {
      label: 'None of these describe me',
      value: GenderIdentity.NONE_DESCRIBE_ME,
    },
    { label: 'Prefer not to answer', value: GenderIdentity.PREFER_NO_ANSWER },
  ],
  sexAtBirth: [
    { label: 'Female', value: SexAtBirth.FEMALE },
    { label: 'Intersex', value: SexAtBirth.INTERSEX },
    { label: 'Male', value: SexAtBirth.MALE },
    {
      label: 'None of these describe me',
      value: SexAtBirth.NONE_OF_THESE_DESCRIBE_ME,
    },
    { label: 'Prefer not to answer', value: SexAtBirth.PREFER_NO_ANSWER },
  ],
  levelOfEducation: [
    {
      label: 'Never attended school/no formal education',
      value: Education.NO_EDUCATION,
    },
    {
      label: 'Primary/Middle School/High School (Grades 1 through 12/GED)',
      value: Education.GRADES_1_12,
    },
    {
      label:
        'Some college, Associate Degree or ' +
        'Technical school (1 to 3 years), or current undergraduate student',
      value: Education.UNDERGRADUATE,
    },
    {
      label:
        'College graduate (4 years or more) or current post-graduate trainee',
      value: Education.COLLEGE_GRADUATE,
    },
    { label: 'Master’s degree', value: Education.MASTER },
    { label: 'Doctorate', value: Education.DOCTORATE },
    { label: 'Prefer not to answer', value: Education.PREFER_NO_ANSWER },
  ],
  Years: [],
};

const year = new Date().getFullYear();
const years = Array.from(new Array(100), (val, index) => ({
  label: year - index,
  value: year - index,
}));

AccountCreationOptions.Years = [
  { label: 'Prefer not to answer', value: 0 },
  ...years,
];
