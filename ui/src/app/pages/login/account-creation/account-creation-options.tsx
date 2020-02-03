import {
  AcademicRole,
  Degree,
  Education,
  EducationalRole,
  Ethnicity,
  Gender,
  IndustryRole,
  NonAcademicAffiliation,
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
    {label: 'B.A.', value: Degree.BA},
    {label: 'B.S.', value: Degree.BS},
    {label: 'B.S.N.', value: Degree.BSN},
    // This label can have a space in it when the following issue is resolved:
    // https://github.com/primefaces/primereact/issues/1137
    {label: 'None(blank)', value: Degree.NONE},
  ],
  roles: [
    {label: `Undergraduate (Bachelor level) student`, value: AcademicRole.UNDERGRADUATE},
    {label: `Graduate trainee (Current student in a Masters, PhD, or Medical school training
        program)`, value: AcademicRole.TRAINEE},
    {label: `Research fellow (a post-doctoral fellow or medical resident in training)`,
      value: AcademicRole.FELLOW},
    {label: `Early career tenure-track researcher`, value: AcademicRole.EARLYCAREER},
    {label: `Non tenure-track researcher`, value: AcademicRole.NONTENURE},
    {label: `Mid-career tenured researcher`, value: AcademicRole.MIDCAREER},
    {label: `Late career tenured researcher`, value: AcademicRole.LATECAREER},
    {label: `Project Personnel (eg: Research Assistant, Data Analyst, Project Manager, Research
        Coordinator or other roles)`, value: AcademicRole.PROJECTPERSONNEL}
  ],
  nonAcademicAffiliations: [
    {label: 'Industry', value: NonAcademicAffiliation.INDUSTRY },
    {label: `Educational institution (High school, Community college, 4-year college, trade
        school)`, value: NonAcademicAffiliation.EDUCATIONALINSTITUTION},
    {label: `Community Scientist (i.e. I am accessing All of Us for independent research, unrelated to my
        professional affiliation)`, value: NonAcademicAffiliation.COMMUNITYSCIENTIST},
    {label: `Other (free text)`, value: NonAcademicAffiliation.FREETEXT}
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
  race: [
    {label: `American Indian or Alaska Native (AIAN)`, value: Race.AIAN},
    {label: `Black or African American`, value: Race.AA},
    {label: `Asian`, value: Race.ASIAN},
    {label: `Native Hawaiian or Other Pacific Islander`, value: Race.NHOPI},
    {label: `White`, value: Race.WHITE},
    {label: `Prefer not to answer`, value: Race.PREFERNOANSWER},
    {label: `None of these describe me`, value: Race.NONE}
  ],
  ethnicity: [
    {label: `Hispanic or Latino`, value: Ethnicity.HISPANIC},
    {label: `Not Hispanic or Latino`, value: Ethnicity.NOTHISPANIC},
    {label: ` Prefer not to answer`, value: Ethnicity.PREFERNOANSWER}
  ],
  gender: [
    {label: 'Man', value: Gender.MALE},
    {label: 'Non-Binary', value: Gender.NONBINARY},
    {label: 'Transgender', value: Gender.TRANSGENDER},
    {label: 'Woman', value: Gender.FEMALE},
    {label: 'None of these describe me', value: Gender.NONE},
    {label: 'Prefer not to answer', value: SexAtBirth.PREFERNOANSWER}
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
