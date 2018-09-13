import {NgRedux, select} from '@angular-redux/store';
import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';
import {
  activeItem,
  codeDropdownOptions,
  CohortSearchActions,
  CohortSearchState,
} from '../redux';
import {getCodeOptions} from '../utils';


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
    @select(activeItem) activeItem$: Observable<any>;
    @select(codeDropdownOptions) options$: Observable<any>;
    constructor(
        private ngRedux: NgRedux<CohortSearchState>,
        private actions: CohortSearchActions
    ) {}

    ngOnInit() {
      this.subscription = this.activeItem$
        .filter(item => !!item)
        .subscribe(item => {
          this.options = getCodeOptions(item.get('type'));
          const selected = this.options
            ? this.options.find(option => option.type === this._type) : null;
          this.dropDownSelected = selected ? selected.name : null;
      });
    }

    launchTree(option) {
      this.getTree(option);
      this.onOptionChange.emit(option.type);
      this.dropDownSelected = option.name;
      this.actions.changeCodeOption();
    }

    getTree(option: any) {
        const itemId = this.actions.generateId('items');
        const criteriaType = option.type;
        const criteriaSubtype = option.subtype;
        const context = {criteriaType, criteriaSubtype, itemId};
        this.actions.setWizardContext(context);
    }

}
