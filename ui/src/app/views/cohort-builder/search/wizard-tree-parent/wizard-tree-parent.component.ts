import {Component, OnDestroy, OnInit, Input, ViewEncapsulation} from '@angular/core';
import {Subscription} from 'rxjs/Subscription';
import 'rxjs/add/operator/mergeMap';

import {BroadcastService} from '../broadcast.service';
import {CohortBuilderService, Criteria, SearchParameter } from 'generated';

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

  public selectCriteria(criteria: Criteria): void {
    let newCriteria = criteria;
    if (!criteria['searchParameters']) {
      newCriteria = { searchParameters: [], ...criteria } as Criteria;
    }
    const {code, domainId} = criteria;
    newCriteria['searchParameters'].push(<SearchParameter>{code, domainId});
    this.broadcastService.selectCriteria(newCriteria);
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }
}
