import {
  AfterViewInit,
  Component,
  ElementRef,
  HostListener,
  Input,
  OnDestroy,
  OnInit,
  ViewChild
} from '@angular/core';
import {PREDEFINED_ATTRIBUTES} from 'app/cohort-search/constant';
import {attributesStore, groupSelectionsStore, ppiQuestions, selectionsStore, subtreeSelectedStore, wizardStore} from 'app/cohort-search/search-state.service';
import {stripHtml} from 'app/cohort-search/utils';
import {AttrName, CriteriaSubType, CriteriaType, DomainType, Operator} from 'generated/fetch';
import {Subscription} from 'rxjs/Subscription';

@Component({
  selector: 'crit-list-node-info',
  templateUrl: './node-info.component.html',
  styleUrls: ['./node-info.component.css']
})
export class NodeInfoComponent implements OnInit, OnDestroy, AfterViewInit {
  @Input() node: any;
  private selected: boolean;
  private selectedChild: boolean;
  private subscription: Subscription;
  @ViewChild('name') name: ElementRef;
  isTruncated = false;
  matched = false;

  ngOnInit() {
    this.subscription = selectionsStore.subscribe(selections => {
      this.selected = selections.includes(this.paramId);
    });

    this.subscription.add(groupSelectionsStore
      .subscribe(groupIds => {
        this.selectedChild = groupIds.some(
          id => this.node.path.split('.')
            .filter(pathId => pathId !== this.node.id.toString())
            .includes(id.toString())
        );
      }));

    this.subscription.add(subtreeSelectedStore
      .filter(selected => !!selected)
      .subscribe(selected => this.matched = selected === this.node.id)
    );
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }


  /*
   * Running the truncation check only on window resize is efficient (running
   * this check with change detection visibly slows the browser), but if any
   * other conditions arise that might dynamically affect the size of the #name
   * div this will need to be attached to those events as well.  Also we need
   * to make sure it is run at least once on initialization of the _child view_
   * (the name div), not the initialization of _this_ component.
   */
  ngAfterViewInit() {
    setTimeout(() => this.checkTruncation(), 1);
  }

  @HostListener('window:resize')
  checkTruncation() {
    const elem = this.name.nativeElement;
    this.isTruncated = elem.offsetWidth < elem.scrollWidth;
  }

  get paramId() {
    return `param${this.node.conceptId && !this.isSurvey ?
        (this.node.conceptId + this.node.value) : this.node.id}`;
  }

  get selectable() {
    return this.node.selectable;
  }

  get displayCode() {
    if ((this.isDrug && this.node.group) || this.isPM || this.isSurvey || this.isSNOMED) {
      return '';
    }
    return this.node.code;
  }

  /*
   * On selection, we examine the selected criterion and see if it needs some
   * attributes. If it does, we set the criterion in "focus".  The explorer
   * listens for their being a node in focus; if there is, it sets its own mode
   * to `SetAttr` (setting attributes) and passes the node down to
   * `crit-attributes`, the entry point defined by the attributes module.
   *
   * If the node does NOT need an attribute we give it a deterministic ID and
   * add it to the selected params in the state.
   */
  select = (event) => {
    /* Prevents the click from reaching the treenode link, which would then
     * fire a request for children (if there are any)
     */
    event.stopPropagation();
    if (this.hasAttributes) {
      attributesStore.next(this.node);
    } else {
      /*
       * Here we set the parameter ID to `param<criterion ID>` - this is
       * deterministic and avoids duplicate parameters for criterion which do
       * not require attributes.  Criterion which require attributes in order
       * to have a complete sense are given a unique ID based on the attribute
       */
      let selections = selectionsStore.getValue();
      if (!selections.includes(this.paramId)) {
        if (this.node.group) {
          const groups = [...groupSelectionsStore.getValue(), this.node.id];
          groupSelectionsStore.next(groups);
        }
        let modifiedName = this.node.name;
        if (this.isSurvey) {
          // get PPI question from store
          const question = ppiQuestions.getValue()[this.node.parentId];
          if (question) {
            modifiedName = question + ' - ' + modifiedName;
          }
        }
        let attributes = [];
        if (this.node.subtype === CriteriaSubType[CriteriaSubType.BP]) {
          const name = stripHtml(this.node.name);
          Object.keys(PREDEFINED_ATTRIBUTES).forEach(key => {
            if (name.indexOf(key) === 0) {
              attributes = PREDEFINED_ATTRIBUTES[key];
            }
          });
        } else if (this.isPMCat) {
          attributes.push({
            name: AttrName.CAT,
            operator: Operator.IN,
            operands: [this.node.value]
          });
        } else if (this.isSurvey && !this.node.group) {
          if (this.node.conceptId === 1585747) {
            attributes.push({
              name: AttrName.NUM,
              operator: Operator.EQUAL,
              operands: [this.node.value]
            });
          } else {
            attributes.push({
              name: AttrName.CAT,
              operator: Operator.IN,
              operands: [this.node.value]
            });
          }
        }
        const param = {
          ...this.node,
          parameterId: this.paramId,
          attributes: attributes,
          name: modifiedName
        };
        const wizard = wizardStore.getValue();
        wizard.item.searchParameters.push(param);
        selections = [this.paramId, ...selections];
        selectionsStore.next(selections);
        wizardStore.next(wizard);
      }
    }
  }

  get showCount() {
    return this.node.count > -1
      && (this.node.selectable
      || (this.node.subtype === CriteriaSubType.LAB
      && this.node.group
      && this.node.code !== null )
      || this.node.type === CriteriaType.CPT4);
  }

  get isPM() {
    return this.node.domainId === DomainType.PHYSICALMEASUREMENT;
  }

  get isDrug() {
    return this.node.domainId === DomainType.DRUG;
  }

  get isSurvey() {
    return this.node.domainId === DomainType.SURVEY;
  }

  get isSNOMED() {
    return this.node.type === CriteriaType.SNOMED;
  }

  get hasAttributes() {
    return this.node.hasAttributes;
  }

  get isSelected() {
    return this.selected || this.selectedChild;
  }

  get isPMCat() {
    return this.node.subtype === CriteriaSubType.WHEEL ||
      this.node.subtype === CriteriaSubType.PREG ||
      this.node.subtype === CriteriaSubType.HRIRR ||
      this.node.subtype === CriteriaSubType.HRNOIRR;
  }
}
