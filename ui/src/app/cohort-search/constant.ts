export const PROGRAM_TYPES = [
  { name: 'Surveys',    type: 'surveys', children: [], disabled: true },
  { name: 'Physical Measurements',    type: 'pm' },
];

export const DOMAIN_TYPES = [
    { name: 'Demographics', type: 'demo' },
    { name: 'ICD9 Codes',   type: 'icd9' },
    { name: 'ICD10 Codes',  type: 'icd10' },
    // { name: 'PheCodes',     type: 'phecode' },
    { name: 'CPT Codes',    type: 'cpt' },
    { name: 'Conditions',    type: 'cond', disabled: true },
    { name: 'Procedures',    type: 'procedures', disabled: true },
    { name: 'Drugs',    type: 'drugs', disabled: true },
    { name: 'Measurements',    type: 'measure', disabled: true },
    { name: 'Visits',    type: 'visits', disabled: true },
    // { name: 'Medications',  type: 'meds' },
    // { name: 'Labs',         type: 'labs' },
    // { name: 'Vitals',       type: 'vitals' },
    // { name: 'Temporal',     type: 'temporal' }
];
