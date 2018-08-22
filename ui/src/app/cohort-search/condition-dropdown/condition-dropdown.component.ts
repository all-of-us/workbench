import {NgRedux, select} from '@angular-redux/store';
import {Component, Input, OnDestroy, OnInit} from '@angular/core';
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

    launchTree(criteria: any) {
        const itemId = this.actions.generateId('items');
      //  const groupId = this.actions.generateId(this.role);
        const criteriaType = criteria.type;
        const fullTree = criteria.fullTree || false;
      //  this.actions.initGroup(this.role, groupId);
      //  const role = this.role;
       const context = {criteriaType, itemId, fullTree};
          this.actions.openWizard(itemId, context);
    }

}
