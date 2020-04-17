import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {searchRequestStore, wizardStore} from 'app/cohort-search/search-state.service';
import {domainToTitle, generateId, stripHtml, typeToTitle} from 'app/cohort-search/utils';
import {triggerEvent} from 'app/utils/analytics';
import {environment} from 'environments/environment';
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
  @Input() dataFilters: Array<string>;
  readonly domainType = DomainType;
  readonly criteriaType = CriteriaType;
  subscription: Subscription;
  selectedIds: Array<string>;
  selections: Array<any>;
  groupSelections: Array<number> = [];
  open = false;
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
  publicUiUrl = environment.publicUiUrl;
  treeSearchTerms = '';
  autocompleteSelection: any;

  ngOnInit() {
    this.subscription = wizardStore
      .filter(wizard => !!wizard)
      .subscribe(wizard => {
        // reset to default each time the modal is opened
        this.wizard = wizard;
        if (!this.open) {
          this.title = wizard.domain === DomainType.PERSON ? typeToTitle(wizard.type) : domainToTitle(wizard.domain);
          this.selections = wizard.item.searchParameters;
          this.selectedIds = this.selections.map(s => s.parameterId);
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
  }

  setScroll = (id: string) => {
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
    this.attributesNode = undefined;
    this.autocompleteSelection = undefined;
    this.hierarchyNode = undefined;
    this.loadingSubtree = false;
    this.selectedIds = [];
    this.selections = [];
    this.treeSearchTerms = '';
    this.open = false;
  }

  back = () => {
    switch (this.mode) {
      case 'tree':
        this.autocompleteSelection = undefined;
        this.backMode = 'list';
        this.hierarchyNode = undefined;
        this.mode = 'list';
        break;
      default:
        this.attributesNode = undefined;
        this.mode = this.backMode;
        break;
    }
  }

  finish() {
    const {domain, groupId, item, role, type} = this.wizard;
    if (domain === DomainType.PERSON) {
      triggerEvent('Cohort Builder Search', 'Click', `Demo - ${typeToTitle(type)} - Finish`);
    }
    const searchRequest = searchRequestStore.getValue();
    item.searchParameters = this.selections;
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
      timeValue: '',
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

  get showDataBrowserLink() {
    return (this.wizard.domain === DomainType.CONDITION
      || this.wizard.domain === DomainType.PROCEDURE
      || this.wizard.domain === DomainType.MEASUREMENT
      || this.wizard.domain === DomainType.DRUG)
      && (this.mode === 'list' || this.mode === 'tree');
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

  get nonAgeDemographics() {
    return this.wizard.domain === DomainType.PERSON && this.wizard.type !== CriteriaType.AGE;
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
    this.loadingSubtree = true;
    this.treeSearchTerms = criterion.name;
    this.autocompleteSelection = criterion;
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
    return this.selections.length === 0 || this.modifiersDisabled;
  }

  setTreeSearchTerms = (input: string) => {
    this.treeSearchTerms = input;
  }

  setAutocompleteSelection = (selection: any) => {
    this.loadingSubtree = true;
    this.autocompleteSelection = selection;
  }

  setAttributes = (criterion: any) => {
    this.backMode = this.mode;
    this.attributesNode = criterion;
    this.mode = 'attributes';
  }

  addSelection = (param: any) => {
    if (this.selectedIds.includes(param.parameterId)) {
      this.selections = this.selections.filter(p => p.parameterId !== param.parameterId);
    } else {
      this.selectedIds = [...this.selectedIds, param.parameterId];
      if (param.group) {
        this.groupSelections = [...this.groupSelections, param.id];
      }
    }
    this.selections = [...this.selections, param];
  }

  removeSelection = (param: any) => {
    this.selectedIds = this.selectedIds.filter(id => id !== param.parameterId);
    this.selections = this.selections.filter(sel => sel.parameterId !== param.parameterId);
    if (param.group) {
      this.groupSelections = this.groupSelections.filter(id => id !== param.id);
    }
  }
}
