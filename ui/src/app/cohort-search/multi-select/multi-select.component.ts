
import {Component, ViewChild, ElementRef, EventEmitter, Input, OnChanges, OnDestroy, OnInit, Output} from '@angular/core';
import {FormControl} from '@angular/forms';
import {List, Set} from 'immutable';
import {Subscription} from 'rxjs/Subscription';
import {NgRedux, select} from '@angular-redux/store';
import {
    activeParameterList,
    CohortSearchActions,
    CohortSearchState,
    subtreeSelected
} from '../redux';
import {Observable} from "rxjs/Observable";

@Component({
    selector: 'crit-multi-select',
    templateUrl: './multi-select.component.html',
    styleUrls: ['./multi-select.component.css']
})
export class MultiSelectComponent implements OnInit, OnChanges, OnDestroy {
    @select(subtreeSelected) selected$: Observable<any>;
    @Input() includeSearchBox = true;
    @Input() options = List();
    @Input() set initialSelection(opts) {
        const _selections = opts.map(opt => opt.get('parameterId')).toSet();
        this.selected = this.selected.union(_selections);
        console.log(JSON.stringify(this.selected))
    }
    @Input() loading: boolean;
    @Input() deleteFlag = false;
    selected = Set<number>();
    filter = new FormControl();
    regex = new RegExp('');
    subscription: Subscription;
    @Input() getParamId: any;
    @Output() addedItems = new EventEmitter<boolean>();
    selectedOption =
        {
            selected: ['']
        };
    @ViewChild('target') input: ElementRef;
    constructor(private actions: CohortSearchActions,
                private ngRedux: NgRedux<CohortSearchState>,) {}

    ngOnChanges(){
        if(this.getParamId){
            this.selectedOption.selected[this.getParamId] = '';
            // this.actions.removeParameter(this.getParamId);
            // this.selectedOptions[i] = this.getParamId

            // console.log("deleted array-------->>>>");
            //  console.log(JSON.stringify( this.selectedOptions));
            // this.demoParamId = this.getParamId
            // this.subscription = this.ngRedux
            //     .select(activeParameterList)
            //     .subscribe(val => {
            //         if(val){
            //             this.selectedOption.selected[this.getParamId] = this.getParamId;
            //             console.log(this.selectedOption.selected[this.getParamId]);
            //             // console.log(JSON.stringify( this.selected));
            //         }
            //     });
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

    // get selectedOptions() {
    //      return this.options.filter(opt => this.selected.has(opt.get('name')));
    // }

    select(opt) {

        this.selectedOption.selected[opt.get('parameterId')]= opt.get('parameterId');
        this.actions.addParameter(opt);
        // this.addedItems.emit(true);


        setTimeout (() => {
            // if(flag){
            this.addedItems.emit(true);
            // this.actions.requestPreview();
            // }
        } , 3000 );

    }

    unsetFilter() {
        this.filter.setValue(null);
    }
}


