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
   * The initial load of the children is deferred until the subtree is first
   * expanded.
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
    console.log(this.node);
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
            const critList = {};
            const childrenCopy = children.toJS()
            childrenCopy.forEach(child => {
              const parent = child.parentId;
              console.log(parent);
              if (critList.hasOwnProperty(parent)) {
                critList[parent].push(child);
              } else {
                critList[parent] = [child];
              }
              /* if (child.get('group') && child.get('parentId') === 0) {
                child.set('children', List());
                critList[child.get('id')] = child;
              } else {
                critList.forEach(item => {
                  if (item.get('id') === child.get('parentId')) {

                  }
                });
              } */
            });
            this.children = fromJS(critList);
            console.log(this.children.toJS());
          } else {
            this.children = children;
          }
        });

      this.subscription = errorSub;
      this.subscription.add(loadingSub);
      this.subscription.add(childSub);
    }
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  loadChildren(event) {
    console.log(this.node.toJS());
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
