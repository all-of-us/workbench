import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {autocompleteStore, ppiQuestions, scrollStore, subtreePathStore, subtreeSelectedStore} from 'app/cohort-search/search-state.service';
import {highlightMatches, stripHtml} from 'app/cohort-search/utils';
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {CriteriaType, DomainType} from 'generated/fetch';
import {Subscription} from 'rxjs/Subscription';

@Component({
  selector: 'crit-list-node',
  templateUrl: './list-node.component.html',
  styleUrls: ['./list-node.component.css']
})
export class ListNodeComponent implements OnInit, OnDestroy {
  @Input() node: any;
  @Input() selections: Array<string>;
  @Input() wizard: any;
  expanded = false;
  children: any;
  original: any;
  expandedTree: any;
  originalTree: any;
  modifiedTree = false;
  searchTerms: string;
  loading = false;
  empty: boolean;
  selected: boolean;
  error = false;
  subscription: Subscription;

  ngOnInit() {
    if (!this.wizard.fullTree || this.node.id === 0) {
      this.subscription = subtreePathStore.subscribe(path => {
        this.expanded = path.includes(this.node.id.toString());
      });

      this.subscription.add(subtreeSelectedStore.subscribe(id => {
        this.selected = id === this.node.id;
        if (this.selected) {
          setTimeout(() => scrollStore.next(this.node.id));
        }
      }));

      this.subscription.add(autocompleteStore.subscribe(searchTerms => {
        this.searchTerms = searchTerms;
        if (this.wizard.fullTree) {
          if (searchTerms && searchTerms.length > 1) {
            this.searchTree();
          } else if (this.children) {
            // TODO highlighted terms still not clearing, need to look into this more
            this.children = this.original = this.clearSearchTree(this.original);
            // this.expanded = false;
            // this.clearSearchTree(this.children);
          }
        }
      }));
    }
    if (this.wizard && this.wizard.fullTree) {
      this.expanded = this.node.expanded || false;
    }
  }

  ngOnDestroy() {
    if (this.subscription) {
      this.subscription.unsubscribe();
    }
    if (this.selected) {
      subtreeSelectedStore.next(undefined);
      scrollStore.next(undefined);
    }
  }

  get nodeId() {
    return `node${this.node.id}`;
  }

  loadChildren(event) {
    if (!event || !!this.children) { return ; }
    this.loading = true;
    const cdrId = +(currentWorkspaceStore.getValue().cdrVersionId);
    const {domainId, id, isStandard, name} = this.node;
    const type = domainId === DomainType.DRUG ? CriteriaType[CriteriaType.ATC] : this.node.type;
    try {
      if (!this.wizard.fullTree) {
        cohortBuilderApi().getCriteriaBy(cdrId, domainId, type, isStandard, id)
          .then(resp => {
            if (resp.items.length === 0 && domainId === DomainType.DRUG) {
              cohortBuilderApi()
                .getCriteriaBy(cdrId, domainId, CriteriaType[CriteriaType.RXNORM], isStandard, id)
                .then(rxResp => {
                  this.empty = rxResp.items.length === 0;
                  this.children = rxResp.items;
                  this.loading = false;
                }, () => this.error = true);
            } else {
              this.empty = resp.items.length === 0;
              this.children = resp.items;
              this.loading = false;
              if (!this.empty && domainId === DomainType.SURVEY && !resp.items[0].group) {
                // save questions in the store so we can display them along with answers if selected
                const questions = ppiQuestions.getValue();
                questions[id] = name;
                ppiQuestions.next(questions);
              }
            }
          });
      } else {
        cohortBuilderApi().getCriteriaBy(cdrId, domainId, type)
          .then(resp => {
            console.log(resp);
            this.empty = resp.items.length === 0;
            this.loading = false;
            let children = [];
            resp.items.forEach(child => {
              child['children'] = [];
              if (child.parentId === 0) {
                children.push(child);
              } else {
                children = this.addChildToParent(child, children);
              }
            });
            this.children = this.original = children;
          });
      }
    } catch (error) {
      console.error(error);
      this.error = true;
      this.loading = false;
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

  toggleExpanded() {
    this.expanded = !this.expanded;
  }

  listSearchTree() {
    this.children = this.original.map(item => item.group ? this.checkChildren(item) : item);
  }

  checkChildren(item: any) {
    const match = item.children.some(it => this.isMatch(it.name));
    if (item.children.some(it => this.isMatch(it.name))) {
      item.expanded = true;
      item.children = item.children.map(child => {
        if (this.isMatch(child.name)) {
          child.name = highlightMatches([this.searchTerms], child.name, false);
        }
        return child.group ? this.checkChildren(child) : child;
      });
    }
    return item;
  }

  isMatch(name: string) {
    return name.toLowerCase().includes(this.searchTerms.toLowerCase());
  }

  searchTree() {
    if (!this.modifiedTree) {
      this.modifiedTree = true;
      this.originalTree = JSON.parse(JSON.stringify(this.children));
    }
    this.expandedTree = JSON.parse(JSON.stringify(this.originalTree));
    const filtered = this.filterTree(this.originalTree, []);
    this.children = this.mergeExpanded(filtered, this.expandedTree);
  }

  clearSearchTree(children: any) {
    return children.map(child => {
      child.name = stripHtml(child.name);
      child.expanded = false;
      if (child.group) {
        child.children = this.clearSearchTree(child.children);
      }
      return child;
    });
  }

  filterTree(tree: Array<any>, path: Array<number>) {
    return tree.map((item, i) => {
      path.push(i);
      if (stripHtml(item.name).toLowerCase().includes(this.searchTerms.toLowerCase())) {
        item.name = highlightMatches([this.searchTerms], item.name, false);
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
}
