import {NgRedux, select} from '@angular-redux/store';
import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
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
export class CodeDropdownComponent implements  OnInit {
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
      this.subscription = this.options$
        .filter(options => !!options)
        .subscribe(options => {
          this.options = options.toJS();
          if (this.options.length) {
            this.dropDownSelected = this.options.find(option => option.type === this._type).name;
          }
      });
    }

    launchTree(option) {
      this.getTree(option);
      this.onOptionChange.emit(option.type);
      this.dropDownSelected = option.name;
    }

    getTree(option: any) {
        const itemId = this.actions.generateId('items');
        const criteriaType = option.type;
        const criteriaSubtype = option.subtype;
        const context = {criteriaType, criteriaSubtype, itemId};
        this.actions.setWizardContext(context);
    }

}
