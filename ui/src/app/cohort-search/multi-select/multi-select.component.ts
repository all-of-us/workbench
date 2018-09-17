
import {Component, EventEmitter, Input, OnDestroy, OnInit, Output} from '@angular/core';
import {FormControl} from '@angular/forms';
import {List, Set} from 'immutable';
import {Subscription} from 'rxjs/Subscription';

import {activeParameterList, CohortSearchActions, CohortSearchState, isParameterActive} from '../redux';
import {NgRedux} from "@angular-redux/store";
import {TreeType} from 'generated';
@Component({
  selector: 'crit-multi-select',
  templateUrl: './multi-select.component.html',
  styleUrls: ['./multi-select.component.css']
})
export class MultiSelectComponent implements OnInit, OnDestroy {
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
    selectedOption:any;
        // {
        //     selected: ['']
        // };
    count = 0;
    constructor(private actions: CohortSearchActions,
                private ngRedux: NgRedux<CohortSearchState>) {}


    ngOnInit() {
        this.subscription = this.filter.valueChanges
            .map(value => value || '')
            .map(value => new RegExp(value, 'i'))
            .subscribe(regex => this.regex = regex);

        this.subscription.add (this.ngRedux
            .select(activeParameterList)
            .subscribe(val => {
                // console.log(val.toJS());
                this.selectedOption = [];
                val.forEach( paramList =>{
                    // console.log(val.toJS());
                    if(paramList.get('type') === TreeType.DEMO){
                        this.selectedOption.push(paramList.get('parameterId'));
                    }
                })
            }));
    }

    ngOnDestroy() {
        this.subscription.unsubscribe();
    }

    get filteredOptions() {
        return this.options
            .filter(opt => this.regex.test(opt.get('name', '')));
    }

    select(opt) {
        this.actions.addParameter(opt);
        if (this.isTimerInitial) {
            clearTimeout ( this.isTimerInitial );
        }
        this.isTimerInitial = setTimeout (() => {
            this.actions.requestPreview();
        } , 2000 );
    }

    unsetFilter() {
        this.filter.setValue(null);
    }
}
