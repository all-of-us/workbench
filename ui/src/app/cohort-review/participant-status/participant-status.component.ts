import {
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output,
} from '@angular/core';
import {FormControl} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';

import {
  CohortReviewService,
  CohortStatus,
  ModifyCohortStatusRequest,
  ParticipantCohortStatus,
} from 'generated';

const CDR_VERSION = 1;

@Component({
  selector: 'app-participant-status',
  templateUrl: './participant-status.component.html',
  styleUrls: ['./participant-status.component.css']
})
export class ParticipantStatusComponent implements OnInit, OnDestroy {

  statusControl = new FormControl();

  private _participant: ParticipantCohortStatus | undefined;

  @Input()
  set participant(value) {
    this._participant = value;
    if (value) {
      this.statusControl.enable({emitEvent: false});
    } else {
      this.statusControl.disable({emitEvent: false});
    }
  }

  get participant() {
    return this._participant;
  }

  @Output() participantChange = new EventEmitter<ParticipantCohortStatus>();

  readonly CohortStatus = CohortStatus;
  private subscription: Subscription;
  private changingStatus = false;

  constructor(
    private reviewAPI: CohortReviewService,
    private route: ActivatedRoute,
  ) {}

  ngOnInit() {
    this.subscription = this.statusControl.valueChanges
      .switchMap(this._callApi)
      .subscribe(this._emit);
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  private _emit = (val) => {
    this.changingStatus = false;
    this.participantChange.emit(val);
  }

  private _callApi = (status: CohortStatus): Observable<ParticipantCohortStatus> => {
    this.changingStatus = true;
    const request = <ModifyCohortStatusRequest>{status};

    const {
      ns: workspaceNamespace,
      wsid: workspaceId,
      cid: cohortId
    } = this.route.snapshot.params;

    const {participantId} = this.participant;

    // DEBUG calls
    // console.log(`Calling updateParticipantCohortStatus with`);
    // console.table([
    //   {arg: 'workspaceNamespace', value: workspaceNamespace},
    //   {arg: 'workspaceId', value: workspaceId},
    //   {arg: 'cohortId', value: cohortId},
    //   {arg: 'CDR Version', value: CDR_VERSION},
    //   {arg: 'participantId', value: participantId},
    //   {arg: 'request', value: JSON.stringify(request)},
    // ]);

    return this.reviewAPI.updateParticipantCohortStatus(
      workspaceNamespace,
      workspaceId,
      cohortId,
      CDR_VERSION,
      participantId,
      request
    );
  }
}
