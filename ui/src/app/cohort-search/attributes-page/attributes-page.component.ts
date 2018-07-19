import {select} from '@angular-redux/store';
import {Component, Input, OnChanges, OnDestroy, OnInit, SimpleChanges} from '@angular/core';
import {NgForm} from '@angular/forms';

import {fromJS, Map} from 'immutable';
import {Subscription} from 'rxjs/Subscription';

import {attributesPreviewStatus, CohortSearchActions} from '../redux';

import {Operator} from 'generated';

import {PM_UNITS} from '../constant';

@Component({
    selector: 'crit-attributes-page',
    templateUrl: './attributes-page.component.html',
    styleUrls: ['./attributes-page.component.css']
})
export class AttributesPageComponent implements OnChanges, OnDestroy, OnInit {
    @select(attributesPreviewStatus) preview$;
    @Input() node: any;
    units = PM_UNITS;
    attrs: any;
    oldVals = ['', ''];
    preview = Map();
    subscription: Subscription;
    alert = false;

    constructor(private actions: CohortSearchActions) { }

    ngOnInit() {
        this.subscription = this.preview$.subscribe(prev => this.preview = prev);
    }

    ngOnDestroy() {
        this.subscription.unsubscribe();
    }

    ngOnChanges(changes: SimpleChanges) {
        if (changes.node.currentValue.size) {
            const currentNode = changes.node.currentValue;
            if (currentNode.get('subtype').substring(0, 2) === 'BP') {
                this.attrs = currentNode.get('predefinedAttributes').toJS();
                this.attrs.map(attr => {
                    attr.operator = '';
                    attr.operands = [null];
                });
            } else {
                this.attrs = [{
                    name: '',
                    operator: '',
                    operands: [null],
                    conceptId: currentNode.get('conceptId', null)
                }];
            }
        }
    }

    selectChange(index: number, newVal: string) {
        if (this.node.get('subtype') === 'BP' && this.oldVals[index] !== newVal) {
            const other = index === 0 ? 1 : 0;
            if (newVal === 'ANY') {
                this.attrs[other].operator = this.oldVals[other] = 'ANY';
            } else if (this.oldVals[index] === 'ANY') {
                this.attrs[other].operator = this.oldVals[other] = '';
            }
            this.oldVals[index] = newVal;
        }
    }

    inputChange(newValue: number) {
        this.alert = newValue < 0 ? true : false;
    }

    get paramId() {
        return `param${this.node.get('id')}`;
    }

    getParamWithAttributes(values: any) {
        let name = this.node.get('name', '') + ' (';
        this.attrs.map((attr, i) => {
            if (i > 0) {
                name += ' / ';
            }
            name += (attr.name !== '' ? attr.name + ' ' : '');
            switch (values['operator' + i]) {
                case 'ANY':
                    attr.operator = Operator.ANY;
                    attr.operands = [];
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
        console.log('not returned');
        name += (this.attrs[0].operator !== Operator.ANY
            ? this.units[this.node.get('subtype')]
            : '') + ')';
        return this.node
            .set('parameterId', this.paramId)
            .set('name', name)
            .set('attributes', fromJS(this.attrs));
    }

    requestPreview(attrform: NgForm) {
        if (this.validateValues(attrform.value)) {
          const param = this.getParamWithAttributes(attrform.value);
          this.actions.addAttributeForPreview(param);
          this.actions.requestAttributePreview();
        } else {
            this.alert = true;
        }
    }

    addAttrs(attrform: NgForm) {
        if (this.validateValues(attrform.value)) {
          const param = this.getParamWithAttributes(attrform.value);
          this.actions.addParameter(param);
          this.actions.hideAttributesPage();
        } else {
          this.alert = true;
        }
    }

    validateValues(values: any) {
      for (const key in values) {
        if (Number.isInteger(values[key]) && values[key] < 0) {
          return false;
        }
      }
      return true;
    }

    cancel() {
        this.actions.hideAttributesPage();
    }

}
