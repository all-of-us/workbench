import {NgRedux, select} from '@angular-redux/store';
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
import {TreeSubType, TreeType} from 'generated';
import {Map} from 'immutable';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';
import {PREDEFINED_ATTRIBUTES} from '../constant';
import {
  CohortSearchActions,
  CohortSearchState,
  isParameterActive,
  isSelectedParent,
  subtreeSelected
} from '../redux';
import {stripHtml} from '../utils';

/*
 * Stub function - some criteria types will have "attributes" that help define
 * them.  Demographics AGE was in this category until we removed demographics
 * to its own modal form.  This function and the overall attribute flow has
 * been left intact in order to provide a "hook-in" location for implementing
 * other types of attribute.
 */
function needsAttributes(node: any) {
  // will change soon to check for attributes property instead of id
  return node.get('hasAttributes') === true;
}


@Component({
  selector: 'crit-node-info',
  templateUrl: './node-info.component.html',
  styleUrls: ['./node-info.component.css']
})
export class NodeInfoComponent implements OnInit, OnDestroy, AfterViewInit {
  @select(subtreeSelected) selected$: Observable<any>;
  @Input() node;
  private isSelected: boolean;
  private isSelectedParent: boolean;
  private subscription: Subscription;
  @ViewChild('name') name: ElementRef;
  isTruncated = false;
  matched = false;

  constructor(
    private ngRedux: NgRedux<CohortSearchState>,
    private actions: CohortSearchActions
  ) {}

  ngOnInit() {
    // set to true so parameters with attrs will show as selected when added
    const noAttr = true;

    this.subscription = this.ngRedux
      .select(isParameterActive(this.paramId))
      .map(val => noAttr && val)
      .subscribe(val => {
        this.isSelected = val;
      });

    this.subscription = this.ngRedux
      .select(isSelectedParent(this.node.get('id')))
      .map(val => noAttr && val)
      .subscribe(val => {
        this.isSelectedParent = val;
      });

    this.subscription.add(this.selected$
      .filter(selectedIds => !!selectedIds)
      .subscribe(selectedIds => this.matched = selectedIds.includes(this.node.get('id'))));
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
    return `param${this.node.get('conceptId') ?
        (this.node.get('conceptId') + this.node.get('code')) : this.node.get('id')}`;
  }

  get selectable() {
    return this.node.get('selectable', false);
  }

  get displayName() {
    const noCode = (this.isDrug() && this.node.get('group'))
      || this.isPM();
    const nameIsCode = this.node.get('name', '') === this.node.get('code', '');
    return (noCode || nameIsCode) ? '' : this.node.get('name', '');
  }

  get popperName() {
    return stripHtml(this.displayName);
  }

  get displayCode() {
    if ((this.isDrug() && this.node.get('group'))
      || this.isPM()) {
      return this.node.get('name', '');
    }
    return this.node.get('code', '');
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
  select(event) {
    /* Prevents the click from reaching the treenode link, which would then
     * fire a request for children (if there are any)
     */
    event.stopPropagation();
    if (needsAttributes(this.node)) {
      if (this.isMeas()) {
        this.actions.fetchAttributes(this.node);
      } else {
        const attributes = this.node.get('subtype') === TreeSubType[TreeSubType.BP]
          ? JSON.parse(JSON.stringify(PREDEFINED_ATTRIBUTES.BP_DETAIL))
          : [{
            name: '',
            operator: null,
            operands: [null],
            conceptId: this.node.get('conceptId', null),
            MIN: 0,
            MAX: 10000
          }];
        this.actions.loadAttributes(this.node, attributes);
      }
    } else {
      /*
       * Here we set the parameter ID to `param<criterion ID>` - this is
       * deterministic and avoids duplicate parameters for criterion which do
       * not require attributes.  Criterion which require attributes in order
       * to have a complete sense are given a unique ID based on the attribute
       */

      if (this.isDrug() && this.node.get('group')) {
        this.actions.fetchAllChildren(TreeType[TreeType.DRUG], this.node.get('id'));
      } else {
        let attributes = [];
        if (this.node.get('subtype') === TreeSubType[TreeSubType.BP]) {
          Object.keys(PREDEFINED_ATTRIBUTES).forEach(name => {
            if (this.node.get('name').indexOf(name) === 0) {
              attributes = PREDEFINED_ATTRIBUTES[name];
            }
          });
        }
        const param = this.node.set('parameterId', this.paramId).set('attributes', attributes);
        this.actions.addParameter(param);
      }
    }
  }

  selectChildren(node: Map<string, any>) {
    if (node.get('group')) {
      node.get('children').forEach(child => {
        this.selectChildren(child);
      });
    } else {
      const param = node.set('parameterId', `param${(node.get('conceptId')
        ? node.get('conceptId') : node.get('id'))}`);
      this.actions.addParameter(param);
    }
  }

  showCount() {
    return this.node.get('count') !== null
      && (this.node.get('selectable')
      || (this.node.get('subtype') === CRITERIA_SUBTYPES.LAB
      && this.node.get('group')
      && this.node.get('code') !== null ));
  }

  isPM() {
    return this.node.get('type') === TreeType[TreeType.PM];
  }

  isDrug() {
    return this.node.get('type') === TreeType[TreeType.DRUG];
  }

  isMeas() {
    return this.node.get('type') === TreeType[TreeType.MEAS];
  }
}
