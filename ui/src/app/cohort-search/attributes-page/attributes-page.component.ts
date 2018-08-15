import {select} from '@angular-redux/store';
import {Component, Input, OnChanges, OnDestroy, OnInit, SimpleChanges} from '@angular/core';
import {NgForm} from '@angular/forms';

import {fromJS, Map} from 'immutable';
import {Subscription} from 'rxjs/Subscription';

import {
    attributesPreviewStatus,
  CohortSearchActions,
  isAttributeLoading,
  loadAttributes,
} from '../redux';

import {Operator} from 'generated';

import {CRITERIA_SUBTYPES, PM_UNITS} from '../constant';

@Component({
    selector: 'crit-attributes-page',
    templateUrl: './attributes-page.component.html',
    styleUrls: ['./attributes-page.component.css']
})
export class AttributesPageComponent implements OnChanges, OnDestroy, OnInit {
    @select(attributesPreviewStatus) preview$;
    @select(isAttributeLoading) loading$;
    @select(loadAttributes) attributes$;
    @Input() node: any;
    units = PM_UNITS;
    attrs: any;
    attributes: any;
    oldVals = ['', ''];
    preview = Map();
    subscription: Subscription;
    negativeAlert = false;
    loading: boolean;

    constructor(private actions: CohortSearchActions) { }

    ngOnInit() {
        this.subscription = this.preview$.subscribe(prev => this.preview = prev);
        this.subscription.add(this.loading$.subscribe(loading => this.loading = loading));
        this.subscription.add(
          this.attributes$.subscribe(attributes => this.attributes = attributes)
        );
    }

    ngOnDestroy() {
        this.subscription.unsubscribe();
    }

    ngOnChanges(changes: SimpleChanges) {
        if (changes.node.currentValue.size) {
            const currentNode = changes.node.currentValue;
            if (currentNode.get('subtype') === CRITERIA_SUBTYPES.BP) {
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
        if (this.node.get('subtype') === CRITERIA_SUBTYPES.BP && this.oldVals[index] !== newVal) {
            const other = index === 0 ? 1 : 0;
            if (newVal === 'ANY') {
                this.attrs[other].operator = this.oldVals[other] = 'ANY';
            } else if (this.oldVals[index] === 'ANY') {
                this.attrs[other].operator = this.oldVals[other] = '';
            }
            this.oldVals[index] = newVal;
        }
    }

    inputChange() {
      this.negativeAlert = false;
        this.attrs.forEach(attr => {
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
        name += (this.attrs[0].operator !== Operator.ANY
            ? this.units[this.node.get('subtype')]
            : '') + ')';
        return this.node
            .set('parameterId', this.paramId)
            .set('name', name)
            .set('attributes', fromJS(this.attrs));
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
