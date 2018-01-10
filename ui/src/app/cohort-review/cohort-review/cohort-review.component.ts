import {Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {Subscription} from 'rxjs/Subscription';

import {ReviewStateService} from '../review-state.service';
import {ReviewStatus} from 'generated';

@Component({
  selector: 'app-cohort-review',
  templateUrl: './cohort-review.component.html',
  styleUrls: ['./cohort-review.component.css']
})
export class CohortReviewComponent implements OnInit, OnDestroy {
  @ViewChild('createSetAnnotationModal') createSetAnnotationModal;
  @ViewChild('createReviewModal') createReviewModal;
  @ViewChild('sidebar') sidebar;
  private subscription: Subscription;

  constructor(
    private state: ReviewStateService,
    private route: ActivatedRoute,
  ) {}

  ngOnInit() {
    this.subscription = this._newReviewSub();
    this.subscription.add(this._annotationSub());
    this.subscription.add(this._sidebarSub());
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  _newReviewSub = () => this.route.data
    .map(({review}) => review.reviewStatus)
    .map(status => status === ReviewStatus.NONE)
    .subscribe(val => val
      ? this.createReviewModal.modal.open()
      : this.createReviewModal.modal.close());

  _annotationSub = () =>
    this.state.annotationsOpen$.subscribe(val => val
      ? this.createSetAnnotationModal.modal.open()
      : this.createSetAnnotationModal.modal.close());

  _sidebarSub = () =>
    this.state.sidebarOpen$.subscribe(val => val
      ? this.sidebar.open()
      : this.sidebar.close());
}
