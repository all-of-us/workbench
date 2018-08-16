import { DomainType, Operator } from 'generated';

export const CRITERIA_TYPES = {
  'PM': 'PM',
  'MEAS': 'MEAS',
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
    { name: 'Measurements',    type: CRITERIA_TYPES.MEAS},
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

export const PREDEFINED_ATTRIBUTES = {
  'Hypotensive': [
    {
      conceptId: 903118,
      name: 'Systolic',
      operands: ['90'],
      operator: Operator.LESSTHANOREQUALTO
    },
    {
      conceptId: 903115,
      name: 'Diastolic',
      operands: ['60'],
      operator: Operator.LESSTHANOREQUALTO
    }
  ],
  'Normal': [
    {
      conceptId: 903118,
      name: 'Systolic',
      operands: ['120'],
      operator: Operator.LESSTHANOREQUALTO
    },
    {
      conceptId: 903115,
      name: 'Diastolic',
      operands: ['80'],
      operator: Operator.LESSTHANOREQUALTO
    }
  ],
  'Pre-Hypertensive': [
    {
      conceptId: 903118,
      name: 'Systolic',
      operands: ['121', '139'],
      operator: Operator.BETWEEN
    },
    {
      conceptId: 903115,
      name: 'Diastolic',
      operands: ['81', '89'],
      operator: Operator.BETWEEN
    }
  ],
  'Hypertensive': [
    {
      conceptId: 903118,
      name: 'Systolic',
      operands: ['140'],
      operator: Operator.GREATERTHANOREQUALTO
    },
    {
      conceptId: 903115,
      name: 'Diastolic',
      operands: ['90'],
      operator: Operator.GREATERTHANOREQUALTO
    }
  ],
};
