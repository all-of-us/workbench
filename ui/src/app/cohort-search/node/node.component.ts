import {NgRedux, select} from '@angular-redux/store';
import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {TreeSubType, TreeType} from 'generated';
import {fromJS} from 'immutable';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';

import {
  activeCriteriaTreeType,
  CohortSearchActions,
  CohortSearchState,
  criteriaChildren,
  criteriaError,
  criteriaSearchTerms,
  criteriaSubtree,
  ingredientsForBrand,
  isCriteriaLoading,
  isEmpty,
  subtreeSelected,
} from 'app/cohort-search/redux';

import {highlightMatches, stripHtml} from 'app/cohort-search/utils';

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
  expandedTree: any;
  originalTree: any;
  modifiedTree = false;
  searchTerms: Array<string>;
  loading = false;
  empty: boolean;
  error = false;
  fullTree: boolean;
  codes: any;
  ingredients = [];
  subscription: Subscription;
  @select(subtreeSelected) selected$: Observable<any>;

  constructor(
    private ngRedux: NgRedux<CohortSearchState>,
    private actions: CohortSearchActions,
  ) {}

  ngOnInit() {
    this.fullTree = this.ngRedux.getState().getIn(['wizard', 'fullTree']);
    this.codes = this.ngRedux.getState().getIn(['wizard', 'codes']);
    if (!this.fullTree || this.node.get('id') === 0) {
      const _type = this.node.get('type');
      const subtype = this.codes ? this.node.get('subtype') : null;
      const parentId = this.node.get('id');
      const errorSub = this.ngRedux
        .select(criteriaError(_type, parentId))
        .map(err => !(err === null || err === undefined))
        .subscribe(err => this.error = err);

      const emptySub = this.ngRedux
        .select(isEmpty(_type, parentId))
        .subscribe(empty => this.empty = empty);

      const loadingSub = this.ngRedux
        .select(isCriteriaLoading(_type, parentId))
        .subscribe(loading => this.loading = loading);

      const childSub = this.ngRedux
        .select(criteriaChildren(_type, subtype, parentId))
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
          this.searchTerms = searchTerms;
          if (this.fullTree) {
            if (searchTerms && searchTerms.length) {
              this.searchTree();
            } else {
              this.clearSearchTree();
            }
          }
        });

      const subtreeSub = this.ngRedux
        .select(criteriaSubtree(_type))
        .subscribe(nodeIds => this.expanded = nodeIds.includes(parentId.toString()));

      const subtreeSelectSub = this.selected$
        .filter(selectedIds => !!selectedIds)
        .subscribe(selectedIds => {
          if (parentId !== 0) {
            const displayName = selectedIds.includes(parentId)
              ? highlightMatches(this.searchTerms, this.node.get('name'), false)
              : stripHtml(this.node.get('name'));
            this.node = this.node.set('name', displayName);
            if (selectedIds[0] === parentId) {
              this.actions.setScrollId(null);
              setTimeout(() => this.actions.setScrollId(parentId));
            }
          }
        });

      const ingredientSub = this.ngRedux
        .select(ingredientsForBrand())
        .subscribe(ingredients => {
          this.ingredients = [];
          ingredients.forEach(item => {
            if (!this.ingredients.includes(item.name)) {
              this.ingredients.push(item.name);
            }
          });
        });

      this.subscription = errorSub;
      this.subscription.add(loadingSub);
      this.subscription.add(emptySub);
      this.subscription.add(childSub);
      this.subscription.add(searchSub);
      this.subscription.add(subtreeSub);
      this.subscription.add(subtreeSelectSub);
      this.subscription.add(ingredientSub);
    }
    if (this.fullTree) {
      this.expanded = this.node.get('expanded', false);
    }
    if (this.secondLevel) {
      setTimeout(() => this.loadChildren(true));
    }
  }

  ngOnDestroy() {
    if (this.subscription) {
      this.subscription.unsubscribe();
    }
  }

  get nodeId() {
    return 'node' + this.node.get('id');
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
    const _type = this.node.get('type');
    const parentId = this.node.get('id');
    /* Criteria are cached, so this will result in an API call only the first
     * time this function is called.  Subsequent calls are no-ops
     */
    if (_type === TreeType[TreeType.DRUG]) {
      this.actions.fetchDrugCriteria(_type, parentId, TreeSubType[TreeSubType.ATC]);
    } else if (this.fullTree) {
      this.actions.fetchAllCriteria(_type, parentId);
    } else if (this.codes && this.node.get('subtype')) {
      this.actions.fetchCriteriaBySubtype(_type, this.node.get('subtype'), parentId);
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
    }
    this.expandedTree = this.originalTree.toJS();
    const filtered = this.filterTree(this.originalTree.toJS(), []);
    this.children = fromJS(this.mergeExpanded(filtered, this.expandedTree));
  }

  clearSearchTree() {
    if (this.modifiedTree) {
      this.children = this.originalTree;
      this.modifiedTree = false;
    }
  }

  filterTree(tree: Array<any>, path: Array<number>) {
    return tree.map((item, i) => {
      path.push(i);
      const matches = this.matchFound(item);
      if (matches.length) {
        item.name = highlightMatches(matches, item.name, false);
        if (path.length > 1) {
          this.setExpanded(path, 0);
        }
      }
      if (item.children.length) {
        item.children = this.filterTree(item.children, path);
      }
      path.pop();
      return item;
    });
  }

  matchFound(item: any) {
    return this.searchTerms.filter(term => {
      return item.name.toLowerCase().includes(term.toLowerCase());
    });
  }

  setExpanded(path: Array<number>, end: number) {
    let obj = this.expandedTree[path[0]];
    for (let x = 1; x < end; x++) {
      obj = obj.children[path[x]];
    }
    if (obj.children.length) {
      obj.expanded = true;
    }
    if (typeof path[end + 1] !== 'undefined') {
      this.setExpanded(path, end + 1);
    }
  }

  mergeExpanded(filtered: Array<any>, expanded: Array<any>) {
    expanded.forEach((item, i) => {
      filtered[i].expanded = item.expanded || false;
      if (filtered[i].children.length) {
        filtered[i].children = this.mergeExpanded(filtered[i].children, item.children);
      }
    });
    return filtered;
  }

  get secondLevel() {
    return this.node.get('parentId') === 0
      && (this.node.get('type') === TreeType[TreeType.ICD10]
      || (this.node.get('type') === TreeType[TreeType.ICD9]
      && this.node.get('subtype') === TreeSubType[TreeSubType.PROC]));
  }
}
