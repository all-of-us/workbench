import {Component, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {Subscription} from 'rxjs/Subscription';

import {SidebarDirective} from '../directives/sidebar.directive';
import {ReviewStateService} from '../review-state.service';

import {ReviewStatus} from 'generated';

const CDR_VERSION = 1;

@Component({
  selector: 'app-cohort-review',
  templateUrl: './cohort-review.component.html',
  styleUrls: ['./cohort-review.component.css']
})
export class CohortReviewComponent implements OnInit {
  private createReviewModalOpen = false;
  private subscription: Subscription;
  @ViewChild('sidebar') sidebar: SidebarDirective;

  constructor(
    private state: ReviewStateService,
    private route: ActivatedRoute,
  ) {}

  ngOnInit() {
    const {review} = this.route.snapshot.data;
    const {ns, wsid, cid} = this.route.snapshot.params;

    this.state.participant.next(null);
    this.state.context.next({
      cdrVersion: CDR_VERSION,
      cohortId: cid,
      workspaceId: wsid,
      workspaceNamespace: ns,
    });

    if (review.reviewStatus === ReviewStatus.NONE) {
      this.createReviewModalOpen = true;
    }

    this.subscription = this.state.sidebarOpen$.subscribe(val => val
      ? this.sidebar.open()
      : this.sidebar.close()
    );
  }
}
