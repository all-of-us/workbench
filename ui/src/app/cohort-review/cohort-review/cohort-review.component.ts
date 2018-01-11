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
  @ViewChild('editSetAnnotationsModal') editSetAnnotationsModal;
  @ViewChild('createReviewModal') createReviewModal;
  @ViewChild('sidebar') sidebar;
  private subscription: Subscription;

  constructor(
    private state: ReviewStateService,
    private route: ActivatedRoute,
  ) {}

  ngOnInit() {
    this.subscription = this._newReviewSub();
    this.subscription.add(this._sidebarSub());
    this.subscription.add(this._isAddingAnnotationSub());
    this.subscription.add(this._isEditingAnnotationsSub());
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  private _newReviewSub = () => this.route.data
    .map(({review}) => review.reviewStatus)
    .map(status => status === ReviewStatus.NONE)
    .subscribe(val => val
      ? this.createReviewModal.modal.open()
      : this.createReviewModal.modal.close());

  private _sidebarSub = () =>
    this.state.sidebarOpen$.subscribe(val => val
      ? this.sidebar.open()
      : this.sidebar.close());

  private _isAddingAnnotationSub = () =>
    this.state.isAddingAnnotation$.subscribe(val => val
      ? this.createSetAnnotationModal.modal.open()
      : this.createSetAnnotationModal.modal.close());

  private _isEditingAnnotationsSub = () =>
    this.state.isEditingAnnotations$.subscribe(val => val
      ? this.editSetAnnotationsModal.modal.open()
      : this.editSetAnnotationsModal.modal.close());

  closeSidebar() {
    this.state.sidebarOpen.next(false);
  }
}
