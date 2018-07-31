import { DomainType } from 'generated';

export const CRITERIA_TYPES = {
  'ICD9': 'ICD9',
  'ICD10': 'ICD10',
  'CPT': 'CPT',
  'DEMO': 'DEMO',
  'SURVEY': 'SURVEY'
};

export const CRITERIA__SUBTYPES = {
    'ATC': 'ATC',
    'BRAND': 'BRAND',
    'GEN': 'GEN',
    'RACE': 'RACE',
    'AGE': 'AGE',
    'DEC': 'DEC',
    'BP': 'BP'
}

export const PROGRAM_TYPES = [
  { name: 'Surveys',    type: CRITERIA_TYPES.SURVEY, children: [], disabled: true },
  { name: 'Physical Measurements',    type: DomainType.PhysicalMeasure },
];

export const DOMAIN_TYPES = [
    { name: 'Demographics', type: CRITERIA_TYPES.DEMO },
    { name: 'Conditions',    type: DomainType.Condition, disabled: true },
    { name: 'Procedures',    type: DomainType.Procedure, disabled: true },
    { name: 'Drugs',    type: DomainType.Drug, fullTree: true },
    { name: 'Measurements',    type: DomainType.Measurement, disabled: true },
    { name: 'Visits',    type: DomainType.Visit, fullTree: true },
    { name: 'ICD9 Codes',   type: CRITERIA_TYPES.ICD9 },
    { name: 'ICD10 Codes',  type: CRITERIA_TYPES.ICD10 },
    // { name: 'PheCodes',     type: 'phecode' },
    { name: 'CPT Codes',    type: CRITERIA_TYPES.CPT },
    // { name: 'Medications',  type: 'meds' },
    // { name: 'Labs',         type: 'labs' },
    // { name: 'Vitals',       type: 'vitals' },
    // { name: 'Temporal',     type: 'temporal' }
];

export const PM_UNITS = {
    'HEIGHT': 'cm',
    'WEIGHT': 'kg' ,
    'BMI': '',
    'WC': 'cm',
    'HC': 'cm',
    'BP': '',
    'HR-DETAIL': 'beats/min'
};

