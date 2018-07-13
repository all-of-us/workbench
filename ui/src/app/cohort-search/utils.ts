import {List} from 'immutable';


export function typeDisplay(parameter): string {
  const subtype = parameter.get('subtype', '');
  const _type = parameter.get('type', '');

  if (_type.match(/^DEMO.*/i)) {
    return {
      'GEN': 'Gender',
      'RACE': 'Race/Ethnicity',
      'AGE': 'Age',
      'DEC': 'Deceased'
    }[subtype] || '';
  } else {
    return parameter.get('code', '');
  }
}

export function nameDisplay(parameter): string {
  const subtype = parameter.get('subtype', '');
  const _type = parameter.get('type', '');
  if (_type.match(/^DEMO.*/i) && subtype.match(/AGE|DEC/i)) {
    return '';
  } else {
    return parameter.get('name', '');
  }
}

export function attributeDisplay(parameter): string {
  const attrs = parameter.get('attributes', '');

  const kind = `${parameter.get('type', '')}${parameter.get('subtype', '')}`;
  if (kind.match(/^DEMO.*AGE/i)) {
    const display = [];
    attrs.forEach(attr => {
      const op = {
        'BETWEEN': 'In Range',
        'EQUAL': 'Equal To',
        'GREATER_THAN': 'Greater Than',
        'LESS_THAN': 'Less Than',
        'GREATER_THAN_OR_EQUAL_TO': 'Greater Than or Equal To',
        'LESS_THAN_OR_EQUAL_TO': 'Less Than or Equal To',
      }[attr.get('operator')];
      const args = attr.get('operands', List()).join(', ');
      display.push(`${op} ${args}`);
    });
    return display.join(' ');
  } else {
    return '';
  }
}


export function typeToTitle(_type: string): string {
  if (_type.match(/^DEMO.*/i)) {
    _type = 'Demographics';
  } else if (_type.match(/^(ICD|CPT).*/i)) {
    _type = _type.toUpperCase();
  } else if (_type.match(/^PM.*/i)) {
    _type = 'Physical Measurement';
  } else if (_type.match(/^VISIT.*/i)) {
    _type = 'Visit';
  }
  return _type;
}
