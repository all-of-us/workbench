import {Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {Subscription} from 'rxjs/Subscription';

import {CreateReviewComponent} from '../create-review/create-review.component';
import {SidebarDirective} from '../directives/sidebar.directive';
import {ReviewStateService} from '../review-state.service';

import {ReviewStatus} from 'generated';

@Component({
  selector: 'app-cohort-review',
  templateUrl: './cohort-review.component.html',
  styleUrls: ['./cohort-review.component.css']
})
export class CohortReviewComponent implements OnInit, OnDestroy {
  @ViewChild('createReviewModal') createReviewModal: CreateReviewComponent;
  @ViewChild('sidebar') sidebar: SidebarDirective;
  private subscription: Subscription;

  constructor(
    private state: ReviewStateService,
    private route: ActivatedRoute,
  ) {}

  ngOnInit() {
    this.broadcast();
    this.subscription = this.newReviewSub();
    this.subscription.add(this.sidebarSub());
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  private broadcast() {
    const {annotationDefinitions, cohort, review} = this.route.snapshot.data;
    const {workspace} = this.route.parent.snapshot.data;

    this.state.annotationDefinitions.next(annotationDefinitions);
    this.state.cohort.next(cohort);
    this.state.review.next(review);
  }

  private newReviewSub = () => this.route.data
    .map(({review}) => review.reviewStatus)
    .map(status => status === ReviewStatus.NONE)
    .subscribe(val => val
      ? this.createReviewModal.modal.open()
      : this.createReviewModal.modal.close())

  private sidebarSub = () =>
    this.state.sidebarOpen$.subscribe(val => val
      ? this.sidebar.open()
      : this.sidebar.close())

  closeSidebar() {
    this.state.sidebarOpen.next(false);
  }
}
