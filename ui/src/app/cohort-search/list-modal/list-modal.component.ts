import {select} from '@angular-redux/store';
import {Component, OnDestroy, OnInit } from '@angular/core';
import {DOMAIN_TYPES, PROGRAM_TYPES} from 'app/cohort-search/constant';
import {
  activeCriteriaSubtype,
  activeCriteriaTreeType,
  activeCriteriaType,
  activeItem,
  activeParameterList,
  CohortSearchActions,
  nodeAttributes,
  previewStatus,
  scrollId,
  subtreeSelected,
  wizardOpen,
} from 'app/cohort-search/redux';
import {scrollStore, searchRequestStore, selectedPathStore, selectedStore, selectionsStore, wizardStore} from 'app/cohort-search/search-state.service';
import {stripHtml, subtypeToTitle, typeToTitle} from 'app/cohort-search/utils';
import {TreeSubType, TreeType} from 'generated';
import {DomainType} from 'generated/fetch';
import {Map} from 'immutable';
import {Observable} from 'rxjs/Observable';
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
  @select(wizardOpen) open$: Observable<boolean>;
  @select(activeCriteriaSubtype) criteriaSubtype$: Observable<any>;
  @select(activeCriteriaType) criteriaType$: Observable<string>;
  @select(activeCriteriaTreeType) isFullTree$: Observable<boolean>;
  @select(activeItem) item$: Observable<any>;
  @select(activeParameterList) selection$: Observable<any>;
  @select(nodeAttributes) attributes$: Observable<any>;
  @select(scrollId) scrollTo$: Observable<any>;
  @select(subtreeSelected) subtree$: Observable<any>;
  @select(previewStatus) preview$;

  readonly domainType = DomainType;
  readonly treeType = TreeType;
  readonly treeSubType = TreeSubType;
  ctype: string;
  subtype: string;
  itemType: string;
  fullTree: boolean;
  subscription: Subscription;
  attributesNode: Map<any, any> = Map();
  selections = {};
  selectionIds: Array<string>;
  selectionList: Array<any>;
  open = false;
  noSelection = true;
  title = '';
  mode: 'tree' | 'list' | 'modifiers' | 'attributes' | 'snomed' = 'list';
  backMode: string;
  count = 0;
  originalNode: any;
  disableCursor = false;
  modifiersDisabled = false;
  preview = Map();
  conceptType: string = null;
  wizard: any;
  attributesCrit: any;
  hierarchyCrit: any;

  constructor(private actions: CohortSearchActions) {}

  ngOnInit() {
    this.subscription = wizardStore
      .filter(wizard => !!wizard)
      .subscribe(wizard => {
        // reset to default each time the modal is opened
        this.wizard = wizard;
        this.selectionList = wizard.item.searchParameters;
        this.noSelection = this.selectionList.length === 0;
        if (!this.open) {
          this.title = wizard.domain;
          this.mode = 'list';
          this.open = true;
        }
      });

    this.subscription.add(selectionsStore.subscribe(list => this.selectionIds = list));

    this.subscription.add(scrollStore.filter(id => !!id).subscribe(id => this.setScroll(id)));

    this.subscription.add(this.preview$.subscribe(prev => this.preview = prev));

    this.subscription.add(this.criteriaType$
      .filter(ctype => !!ctype)
      .subscribe(ctype => {
        this.ctype = ctype;
      })
    );

    this.subscription.add(this.criteriaSubtype$
      .subscribe(subtype => {
        this.subtype = subtype;
      })
    );

    this.subscription.add(this.isFullTree$.subscribe(fullTree => this.fullTree = fullTree));
    this.subscription.add(this.selection$
      .subscribe(selections => {
        this.selections = {};
        // this.noSelection = selections.size === 0;
        selections.forEach(selection => {
          if (this.isCondOrProc) {
            this.conceptType = selection.get('type') === TreeType[TreeType.SNOMED]
              ? 'standard' : 'source';
          }
          this.addSelectionToGroup(selection);
        });
        if (this.conceptType === 'standard') {
          this.setMode('snomed');
        }
      })
    );
    this.subscription.add(this.attributes$
      .subscribe(node => {
        this.attributesNode = node;
        if (node.size === 0) {
          // this.mode = 'tree';
        } else {
          // this.mode = 'attributes';
        }
      })
    );

    this.subscription.add(this.scrollTo$
      .filter(nodeId => !!nodeId)
      .subscribe(nodeId => {
        this.setScroll(nodeId);
      })
    );

    this.subscription.add(this.subtree$
      .filter(nodeIds => !!nodeIds)
      .subscribe(nodeIds => {
        this.disableCursor = nodeIds.length > 0;
      })
    );

    this.subscription.add(this.item$.subscribe(item => {
      this.itemType = item.get('type');
      // this.title = 'Codes';
      for (const crit of DOMAIN_TYPES) {
        const regex = new RegExp(`.*${crit.type}.*`, 'i');
        if (regex.test(this.itemType)) {
          this.title = crit.name;
          if (crit.type === TreeType.DEMO) {
            this.title += ' - ' + subtypeToTitle(this.subtype);
          }
        }
      }
      for (const crit of PROGRAM_TYPES) {
        const regex = new RegExp(`.*${crit.type}.*`, 'i');
        if (regex.test(this.itemType)) {
          this.title = crit.name;
        }
      }
    }));
    this.originalNode = this.rootNode;
  }
  addSelectionToGroup(selection: any) {
    if (selection.get('type') === TreeType[TreeType.DEMO]) {
      if (selection.get('subtype') === this.subtype) {
        const key = selection.get('subtype');
        if (this.selections[key] && !this.selections[key].includes(selection)) {
          this.selections[key].push(selection);
        } else {
          this.selections[key] = [selection];
        }
      }
    } else {
      const key = selection.get('type');
      if (this.selections[key] && !this.selections[key].includes(selection)) {
        this.selections[key].push(selection);
      } else {
        this.selections[key] = [selection];
      }
    }
  }
  setScroll(id: string) {
    const nodeId = `node${id}`;
    const node = document.getElementById(nodeId);
    if (node) {
      setTimeout(() => node.scrollIntoView({behavior: 'smooth'}), 200);
    }
    this.disableCursor = false;
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  close() {
    wizardStore.next(undefined);
    selectionsStore.next([]);
    this.open = false;
  }

  back() {
    if (this.attributesCrit) {
      this.mode = 'tree';
      this.attributesCrit = undefined;
    }
    if (this.mode === 'snomed') {
      this.setMode('tree');
    }
  }

  finish() {
    const {groupId, item, role} = this.wizard;
    const searchRequest = searchRequestStore.getValue();
    const groupIndex = searchRequest[role].findIndex(grp => grp.id === groupId);
    const itemIndex = searchRequest[role][groupIndex].items.findIndex(it => it.id === item.id);
    if (itemIndex > -1) {
      searchRequest[role][groupIndex][itemIndex] = item;
    } else {
      searchRequest[role][groupIndex].items.push(item);
    }
    searchRequestStore.next(searchRequest);
    this.close();
    // this.actions.finishWizard();
  }

  /* Used to bootstrap the criteria tree */
  get rootNode() {
    return Map({
      type: this.ctype,
      subtype: this.subtype,
      fullTree: this.fullTree,
      id: 0,    // root parent ID is always 0
    });
  }

  get snomedNode() {
    return Map({
      type: TreeType.SNOMED,
      subtype: this.subtype === TreeSubType[TreeSubType.CM]
        ? TreeSubType.CM : TreeSubType.PCS,
      fullTree: this.fullTree,
      id: 0,    // root parent ID is always 0
    });
  }

  get attributeTitle() {
    const domain = this.attributesCrit.domainId;
    return domain === DomainType[DomainType.PHYSICALMEASUREMENT]
      ? stripHtml(this.attributesCrit.name)
      : domain + ' Detail';
  }

  get showModifiers() {
    return this.itemType !== TreeType[TreeType.PM] &&
      this.itemType !== TreeType[TreeType.DEMO] &&
      this.itemType !== TreeType[TreeType.PPI];
  }

  get isCondOrProc() {
    return this.itemType === TreeType[TreeType.CONDITION]
    || this.itemType === TreeType[TreeType.PROCEDURE];
  }

  get showNext() {
    return this.showModifiers && this.mode !== 'modifiers';
  }

  get showBack() {
    return this.showModifiers && this.mode === 'modifiers';
  }

  get treeClass() {
    if (this.ctype === TreeType.DEMO) {
      return this.subtype === TreeSubType.AGE ? 'col-md-12' : 'col-md-6';
    }
    return 'col-md-8';
  }

  get sidebarClass() {
    return this.ctype === TreeType.DEMO ? 'col-md-6' : 'col-md-4';
  }

  setMode(mode: any) {
    if (mode !== 'tree' && this.ctype !== TreeType[TreeType.SNOMED]) {
      this.originalNode = Map({
        type: this.ctype,
        subtype: this.subtype,
        fullTree: this.fullTree,
        id: 0,
      });
    }
    if (mode !== 'modifiers') {
      const node = mode === 'tree' ? this.originalNode : this.snomedNode;
      const criteriaType = node.get('type');
      const criteriaSubtype = node.get('subtype');
      const context = {criteriaType, criteriaSubtype};
      this.actions.setWizardContext(context);
    } else {
      this.backMode = this.mode;
    }
    this.mode = mode;
  }

  launchAttributes = (criterion: any) => {
    this.attributesCrit = criterion;
    this.mode = 'attributes';
  }

  showHierarchy = (criterion: any) => {
    selectedPathStore.next(criterion.path.split('.'));
    selectedStore.next(criterion.id);
    this.hierarchyCrit = {
      type: criterion.type,
      subtype: criterion.subtype,
      fullTree: false,
      id: 0,    // root parent ID is always 0
    };
    this.mode = 'tree';
  }

  selectionHeader(_type: string) {
    return this.itemType === TreeType[TreeType.DEMO] ? subtypeToTitle(_type) : typeToTitle(_type);
  }

  modifiersFlag = (disabled: boolean) => {
    this.modifiersDisabled = disabled;
  }

  get disableFlag() {
    return this.noSelection
      || this.preview.get('requesting')
      || this.preview.get('count') === 0
      || this.modifiersDisabled;
  }
}
