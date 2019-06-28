import {ChangeDetectorRef, Component, Input, OnInit} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';

import {AttrName, Operator, TreeSubType, TreeType} from 'generated';
import {CriteriaSubType, DomainType, SearchGroup} from 'generated/fetch';

import {PM_UNITS, PREDEFINED_ATTRIBUTES} from 'app/cohort-search/constant';
import {selectionsStore, wizardStore} from 'app/cohort-search/search-state.service';
import {mapParameter, stripHtml} from 'app/cohort-search/utils';
import {numberAndNegativeValidator, rangeValidator} from 'app/cohort-search/validators';
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore} from 'app/utils/navigation';

@Component({
  selector: 'crit-list-attributes-page',
  templateUrl: './list-attributes-page.component.html',
  styleUrls: ['./list-attributes-page.component.css']
})
export class ListAttributesPageComponent implements OnInit {
  @Input() criterion: any;
  @Input() close: Function;
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
  count = -1;
  loading: boolean;
  error: boolean;
  selectedCode: any;
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

  constructor(private cdref: ChangeDetectorRef) {}

  ngOnInit() {
    if (this.isMeasurement) {
      const cdrid = +(currentWorkspaceStore.getValue().cdrVersionId);
      cohortBuilderApi().getCriteriaAttributeByConceptId(cdrid, this.criterion.conceptId)
        .then(resp => {
          resp.items.forEach(attr => {
            switch (attr.type) {
              case AttrName.NUM:
                const NUM = <FormGroup>this.form.controls.NUM;
                if (!this.attrs.NUM.length) {
                  NUM.addControl('num0', new FormGroup({
                    operator: new FormControl(),
                    valueA: new FormControl(),
                    valueB: new FormControl(),
                  }));
                  this.dropdowns.labels[0] = 'Numeric Values';
                  this.attrs.NUM.push({
                    name: AttrName.NUM,
                    operator: null,
                    operands: [],
                    conceptId: this.criterion.conceptId,
                    [attr.conceptName]: attr.estCount
                  });
                } else {
                  this.attrs.NUM[0][attr.conceptName] = attr.estCount;
                }
                break;
              case AttrName.CAT:
                if (parseInt(attr.estCount, 10) > 0) {
                  attr['checked'] = false;
                  this.attrs.CAT.push(attr);
                }
            }
          });
        });
    } else {
      this.options.unshift({value: AttrName.ANY, name: 'Any', display: 'Any', code: 'Any'});
      this.attrs.NUM = this.criterion.subtype === CriteriaSubType[CriteriaSubType.BP]
          ? JSON.parse(JSON.stringify(PREDEFINED_ATTRIBUTES.BP_DETAIL))
          : [{
            name: this.criterion.subtype,
            operator: null,
            operands: [null],
            MIN: 0,
            MAX: 10000
          }];
      if (this.attrs.NUM) {
        const NUM = <FormGroup>this.form.controls.NUM;
        this.selectedCode = 'Any';
        this.attrs.NUM.forEach((attr, i) => {
          attr.operator = AttrName.ANY;
          this.dropdowns.selected[i] = AttrName.ANY;
          this.dropdowns.oldVals[i] = AttrName.ANY;
          NUM.addControl('num' + i, new FormGroup({
            operator: new FormControl(),
            valueA: new FormControl(),
            valueB: new FormControl(),
          }));
          this.dropdowns.labels[i] = attr.name;
        });
        this.count = this.criterion.count;
      }
    }
  }

  radioChange() {
    if (this.attrs.EXISTS) {
      this.form.controls.NUM.reset();
      this.selectedCode = 'Any';
      this.count = this.criterion.count;
    } else {
      this.refresh();
    }
  }

  selectChange(index: number, option: any) {
    if (this.attrs.NUM[index].operator !== option.value) {
      this.attrs.NUM[index].operator = option.value;
      this.dropdowns.selected[index] = option.name;
      if (this.criterion.subtype === 'BP' && this.dropdowns.oldVals[index] !== option.value) {
        const other = index === 0 ? 1 : 0;
        if (this.dropdowns.codes[other] === '') {
          this.dropdowns.codes = [option.code, option.code];
        } else {
          this.dropdowns.codes[index] = option.code;
        }
        if (!this.dropdowns.codes.includes('')) {
          this.selectedCode = (this.dropdowns.codes.join(''));
        }
        if (option.value === AttrName.ANY) {
          this.attrs.NUM[other].operator = this.dropdowns.oldVals[other] = AttrName.ANY;
          this.dropdowns.selected[other] = 'Any';
        } else if (this.dropdowns.oldVals[index] === AttrName.ANY) {
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
      this.count = option.value === AttrName.ANY
        ? this.criterion.count : -1;
    }
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
      if (value && value.length > 10) {
        value = value.slice(0, 10);
        this.form.controls.NUM.get(['num' + index, name]).setValue(value, {emitEvent: false});
      }
    }
    this.count = -1;
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
    this.count = -1;
    this.form.reset();
    this.attrs.NUM.forEach(num => {
      num.operator = null;
      num.operands = [null];
    });
    this.attrs.CAT.forEach(cat => {
      cat.checked = false;
    });
  }

  get paramId() {
    return `param${this.criterion.conceptId
        ? (this.criterion.conceptId + (this.selectedCode))
        : (this.criterion.id + (this.selectedCode))}`;
  }

  get displayName() {
    return stripHtml(this.criterion.name);
  }

  get paramWithAttributes() {
    let name;
    const attrs = [];
    if (this.attrs.EXISTS) {
      name = this.criterion.name + ' (Any)';
    } else {
      name = this.paramName;
      this.attrs.NUM.forEach((attr, i) => {
        const paramAttr = {
          name: AttrName.NUM,
          operator: attr.operator,
          operands: attr.operator === 'BETWEEN' ? attr.operands : [attr.operands[0]],
          conceptId: attr.conceptId
        };
        if (attr.operator === AttrName.ANY && this.criterion.subtype === TreeSubType.BP) {
          paramAttr.name = AttrName.ANY;
          paramAttr.operands = [];
          delete(paramAttr.operator);
          attrs.push(paramAttr);
        } else if (attr.operator !== AttrName.ANY) {
          attrs.push(paramAttr);
        }
      });

      const catOperands = this.attrs.CAT.reduce((checked, current) => {
        if (current.checked) {
          checked.push(current.valueAsConceptId);
        }
        return checked;
      }, []);
      if (catOperands.length) {
        attrs.push({name: AttrName.CAT, operator: Operator.IN, operands: catOperands});
      }
      name += (this.isPM && attrs[0] && attrs[0].name !== AttrName.ANY
        ? this.units[this.criterion.subtype]
        : '') + ')';
    }
    return {
      ...this.criterion,
      parameterId: this.paramId,
      name: name,
      attributes: attrs
    };
  }

  get paramName() {
    let name = this.criterion.name + ' (';
    this.attrs.NUM.forEach((attr, i) => {
      if (this.form.value.NUM['num' + i].operator === AttrName.ANY) {
        if (i === 0) {
          name += 'Any';
        }
      } else {
        if (i > 0) {
          name += ' / ';
        }
        if (this.criterion.subtype === TreeSubType[TreeSubType.BP]) {
          name += attr.name + ' ';
        }
        name += this.options.find(option => option.value === attr.operator).display
          + attr.operands.join('-');
      }
    });
    return name;
  }

  requestPreview() {
    this.loading = true;
    this.error = false;
    const param = this.paramWithAttributes;
    // TODO make api call for count (call doesn't exist yet)
    const cdrVersionId = +(currentWorkspaceStore.getValue().cdrVersionId);
    const request = {
      excludes: [],
      includes: [<SearchGroup>{
        items: [{
          type: param.domainId,
          searchParameters: [mapParameter(param)],
          modifiers: []
        }],
      }]
    };
    cohortBuilderApi().countParticipants(cdrVersionId, request).then(response => {
      this.count = response;
      this.loading = false;
    }, () => {
      this.error = true;
      this.loading = false;
    });
  }

  addAttrs() {
    const param = this.paramWithAttributes;
    const wizard = wizardStore.getValue();
    let selections = selectionsStore.getValue();
    if (!selections.includes(param.parameterId)) {
      selections = [param.parameterId, ...selections];
      selectionsStore.next(selections);
    } else {
      wizard.item.searchParameters = wizard.item.searchParameters
        .filter(p => p.parameterId !== param.parameterId);
    }
    wizard.item.searchParameters.push(param);
    wizardStore.next(wizard);
    this.close();
  }

  showInput(index: number) {
    return this.attrs.NUM[index].operator
      && this.form.value.NUM['num' + index].operator !== AttrName.ANY;
  }

  isBetween(index: number) {
    return this.form.value.NUM['num' + index].operator === Operator.BETWEEN;
  }

  get hasUnits() {
    return typeof PM_UNITS[this.criterion.subtype] !== 'undefined';
  }

  get isMeasurement() {
    return this.criterion.domainId === DomainType[DomainType.MEASUREMENT];
  }

  get isPM() {
    return this.criterion.type === TreeType[TreeType.PM];
  }

  get showCalc() {
    let notAny = true;
    if (this.isPM) {
      notAny = this.attrs.NUM && this.attrs.NUM[0].operator !== AttrName.ANY;
    }
    return !this.attrs.EXISTS && notAny;
  }

  get disabled() {
    return this.form.invalid ||
      this.loading ||
      this.attrs.NUM.every(attr => attr.operator === 'ANY');
  }

  get showAdd() {
    // TODO bring this condition back when we have api calls to calculate counts
    // const any = this.isPM ? this.attrs.NUM[0].operator === AttrName.ANY : false;
    // return (this.preview && this.preview.count && !this.preview.requesting) || any;
    return true;
  }
}
