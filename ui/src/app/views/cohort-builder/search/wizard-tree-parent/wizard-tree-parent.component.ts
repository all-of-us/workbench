import { Component, OnInit, Input, OnDestroy, ViewEncapsulation } from '@angular/core';
import { BroadcastService } from '../service';
import { SearchParameter, SearchCriteria } from '../model';
import { Subscription } from 'rxjs/Subscription';
import 'rxjs/add/operator/mergeMap';
import { CohortBuilderService } from 'generated';
import { Criteria } from 'generated';

@Component({
  selector: 'app-wizard-tree-parent',
  templateUrl: 'wizard-tree-parent.component.html',
  styleUrls: ['wizard-tree-parent.component.css'],
  encapsulation: ViewEncapsulation.None
})
export class WizardTreeParentComponent implements OnInit, OnDestroy {

  nodes: Criteria[];
  private subscription: Subscription;
  loading: boolean;

  constructor(private cohortBuilderService: CohortBuilderService,
              private broadcastService: BroadcastService) {}

  ngOnInit(): void {
    this.loading = true;
    this.subscription =
      this.broadcastService.selectedCriteriaType$
      .mergeMap(criteriaType => this.cohortBuilderService.getCriteriaByTypeAndParentId(
          criteriaType, 0))
      .subscribe(nodes => {
        this.nodes = nodes.items;
        this.loading = false;
      });
  }

  public selectCriteria(node: SearchCriteria): void {
    if (!node.searchParameters) {
      node.searchParameters = [];
    }
    node.searchParameters.push(new SearchParameter(node.code, node.domainId));
    this.broadcastService.selectCriteria(node);
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

}


