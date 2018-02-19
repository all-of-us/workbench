import {Component, Input, OnInit} from '@angular/core';
import {FormControl} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {Observable} from 'rxjs/Observable';
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
  selector: 'app-annotation-item',
  templateUrl: './annotation-item.component.html',
  styleUrls: ['./annotation-item.component.css']
})
export class AnnotationItemComponent implements OnInit  {
  @Input() definition: Definition;
  @Input() value: Annotation;
  @Input() showDataType: boolean;

  private control = new FormControl();
  private subscription: Subscription;
  private expandText = false;
  readonly kinds = AnnotationType;

  constructor(
    private state: ReviewStateService,
    private reviewAPI: CohortReviewService,
    private route: ActivatedRoute,
  ) {}

  ngOnInit() {
    // TODO (jms) - once the backend is in place, here we'll establish
    // delegating to create / update / destroy as is appropriate for changes to
    // this.control
    this.subscription = Observable.combineLatest(
        this.control.valueChanges,
        this.control.statusChanges)
      .subscribe(console.log);
  }

  create(value): Observable<Annotation> {
    const {ns, wsid, cid, cdrid, pid} = this.pathParams;
    const request = <Annotation>{
      ...this.value,
      [this.valuePropertyName]: value
    };
    return this.reviewAPI
      .createParticipantCohortAnnotation(ns, wsid, cid, cdrid, pid, request);
  }

  update(value): Observable<Annotation> {
    const {ns, wsid, cid, cdrid, pid} = this.pathParams;
    const aid = this.definition.cohortAnnotationDefinitionId;
    const request = <Request>{
      [this.valuePropertyName]: value
    };
    return this.reviewAPI
      .updateParticipantCohortAnnotation(ns, wsid, cid, cdrid, pid, aid, request);
  }

  delete(): Observable<{}> {
    const {ns, wsid, cid, cdrid, pid} = this.pathParams;
    const aid = this.definition.cohortAnnotationDefinitionId;
    return this.reviewAPI
      .deleteParticipantCohortAnnotation(ns, wsid, cid, cdrid, pid, aid);
  }

  refresh(): Observable<Annotation[]> {
    const {ns, wsid, cid, cdrid, pid} = this.pathParams;
    return (this.reviewAPI
      .getParticipantCohortAnnotations(ns, wsid, cid, cdrid, pid)
      .pluck('items') as Observable<Annotation[]>);
  }

  toggleExpandText() {
    this.expandText = !this.expandText;
  }

  private get pathParams() {
    const {ns, wsid, cid} = this.route.snapshot.params;
    const pid = this.value.participantId;
    const cdrid = +(this.route.snapshot.data.workspace.cdrVersionId);
    return {ns, wsid, cid, pid, cdrid};
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

  get datatypeDisplay() {
    return this.showDataType
      ? ` (${this.definition.annotationType})`
      : '';
  }
}
