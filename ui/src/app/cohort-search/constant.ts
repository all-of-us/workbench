export const PROGRAM_TYPES = [
  { name: 'Physical Measurements',    type: 'pm' },
];

export const DOMAIN_TYPES = [
    { name: 'Demographics', type: 'demo' },
    { name: 'ICD9 Codes',   type: 'icd9' },
    { name: 'ICD10 Codes',  type: 'icd10' },
    // { name: 'PheCodes',     type: 'phecode' },
    { name: 'CPT Codes',    type: 'cpt' },
    // { name: 'Medications',  type: 'meds' },
    // { name: 'Labs',         type: 'labs' },
    // { name: 'Vitals',       type: 'vitals' },
    // { name: 'Temporal',     type: 'temporal' }
];

export const PM_UNITS = {
    HEIGHT: 'cm',
    WEIGHT: 'kg' ,
    BMI: '',
    WC: 'cm',
    HC: 'cm',
    BP: '',
    'HR-DETAIL': 'beats/min'
};
