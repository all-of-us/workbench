import {Component, EventEmitter, Output} from '@angular/core';

import {ReviewStateService} from '../review-state.service';

@Component({
  selector: 'app-review-header',
  templateUrl: './review-header.component.html',
  styleUrls: ['./review-header.component.css']
})
export class ReviewHeaderComponent {
  @Output() goToOverview = new EventEmitter<boolean>();
  @Output() toggleNav = new EventEmitter<boolean>();
  @Output() toggleStatusBar = new EventEmitter<boolean>();

  /* tslint:disable-next-line */
  constructor(private state: ReviewStateService) {}
}
