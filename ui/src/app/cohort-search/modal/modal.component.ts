import {select} from '@angular-redux/store';
import {Component, OnDestroy, OnInit} from '@angular/core';
import {DomainType, TreeType} from 'generated';
import {Map} from 'immutable';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';
import {DOMAIN_TYPES, PROGRAM_TYPES} from '../constant';
import {
  activeCriteriaSubtype,
  activeCriteriaTreeType,
  activeCriteriaType,
  activeItem,
  activeParameterList,
  CohortSearchActions,
  nodeAttributes,
  subtreeSelected,
  wizardOpen,
} from '../redux';
import {stripHtml, subtypeToTitle, typeToTitle} from '../utils';


@Component({
  selector: 'app-modal',
  templateUrl: './modal.component.html',
  styleUrls: [
    './modal.component.css',
    '../../styles/buttons.css',
  ]
})
export class ModalComponent implements OnInit, OnDestroy {
  @select(wizardOpen) open$: Observable<boolean>;
  @select(activeCriteriaSubtype) criteriaSubtype$: Observable<any>;
  @select(activeCriteriaType) criteriaType$: Observable<string>;
  @select(activeCriteriaTreeType) isFullTree$: Observable<boolean>;
  @select(activeItem) item$: Observable<any>;
  @select(activeParameterList) selection$: Observable<any>;
  @select(nodeAttributes) attributes$: Observable<any>;
  @select(subtreeSelected) scrollTo$: Observable<any>;

  readonly domainType = DomainType;
  readonly treeType = TreeType;
  ctype: string;
  subtype: string;
  itemType: string;
  fullTree: boolean;
  subscription: Subscription;
  attributesNode: Map<any, any> = Map();
  selections = {};
  objectKey = Object.keys;

  open = false;
  noSelection = true;
  title = '';
  mode: 'tree' | 'modifiers' | 'attributes' = 'tree'; // default to criteria tree

  count = 0;
  constructor(private actions: CohortSearchActions) {}

  ngOnInit() {
    this.subscription = this.open$
      .filter(open => !!open)
      .subscribe(_ => {
        // reset to default each time the modal is opened
        this.mode = 'tree';
        this.open = true;
      });

    this.subscription.add(this.criteriaType$
      .filter(ctype => !!ctype)
      .subscribe(ctype => {
        this.ctype = ctype;
      })
    );

    this.subscription.add(this.isFullTree$.subscribe(fullTree => this.fullTree = fullTree));

    this.subscription.add(this.selection$
      .subscribe(selections => {
        this.selections = {};
        this.noSelection = selections.size === 0;
        selections.forEach(selection => {
          this.addSelectionToGroup(selection);
        });
      })
    );

    this.subscription.add(this.attributes$
      .subscribe(node => {
        this.attributesNode = node;
        if (node.size === 0) {
          this.mode = 'tree';
        } else {
          this.mode = 'attributes';
        }
      })
    );

    this.subscription.add(this.scrollTo$
      .filter(nodeId => !!nodeId)
      .subscribe(nodeId => {
        if (nodeId) {
          this.setScroll(nodeId);
        }
      })
    );

    this.subscription.add(this.item$.subscribe(item => {
      this.itemType = item.get('type');
      this.title = 'Codes';
      for (const crit of DOMAIN_TYPES) {
        const regex = new RegExp(`.*${crit.type}.*`, 'i');
        if (regex.test(this.itemType)) {
          this.title = crit.name;
        }
      }
      for (const crit of PROGRAM_TYPES) {
        const regex = new RegExp(`.*${crit.type}.*`, 'i');
        if (regex.test(this.itemType)) {
          this.title = crit.name;
        }
      }
    }));

    this.subscription.add(this.criteriaSubtype$
      .subscribe(subtype => {
        this.subtype = subtype;
      })
    );
  }

  addSelectionToGroup(selection: any) {
    const key = selection.get('type') === TreeType[TreeType.DEMO]
      ? selection.get('subtype') : selection.get('type');
    if (this.selections[key] && !this.selections[key].includes(selection)) {
      this.selections[key].push(selection);
    } else {
      this.selections[key] = [selection];
    }
  }

  setScroll(nodeId: string) {
    let node: any;
    Observable.interval(100)
      .takeWhile(() => !node)
      .subscribe(i => {
        node = document.getElementById('node' + nodeId.toString());
        if (node && i < 100) {
          node.scrollIntoView({behavior: 'smooth', block: 'start'});
        }
      });
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  cancel() {
    this.selections = {};
    this.open = false;
    this.actions.cancelWizard();
  }

  back() {
    this.actions.hideAttributesPage();
  }

  finish() {
    this.selections = {};
    this.open = false;
    this.actions.finishWizard();
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

  get selectionTitle() {
    const _type = [
      TreeType[TreeType.CONDITION],
      TreeType[TreeType.PROCEDURE]
    ].includes(this.itemType)
      ? this.itemType : this.ctype;
    const title = typeToTitle(_type);
    return title
      ? `Add Selected ${title} Criteria to Cohort`
      : 'No Selection';
  }

  get attributeTitle() {
    return this.ctype === TreeType[TreeType.PM]
      ? stripHtml(this.attributesNode.get('name'))
      : typeToTitle(this.ctype) + ' Detail';
  }

  get showHeader() {
    return this.itemType === TreeType[TreeType.CONDITION]
    || this.itemType === TreeType[TreeType.PROCEDURE]
    || this.itemType === TreeType[TreeType.DEMO];
  }

  selectionHeader(_type: string) {
    return this.itemType === TreeType[TreeType.DEMO] ? subtypeToTitle(_type) : typeToTitle(_type);
  }
}
