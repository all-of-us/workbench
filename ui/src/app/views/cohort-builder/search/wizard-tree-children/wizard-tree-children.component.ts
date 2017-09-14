import { Component, OnInit, Input, OnDestroy, ViewEncapsulation } from '@angular/core';
import { BroadcastService } from '../service';
import { SearchParameter, SearchCriteria } from '../model';
import { Subscription } from 'rxjs/Subscription';
import { CohortBuilderService } from 'generated';
import { Criteria } from 'generated';

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
