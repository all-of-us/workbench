import {select} from '@angular-redux/store';
import {Component, Input, OnDestroy, OnInit, OnChanges} from '@angular/core';
import {DomainType} from 'generated';
import {Map} from 'immutable';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';
import {CRITERIA_TYPES, DOMAIN_TYPES, PROGRAM_TYPES} from '../constant';
import {
  activeCriteriaTreeType,
  activeCriteriaType,
  activeParameterList,
  attributesPage,
  CohortSearchActions,
  wizardOpen,
} from '../redux';
import {typeToTitle} from '../utils';
import {CohortAnnotationDefinition} from "../../../generated";


@Component({
  selector: 'app-modal',
  templateUrl: './modal.component.html',
  styleUrls: [
    './modal.component.css',
    '../../styles/buttons.css',
  ]
})
export class ModalComponent implements OnInit, OnDestroy, OnChanges {
  @select(wizardOpen) open$: Observable<boolean>;
  @select(activeCriteriaType) criteriaType$: Observable<string>;
  @select(activeCriteriaTreeType) isFullTree$: Observable<boolean>;
  @select(activeParameterList) selection$: Observable<any>;
  @select(attributesPage) attributes$: Observable<any>;

  readonly domainType = DomainType;
  readonly criteriaTypes = CRITERIA_TYPES;
  ctype: string;
  fullTree: boolean;
  subscription: Subscription;
  attributesNode: any;
  open = false;
  noSelection = true;
  title = '';
  mode: 'tree' | 'modifiers' | 'attributes' = 'tree'; // default to criteria tree

  constructor(private actions: CohortSearchActions) {}

  ngOnChanges(){

  }

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
        this.title = 'Codes';
        for (const crit of DOMAIN_TYPES) {
          const regex = new RegExp(`.*${crit.type}.*`, 'i');
          if (regex.test(this.ctype)) {
            this.title = crit.name;
          }
        }
        for (const crit of PROGRAM_TYPES) {
          const regex = new RegExp(`.*${crit.type}.*`, 'i');
          if (regex.test(this.ctype)) {
            this.title = crit.name;
          }
        }
      })
    );

    this.subscription.add(this.isFullTree$
      .subscribe(fullTree => this.fullTree = fullTree)
    );

    this.subscription.add(this.selection$
      .map(sel => sel.size === 0)
      .subscribe(sel => this.noSelection = sel)
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
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  cancel() {
    this.open = false;
    this.actions.cancelWizard();
  }

  back() {
    this.actions.hideAttributesPage();
  }

  finish() {
    this.open = false;
    this.actions.finishWizard();
  }

  /* Used to bootstrap the criteria tree */
  get rootNode() {
      return Map({

          type: this.ctype,
          fullTree: this.fullTree,
          id: 0,    // root parent ID is always 0
      });
  }

  get selectionTitle() {
    const title = typeToTitle(this.ctype);
    return title
      ? `Add Selected ${title} Criteria to Cohort`
      : 'No Selection';
  }

    showTitle() {
        return this.title === 'Drugs' || this.title == 'Visits' || this.title == 'CPT Codes' || this.title == 'Demographics';
    }
}
