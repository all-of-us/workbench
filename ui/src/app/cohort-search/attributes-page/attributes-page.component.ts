import {select} from '@angular-redux/store';
import {Component, Input, OnChanges, OnDestroy, OnInit, SimpleChanges} from '@angular/core';
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

@Component({
  selector: 'crit-attributes-page',
  templateUrl: './attributes-page.component.html',
  styleUrls: ['./attributes-page.component.css']
})
export class AttributesPageComponent implements OnChanges, OnDestroy, OnInit {
    @select(attributesPreviewStatus) preview$;
    @select(isAttributeLoading) loading$;
    @select(nodeAttributes) node$;
    node: Map<any, any>;
    units = PM_UNITS;
    attrs = {NUM: [], CAT: []};
    attributes: any;
    dropdowns = {
        selected: ['', ''],
        oldVals: ['', ''],
        labels: ['', '']
    };
    preview = Map();
    subscription: Subscription;
    negativeAlert = false;
    loading: boolean;
    options: any;

    testAttrs = [
      {
        id: 1,
        conceptId: 112345,
        valueAsConceptId: null,
        conceptName: 'MIN',
        type: 'NUM',
        estCount: '1'
      },
      {
        id: 2,
        conceptId: 12345,
        valueAsConceptId: null,
        conceptName: 'MAX',
        type: 'NUM',
        estCount: '16'
      },
      {
        id: 3,
        conceptId: 12345,
        valueAsConceptId: 2345,
        conceptName: 'POSITIVE',
        type: 'CAT',
        estCount: '465'
      },
      {
        id: 4,
        conceptId: 12345,
        valueAsConceptId: 2346,
        conceptName: 'PRESENT',
        type: 'CAT',
        estCount: '287'
      }
    ]

  constructor(private actions: CohortSearchActions) {
    this.options = [
      {value: 'ANY', name: 'Any'},
      {value: 'EQUAL', name: 'Equals'},
      {value: 'GREATER_THAN_OR_EQUAL_TO', name: 'Greater than or Equal to'},
      {value: 'LESS_THAN_OR_EQUAL_TO', name: 'Less than or Equal to'},
      {value: 'BETWEEN', name: 'Between'},
    ];
  }

    ngOnInit() {
        this.subscription = this.preview$.subscribe(prev => this.preview = prev);
        this.subscription.add(this.loading$.subscribe(loading => this.loading = loading));
        this.subscription.add(this.node$.subscribe(node => {
          console.log(node.toJS());
          this.node = node;
          if (this.node.get('type') === CRITERIA_TYPES.MEAS) {
            console.log(this.node.toJS());
            // const attrs = {NUM: [], CAT: []};
            this.testAttrs.forEach(attr => {
              switch (attr.type) {
                case 'NUM':
                  if (this.attrs.NUM.length) {
                    this.attrs.NUM[0][attr.conceptName] = attr.estCount;
                  } else {
                    this.attrs.NUM.push({
                      name: '',
                      operator: '',
                      operands: [null],
                      conceptId: attr.conceptId
                    });
                    this.attrs.NUM[0][attr.conceptName] = attr.estCount;
                  }
                  break;
                case 'CAT':
                  if (parseInt(attr.estCount, 10) > 0) {
                    this.attrs.CAT.push(attr);
                  }
              }
            });
          } else {
            this.attrs.NUM = this.node.get('attributes');
            if (this.node.get('subtype') === CRITERIA_SUBTYPES.BP) {
              this.attrs.NUM.forEach((attr, i) => this.dropdowns.labels[i] = attr.name);
            }
          }
        }));
    }

    ngOnDestroy() {
        this.subscription.unsubscribe();
    }

    ngOnChanges(changes: SimpleChanges) {
        // if (changes.node.currentValue.size) {
        //     const currentNode = changes.node.currentValue;
        //     if (currentNode.get('subtype') === CRITERIA_SUBTYPES.BP) {
        //         this.attrs = currentNode.get('predefinedAttributes').toJS();
        //         this.attrs.map(attr => {
        //             attr.operator = '';
        //             attr.operands = [null];
        //             this.labels.push(attr.name);
        //         });
        //     } else {
        //         this.labels = [''];
        //         this.attrs = [{
        //             name: '',
        //             operator: '',
        //             operands: [null],
        //             conceptId: currentNode.get('conceptId', null)
        //         }];
        //     }
        // }
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
        this.attrs.NUM[other].operator = this.dropdowns.oldVals[other] = '';
        this.dropdowns.selected[other] = '';
      }
      this.dropdowns.oldVals[index] = option.value;
    }
  }

  inputChange() {
    this.negativeAlert = false;
    this.attrs.NUM.forEach(attr => {
      attr.operands.forEach(operand => {
        if (operand < 0) {
          this.negativeAlert = true;
        }
      });
    });
  }

  get paramId() {
    return `param${this.node.get('id')}`;
  }

  getParamWithAttributes(values: any) {
    let name = this.node.get('name', '') + ' (';
    this.attrs.NUM.map((attr, i) => {
      if (i > 0) {
        name += ' / ';
      }
      name += (attr.name !== '' ? attr.name + ' ' : '');
      switch (values['operator' + i]) {
        case 'ANY':
          attr.operator = 'ANY';
          attr.operands = [];
          attr.name = 'ANY';
          name += 'Any';
          break;
        case 'BETWEEN':
          attr.operator = Operator.BETWEEN;
          attr.operands = [values['valueA' + i], values['valueB' + i]];
          name += values['valueA' + i] + '-' + values['valueB' + i];
          break;
        case 'EQUAL':
          attr.operator = Operator.EQUAL;
          attr.operands = [values['valueA' + i]];
          name += '= ' + values['valueA' + i];
          break;
        case 'LESS_THAN_OR_EQUAL_TO':
          attr.operator = Operator.LESSTHANOREQUALTO;
          attr.operands = [values['valueA' + i]];
          name += '<= ' + values['valueA' + i];
          break;
        case 'GREATER_THAN_OR_EQUAL_TO':
          attr.operator = Operator.GREATERTHANOREQUALTO;
          attr.operands = [values['valueA' + i]];
          name += '>= ' + values['valueA' + i];
          break;
      }
    });
    name += (this.attrs.NUM[0].name !== 'ANY'
      ? this.units[this.node.get('subtype')]
      : '') + ')';
    return this.node
      .set('parameterId', this.paramId)
      .set('name', name)
      .set('attributes', fromJS(this.attrs.NUM));
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

}
