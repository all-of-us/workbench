import {Component, OnDestroy, OnInit } from '@angular/core';
import {
  attributesStore,
  autocompleteStore,
  scrollStore,
  searchRequestStore,
  selectionsStore,
  subtreePathStore,
  subtreeSelectedStore,
  wizardStore
} from 'app/cohort-search/search-state.service';
import {domainToTitle, generateId, stripHtml, typeToTitle} from 'app/cohort-search/utils';
import {triggerEvent} from 'app/utils/analytics';
import {CriteriaType, DomainType, TemporalMention, TemporalTime} from 'generated/fetch';
import {Subscription} from 'rxjs/Subscription';


@Component({
  selector: 'app-list-modal',
  templateUrl: './modal.component.html',
  styleUrls: [
    './modal.component.css',
    '../../styles/buttons.css',
  ]
})
export class ModalComponent implements OnInit, OnDestroy {

  readonly domainType = DomainType;
  readonly criteriaType = CriteriaType;
  subscription: Subscription;
  selections = {};
  selectionIds: Array<string>;
  selectionList: Array<any>;
  open = false;
  noSelection = true;
  title = '';
  mode = 'list';
  backMode: string;
  count = 0;
  loadingSubtree = false;
  modifiersDisabled = false;
  conceptType: string = null;
  wizard: any;
  attributesNode: any;
  hierarchyNode: any;

  constructor() {}

  ngOnInit() {
    this.subscription = wizardStore
      .filter(wizard => !!wizard)
      .subscribe(wizard => {
        // reset to default each time the modal is opened
        this.wizard = wizard;
        this.selectionList = wizard.item.searchParameters;
        this.noSelection = this.selectionList.length === 0;
        if (!this.open) {
          this.title = wizard.domain === DomainType.PERSON ? typeToTitle(wizard.type) : domainToTitle(wizard.domain);
          if (this.initTree) {
            this.hierarchyNode = {
              domainId: wizard.domain,
              type: wizard.type,
              isStandard: wizard.standard,
              id: 0,
            };
            this.backMode = 'tree';
            this.mode = 'tree';
          } else {
            this.backMode = 'list';
            this.mode = 'list';
          }
          this.open = true;
        }
      });

    this.subscription.add(selectionsStore.subscribe(list => this.selectionIds = list));
    this.subscription.add(subtreeSelectedStore.subscribe(
      sel => this.loadingSubtree = sel !== undefined));
    this.subscription.add(scrollStore.filter(id => !!id).subscribe(id => this.setScroll(id)));
    this.subscription.add(attributesStore
      .filter(crit => !!crit)
      .subscribe(criterion => {
        this.backMode = this.mode;
        this.attributesNode = criterion;
        this.mode = 'attributes';
      }));
  }

  setScroll(id: string) {
    const nodeId = `node${id}`;
    const node = document.getElementById(nodeId);
    if (node) {
      setTimeout(() => node.scrollIntoView({behavior: 'smooth', block: 'center'}), 200);
    }
    this.loadingSubtree = false;
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  close() {
    wizardStore.next(undefined);
    autocompleteStore.next('');
    selectionsStore.next([]);
    subtreePathStore.next([]);
    attributesStore.next( undefined);
    this.hierarchyNode = undefined;
    this.loadingSubtree = false;
    this.open = false;
  }

  back = () => {
    switch (this.mode) {
      case 'tree':
        this.backMode = 'list';
        this.mode = 'list';
        break;
      default:
        this.mode = this.backMode;
        this.attributesNode = undefined;
        break;
    }
  }

  finish() {
    const {domain, groupId, item, role, type} = this.wizard;
    if (domain === DomainType.PERSON) {
      triggerEvent('Cohort Builder Search', 'Click', `Demo - ${typeToTitle(type)} - Finish`);
    }
    const searchRequest = searchRequestStore.getValue();
    if (groupId) {
      const groupIndex = searchRequest[role].findIndex(grp => grp.id === groupId);
      if (groupIndex > -1) {
        const itemIndex = searchRequest[role][groupIndex].items.findIndex(it => it.id === item.id);
        if (itemIndex > -1) {
          searchRequest[role][groupIndex].items[itemIndex] = item;
        } else {
          searchRequest[role][groupIndex].items.push(item);
        }
      }
    } else {
      const group = this.initGroup(role, item);
      searchRequest[role].push(group);
    }
    searchRequestStore.next(searchRequest);
    this.close();
  }

  initGroup(role: string, item: any) {
    return {
      id: generateId(role),
      items: [item],
      count: null,
      temporal: false,
      mention: TemporalMention.ANYMENTION,
      time: TemporalTime.DURINGSAMEENCOUNTERAS,
      timeValue: 0,
      timeFrame: '',
      isRequesting: false,
      status: 'active'
    };
  }

  get attributeTitle() {
    const domain = this.attributesNode.domainId;
    return domain === DomainType.PHYSICALMEASUREMENT
      ? stripHtml(this.attributesNode.name)
      : domain + ' Detail';
  }

  get showModifiers() {
    return this.wizard.domain !== DomainType.PHYSICALMEASUREMENT &&
      this.wizard.domain !== DomainType.PERSON &&
      this.wizard.domain !== DomainType.SURVEY;
  }

  get initTree() {
    return this.wizard.domain === DomainType.PHYSICALMEASUREMENT
      || this.wizard.domain === DomainType.SURVEY
      || this.wizard.domain === DomainType.VISIT;
  }

  get showNext() {
    return this.showModifiers && this.mode !== 'modifiers';
  }

  get showBack() {
    return this.showModifiers && this.mode === 'modifiers';
  }

  get treeClass() {
    if (this.wizard.domain === DomainType.PERSON) {
      return this.wizard.type === CriteriaType.AGE ? 'col-md-12' : 'col-md-6';
    }
    return 'col-md-8';
  }

  get sidebarClass() {
    return this.wizard.domain === DomainType.PERSON ? 'col-md-6' : 'col-md-4';
  }

  setMode(mode: any) {
    if (mode === 'modifiers') {
      triggerEvent(
        'Cohort Builder Search',
        'Click',
        `Modifiers - ${domainToTitle(this.wizard.domain)} - Cohort Builder Search`
      );
    }
    if (this.mode !== 'attributes') {
      this.backMode = this.mode;
    }
    this.mode = mode;
  }

  showHierarchy = (criterion: any) => {
    autocompleteStore.next(criterion.name);
    subtreePathStore.next(criterion.path.split('.'));
    subtreeSelectedStore.next(criterion.id);
    this.hierarchyNode = {
      domainId: criterion.domainId,
      type: criterion.type,
      subtype: criterion.subtype,
      isStandard: criterion.isStandard,
      id: 0,    // root parent ID is always 0
    };
    this.backMode = 'tree';
    this.mode = 'tree';
  }

  modifiersFlag = (disabled: boolean) => {
    this.modifiersDisabled = disabled;
  }

  get disableFlag() {
    return this.noSelection || this.modifiersDisabled;
  }
}
