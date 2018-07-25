import { DomainType } from 'generated';

export const CRITERIA_TYPES = {
  'PM': 'PM',
  'ICD9': 'ICD9',
  'ICD10': 'ICD10',
  'CPT': 'CPT',
  'DEMO': 'DEMO',
  'PHECODE': 'PHECODE',
  'SURVEY': 'SURVEY'
};

export const CRITERIA_SUBTYPES = {
  'ATC': 'ATC',
  'BRAND': 'BRAND',
  'GEN': 'GEN',
  'RACE': 'RACE',
  'ETH': 'ETH',
  'AGE': 'AGE',
  'DEC': 'DEC',
  'BP': 'BP'
};

export const PROGRAM_TYPES = [
  { name: 'Surveys',    type: CRITERIA_TYPES.SURVEY, children: [], disabled: true },
  { name: 'Physical Measurements',    type: CRITERIA_TYPES.PM },
];

export const DOMAIN_TYPES = [
    { name: 'Demographics', type: CRITERIA_TYPES.DEMO },
    { name: 'Conditions',    type: DomainType.CONDITION, disabled: true },
    { name: 'Procedures',    type: DomainType.PROCEDURE, disabled: true },
    { name: 'Drugs',    type: DomainType.DRUG, fullTree: true },
    { name: 'Measurements',    type: DomainType.MEASUREMENT, disabled: true },
    { name: 'Visits',    type: DomainType.VISIT, fullTree: true },
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
