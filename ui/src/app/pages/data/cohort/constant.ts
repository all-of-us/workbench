import { AttrName, ModifierType, Operator } from 'generated/fetch';

export const PM_UNITS = {
  HEIGHT: 'cm',
  WEIGHT: 'kg',
  WC: 'cm',
  HC: 'cm',
  'HR-DETAIL': 'beats/min',
};

/* Systolic (conceptId: 903118) should always be the first element in the attributes array */
export const PREDEFINED_ATTRIBUTES = {
  Hypotensive: [
    {
      conceptId: 903118,
      name: AttrName.NUM,
      operands: ['90'],
      operator: Operator.LESS_THAN_OR_EQUAL_TO,
    },
    {
      conceptId: 903115,
      name: AttrName.NUM,
      operands: ['60'],
      operator: Operator.LESS_THAN_OR_EQUAL_TO,
    },
  ],
  Normal: [
    {
      conceptId: 903118,
      name: AttrName.NUM,
      operands: ['120'],
      operator: Operator.LESS_THAN_OR_EQUAL_TO,
    },
    {
      conceptId: 903115,
      name: AttrName.NUM,
      operands: ['80'],
      operator: Operator.LESS_THAN_OR_EQUAL_TO,
    },
  ],
  'Pre-Hypertensive': [
    {
      conceptId: 903118,
      name: AttrName.NUM,
      operands: ['121', '139'],
      operator: Operator.BETWEEN,
    },
    {
      conceptId: 903115,
      name: AttrName.NUM,
      operands: ['81', '89'],
      operator: Operator.BETWEEN,
    },
  ],
  Hypertensive: [
    {
      conceptId: 903118,
      name: AttrName.NUM,
      operands: ['140'],
      operator: Operator.GREATER_THAN_OR_EQUAL_TO,
    },
    {
      conceptId: 903115,
      name: AttrName.NUM,
      operands: ['90'],
      operator: Operator.GREATER_THAN_OR_EQUAL_TO,
    },
  ],
  BP_DETAIL: [
    {
      conceptId: 903118,
      name: 'Systolic',
      operands: [],
      operator: AttrName.ANY,
      MIN: 0,
      MAX: 1000,
    },
    {
      conceptId: 903115,
      name: 'Diastolic',
      operands: [],
      operator: AttrName.ANY,
      MIN: 0,
      MAX: 1000,
    },
  ],
};

export const MODIFIERS_MAP = {
  [ModifierType.AGE_AT_EVENT]: {
    name: 'Age At Event',
    operators: {
      [Operator.BETWEEN]: 'Between',
      [Operator.GREATER_THAN_OR_EQUAL_TO]: 'Greater Than or Equal To',
      [Operator.LESS_THAN_OR_EQUAL_TO]: 'Less Than or Equal To',
    },
  },
  [ModifierType.CATI]: {
    name: 'CATI(Computer Assisted Telephone Interview)',
    operators: {
      42530794: 'CATI(Computer Assisted Telephone Interview)',
      42531021: 'Non-CATI(Non Computer Assisted Telephone Interview)',
    },
  },
  [ModifierType.ENCOUNTERS]: {
    name: 'During Visit Type',
    operators: {
      [Operator.IN]: '',
    },
  },
  [ModifierType.EVENT_DATE]: {
    name: 'Event Date',
    operators: {
      [Operator.BETWEEN]: 'Between',
      [Operator.GREATER_THAN_OR_EQUAL_TO]: 'On or After',
      [Operator.LESS_THAN_OR_EQUAL_TO]: 'On or Before',
    },
  },
  [ModifierType.NUM_OF_OCCURRENCES]: {
    name: 'Number Of Occurrence Dates',
    operators: {
      [Operator.GREATER_THAN_OR_EQUAL_TO]: 'N or More',
    },
  },
};
