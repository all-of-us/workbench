import {
  Degree,
  Education,
  Ethnicity,
  GenderIdentity,
  InstitutionalRole,
  OrganizationType,
  Race,
  SexAtBirth
} from 'generated/fetch';

export const AccountCreationOptions = {
  degree: [
    {label: 'Ph.D.', value: Degree.PHD},
    {label: 'M.D.', value: Degree.MD},
    {label: 'J.D.', value: Degree.JD},
    {label: 'Ed.D.', value: Degree.EDD},
    {label: 'M.S.N.', value: Degree.MSN},
    {label: 'M.S.', value: Degree.MS},
    {label: 'M.A.', value: Degree.MA},
    {label: 'M.B.A.', value: Degree.MBA},
    {label: 'M.E.', value: Degree.ME},
    {label: 'M.S.W.', value: Degree.MSW},
    {label: 'M.P.H.', value: Degree.MPH},
    {label: 'B.A.', value: Degree.BA},
    {label: 'B.S.', value: Degree.BS},
    {label: 'B.S.N.', value: Degree.BSN},
  ],
  institutionalRoleOptions: [
    {label: `Undergraduate (Bachelor level) student`,
      value: InstitutionalRole.UNDERGRADUATE},
    {label: `Graduate trainee (Current student in a Masters, PhD, or Medical school training program)`,
      value: InstitutionalRole.TRAINEE},
    {label: `Research fellow (a post-doctoral fellow or medical resident in training)`,
      value: InstitutionalRole.FELLOW},
    {label: `Early career tenure-track researcher`,
      value: InstitutionalRole.EARLYCAREER},
    {label: `Mid-career tenured researcher`,
      value: InstitutionalRole.MIDCAREER},
    {label: `Late career tenured researcher`,
      value: InstitutionalRole.LATECAREER},
    {label: `Project Personnel (eg: Research Assistant, Data Analyst, Project Manager, Research Coordinator, or other roles)`,
      value: InstitutionalRole.PROJECTPERSONNEL},
    {label: 'Research Assistant (pre-doctoral)',
      value: InstitutionalRole.PREDOCTORAL},
    {label: 'Research associate (post-doctoral; early/mid career)',
      value: InstitutionalRole.POSTDOCTORAL},
    {label: 'Senior Researcher (PI/Team Lead, senior scientist)',
      value: InstitutionalRole.SENIORRESEARCHER},
    {label: 'Teacher/Instructor/Professor',
      value: InstitutionalRole.TEACHER},
    {label: 'Student',
      value: InstitutionalRole.STUDENT},
    {label: 'Administrator',
      value: InstitutionalRole.ADMIN},
    {label: 'Other (free text)',
      value: InstitutionalRole.OTHER},
  ],
  institutionalRolesByOrganizationType: [
    {type: OrganizationType.ACADEMICRESEARCHINSTITUTION,
      roles: [
        InstitutionalRole.UNDERGRADUATE,
        InstitutionalRole.TRAINEE,
        InstitutionalRole.FELLOW,
        InstitutionalRole.EARLYCAREER,
        InstitutionalRole.MIDCAREER,
        InstitutionalRole.LATECAREER,
        InstitutionalRole.PROJECTPERSONNEL,
        InstitutionalRole.OTHER,
      ]},
    {type: OrganizationType.INDUSTRY,
      // identical to the HEALTHCENTERNONPROFIT roles
      roles: [
        InstitutionalRole.PREDOCTORAL,
        InstitutionalRole.POSTDOCTORAL,
        InstitutionalRole.SENIORRESEARCHER,
        InstitutionalRole.PROJECTPERSONNEL,
        InstitutionalRole.OTHER,
      ]},
    {type: OrganizationType.HEALTHCENTERNONPROFIT,
      // identical to the INDUSTRY roles
      roles: [
        InstitutionalRole.PREDOCTORAL,
        InstitutionalRole.POSTDOCTORAL,
        InstitutionalRole.SENIORRESEARCHER,
        InstitutionalRole.PROJECTPERSONNEL,
        InstitutionalRole.OTHER,
      ]},
    {type: OrganizationType.EDUCATIONALINSTITUTION,
      roles: [
        InstitutionalRole.TEACHER,
        InstitutionalRole.STUDENT,
        InstitutionalRole.ADMIN,
        InstitutionalRole.PROJECTPERSONNEL,
        InstitutionalRole.OTHER,
      ]},
    {type: OrganizationType.OTHER,
      // display all roles for OTHER
      roles: Object.keys(InstitutionalRole).map(k => InstitutionalRole[k])}
  ],
  race: [
    {label: `American Indian or Alaska Native (AI/AN)`, value: Race.AIAN},
    {label: `Black or African American`, value: Race.AA},
    {label: `Asian`, value: Race.ASIAN},
    {label: `Native Hawaiian or Other Pacific Islander`, value: Race.NHOPI},
    {label: `White`, value: Race.WHITE},
    {label: `None of these describe me`, value: Race.NONE},
    {label: `Prefer not to answer`, value: Race.PREFERNOANSWER},
  ],
  ethnicity: [
    {label: `Hispanic or Latino`, value: Ethnicity.HISPANIC},
    {label: `Not Hispanic or Latino`, value: Ethnicity.NOTHISPANIC},
    {label: ` Prefer not to answer`, value: Ethnicity.PREFERNOANSWER}
  ],
  genderIdentity: [
    {label: 'Man', value: GenderIdentity.MAN},
    {label: 'Non-Binary', value: GenderIdentity.NONBINARY},
    {label: 'Transgender', value: GenderIdentity.TRANSGENDER},
    {label: 'Woman', value: GenderIdentity.WOMAN},
    {label: 'None of these describe me', value: GenderIdentity.NONEDESCRIBEME},
    {label: 'Prefer not to answer', value: GenderIdentity.PREFERNOANSWER}
  ],
  sexAtBirth: [
    {label: 'Female', value: SexAtBirth.FEMALE},
    {label: 'Intersex', value: SexAtBirth.INTERSEX},
    {label: 'Male', value: SexAtBirth.MALE},
    {label: 'None of these describe me', value: SexAtBirth.NONEOFTHESEDESCRIBEME},
    {label: 'Prefer not to answer', value: SexAtBirth.PREFERNOANSWER}
  ],
  levelOfEducation: [
    {label: 'Never attended school/no formal education', value: Education.NOEDUCATION},
    {label: 'Primary/Middle School/High School (Grades 1 through 12/GED)', value: Education.GRADES112},
    {label: 'Some college, Associate Degree or ' +
      'Technical school (1 to 3 years), or current undergraduate student', value: Education.UNDERGRADUATE},
    {label: 'College graduate (4 years or more) or current post-graduate trainee', value: Education.COLLEGEGRADUATE},
    {label: 'Master’s degree', value: Education.MASTER},
    {label: 'Doctorate', value: Education.DOCTORATE},
    {label: 'Prefer not to answer', value: Education.PREFERNOANSWER}
  ],
  Years: []
};

const year = (new Date()).getFullYear();
const years = Array.from(new Array(100), ( val, index) => year - index);

AccountCreationOptions.Years = years.map((yearOpt) => ({label: yearOpt, value: yearOpt}));
