import {Component, OnDestroy, OnInit, Input, ViewEncapsulation} from '@angular/core';
import {Subscription} from 'rxjs/Subscription';

import {BroadcastService} from '../broadcast.service';
import {CohortBuilderService, Criteria, SearchParameter} from 'generated';

@Component({
  selector: 'app-wizard-tree-children',
  templateUrl: 'wizard-tree-children.component.html',
  styleUrls: ['wizard-tree-children.component.css'],
  encapsulation: ViewEncapsulation.None
})
export class WizardTreeChildrenComponent implements OnInit, OnDestroy {

  @Input() node: Criteria;
  loading: boolean;
  nodes: Criteria[] = [];
  subscription: Subscription;

  constructor(private cohortBuilderService: CohortBuilderService,
              private broadcastService: BroadcastService) { }

  ngOnInit() {
    this.loading = true;
    this.subscription = this.cohortBuilderService.getCriteriaByTypeAndParentId(
        this.node.type.toLocaleLowerCase(), this.node.id)
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
