import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {ppiQuestions, scrollStore, subtreePathStore, subtreeSelectedStore} from 'app/cohort-search/search-state.service';
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
  loading = false;
  empty: boolean;
  selected: boolean;
  error = false;
  subscription: Subscription;

  ngOnInit() {
    this.subscription = subtreePathStore.subscribe(path => {
      this.expanded = path.includes(this.node.id.toString());
    });

    this.subscription.add(subtreeSelectedStore.subscribe(id => {
      this.selected = id === this.node.id;
      if (this.selected) {
        setTimeout(() => scrollStore.next(this.node.id));
      }
    }));
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
            this.children = children;
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
}
