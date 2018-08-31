import { Operator, TreeType } from 'generated';

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
  { name: 'Surveys',    type: TreeType.SURVEY, children: [], disabled: true },
  { name: 'Physical Measurements',    type: TreeType.PM, fullTree: true },
];

export const DOMAIN_TYPES = [
    { name: 'Demographics', type: TreeType.DEMO },
    { name: 'Conditions',    type: TreeType.CONDITION, disabled: true },
    { name: 'Procedures',    type: TreeType.PROCEDURE, disabled: true },
    { name: 'Drugs',    type: TreeType.DRUG },
    { name: 'Measurements',    type: TreeType.MEAS },
    { name: 'Visits',    type: TreeType.VISIT, fullTree: true },
    { name: 'ICD9 Codes',   type: TreeType.ICD9 },
    { name: 'ICD10 Codes',  type: TreeType.ICD10 },
    // { name: 'PheCodes',     type: 'phecode' },
    { name: 'CPT Codes',    type: TreeType.CPT },
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
  'BP_DETAIL': [
    {
      conceptId: 903118,
      name: 'Systolic',
      operands: [null],
      operator: null,
      MIN: 0,
      MAX: 1000
    },
    {
      conceptId: 903115,
      name: 'Diastolic',
      operands: [null],
      operator: null,
      MIN: 0,
      MAX: 1000
    }
  ],
};
