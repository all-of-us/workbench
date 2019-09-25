import {
  AffiliationRole,
  Education,
  EducationalRole,
  Ethnicity,
  Gender,
  IndustryRole,
  Race,
  Role
} from 'generated/fetch';

export const AccountCreationOptions = {
  roles: [
    {label: `Undergraduate (Bachelor level) student`, value: Role.UNDERGRADUATE},
    {label: `Graduate trainee (Current student in a Masters, PhD, or Medical school training
        program)`, value: Role.TRAINEE},
    {label: `Research fellow (a post-doctoral fellow or medical resident in training)`,
      value: Role.FELLOW},
    {label: `Early career tenure-track researcher`, value: Role.EARLYCAREER},
    {label: `Non tenure-track researcher`, value: Role.NONTENURE},
    {label: `Mid-career tenured researcher`, value: Role.MIDCAREER},
    {label: `Late career tenured researcher`, value: Role.LATECAREER},
    {label: `Project Personnel (eg: Research Assistant, Data Analyst, Project Manager, Research
        Coordinator or other roles)`, value: Role.PROJECTPERSONNEL}
  ],
  affiliations: [
    {label: 'Industry', value: AffiliationRole.INDUSTRY },
    {label: `Educational institution (High school, Community college, 4-year college, trade
        school)`, value: AffiliationRole.EDUCATIONALINSTITUTION},
    {label: `Community Scientist (i.e. I am accessing AoU for independent research, unrelated to my
        professional affiliation)`, value: AffiliationRole.COMMUNITYSCIENTIST},
    {label: `Other (free text)`, value: AffiliationRole.FREETEXT}
  ],
  industryRole: [
    {label: 'Research Assistant (pre-doctoral)', value: IndustryRole.PREDOCTORAL},
    {label: 'Research associate (post-doctoral; early/mid career)', value: IndustryRole.EARLY},
    {label: 'Senior Researcher (PI/Team Lead)', value: IndustryRole.PI},
    {label: 'Other (free text)', value: IndustryRole.FREETEXT}
  ],
  educationRole: [
    {label: 'Teacher/Professor', value: EducationalRole.TEACHER},
    {label: 'Student', value: EducationalRole.STUDENT},
    {label: 'Administrator', value: EducationalRole.ADMIN},
    {label: 'Other (free text)', value: EducationalRole.FREETEXT}
  ],
  Race: [
    {label: `American Indian or Alaska Native (AIAN)`, value: Race.AIAN},
    {label: `Black or African American`, value: Race.AA},
    {label: `Asian`, value: Race.ASIAN},
    {label: `Native Hawaiian or Other Pacific Islander`, value: Race.NHOPI},
    {label: `White`, value: Race.WHITE},
    {label: `Prefer not to answer`, value: Race.PREFERNOANSWER},
    {label: `None of these describe me`, value: Race.NONE}
  ],
  Ethnicity: [
    {label: `Hispanic or Latino`, value: Ethnicity.HISPANIC},
    {label: `Not Hispanic or Latino`, value: Ethnicity.NOTHISPANIC},
    {label: ` Prefer not to answer`, value: Ethnicity.PREFERNOANSWER}
  ],
  Gender: [
    {label: 'Male', value: Gender.MALE},
    {label: 'Female', value: Gender.FEMALE},
    {label: 'Non-binary', value: Gender.NONBINARY},
    {label: 'Transgender', value: Gender.TRANSGENDER},
    {label: 'Intersex', value: Gender.INTERSEX},
    {label: `Prefer not to answer`, value: Gender.PREFERNOANSWER},
    {label: `None of these describe me`, value: Gender.NONE}
  ],
  LevelOfEducation: [
    {label: 'Never attended school/no formal education', value: Education.NOEDUCATION},
    {label: 'Primary/Middle School/High School (Grades 1 through 12/GED)', value: Education.GRADES112},
    {label: 'Some college, Associate Degree or ' +
      'Technical school (1 to 3 years) or current undergraduate student', value: Education.UNDERGRADUATE},
    {label: 'College graduate (4 years or more) or current post-graduate trainee', value: Education.COLLEGEGRADUATE},
    {label: 'Master’s degree', value: Education.MASTER},
    {label: 'Doctorate', value: Education.DOCTORATE}
  ],
  Years: []
};

const year = (new Date()).getFullYear();
const years = Array.from(new Array(100), ( val, index) => year - index);

AccountCreationOptions.Years = years.map((yearOpt) => ({label: yearOpt, value: yearOpt}));
