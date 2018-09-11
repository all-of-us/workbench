import {NgRedux, select} from '@angular-redux/store';
import {Component, EventEmitter, Input, OnChanges, OnInit, Output} from '@angular/core';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';
import {
  codeDropdownOptions,
  CohortSearchActions,
  CohortSearchState,
} from '../redux';


@Component({
    selector: 'crit-code-dropdown',
    templateUrl: './code-dropdown.component.html',
    styleUrls: ['./code-dropdown.component.css']
})
export class CodeDropdownComponent implements  OnChanges, OnInit {
    dropDownSelected: string;
    options: any;
    subscription: Subscription;
    @Input() _type: string;
    @Output() onOptionChange = new EventEmitter<string>();
    @select(codeDropdownOptions) options$: Observable<any>;
    constructor(
        private ngRedux: NgRedux<CohortSearchState>,
        private actions: CohortSearchActions
    ) {}

    ngOnInit() {
      this.subscription = this.options$.subscribe(options => {
        this.options = options.toJS();
        if (this.options.length) {
          const selected = this.options.find(option => option.type === this._type);
          console.log(this.options);
          console.log(this._type);
        }
      });
    }

    ngOnChanges() {
        // if (this.selectedType) {
        //     this.dropDownSelected = this.selectedType;
        // } else {
        //     this.dropDownSelected = '';
        // }

    }

    launchTree(option) {
      this.getTree(option);
      this.onOptionChange.emit(option.type);
      this.dropDownSelected = option.name;
    }

    getTree(option: any) {
        const itemId = this.actions.generateId('items');
        const criteriaType = option.type;
        const context = {criteriaType, itemId};
        this.actions.setWizardContext(context);
      //  this.actions.resetStore();
    }

}
