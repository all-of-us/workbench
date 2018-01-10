import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';

import {ReviewStateService} from '../review-state.service';

import {
  CohortReviewService,
  CohortAnnotationDefinitionService,
  CohortAnnotationDefinitionListResponse,
} from 'generated';

@Component({
  selector: 'app-annotations',
  templateUrl: './annotations.component.html',
})
export class AnnotationsComponent implements OnInit {
  private annotations$;

  constructor(
    private state: ReviewStateService,
    private reviewAPI: CohortReviewService,
    private annotationAPI: CohortAnnotationDefinitionService,
    private route: ActivatedRoute,
  ) {}

  ngOnInit() {
    this.annotations$ = this.route.params
      .switchMap(({ns, wsid, cid}) =>
        this.annotationAPI.getCohortAnnotationDefinitions(ns, wsid, cid))
      .pluck('items');
  }
}
