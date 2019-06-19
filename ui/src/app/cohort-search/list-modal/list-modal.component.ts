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
import {domainToTitle, stripHtml} from 'app/cohort-search/utils';
import {CriteriaType, DomainType} from 'generated/fetch';
import {Subscription} from 'rxjs/Subscription';


@Component({
  selector: 'app-list-modal',
  templateUrl: './list-modal.component.html',
  styleUrls: [
    './list-modal.component.css',
    '../../styles/buttons.css',
  ]
})
export class ListModalComponent implements OnInit, OnDestroy {

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
  disableCursor = false;
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
          this.title = domainToTitle(wizard.domain);
          if (wizard.domain === DomainType.PHYSICALMEASUREMENT) {
            this.hierarchyNode = {
              domainId: wizard.domain,
              type: wizard.type,
              isStandard: wizard.standard,
              id: 0,    // root parent ID is always 0
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
    this.disableCursor = false;
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  close() {
    const {groupId, role} = this.wizard;
    const sr = searchRequestStore.getValue();
    const group = sr[role].find(grp => grp.id === groupId);
    // TODO change condition to check for total count instead of items when api call is ready
    if (group.items.length === 0) {
      sr[role] = sr[role].filter(grp => grp.id !== groupId);
      searchRequestStore.next(sr);
    }
    wizardStore.next(undefined);
    selectionsStore.next([]);
    subtreePathStore.next([]);
    this.hierarchyNode = null;
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
    const {groupId, item, role} = this.wizard;
    const searchRequest = searchRequestStore.getValue();
    const groupIndex = searchRequest[role].findIndex(grp => grp.id === groupId);
    const itemIndex = searchRequest[role][groupIndex].items.findIndex(it => it.id === item.id);
    if (itemIndex > -1) {
      searchRequest[role][groupIndex].items[itemIndex] = item;
    } else {
      searchRequest[role][groupIndex].items.push(item);
    }
    searchRequestStore.next(searchRequest);
    this.close();
  }

  get attributeTitle() {
    const domain = this.attributesNode.domainId;
    return domain === DomainType[DomainType.PHYSICALMEASUREMENT]
      ? stripHtml(this.attributesNode.name)
      : domain + ' Detail';
  }

  get showModifiers() {
    return this.wizard.domain !== DomainType.PHYSICALMEASUREMENT &&
      this.wizard.domain !== DomainType.PERSON &&
      this.wizard.domain !== DomainType.SURVEY;
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
