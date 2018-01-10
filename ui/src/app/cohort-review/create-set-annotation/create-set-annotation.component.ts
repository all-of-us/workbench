import {Component, OnInit, ViewChild} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {ActivatedRoute, Router} from '@angular/router';
import {Observable} from 'rxjs/Observable';

import {ReviewStateService} from '../review-state.service';

import {
  CohortReviewService,
  CreateReviewRequest,
} from 'generated';

@Component({
  selector: 'app-create-set-annotation',
  templateUrl: './create-set-annotation.component.html',
  styleUrls: ['./create-set-annotation.component.css']
})
export class CreateSetAnnotationComponent implements OnInit {
  @ViewChild('modal') modal;

  constructor(
    private reviewAPI: CohortReviewService,
    private state: ReviewStateService,
    private router: Router,
    private route: ActivatedRoute,
  ) { }

  ngOnInit() {
  }

  _close() {
    this.modal.close();
    this.state.annotationsOpen.next(false);
  }
}
