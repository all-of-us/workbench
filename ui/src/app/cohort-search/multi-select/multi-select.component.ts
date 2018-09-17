
import {Component, EventEmitter, Input, OnChanges, OnDestroy, OnInit, Output} from '@angular/core';
import {FormControl} from '@angular/forms';
import {List, Set} from 'immutable';
import {Subscription} from 'rxjs/Subscription';

import {CohortSearchActions} from '../redux';

@Component({
  selector: 'crit-multi-select',
  templateUrl: './multi-select.component.html',
  styleUrls: ['./multi-select.component.css']
})
export class MultiSelectComponent implements OnInit, OnChanges, OnDestroy {
    @Input() includeSearchBox = true;
    @Input() options = List();
    @Input() set initialSelection(opts) {
        const _selections = opts.map(opt => opt.get('parameterId')).toSet();
        this.selected = this.selected.union(_selections);
    }
    @Input() loading: boolean;
    @Input() deleteFlag = false;
    selected = Set<number>();
    filter = new FormControl();
    regex = new RegExp('');
    subscription: Subscription;
    @Input() getParamId: any;
    @Output() addedItems = new EventEmitter<boolean>();
    isTimerInitial: any = false;
    selectedOption =
        {
            selected: ['']
        };
    count = 0;
    constructor(private actions: CohortSearchActions) {}

    ngOnChanges() {
        if (this.getParamId) {
            this.selectedOption.selected[this.getParamId] = '';
        }
    }

    ngOnInit() {
        this.subscription = this.filter.valueChanges
            .map(value => value || '')
            .map(value => new RegExp(value, 'i'))
            .subscribe(regex => this.regex = regex);
    }

    ngOnDestroy() {
        this.subscription.unsubscribe();
    }

    get filteredOptions() {
        return this.options
            .filter(opt => this.regex.test(opt.get('name', '')))
            .filter(opt => !this.selected.has(opt.get('parameterId')));
    }

    select(opt) {
        this.selectedOption.selected[opt.get('parameterId')] = opt.get('parameterId');
        this.actions.addParameter(opt);
        if(this.isTimerInitial) clearTimeout ( this.isTimerInitial );
        this.isTimerInitial = setTimeout (() => {
            this.actions.requestPreview();
        } , 2000 );
    }

    unsetFilter() {
        this.filter.setValue(null);
    }
}


