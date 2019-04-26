import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {scrollStore, selectedPathStore, selectedStore} from 'app/cohort-search/search-state.service';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {CohortBuilderService, TreeSubType, TreeType} from 'generated';
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
    this.subscription = selectedPathStore.subscribe(path => {
      this.expanded = path.includes(this.node.id.toString());
    });

    this.subscription.add(selectedStore.subscribe(id => {
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
      selectedStore.next(undefined);
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
    const _type = this.node.type;
    const parentId = this.node.id;
    this.api.getCriteriaBy(cdrid, _type, null, parentId)
      .toPromise()
      .then(resp => {
        this.children = resp.items;
        this.loading = false;
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
