import {TreeSubType, TreeType} from 'generated';
import {CriteriaType, DomainType, SearchGroup, SearchGroupItem, SearchParameter, SearchRequest} from 'generated/fetch';
import {List} from 'immutable';
import {DOMAIN_TYPES} from './constant';
import {idsInUse} from './search-state.service';

export function typeDisplay(parameter): string {
  const subtype = parameter.get('subtype', '');
  const _type = parameter.get('type', '');
  if (_type.match(/^DEMO.*/i)) {
    return {
      'GEN': 'Gender',
      'RACE': 'Race',
      'ETH': 'Ethnicity',
      'AGE': 'Age',
      'DEC': 'Deceased'
    }[subtype] || '';
  } else if (!_type.match(/^SNOMED.*/i)) {
    return parameter.get('code', '');
  }
}

export function listTypeDisplay(parameter): string {
  const {domainId, type} = parameter;
  if (domainId === DomainType.PERSON) {
    return {
      'GENDER': 'Gender',
      'RACE': 'Race',
      'ETHNICITY': 'Ethnicity',
      'AGE': 'Age',
      'DECEASED': 'Deceased'
    }[type] || '';
  } else if (type === CriteriaType.SNOMED) {
    return parameter.code;
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

export function listNameDisplay(parameter): string {
  if (parameter.type === CriteriaType.AGE || parameter.type === CriteriaType.DECEASED) {
    return '';
  } else {
    return stripHtml(parameter.name);
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

export function listAttributeDisplay(parameter): string {
  if (parameter.type === CriteriaType.AGE) {
    const attrs = parameter.attributes;
    const display = [];
    attrs.forEach(attr => {
      const op = {
        'BETWEEN': 'In Range',
        'EQUAL': 'Equal To',
        'GREATER_THAN': 'Greater Than',
        'LESS_THAN': 'Less Than',
        'GREATER_THAN_OR_EQUAL_TO': 'Greater Than or Equal To',
        'LESS_THAN_OR_EQUAL_TO': 'Less Than or Equal To',
      }[attr.operator];
      const args = attr.operands.join(', ');
      display.push(`${op} ${args}`);
    });
    return display.join(' ');
  } else {
    return '';
  }
}

export function domainToTitle(domain: any): string {
  switch (domain) {
    case DomainType.PERSON:
      domain = 'Demographics';
      break;
    case DomainType.MEASUREMENT:
      domain = 'Measurements';
      break;
    case DomainType.PHYSICALMEASUREMENT:
      domain = 'Physical Measurements';
      break;
    case DomainType.VISIT:
      domain = 'Visit';
      break;
    case DomainType.DRUG:
      domain = 'Drugs';
      break;
    case DomainType.CONDITION:
      domain = 'Conditions';
      break;
    case DomainType.PROCEDURE:
      domain = 'Procedures';
      break;
    case DomainType.LAB:
      domain = 'Labs';
      break;
  }
  return domain;
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

export function getChartObj(chartObj: any) {
  this.chart = chartObj;
  const chartRef = this.chart.container.parentElement;
  if (this.chart && typeof ResizeObserver === 'function') {
    // Unbind window.onresize handler so we don't do double redraws
    if (this.chart.unbindReflow) {
      this.chart.unbindReflow();
    }
    // create observer to redraw charts on div resize
    const ro = new ResizeObserver(() => {
      if (this.chart) {
        this.chart.reflow();
      }
    });
    ro.observe(chartRef);
  }
}

export function generateId(prefix?: string): string {
  prefix = prefix || 'id';
  let newId = `${prefix}_${genSuffix()}`;
  const ids = idsInUse.getValue();
  while (ids.has(newId)) {
    newId = `${prefix}_${genSuffix()}`;
  }
  ids.add(newId);
  idsInUse.next(ids);
  return newId;
}

function genSuffix(): string {
  return Math.random().toString(36).substr(2, 9);
}

export function mapRequest(sr: any) {
  return <SearchRequest>{
    includes: sr.includes.map(mapGroup),
    excludes: sr.excludes.map(mapGroup),
  };
}

export function mapGroup(group: any) {
  const items = group.items.map(mapGroupItem);
  return <SearchGroup>{id: group.id, items, temporal: false};
}

export function mapGroupItem(item: any) {
  const {id, type, modifiers} = item;
  const searchParameters = item.searchParameters.map(mapParameter);
  return <SearchGroupItem>{id, type, searchParameters, modifiers};
}

export function mapParameter(sp: any) {
  const {parameterId, name, domainId, type, subtype, group, attributes, conceptId, code} = sp;
  const param = <SearchParameter>{
    parameterId,
    name: stripHtml(name),
    domain: domainId,
    type,
    subtype,
    group,
    attributes
  };
  if (conceptId) {
    param.conceptId = conceptId;
  }
  if (type === CriteriaType.DECEASED) {
    param.value = name;
  } else if (code && ([
    CriteriaType.ICD9CM,
    CriteriaType.ICD9Proc,
    CriteriaType.ICD10CM,
    CriteriaType.ICD10PCS
  ].includes(type))) {
    param.value = code;
  }
  return param;
}
