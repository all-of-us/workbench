import {Component, OnDestroy, OnInit} from '@angular/core';
import {Subscription} from 'rxjs/Subscription';

import {
  AnnotationManagerState,
  ReviewStateService
} from '../review-state.service';

@Component({
  selector: 'app-annotation-manager',
  templateUrl: './annotation-manager.component.html',
  styleUrls: ['./annotation-manager.component.css']
})
export class AnnotationManagerComponent implements OnInit, OnDestroy {
  private subscription: Subscription;
  private mgrState: AnnotationManagerState;

  constructor(
    private state: ReviewStateService,
  ) {}

  ngOnInit() {
    this.subscription = this.state.annotationMgrState$
      .subscribe(mgrState => this.mgrState = mgrState);
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  get mode() {
    return this.mgrState.mode || 'overview';
  }

  set mode(value) {
    this.state.annotationMgrState.next({
      ...this.mgrState,
      mode: value
    });
  }

  get open() {
    return this.mgrState.open || false;
  }

  set open(flag: boolean) {
    this.state.annotationMgrState.next({
      ...this.mgrState,
      open: flag
    });
  }

  get twoThirds() {
    return `${Math.floor((window.innerHeight / 3) * 2)}px`;
  }
}
