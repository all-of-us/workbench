import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {scrollStore, subtreePathStore, subtreeSelectedStore} from 'app/cohort-search/search-state.service';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {CohortBuilderService, TreeSubType, TreeType} from 'generated';
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
  fullTree = false;
  ingredients = [];
  subscription: Subscription;

  constructor(private api: CohortBuilderService) {}

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
    if (!event) { return ; }
    this.loading = true;
    const cdrid = +(currentWorkspaceStore.getValue().cdrVersionId);
    const {domainId, id} = this.node;
    const type = domainId === DomainType.DRUG ? CriteriaType[CriteriaType.ATC] : this.node.type;
    this.api.getCriteriaBy(cdrid, domainId, type, id)
      .toPromise()
      .then(resp => {
        if (resp.items.length === 0 && domainId === DomainType.DRUG) {
          this.api.getCriteriaBy(cdrid, domainId, CriteriaType[CriteriaType.RXNORM], id)
            .toPromise()
            .then(rxResp => {
              this.children = rxResp.items;
              this.loading = false;
            });
        } else {
          this.children = resp.items;
          this.loading = false;
        }
      });
  }

  toggleExpanded() {
    this.expanded = !this.expanded;
  }

  get secondLevel() {
    return this.node.parentId === 0
      && (this.node.type === TreeType[TreeType.ICD10]
      || (this.node.type === TreeType[TreeType.ICD9]
      && this.node.subtype === TreeSubType[TreeSubType.PROC]));
  }
}
