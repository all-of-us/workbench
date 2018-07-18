import {NgRedux, select} from '@angular-redux/store';
import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {fromJS, List, Map} from 'immutable';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';

import {
  CohortSearchActions,
  CohortSearchState,
  criteriaChildren,
  criteriaError,
  criteriaSearchOriginalTree,
  criteriaSearchTerms,
  isCriteriaLoading,
} from '../redux';

@Component({
  selector: 'crit-node',
  templateUrl: './node.component.html',
  styleUrls: ['./node.component.css']
})
export class NodeComponent implements OnInit, OnDestroy {
  @Input() node;

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
  expandedTree: any;
  originalTree: any;
  modifiedTree = false;
  searchTerms: string;
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

      const searchSub = this.ngRedux
        .select(criteriaSearchTerms())
        .subscribe(searchTerms => {
          this.searchTerms = searchTerms
          if (searchTerms && searchTerms.length > 2) {
            this.searchTree();
          } else {
            this.clearSearchTree();
          }
        });

      this.subscription = errorSub;
      this.subscription.add(loadingSub);
      this.subscription.add(childSub);
      this.subscription.add(searchSub);
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

  searchTree() {
    if (!this.modifiedTree) {
      this.modifiedTree = true;
      this.originalTree = this.children;
      // this.actions.setCriteriaSearchOriginalTree(this.children);
    }
    this.expandedTree = this.originalTree.toJS();
    const filtered = this.filterTree(this.originalTree.toJS(), []);
    this.children = fromJS(this.mergeExpanded(filtered, this.expandedTree));
  }

  clearSearchTree() {
    // this.searchTerm = '';
    if (this.modifiedTree) {
      this.children = this.originalTree;
      // this.children = this.ngRedux.getState().getIn(['criteria', 'search', 'originalTree']);
      this.modifiedTree = false;
    }
  }

  filterTree(tree: Array<any>, path: Array<number>) {
    const filtered = tree.map((item, i) => {
      path.push(i);
      if (this.matchFound(item)) {
        const start = item.name.toLowerCase().indexOf(this.searchTerms.toLowerCase());
        const end = start + this.searchTerms.length;
        item.name = '<b>' + item.name.slice(0, start) + '<span style="color: #659F3D">'
          + item.name.slice(start, end) + '</span>'
          + item.name.slice(end) + '</b>';
        if (path.length) {
          this.setExpanded(path, 0);
        } else {
          this.expandedTree[i].expanded = true;
        }
      }
      if (item.children.length) {
        item.children = this.filterTree(item.children, path);
      }
      path.pop();
      return item;
    });
    return filtered;
  }

  matchFound(item: any) {
    return item.name.toLowerCase().includes(this.searchTerms.toLowerCase())
      || item.conceptId.toString().includes(this.searchTerms);
  }

  setExpanded(path: Array<number>, start: number) {
    let obj = this.expandedTree[path[0]];
    if (start > 0) {
      for (let x = 1; x <= start; x++) {
        obj = obj.children[path[x]];
      }
    }
    if (obj.children.length) {
      obj.expanded = true;
    }
    if (path[start + 1]) {
      this.setExpanded(path, start + 1);
    }
  }

  mergeExpanded(filtered: Array<any>, expanded: Array<any>) {
    expanded.forEach((item, i) => {
      if (item.expanded) {
        filtered[i].expanded = true;
      }
      if (filtered[i].children.length) {
        filtered[i].children = this.mergeExpanded(filtered[i].children, item.children);
      }
    });
    return filtered;
  }
}
