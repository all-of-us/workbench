import {NgRedux, select} from '@angular-redux/store';
import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {fromJS, List, Map} from 'immutable';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';

import {
  activeCriteriaTreeType,
  CohortSearchActions,
  CohortSearchState,
  criteriaChildren,
  criteriaError,
  isCriteriaLoading,
} from '../redux';

@Component({
  selector: 'crit-node',
  templateUrl: './node.component.html',
  styleUrls: ['./node.component.css']
})
export class NodeComponent implements OnInit, OnDestroy {
  @Input() node;
  @select(activeCriteriaTreeType) isFullTree$: Observable<boolean>;

  /*
   * Each node component represents one criterion.  If that criterion has any
   * descendent criteria, then it is expandable (`node.get('group')` should
   * return true).  If the criterion has no children, none of the following
   * attributes apply; otherwise: `expanded`: is the tree node expanded?
   * `loading`: are we in the process of fetching this criterion's children?
   * `error`: was there an error loading the children of this criterion?
   * `children`: when loaded, this node's children are stored here.
   *
   * If the root criterion has 'fullTree' set to true, the entire tree is
   * loaded. Otherwise, the initial load of the children is deferred until
   * the subtree is first expanded.
   *
   * In the future, we may want put full trees in a separate component to
   * make it cleaner.
   */
  expanded = false;
  children: any;
  loading = false;
  error = false;
  fullTree: boolean;
  subscription: Subscription;

  constructor(
    private ngRedux: NgRedux<CohortSearchState>,
    private actions: CohortSearchActions,
  ) {}

  ngOnInit() {
    this.fullTree = this.ngRedux.getState().getIn(['wizard', 'fullTree']);
    if (!this.fullTree || this.node.get('id') === 0) {
      const _type = this.node.get('type').toLowerCase();
      const parentId = this.node.get('id');
      const errorSub = this.ngRedux
        .select(criteriaError(_type, parentId))
        .map(err => !(err === null || err === undefined))
        .subscribe(err => this.error = err);

      const loadingSub = this.ngRedux
        .select(isCriteriaLoading(_type, parentId))
        .subscribe(loading => this.loading = loading);

      const childSub = this.ngRedux
        .select(criteriaChildren(_type, parentId))
        .subscribe(children => {
          if (this.fullTree) {
            let criteriaList = [];
            children.toJS().forEach(child => {
              child.children = [];
              if (child.parentId === 0) {
                criteriaList.push(child);
              } else {
                criteriaList = this.addChildToParent(child, criteriaList);
              }
            });
            this.children = fromJS(criteriaList);
          } else {
            this.children = children;
          }
        });

      this.subscription = errorSub;
      this.subscription.add(loadingSub);
      this.subscription.add(childSub);
    }
    this.expanded = this.node.get('expanded', false);
  }

  ngOnDestroy() {
    if (this.subscription) {
      this.subscription.unsubscribe();
    }
  }

  addChildToParent(child, itemList) {
    for (const item of itemList) {
      if (!item.group) {
        continue;
      }
      if (item.id === child.parentId) {
        item.children.push(child);
        return itemList;
      }
      if (item.children.length) {
        const childList = this.addChildToParent(child, item.children);
        if (childList) {
          item.children = childList;
          return itemList;
        }
      }
    }
  }

  loadChildren(event) {
    if (!event) { return ; }
    const _type = this.node.get('type').toLowerCase();
    const parentId = this.node.get('id');
    /* Criteria are cached, so this will result in an API call only the first
     * time this function is called.  Subsequent calls are no-ops
     */
    if (this.fullTree) {
      this.actions.fetchAllCriteria(_type, parentId);
    } else {
      this.actions.fetchCriteria(_type, parentId);
    }
  }

  toggleExpanded() {
    this.expanded = !this.expanded;
  }
}
