import {select} from '@angular-redux/store';
import {Component, OnDestroy, OnInit} from '@angular/core';
import {NgForm} from '@angular/forms';

import {fromJS, Map} from 'immutable';
import {Subscription} from 'rxjs/Subscription';

import {
  attributesPreviewStatus,
  CohortSearchActions,
  isAttributeLoading,
  nodeAttributes,
} from '../redux';

import {Operator} from 'generated';

import {CRITERIA_SUBTYPES, CRITERIA_TYPES, PM_UNITS} from '../constant';
import {stripHtml} from '../utils';

@Component({
  selector: 'crit-attributes-page',
  templateUrl: './attributes-page.component.html',
  styleUrls: ['./attributes-page.component.css']
})
export class AttributesPageComponent implements OnDestroy, OnInit {
  @select(attributesPreviewStatus) preview$;
  @select(isAttributeLoading) loading$;
  @select(nodeAttributes) node$;
  node: Map<any, any>;
  units = PM_UNITS;
  attrs = {EXISTS: false, NUM: [], CAT: []};
  attributes: any;
  dropdowns = {
      selected: ['', ''],
      oldVals: ['', ''],
      labels: ['', '']
  };
  preview = Map();
  subscription: Subscription;
  rangeAlert = false;
  loading: boolean;
  options = [
    {value: 'EQUAL', name: 'Equals'},
    {value: 'GREATER_THAN_OR_EQUAL_TO', name: 'Greater than or Equal to'},
    {value: 'LESS_THAN_OR_EQUAL_TO', name: 'Less than or Equal to'},
    {value: 'BETWEEN', name: 'Between'},
  ];

  readonly criteriaTypes = CRITERIA_TYPES;

  constructor(private actions: CohortSearchActions) {}

  ngOnInit() {
    this.subscription = this.preview$.subscribe(prev => this.preview = prev);
    this.subscription.add(this.loading$.subscribe(loading => this.loading = loading));
    this.subscription.add(this.node$.subscribe(node => {
      this.node = node;
      if (this.node.get('type') === CRITERIA_TYPES.MEAS) {
        this.node.get('attributes').forEach(attr => {
          switch (attr.type) {
            case 'NUM':
              if (this.attrs.NUM.length) {
                this.attrs.NUM[0][attr.conceptName] = attr.estCount;
              } else {
                this.dropdowns.labels[0] = 'Numeric Values';
                this.attrs.NUM.push({
                  name: 'NUM',
                  operator: null,
                  operands: [null],
                  conceptId: attr.conceptId
                });
                this.attrs.NUM[0][attr.conceptName] = attr.estCount;
              }
              break;
            case 'CAT':
              if (parseInt(attr.estCount, 10) > 0) {
                attr['checked'] = false;
                this.attrs.CAT.push(attr);
              }
          }
        });
      } else {
        this.options.unshift({value: 'ANY', name: 'Any'});
        this.attrs.NUM = this.node.get('attributes');
        if (this.attrs.NUM) {
          this.attrs.NUM.forEach((attr, i) => {
            attr.operator = 'ANY';
            this.dropdowns.selected[i] = 'ANY';
            this.dropdowns.oldVals[i] = 'ANY';
            if (this.node.get('subtype') === CRITERIA_SUBTYPES.BP) {
              this.dropdowns.labels[i] = attr.name;
            }
          });
          this.preview = this.preview.set('count', this.node.get('count'));
        }
      }
    }));
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  radioChange() {
    this.preview = this.preview.set('count', this.node.get('count'));
  }

  selectChange(index: number, option: any) {
    this.attrs.NUM[index].operator = option.value;
    this.dropdowns.selected[index] = option.name;
    if (this.node.get('subtype') === 'BP' && this.dropdowns.oldVals[index] !== option.value) {
      const other = index === 0 ? 1 : 0;
      if (option.value === 'ANY') {
        this.attrs.NUM[other].operator = this.dropdowns.oldVals[other] = 'ANY';
        this.dropdowns.selected[other] = 'Any';
      } else if (this.dropdowns.oldVals[index] === 'ANY') {
        this.attrs.NUM[other].operator = this.dropdowns.oldVals[other] = option.value;
        this.dropdowns.selected[other] = option.name;
      }
      this.dropdowns.oldVals[index] = option.value;
    }
    this.preview = option.value === 'ANY'
      ? this.preview.set('count', this.node.get('count')) : Map();
  }

  inputChange() {
    this.rangeAlert = false;
    this.attrs.NUM.forEach(attr => {
      attr.operands.filter(operand => !!operand)
        .forEach(operand => {
        if (operand < attr.MIN
          || (this.node.get('type') === CRITERIA_TYPES.PM ? false : operand > attr.MAX)) {
          this.rangeAlert = true;
        }
      });
    });
    this.preview = Map();
  }

  isValid(form: NgForm) {
    if (this.node.get('type') === CRITERIA_TYPES.PM || !form.valid) {
      return form.valid;
    }
    let valid = false;
    this.attrs.NUM.forEach(num => {
      if (num.operator) {
        valid = true;
      }
    });
    this.attrs.CAT.forEach(cat => {
      if (cat.checked) {
        valid = true;
      }
    });
    return valid;
  }

  refresh() {
    this.preview = Map();
    this.rangeAlert = false;
    this.attrs.EXISTS = false;
    this.attrs.NUM.forEach(num => {
      num.operator = null;
      num.operands = [null];
    });
    this.attrs.CAT.forEach(cat => {
      cat.checked = false;
    });
  }

  get paramId() {
    return `param${this.node.get('conceptId') ? this.node.get('conceptId') : this.node.get('id')}`;
  }

  get displayName() {
    return stripHtml(this.node.get('name'));
  }

  getParamWithAttributes(values: any) {
    let name = this.node.get('name', '') + ' (';
    let attrs = [];
    if (this.attrs.EXISTS) {
      name += 'Any)';
      attrs.push({
        name: 'ANY',
        operator: null,
        operands: [null],
        conceptId: this.node.get('conceptId')
      });
    } else {
      this.attrs.NUM.forEach((attr, i) => {
        const paramAttr = {
          name: attr.name,
          operator: attr.operator,
          operands: attr.operands,
          conceptId: attr.conceptId
        };
        if (i > 0) {
          name += ' / ';
        }
        name += (this.node.get('subtype') === CRITERIA_SUBTYPES.BP
          && values['operator' + i] !== 'ANY')
          ? attr.name + ' ' : '';
        switch (values['operator' + i]) {
          case 'ANY':
            paramAttr.operands = [];
            paramAttr.name = 'ANY';
            name += 'Any';
            break;
          case 'BETWEEN':
            paramAttr.operator = Operator.BETWEEN;
            paramAttr.operands = [values['valueA' + i], values['valueB' + i]];
            name += values['valueA' + i] + '-' + values['valueB' + i];
            break;
          case 'EQUAL':
            paramAttr.operator = Operator.EQUAL;
            paramAttr.operands = [values['valueA' + i]];
            name += '= ' + values['valueA' + i];
            break;
          case 'LESS_THAN_OR_EQUAL_TO':
            paramAttr.operator = Operator.LESSTHANOREQUALTO;
            paramAttr.operands = [values['valueA' + i]];
            name += '<= ' + values['valueA' + i];
            break;
          case 'GREATER_THAN_OR_EQUAL_TO':
            paramAttr.operator = Operator.GREATERTHANOREQUALTO;
            paramAttr.operands = [values['valueA' + i]];
            name += '>= ' + values['valueA' + i];
            break;
        }
        attrs.push(paramAttr);
      });
      const catAttr = {name: 'CAT', operator: Operator.IN, operands: []};
      this.attrs.CAT.forEach(attr => {
        if (attr.checked) {
          catAttr.operands.push(attr.valueAsConceptId);
        }
      });
      if (catAttr.operands.length) {
        attrs.push(catAttr);
      }
      if (this.attrs.NUM.length && this.attrs.CAT.length) {
        attrs = attrs.map(attr => {
          attr.name = 'BOTH';
          return attr;
        });
      }
      name += (this.node.get('type') === CRITERIA_TYPES.PM && attrs[0].name !== 'ANY'
        ? this.units[this.node.get('subtype')]
        : '') + ')';
    }
    return this.node
      .set('parameterId', this.paramId)
      .set('name', name)
      .set('attributes', fromJS(attrs));
  }

  requestPreview(attrform: NgForm) {
    const param = this.getParamWithAttributes(attrform.value);
    this.actions.addAttributeForPreview(param);
    this.actions.requestAttributePreview();
  }

  addAttrs(attrform: NgForm) {
    const param = this.getParamWithAttributes(attrform.value);
    this.actions.addParameter(param);
    this.actions.hideAttributesPage();
  }

  cancel() {
    this.actions.hideAttributesPage();
  }

  showInput(index: number, attrform: NgForm) {
    return this.attrs.NUM[index].operator && attrform.value['operator' + index] !== 'ANY';
  }

  isBetween(index: number, attrform: NgForm) {
    return attrform.value['operator' + index] === Operator.BETWEEN;
  }

  hasUnits() {
    return typeof PM_UNITS[this.node.get('subtype')] !== 'undefined';
  }

  isMeasurement() {
    return this.node.get('type') === CRITERIA_TYPES.MEAS;
  }

  isPM() {
    return this.node.get('type') === CRITERIA_TYPES.PM;
  }

  showCalc() {
    let notAny = true;
    if (this.isPM()) {
      notAny = this.attrs.NUM[0].operator !== 'ANY';
    }
    return !this.attrs.EXISTS && notAny;
  }

  showAdd() {
    let any = false;
    if (this.isPM()) {
      any = this.attrs.NUM[0].operator === 'ANY';
    }
    return (this.preview.get('count') && !this.preview.get('requesting')) || any;
  }
}
