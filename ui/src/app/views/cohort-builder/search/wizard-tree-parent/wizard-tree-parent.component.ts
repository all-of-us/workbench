import { Component, OnInit, Input, OnDestroy, ViewEncapsulation } from '@angular/core';
import { SearchService, BroadcastService } from '../service';
import { Criteria, SearchParameter } from '../model';
import { Subscription } from 'rxjs/Subscription';
import 'rxjs/add/operator/mergeMap';

@Component({
  selector: 'app-wizard-tree-parent',
  templateUrl: 'wizard-tree-parent.component.html',
  styleUrls: ['wizard-tree-parent.component.css'],
  encapsulation: ViewEncapsulation.None
})
export class WizardTreeParentComponent implements OnInit, OnDestroy {

  nodes: Criteria[] = [];
  private subscription: Subscription;
  loading: boolean;

  constructor(private searchService: SearchService,
              private broadcastService: BroadcastService) {}

  ngOnInit(): void {
    this.loading = true;
    this.subscription =
      this.broadcastService.selectedCriteriaType$
      .mergeMap(criteriaType => this.searchService.getParentNodes(criteriaType))
      .subscribe(nodes => {
        this.nodes = nodes;
        this.loading = false;
      });
  }

  public selectCriteria(node): void {
    node.values.push(new SearchParameter(node.code, node.domainId));
    this.broadcastService.selectCriteria(node);
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

}


