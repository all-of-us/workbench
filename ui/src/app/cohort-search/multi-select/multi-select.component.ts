
import {Component, ElementRef, EventEmitter, Input, OnChanges, OnDestroy, OnInit, Output, ViewChild} from '@angular/core';
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
import {Observable} from 'rxjs/Observable';

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
    @ViewChild('target') input: ElementRef;
    count: number = 0;
    constructor(private actions: CohortSearchActions,
                private ngRedux: NgRedux<CohortSearchState>) {}

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

    // get selectedOptions() {
    //      return this.options.filter(opt => this.selected.has(opt.get('name')));
    // }

    select(opt) {

        this.selectedOption.selected[opt.get('parameterId')] = opt.get('parameterId');
        this.actions.addParameter(opt);
        // this.addedItems.emit(true);
         console.log("timer value", this.isTimerInitial);

        if(this.isTimerInitial) clearTimeout( this.isTimerInitial )

        this.isTimerInitial = setTimeout (() => {
            this.actions.requestPreview();
        } , 3000 );

    }


    unsetFilter() {
        this.filter.setValue(null);
    }
}


