import {Component, Input, OnInit} from '@angular/core';
import {FormControl} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';

import {
  AnnotationType,
  CohortAnnotationDefinition,
  CohortReviewService,
  ModifyParticipantCohortAnnotationRequest,
  ParticipantCohortAnnotation,
} from 'generated';

interface Annotation {
  definition: CohortAnnotationDefinition;
  value: ParticipantCohortAnnotation;
}


@Component({
  selector: 'app-annotation-item',
  templateUrl: './annotation-item.component.html',
  styleUrls: ['./annotation-item.component.css']
})
export class AnnotationItemComponent implements OnInit {
  readonly kinds = AnnotationType;

  @Input() annotation: Annotation;
  @Input() showDataType: boolean;

  private control = new FormControl();
  private expandText = false;

  constructor(
    private reviewAPI: CohortReviewService,
    private route: ActivatedRoute,
  ) {}

  ngOnInit() {
    const oldValue = this.annotation.value[this.valuePropertyName];
    if (oldValue !== undefined) {
      this.control.setValue(oldValue);
    }
  }

  handleInput() {
    /* Parameters from the path */
    const {ns, wsid, cid} = this.route.parent.snapshot.params;
    const pid = this.annotation.value.participantId;
    const cdrid = +(this.route.parent.snapshot.data.workspace.cdrVersionId);

    const newValue = this.control.value;
    const oldValue = this.annotation.value[this.valuePropertyName];
    const defnId = this.annotation.definition.cohortAnnotationDefinitionId;
    const annoId = this.annotation.value.annotationId;

    let apiCall;

    // Nothing to see here - if there's no change, no need to hit the server
    if (newValue === oldValue) {
      return ;
    }

    // If there is an annotation ID then the annotation has already been
    // created, so this must be either delete or update
    if (annoId !== undefined) {
      // If the new value isn't anything, this is a delete
      if (newValue === '' || newValue === null) {
        apiCall = this.reviewAPI
          .deleteParticipantCohortAnnotation(ns, wsid, cid, cdrid, pid, annoId);
      } else {
        const request = <ModifyParticipantCohortAnnotationRequest>{
          [this.valuePropertyName]: newValue,
        };
        apiCall = this.reviewAPI
          .updateParticipantCohortAnnotation(ns, wsid, cid, cdrid, pid, annoId, request);
      }
    } else {
    // There's no annotation ID so this must be a create
      const request = <ParticipantCohortAnnotation> {
        cohortAnnotationDefinitionId: defnId,
        ...this.annotation.value,
        [this.valuePropertyName]: newValue,
      };
      apiCall = this.reviewAPI
        .createParticipantCohortAnnotation(ns, wsid, cid, cdrid, pid, request);
    }

    apiCall.subscribe();
  }

  toggleExpandText() {
    this.expandText = !this.expandText;
  }

  get valuePropertyName() {
    return {
      [AnnotationType.STRING]:   'annotationValueString',
      [AnnotationType.ENUM]:     'annotationValueEnum',
      [AnnotationType.DATE]:     'annotationValueDate',
      [AnnotationType.BOOLEAN]:  'annotationValueBoolean',
      [AnnotationType.INTEGER]:  'annotationValueInteger'
    }[this.annotation.definition.annotationType];
  }

  get machineName() {
    return this.annotation.definition.columnName.split(' ').join('-');
  }

  get datatypeDisplay() {
    return this.showDataType
      ? ` (${this.annotation.definition.annotationType})`
      : '';
  }
}
