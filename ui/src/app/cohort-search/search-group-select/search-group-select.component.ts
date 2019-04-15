import {AfterViewInit, Component, Input} from '@angular/core';

import {DOMAIN_TYPES, LIST_DOMAIN_TYPES, LIST_PROGRAM_TYPES, PROGRAM_TYPES} from 'app/cohort-search/constant';
import {CohortSearchActions} from 'app/cohort-search/redux';
import {searchRequestStore, wizardStore} from 'app/cohort-search/search-state.service';
import {environment} from 'environments/environment';

import {SearchRequest} from 'generated';
import {SearchGroup} from '../../../generated/fetch';

@Component({
  selector: 'app-search-group-select',
  templateUrl: './search-group-select.component.html',
  styleUrls: ['./search-group-select.component.css']
})
export class SearchGroupSelectComponent implements AfterViewInit {
  @Input() role: keyof SearchRequest;
  @Input() index: number;

  readonly domainTypes = environment.enableCBListSearch ? LIST_DOMAIN_TYPES : DOMAIN_TYPES;
  readonly programTypes = environment.enableCBListSearch ? LIST_PROGRAM_TYPES : PROGRAM_TYPES;
  position = 'bottom-left';

  demoOpen = false;
  demoMenuHover = false;

  constructor(private actions: CohortSearchActions) {}

  ngAfterViewInit(): void {
    /* Open nested menu on hover */
    const demoItem = document.getElementById('DEMO-' + this.index);
    if (demoItem) {
      demoItem.addEventListener('mouseenter', () => {
        this.demoOpen = true;
        setTimeout(() => {
          const demoMenu = document.getElementById('demo-menu-' + this.index);
          demoMenu.addEventListener('mouseenter', () => this.demoMenuHover = true);
          demoMenu.addEventListener('mouseleave', () => this.demoMenuHover = false);
        });
      });
      demoItem.addEventListener('mouseleave', () => this.demoOpen = false);
    }
  }

  launchWizard(criteria: any) {
    const itemId = this.actions.generateId('items');
    const groupId = this.actions.generateId(this.role);
    const fullTree = criteria.fullTree || false;
    const codes = criteria.codes || false;
    const role = this.role;
    let context: any;
    if (environment.enableCBListSearch) {
      const {domain, type} = criteria;
      const searchRequest = searchRequestStore.getValue();
      const group = this.initGroup(groupId);
      searchRequest[this.role].push(group);
      searchRequestStore.next(searchRequest);
      context = {domain, type, role, groupId, itemId, fullTree, codes};
      wizardStore.next(context);
    } else {
      this.actions.initGroup(this.role, groupId);
      const criteriaType = criteria.codes ? criteria.codes[0].type : criteria.type;
      const criteriaSubtype = criteria.codes ? criteria.codes[0].subtype : criteria.subtype;
      context = {criteriaType, criteriaSubtype, role, groupId, itemId, fullTree, codes};
      this.actions.openWizard(itemId, criteria.type, context);
    }
  }

  initGroup(id: string) {
    return {
      id,
      items: [],
      count: null,
      temporal: false,
      mention: null,
      time: null,
      timeValue: 0,
      timeFrame: '',
      isRequesting: false,
      status: 'active'
    } as SearchGroup;
  }

  setMenuPosition() {
    const dropdown = document.getElementById(this.role + '-button').getBoundingClientRect();
    this.position = (window.innerHeight - dropdown.bottom < 315) ? 'top-left' : 'bottom-left';
  }
}
