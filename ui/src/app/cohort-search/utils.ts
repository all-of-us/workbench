import {DomainType, TreeSubType, TreeType} from 'generated';
import {List} from 'immutable';
import {DOMAIN_TYPES} from './constant';


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
  } else if (!_type.match(/^SNOMED.*/i)) {
    return parameter.get('code', '');
  }
}

export function nameDisplay(parameter): string {
  const subtype = parameter.get('subtype', '');
  const _type = parameter.get('type', '');
  if (_type.match(/^DEMO.*/i) && subtype.match(/AGE|DEC/i)) {
    return '';
  } else {
    return stripHtml(parameter.get('name', ''));
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
  switch (_type) {
    case TreeType[TreeType.DEMO]:
      _type = 'Demographics';
      break;
    case TreeType[TreeType.MEAS]:
      _type = 'Measurements';
      break;
    case TreeType[TreeType.PM]:
      _type = 'Physical Measurements';
      break;
    case TreeType[TreeType.VISIT]:
      _type = 'Visit';
      break;
    case TreeType[TreeType.DRUG]:
      _type = 'Drugs';
      break;
    case TreeType[TreeType.CONDITION]:
      _type = 'Conditions';
      break;
    case TreeType[TreeType.PROCEDURE]:
      _type = 'Procedures';
      break;
    case DomainType[DomainType.LAB]:
      _type = 'Labs';
      break;
  }
  return _type;
}

export function subtypeToTitle(subtype: string): string {
  let title;
  switch (subtype) {
    case TreeSubType[TreeSubType.AGE]:
      title = 'Current Age/Deceased';
      break;
    case TreeSubType[TreeSubType.DEC]:
      title = 'Deceased';
      break;
    case TreeSubType[TreeSubType.ETH]:
      title = 'Ethnicity';
      break;
    case TreeSubType[TreeSubType.GEN]:
      title = 'Gender';
      break;
    case TreeSubType[TreeSubType.RACE]:
      title = 'Race';
      break;
  }
  return title;
}

export function highlightMatches(
  terms: Array<string>,
  name: string,
  fullText?: boolean,
  id?: string
) {
  id = id || '';
  const _class = (id !== '' ? 'match' + id + ' ' : '') + 'search-keyword';
  name = stripHtml(name);
  terms.forEach(term => {
    if (fullText) {
      const searchTerms = term.trim().split(new RegExp(',| '));
      searchTerms
        .filter(text => text.length > 2)
        .forEach((searchTerm, s) => {
          let re;
          if (s === (searchTerms.length - 1)) {
            re = new RegExp(searchTerm, 'gi');
          } else {
            re = new RegExp('\\b' + searchTerm + '\\b', 'gi');
          }
          name = name.replace(re, '<span class="' + _class + '">$&</span>');
        });
    } else {
      const re = new RegExp(term.replace(/(?=[\[\]()+])/g, '\\'), 'gi');
      name = name.replace(re, '<span class="' + _class + '">$&</span>');
    }
  });
  return name;
}

export function stripHtml(string: string) {
  return string.replace(/<(.|\n)*?>/g, '');
}

export function getCodeOptions(itemType: string) {
  const item = DOMAIN_TYPES.find(domain => TreeType[domain.type] === itemType);
  return (item && item['codes']) ? item['codes'] : false;
}
