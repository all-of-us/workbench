import { AttrName, Operator, TreeSubType, TreeType } from 'generated';
import {DomainType} from 'generated/fetch';

export const PROGRAM_TYPES = [
  { name: 'Surveys', type: TreeType.PPI, subtype: null },
  { name: 'Physical Measurements', type: TreeType.PM, fullTree: true, subtype: null },
];

export const DOMAIN_TYPES = [
  {
    name: 'Demographics', type: TreeType.DEMO, subtype: null, children: [
      {name: 'Current Age/Deceased', type: TreeType.DEMO, subtype: TreeSubType.AGE},
      {name: 'Gender', type: TreeType.DEMO, subtype: TreeSubType.GEN},
      {name: 'Race', type: TreeType.DEMO, subtype: TreeSubType.RACE},
      {name: 'Ethnicity', type: TreeType.DEMO, subtype: TreeSubType.ETH},
    ]
  },
  {
    name: 'Conditions', type: TreeType.CONDITION, subtype: null, codes: [
      {name: 'ICD9 Codes', type: TreeType.ICD9, subtype: TreeSubType.CM},
      {name: 'ICD10 Codes', type: TreeType.ICD10, subtype: TreeSubType.CM}
    ]
  },
  {
    name: 'Procedures', type: TreeType.PROCEDURE, subtype: null, codes: [
      {name: 'ICD9 Codes', type: TreeType.ICD9, subtype: TreeSubType.PROC},
      {name: 'ICD10 Codes', type: TreeType.ICD10, subtype: TreeSubType.PCS},
      {name: 'CPT Codes', type: TreeType.CPT, subtype: null}
    ]
  },
  {name: 'Drugs', type: TreeType.DRUG, subtype: null},
  {name: 'Measurements', type: TreeType.MEAS, subtype: null},
  {name: 'Visits', type: TreeType.VISIT, fullTree: true, subtype: null}
];

export const LIST_PROGRAM_TYPES = [];

export const LIST_DOMAIN_TYPES = [
  {name: 'Measurements', domain: DomainType.MEASUREMENT, type: null},
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
      name: AttrName.NUM,
      operands: ['90'],
      operator: Operator.LESSTHANOREQUALTO
    },
    {
      conceptId: 903115,
      name: AttrName.NUM,
      operands: ['60'],
      operator: Operator.LESSTHANOREQUALTO
    }
  ],
  'Normal': [
    {
      conceptId: 903118,
      name: AttrName.NUM,
      operands: ['120'],
      operator: Operator.LESSTHANOREQUALTO
    },
    {
      conceptId: 903115,
      name: AttrName.NUM,
      operands: ['80'],
      operator: Operator.LESSTHANOREQUALTO
    }
  ],
  'Pre-Hypertensive': [
    {
      conceptId: 903118,
      name: AttrName.NUM,
      operands: ['121', '139'],
      operator: Operator.BETWEEN
    },
    {
      conceptId: 903115,
      name: AttrName.NUM,
      operands: ['81', '89'],
      operator: Operator.BETWEEN
    }
  ],
  'Hypertensive': [
    {
      conceptId: 903118,
      name: AttrName.NUM,
      operands: ['140'],
      operator: Operator.GREATERTHANOREQUALTO
    },
    {
      conceptId: 903115,
      name: AttrName.NUM,
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
