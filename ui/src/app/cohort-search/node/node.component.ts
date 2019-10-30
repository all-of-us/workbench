import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {autocompleteStore, ppiQuestions, scrollStore, subtreePathStore, subtreeSelectedStore} from 'app/cohort-search/search-state.service';
import {domainToTitle, highlightMatches, stripHtml, subTypeToTitle} from 'app/cohort-search/utils';
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import {triggerEvent} from 'app/utils/analytics';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {CriteriaType, DomainType} from 'generated/fetch';
import {Subscription} from 'rxjs/Subscription';

@Component({
  selector: 'crit-list-node',
  templateUrl: './node.component.html',
  styleUrls: ['./node.component.css']
})
export class NodeComponent implements OnInit, OnDestroy {
  @Input() node: any;
  @Input() selections: Array<string>;
  @Input() wizard: any;
  expanded = false;
  children: any;
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
        if (this.error && id !== undefined) {
          subtreeSelectedStore.next(undefined);
        }
      }));

      this.subscription.add(autocompleteStore.subscribe(searchTerms => {
        this.searchTerms = searchTerms;
        if (this.wizard.fullTree && this.children) {
          this.children = this.filterTree(JSON.parse(JSON.stringify(this.children)), () => {});
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
    // TODO remove condition to only track SURVEY domain for 'Phase 2' of CB Google Analytics
    if (this.node.domainId === DomainType.SURVEY) {
      this.trackEvent();
    }
    if (!event || (this.node.id !== 0 && !!this.children)) { return; }
    this.loading = true;
    const cdrId = +(currentWorkspaceStore.getValue().cdrVersionId);
    const {count, domainId, id, isStandard, name} = this.node;
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
                questions[id] = {count, name};
                ppiQuestions.next(questions);
              }
            }
          });
      } else {
        cohortBuilderApi().getCriteriaBy(cdrId, domainId, type)
          .then(resp => {
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
            this.children = children;
          });
      }
    } catch (error) {
      console.error(error);
      this.error = true;
      this.loading = false;
      subtreeSelectedStore.next(undefined);
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

  trackEvent() {
    const {domainId, name, parentId, subtype} = this.node;
    if (parentId === 0 && this.expanded) {
      const formattedName = domainId === DomainType.SURVEY ? name : subTypeToTitle(subtype);
      triggerEvent(
        'Cohort Builder Search',
        'Click',
        `${domainToTitle(domainId)} - ${formattedName} - Expand`
      );
    }
  }

  isMatch(name: string) {
    return stripHtml(name).toLowerCase().includes(this.searchTerms.toLowerCase());
  }

  filterTree(tree: Array<any>, expand: Function) {
    return tree.map((item) => {
      item.name = stripHtml(item.name);
      if (this.searchTerms.length > 1) {
        item.expanded = item.children.some(it => this.isMatch(it.name));
      } else {
        item.expanded = false;
      }
      const func = () => {
        item.expanded = true;
        expand();
      };
      if (this.searchTerms.length > 1 && this.isMatch(item.name)) {
        item.name = highlightMatches([this.searchTerms], item.name, false);
        expand();
      }
      if (item.children.length) {
        item.children = this.filterTree(item.children, func);
      }
      return item;
    });
  }
}
