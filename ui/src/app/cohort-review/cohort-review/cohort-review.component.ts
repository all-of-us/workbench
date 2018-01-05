import {Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {Subscription} from 'rxjs/Subscription';

import {SidebarDirective} from '../directives/sidebar.directive';
import {ReviewStateService} from '../review-state.service';

import {ReviewStatus} from 'generated';

@Component({
  selector: 'app-cohort-review',
  templateUrl: './cohort-review.component.html',
  styleUrls: ['./cohort-review.component.css']
})
export class CohortReviewComponent implements OnInit, OnDestroy {
  private createReviewModalOpen = false;
  private subscription: Subscription;
  @ViewChild('sidebar') sidebar: SidebarDirective;

  constructor(
    private state: ReviewStateService,
    private route: ActivatedRoute,
  ) {}

  ngOnInit() {
    const {reviewStatus} = this.route.snapshot.data.review;
    this.createReviewModalOpen = reviewStatus === ReviewStatus.NONE;
    this.subscription = this.state.sidebarOpen$.subscribe(val => val
      ? this.sidebar.open()
      : this.sidebar.close()
    );
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }
}
