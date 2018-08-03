export const PROGRAM_TYPES = [
  { name: 'Surveys',    type: 'surveys', children: [], disabled: true },
  { name: 'Physical Measurements',    type: 'pm' },
];

export const DOMAIN_TYPES = [
    { name: 'Demographics', type: 'demo' },
    { name: 'Conditions',    type: 'cond', disabled: true },
    { name: 'Procedures',    type: 'procedures', disabled: true },
    { name: 'Drugs',    type: 'drugs', disabled: true },
    { name: 'Measurements',    type: 'meas' },
    { name: 'Visits',    type: 'visit', fullTree: true },
    { name: 'ICD9 Codes',   type: 'icd9' },
    { name: 'ICD10 Codes',  type: 'icd10' },
    // { name: 'PheCodes',     type: 'phecode' },
    { name: 'CPT Codes',    type: 'cpt' },
    // { name: 'Medications',  type: 'meds' },
    // { name: 'Labs',         type: 'lab' },
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
