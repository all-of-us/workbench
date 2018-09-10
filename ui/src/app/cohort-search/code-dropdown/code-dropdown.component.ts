import {NgRedux} from '@angular-redux/store';
import {Component, EventEmitter, Input, OnChanges, Output} from '@angular/core';
import {DOMAIN_TYPES} from '../constant';
import {
    CohortSearchActions,
    CohortSearchState,
} from '../redux';


@Component({
    selector: 'crit-code-dropdown',
    templateUrl: './code-dropdown.component.html',
    styleUrls: ['./code-dropdown.component.css']
})
export class CodeDropdownComponent implements  OnChanges {
     @Input() selectedType;
    dropDownSelected = '';
    @Output() onOptionChange = new EventEmitter<string>();
    readonly domainTypes = DOMAIN_TYPES;
    constructor(
        private ngRedux: NgRedux<CohortSearchState>,
        private actions: CohortSearchActions
    ) {}

    ngOnChanges() {
        if (this.selectedType) {
            this.dropDownSelected = this.selectedType;
        } else {
            this.dropDownSelected = '';
        }

    }

    launchTree(criteria) {
        this.getTree(criteria);
        this.onOptionChange.emit(criteria.name);
    }

    getTree(criteria: any) {
        const itemId = this.actions.generateId('items');
        const criteriaType = criteria.type;
        const context = {criteriaType, itemId};
        this.actions.setWizardContext(context);
      //  this.actions.resetStore();
    }

}
