import {Component, Input, OnInit} from '@angular/core';
import {DOMAIN_TYPES, PROGRAM_TYPES} from 'app/cohort-search/constant';
import {criteriaMenuOptionsStore, wizardStore} from 'app/cohort-search/search-state.service';
import {domainToTitle, generateId, typeToTitle} from 'app/cohort-search/utils';
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import {triggerEvent} from 'app/utils/analytics';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {CriteriaType, DomainType, SearchRequest} from 'generated/fetch';

@Component({
  selector: 'app-search-group-select',
  templateUrl: './search-group-select.component.html',
  styleUrls: ['./search-group-select.component.css']
})
export class SearchGroupSelectComponent implements OnInit {
  @Input() role: keyof SearchRequest;
  @Input() index: number;

  position = 'bottom-left';
  demoOpen = false;
  demoMenuHover = false;
  category: string;
  criteriaMenuOptions: any;

  ngOnInit(): void {
    const {cdrVersionId} = currentWorkspaceStore.getValue();
    criteriaMenuOptionsStore.subscribe(options => {
      if (this.role === 'includes' && !options[cdrVersionId]) {
        this.getMenuOptions();
      } else if (!!options[cdrVersionId]) {
        this.criteriaMenuOptions = options[cdrVersionId];
        setTimeout(() => this.setDemoMenuHover());
      }
    });
  }

  getMenuOptions() {
    const {cdrVersionId} = currentWorkspaceStore.getValue();
    const criteriaMenuOptions = criteriaMenuOptionsStore.getValue();
    cohortBuilderApi().findCriteriaMenuOptions(+cdrVersionId).then(res => {
      criteriaMenuOptions[cdrVersionId] = res.items.reduce((acc, opt) => {
        if (PROGRAM_TYPES.includes(DomainType[opt.domain])) {
          const option = {name: domainToTitle(opt.domain), domain: opt.domain, type: opt.types[0].type};
          if (opt.domain === DomainType[DomainType.PERSON]) {
            option['children'] = opt.types
              .filter(subopt => subopt.type !== CriteriaType[CriteriaType.DECEASED])
              .map(subopt => ({name: typeToTitle(subopt.type), domain: opt.domain, type: subopt.type}));
          }
          acc.programTypes.push(option);
        }
        if (DOMAIN_TYPES.includes(DomainType[opt.domain])) {
          acc.domainTypes.push({name: domainToTitle(opt.domain), domain: opt.domain, type: opt.types[0].type});
        }
        return acc;
      }, {programTypes: [], domainTypes: []});
      criteriaMenuOptionsStore.next(criteriaMenuOptions);
    });
  }

  setDemoMenuHover() {
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
    const {domain, type, standard} = criteria;
    const category = `${this.role === 'includes' ? 'Add' : 'Excludes'} Criteria`;
    // If domain is PERSON, list the type as well as the domain in the label
    const label = domainToTitle(domain) +
      (domain === DomainType.PERSON ? ' - ' + typeToTitle(type) : '') +
      ' - Cohort Builder';
    triggerEvent(category, 'Click', `${category} - ${label}`);
    const fullTree = criteria.fullTree || false;
    const role = this.role;
    let context: any;
    const itemId = generateId('items');
    const groupId = null;
    const item = this.initItem(itemId, domain, fullTree);
    context = {item, domain, type, standard, role, groupId, itemId, fullTree};
    wizardStore.next(context);
  }

  initItem(id: string, type: string, fullTree: boolean) {
    return {
      id,
      type,
      searchParameters: [],
      modifiers: [],
      count: null,
      temporalGroup: 0,
      isRequesting: false,
      status: 'active',
      fullTree
    };
  }

  setMenuPosition() {
    const category = `${this.role === 'includes' ? 'Add' : 'Excludes'} Criteria`;
    triggerEvent(category, 'Click', `${category} Dropdown - Cohort Builder`);
    const dropdown = document.getElementById(this.role + '-button').getBoundingClientRect();
    this.position = (window.innerHeight - dropdown.bottom < 315) ? 'top-left' : 'bottom-left';
  }
}
