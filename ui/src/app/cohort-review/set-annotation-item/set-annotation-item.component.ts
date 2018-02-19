import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {ActivatedRoute} from '@angular/router';

import {ReviewStateService} from '../review-state.service';

import {CohortAnnotationDefinition, CohortAnnotationDefinitionService} from 'generated';

@Component({
  selector: 'app-set-annotation-item',
  templateUrl: './set-annotation-item.component.html',
  styleUrls: ['./set-annotation-item.component.css']
})
export class SetAnnotationItemComponent implements OnInit {
  @Input() definition: CohortAnnotationDefinition;
  @Output() isPosting = new EventEmitter<boolean>();

  constructor(
    private route: ActivatedRoute,
    private annotationAPI: CohortAnnotationDefinitionService,
    private state: ReviewStateService,
  ) { }

  ngOnInit() {
  }

  /* Delete the annotation definition */
  delete(defn: CohortAnnotationDefinition): void {
    const {ns, wsid, cid} = this.route.snapshot.params;
    const id = defn.cohortAnnotationDefinitionId;
    this.isPosting.emit(true);

    this.annotationAPI
      .deleteCohortAnnotationDefinition(ns, wsid, cid, id)
      .switchMap(_ => this.annotationAPI
        .getCohortAnnotationDefinitions(ns, wsid, cid)
        .pluck('items'))
      .do((defns: CohortAnnotationDefinition[]) =>
        this.state.annotationDefinitions.next(defns))
      .subscribe(_ => this.isPosting.emit(false));
  }
}
