/* tslint:disable:no-unused-variable */
// TODO (jms) - this is a stub, when written, make sure it fully passes linting
import {Component, Input, OnInit} from '@angular/core';
import {FormControl} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {Subscription} from 'rxjs/Subscription';

import {ReviewStateService} from '../review-state.service';

import {
  AnnotationType,
  CohortAnnotationDefinition as Definition,
  CohortReviewService,
  ModifyParticipantCohortAnnotationRequest as Request,
  ParticipantCohortAnnotation as Annotation,
} from 'generated';

@Component({
  selector: 'app-participant-annotation',
  templateUrl: './participant-annotation.component.html',
  styleUrls: ['./participant-annotation.component.css']
})
export class ParticipantAnnotationComponent implements OnInit {
  @Input() definition: Definition;
  @Input() value: Annotation;
  @Input() verbose: boolean;

  private control = new FormControl();
  private subscription: Subscription;
  private expandText = false;
  readonly kinds = AnnotationType;

  private _create;
  private _update;
  private _delete;

  constructor(
    private state: ReviewStateService,
    private reviewAPI: CohortReviewService,
    private route: ActivatedRoute,
  ) {
    this._create = reviewAPI.createParticipantCohortAnnotation;
    this._update = reviewAPI.updateParticipantCohortAnnotation;
    this._delete = reviewAPI.deleteParticipantCohortAnnotation;
  }

  ngOnInit() {
  }

  create(kind, value) {
    const {ns, wsid, cid} = this.route.snapshot.params;
    const pid = this.value.participantId;
    const request = <Annotation>{
      ...this.value,
      [this.valuePropertyName]: value
    };
    return this._create(ns, wsid, cid, pid, request);
  }

  update(value) {
    const {ns, wsid, cid} = this.route.snapshot.params;
    const aid = this.definition.cohortAnnotationDefinitionId;
    const pid = this.value.participantId;
    const request = <Request>{
      [this.valuePropertyName]: value
    };
    return this._update(ns, wsid, cid, pid, aid, request);
  }

  delete() {
    const {ns, wsid, cid} = this.route.snapshot.params;
    const aid = this.definition.cohortAnnotationDefinitionId;
    const pid = this.value.participantId;
    return this._delete(ns, wsid, cid, pid, aid);
  }

  get valuePropertyName() {
    return {
      [this.kinds.STRING]:   'valueString',
      [this.kinds.ENUM]:     'valueEnum',
      [this.kinds.DATE]:     'valueDate',
      [this.kinds.BOOLEAN]:  'valueBoolean',
      [this.kinds.INTEGER]:  'valueInteger'
    }[this.definition.annotationType];
  }

  get machineName() {
    return this.definition.columnName.split(' ').join('-');
  }

  get datatype() {
    return this.verbose
      ? ` (${this.definition.annotationType})`
      : '';
  }

  toggleExpandText() {
    this.expandText = !this.expandText;
  }

  edit() {
    this.state.annotationMgrState.next({
      open: true,
      mode: 'edit',
      defn: this.definition
    });
  }
}
