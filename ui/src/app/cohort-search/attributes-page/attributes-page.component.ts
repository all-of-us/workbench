import {select} from '@angular-redux/store';
import {ChangeDetectorRef, Component, OnDestroy, OnInit} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';

import {Operator, TreeSubType, TreeType} from 'generated';
import {fromJS, Map} from 'immutable';
import {Subscription} from 'rxjs/Subscription';

import {PM_UNITS} from 'app/cohort-search/constant';
import {
attributesPreviewStatus,
CohortSearchActions,
isAttributeLoading,
nodeAttributes,
previewError,
} from 'app/cohort-search/redux';
import {stripHtml} from 'app/cohort-search/utils';
import {numberAndNegativeValidator, rangeValidator} from 'app/cohort-search/validators';

@Component({
  selector: 'crit-attributes-page',
  templateUrl: './attributes-page.component.html',
  styleUrls: ['./attributes-page.component.css']
})
export class AttributesPageComponent implements OnDestroy, OnInit {
  @select(attributesPreviewStatus) preview$;
  @select(isAttributeLoading) loading$;
  @select(nodeAttributes) node$;
  @select(previewError) error$;
  node: Map<any, any>;
  units = PM_UNITS;
  attrs = {EXISTS: false, NUM: [], CAT: []};
  attributes: any;
  dropdowns = {
    selected: ['', ''],
    oldVals: ['', ''],
    labels: ['', ''],
    codes: ['', '']
  };
  preview = Map();
  subscription: Subscription;
  loading: boolean;
  selectedCode: any;
  resetDisable = false;
  options = [
    {
      value: 'EQUAL',
      name: 'Equals',
      display: '= ',
      code: '01'
    },
    {
      value: 'GREATER_THAN_OR_EQUAL_TO',
      name: 'Greater than or Equal to',
      display: '>= ',
      code: '02'
    },
    {
      value: 'LESS_THAN_OR_EQUAL_TO',
      name: 'Less than or Equal to',
      display: '<= ',
      code: '03'
    },
    {
      value: 'BETWEEN',
      name: 'Between',
      display: '',
      code: '04'
    },
  ];

  form = new FormGroup({
    EXISTS: new FormControl(false),
    NUM: new FormGroup({}),
    CAT: new FormGroup({})
  });

  constructor(private actions: CohortSearchActions, private cdref: ChangeDetectorRef) {}

  ngOnInit() {
    this.subscription = this.preview$.subscribe(prev => {
      this.preview = prev;
    });
    this.subscription.add(this.loading$.subscribe(loading => this.loading = loading));
    this.subscription.add(this.node$.subscribe(node => {
      this.node = node;
      if (this.isMeasurement) {
        this.node.get('attributes').forEach(attr => {
          switch (attr.type) {
            case 'NUM':
              const NUM = <FormGroup>this.form.controls.NUM;
              if (!this.attrs.NUM.length) {
                NUM.addControl('num0', new FormGroup({
                  operator: new FormControl(),
                  valueA: new FormControl(),
                  valueB: new FormControl(),
                }));
                this.dropdowns.labels[0] = 'Numeric Values';
                this.attrs.NUM.push({
                  name: 'NUM',
                  operator: null,
                  operands: [],
                  conceptId: attr.conceptId,
                  [attr.conceptName]: attr.estCount
                });
              } else {
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
        this.options.unshift({value: 'ANY', name: 'Any', display: 'Any', code: 'Any'});
        this.attrs.NUM = this.node.get('attributes');
        if (this.attrs.NUM) {
          const NUM = <FormGroup>this.form.controls.NUM;
          this.selectedCode = 'Any';
          this.attrs.NUM.forEach((attr, i) => {
            attr.operator = 'ANY';
            this.dropdowns.selected[i] = 'ANY';
            this.dropdowns.oldVals[i] = 'ANY';
            NUM.addControl('num' + i, new FormGroup({
              operator: new FormControl(),
              valueA: new FormControl(),
              valueB: new FormControl(),
            }));
            this.dropdowns.labels[i] = attr.name;
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
    this.form.controls.NUM.reset();
    this.resetDisable = true;
    this.selectedCode = 'Any';
    this.preview = this.preview.set('count', this.node.get('count'));
  }

  selectChange(index: number, option: any) {
    this.resetDisable = true;
    this.attrs.NUM[index].operator = option.value;
    this.dropdowns.selected[index] = option.name;
    if (this.node.get('subtype') === 'BP' && this.dropdowns.oldVals[index] !== option.value) {
      const other = index === 0 ? 1 : 0;
      if (this.dropdowns.codes[other] === '') {
        this.dropdowns.codes = [option.code, option.code];
      } else {
        this.dropdowns.codes[index] = option.code;
      }
      if (!this.dropdowns.codes.includes('')) {
        this.selectedCode = (this.dropdowns.codes.join(''));
      }
      if (option.value === 'ANY') {
        this.attrs.NUM[other].operator = this.dropdowns.oldVals[other] = 'ANY';
        this.dropdowns.selected[other] = 'Any';
      } else if (this.dropdowns.oldVals[index] === 'ANY') {
        this.attrs.NUM[other].operator = this.dropdowns.oldVals[other] = option.value;
        this.dropdowns.selected[other] = option.name;
      }
      this.dropdowns.oldVals[index] = option.value;
    } else {
      if (option.value !== 'BETWEEN') {
        this.form.controls.NUM.get(['num' + index, 'valueB']).reset();
      }
      this.selectedCode = option.code;
    }
    this.setValidation(option.name);
    this.preview = option.value === 'ANY'
      ? this.preview.set('count', this.node.get('count')) : Map();
  }

  setValidation(option: string) {
    if (option === 'Any') {
      this.form.controls.NUM.get(['num0', 'valueA']).clearValidators();
      this.form.controls.NUM.get(['num0', 'valueB']).clearValidators();
      if (this.attrs.NUM.length === 2) {
        this.form.controls.NUM.get(['num1', 'valueA']).clearValidators();
        this.form.controls.NUM.get(['num1', 'valueB']).clearValidators();
      }
      this.form.controls.NUM.reset();
    } else {
      const validators = [Validators.required];
      if (this.isMeasurement) {
        const min = parseFloat(this.attrs.NUM[0].MIN);
        const max = parseFloat(this.attrs.NUM[0].MAX);
        validators.push(rangeValidator('Values', min, max));
      } else {
        validators.push(numberAndNegativeValidator('Form'));
      }
      this.attrs.NUM.forEach((attr, i) => {
        this.form.controls.NUM.get(['num' + i, 'valueA']).setValidators(validators);
        if (option === 'Between') {
          this.form.controls.NUM.get(['num' + i, 'valueB']).setValidators(validators);
        } else {
          this.form.controls.NUM.get(['num' + i, 'valueB']).clearValidators();
          this.form.controls.NUM.get(['num' + i, 'valueB']).reset();
        }
      });
      this.cdref.detectChanges();
    }
  }

  inputChange(input: number, index: number, name: string) {
    if (input) {
      let value = input.toString();
      if (value && value.length > 3) {
        value = value.slice(0, 3);
        this.form.controls.NUM.get(['num' + index, name]).setValue(value, {emitEvent: false});
      }
    }
    this.preview = Map();
  }

  get isValid() {
    if (this.isPM || !this.form.valid) {
      return this.form.valid;
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
    this.form.reset();
    this.attrs.EXISTS = false;
    this.resetDisable = false;
    this.attrs.NUM.forEach(num => {
      num.operator = null;
      num.operands = [null];
    });
    this.attrs.CAT.forEach(cat => {
      cat.checked = false;
    });
  }

  get paramId() {
    return `param${this.node.get('conceptId')
        ? (this.node.get('conceptId') + (this.selectedCode))
        : (this.node.get('id') + (this.selectedCode))}`;
  }

  get displayName() {
    return stripHtml(this.node.get('name'));
  }

  get paramWithAttributes() {
    let name;
    const attrs = [];
    if (this.attrs.EXISTS) {
      name = this.node.get('name', '') + ' (Any)';
      attrs.push({
        name: 'ANY',
        operands: []
      });
    } else {
      name = this.paramName;
      this.attrs.NUM.forEach((attr, i) => {
        const paramAttr = {
          name: 'NUM',
          operator: attr.operator,
          operands: attr.operator === 'BETWEEN' ? attr.operands : [attr.operands[0]],
          conceptId: attr.conceptId
        };
        if (this.form.value.NUM['num' + i].operator === 'ANY') {
          paramAttr.name = 'ANY';
          paramAttr.operands = [];
          delete(paramAttr.operator);
        }
        attrs.push(paramAttr);
      });

      const catOperands = this.attrs.CAT.reduce((checked, current) => {
        if (current.checked) {
          checked.push(current.valueAsConceptId);
        }
        return checked;
      }, []);
      if (catOperands.length) {
        attrs.push({name: 'CAT', operator: Operator.IN, operands: catOperands});
      }
      name += (this.isPM && attrs[0].name !== 'ANY'
        ? this.units[this.node.get('subtype')]
        : '') + ')';
    }
    return this.node
      .set('parameterId', this.paramId)
      .set('name', name)
      .set('attributes', fromJS(attrs));
  }

  get paramName() {
    let name = this.node.get('name', '') + ' (';
    this.attrs.NUM.forEach((attr, i) => {
      if (this.form.value.NUM['num' + i].operator === 'ANY') {
        if (i === 0) {
          name += 'Any';
        }
      } else {
        if (i > 0) {
          name += ' / ';
        }
        if (this.node.get('subtype') === TreeSubType[TreeSubType.BP]) {
          name += attr.name + ' ';
        }
        name += this.options.find(option => option.value === attr.operator).display
          + attr.operands.join('-');
      }
    });
    return name;
  }

  requestPreview() {
    const param = this.paramWithAttributes;
    this.actions.requestAttributePreview(param);
  }

  addAttrs() {
    const param = this.paramWithAttributes;
    this.actions.addParameter(param);
    this.actions.hideAttributesPage();
  }

  cancel() {
    this.actions.hideAttributesPage();
  }

  showInput(index: number) {
    return this.attrs.NUM[index].operator
      && this.form.value.NUM['num' + index].operator !== 'ANY';
  }

  isBetween(index: number) {
    return this.form.value.NUM['num' + index].operator === Operator.BETWEEN;
  }

  get hasUnits() {
    return typeof PM_UNITS[this.node.get('subtype')] !== 'undefined';
  }

  get isMeasurement() {
    return this.node.get('type') === TreeType[TreeType.MEAS];
  }

  get isPM() {
    return this.node.get('type') === TreeType[TreeType.PM];
  }

  get showCalc() {
    let notAny = true;
    if (this.isPM) {
      notAny = this.attrs.NUM[0].operator !== 'ANY';
    }
    return !this.attrs.EXISTS && notAny;
  }

  get showAdd() {
    let any = false;
    if (this.isPM) {
      any = this.attrs.NUM[0].operator === 'ANY';
    }
    return (this.preview.get('count') && !this.preview.get('requesting')) || any;
  }
}
