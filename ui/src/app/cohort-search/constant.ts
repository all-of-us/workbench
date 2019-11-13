import {AttrName, DomainType, ModifierType, Operator} from 'generated/fetch';

export const PROGRAM_TYPES = [
  DomainType[DomainType.PERSON],
  DomainType[DomainType.SURVEY],
  DomainType[DomainType.PHYSICALMEASUREMENT]
];

export const DOMAIN_TYPES = [
  DomainType[DomainType.CONDITION],
  DomainType[DomainType.PROCEDURE],
  DomainType[DomainType.DRUG],
  DomainType[DomainType.MEASUREMENT],
  DomainType[DomainType.VISIT]
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

/* Systolic (conceptId: 903118) should always be the first element in the attributes array */
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
      operands: [],
      operator: AttrName.ANY,
      MIN: 0,
      MAX: 1000
    },
    {
      conceptId: 903115,
      name: 'Diastolic',
      operands: [],
      operator: AttrName.ANY,
      MIN: 0,
      MAX: 1000
    }
  ],
};

export const MODIFIERS_MAP = {
  [ModifierType.AGEATEVENT]: {
    name: 'Age At Event',
    operators: {
      [Operator.BETWEEN]: 'Between',
      [Operator.GREATERTHANOREQUALTO]: 'Greater Than or Equal To',
      [Operator.LESSTHANOREQUALTO]: 'Less Than or Equal To',
    }
  },
  [ModifierType.ENCOUNTERS]: {
    name: 'During Visit Type',
    operators: {
      [Operator.IN]: ''
    }
  },
  [ModifierType.EVENTDATE]: {
    name: 'Event Date',
    operators: {
      [Operator.BETWEEN]: 'Between',
      [Operator.GREATERTHANOREQUALTO]: 'On or After',
      [Operator.LESSTHANOREQUALTO]: 'On or Before',
    }
  },
  [ModifierType.NUMOFOCCURRENCES]: {
    name: 'Has Occurrences',
    operators: {
      [Operator.GREATERTHANOREQUALTO]: 'N or More',
    }
  },
};
