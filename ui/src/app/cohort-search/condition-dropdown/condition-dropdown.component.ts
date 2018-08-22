import {NgRedux, select} from '@angular-redux/store';
import {Component, EventEmitter, Input, OnDestroy, OnInit, Output} from '@angular/core';
import {DomainType} from 'generated';
import {Subscription} from 'rxjs/Subscription';
import {CRITERIA_SUBTYPES, DOMAIN_TYPES} from '../constant';
import {
    autocompleteError,
    autocompleteOptions,
    CohortSearchActions,
    CohortSearchState,
    ingredientsForBrand,
    isAutocompleteLoading,
} from '../redux';

import {highlightMatches} from '../utils';
import {SearchRequest} from "../../../generated";

@Component({
    selector: 'app-crit-dropdown',
    templateUrl: './condition-dropdown.component.html',
    styleUrls: ['./condition-dropdown.component.css']
})
export class ConditionDropdownComponent implements OnInit, OnDestroy {
    @Input() _type;
    @Input() role: keyof SearchRequest;
    searchTerm = '';
    options = [];
    ingredients: any;
    loading = false;
    noResults = false;
    optionSelected = false;
    multiIngredient = false;
    error = false;
    subscription: Subscription;
    @Output() onOptionChange = new EventEmitter<string>();
    readonly domainTypes = DOMAIN_TYPES;
    constructor(
        private ngRedux: NgRedux<CohortSearchState>,
        private actions: CohortSearchActions
    ) {}

    ngOnInit() {

    }

    ngOnDestroy() {
        // this.options = [];
        // this.subscription.unsubscribe();
    }
    launchTree(criteria) {
        console.log(criteria.name);
        // type: criteria.type
        this.getTree(criteria);
        this.onOptionChange.emit(criteria.name);

        //    check for launchwizard function
    }


    getTree(criteria: any) {
        const itemId = this.actions.generateId('items');
        const groupId = this.actions.generateId("excludes");
        const criteriaType = criteria.type;
        const fullTree = criteria.fullTree || false;
       this.actions.initGroup("excludes", groupId);
        const role = "excludes";
       const context = {criteriaType, role, groupId, itemId, fullTree};
          this.actions.setWizardContext(context);
    }

}
