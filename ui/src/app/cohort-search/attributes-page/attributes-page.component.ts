import {select} from '@angular-redux/store';
import {Component, Input, OnChanges, OnInit, SimpleChanges} from '@angular/core';
import {NgForm} from '@angular/forms';

import {Map} from 'immutable';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';

import {CohortSearchActions, previewStatus} from '../redux';

@Component({
    selector: 'crit-attributes-page',
    templateUrl: './attributes-page.component.html',
    styleUrls: ['./attributes-page.component.css']
})
export class AttributesPageComponent implements OnChanges, OnInit {
    @select(previewStatus) preview$: Observable<any>;
    @Input() node: any;
    fields: Array<string>;
    preview = Map();
    subscription: Subscription;

    constructor(private actions: CohortSearchActions) { }

    ngOnInit() {
        this.subscription = this.preview$.subscribe(prev => {
            if (!prev.get('requesting')) {
                // remove parameter from list once preview request has finished
                this.actions.removeParameter(this.paramId);
            }
            this.preview = prev;
        });
    }

    ngOnChanges(changes: SimpleChanges) {
        if (changes.node) {
            const currentNode = changes.node.currentValue;
            if (currentNode.get('subtype') === 'BP') {
                this.fields = ['Systolic', 'Diastolic'];
            } else {
                this.fields = [''];
            }
        }
    }

    get paramId() {
        return `param${this.node.get('id')}`;
    }

    getParamWithAttributes(values: any) {
        let code = '';
        let name = this.node.get('name', '') + ' (';
        this.fields.forEach((field, i) => {
            if (i > 0) {
                code += ';';
                name += ' / ';
            }
            if (values['operator' + i] === 'between') {
                code += 'between;' + values['valueA' + i] + ' and ' + values['valueB' + i];
                name += (field !== '' ? field + ' ' : '')
                    + values['valueA' + i] + '-'
                    + values['valueB' + i];
            } else {
                code += code += values['operator' + i] + ';' + values['valueA' + i];
                name += (field !== '' ? field + ' ' : '')
                    + values['operator' + i] + ' '
                    + values['valueA' + i];
            }
        });
        name += ')';
        return this.node
            .set('parameterId', this.paramId)
            .set('code', code)
            .set('name', name);
    }

    requestPreview(attrs: NgForm) {
        const param = this.getParamWithAttributes(attrs.value);
        this.actions.addParameter(param);
        this.actions.requestPreview();
    }

    addAttrs(attrs: NgForm) {
        const param = this.getParamWithAttributes(attrs.value);
        this.actions.addParameter(param);
        this.actions.hideAttributesPage();
    }

    cancel() {
        this.actions.hideAttributesPage();
    }

}
