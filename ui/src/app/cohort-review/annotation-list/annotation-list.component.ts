import {Component, Input, OnChanges} from '@angular/core';
import * as fp from 'lodash/fp';

import {cohortReviewStore} from 'app/cohort-review/review-state.service';

import {
  CohortAnnotationDefinition,
  ParticipantCohortAnnotation,
} from 'generated';


interface Annotation {
  definition: CohortAnnotationDefinition;
  value: ParticipantCohortAnnotation;
}

@Component({
  selector: 'app-annotation-list',
  templateUrl: './annotation-list.component.html',
  styleUrls: ['./annotation-list.component.css'],
})
export class AnnotationListComponent implements OnChanges {
  annotationList: Annotation[] = [];
  @Input() participant;
  @Input() annotations: ParticipantCohortAnnotation[];
  @Input() annotationDefinitions: CohortAnnotationDefinition[];
  @Input() setAnnotations: Function;
  @Input() openCreateDefinitionModal: Function;
  @Input() openEditDefinitionsModal: Function;

  constructor() {
    this.setAnnotation = this.setAnnotation.bind(this);
  }

  ngOnChanges(changes) {
    const {cohortReviewId} = cohortReviewStore.getValue();
    this.annotationList = this.annotationDefinitions.map(ad => {
      return {
        definition: ad,
        value: this.annotations.find(a => {
          return a.cohortAnnotationDefinitionId === ad.cohortAnnotationDefinitionId;
        }) || {
          participantId: this.participant.id,
          cohortReviewId,
          cohortAnnotationDefinitionId: ad.cohortAnnotationDefinitionId
        }
      };
    });
  }

  openManager(): void {
    this.openCreateDefinitionModal();
  }

  openEditManager(): void {
    this.openEditDefinitionsModal();
  }

  setAnnotation(cohortAnnotationDefinitionId, update: ParticipantCohortAnnotation) {
    const filtered = fp.remove({cohortAnnotationDefinitionId}, this.annotations);
    this.setAnnotations(filtered.concat(update.annotationId ? [update] : []));
  }
}
