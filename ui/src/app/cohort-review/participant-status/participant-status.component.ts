import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {FormControl} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';

import {Participant} from '../participant.model';

import {WorkspaceStorageService} from 'app/services/workspace-storage.service';

import {
  CohortReviewService,
  CohortStatus,
  ModifyCohortStatusRequest,
  ParticipantCohortStatus,
} from 'generated';

const validStatuses = [
  CohortStatus.INCLUDED,
  CohortStatus.EXCLUDED,
  CohortStatus.NEEDSFURTHERREVIEW,
];


@Component({
  selector: 'app-participant-status',
  templateUrl: './participant-status.component.html',
  styleUrls: ['./participant-status.component.css']
})
export class ParticipantStatusComponent implements OnInit, OnDestroy {

  readonly cohortStatusList = validStatuses.map(status => ({
    value: status,
    display: Participant.formatStatusForText(status),
  }));

  private _participant: Participant;
  private cdrId: number;

  @Input() set participant(p: Participant) {
    this._participant = p;
    const status = p.status === CohortStatus.NOTREVIEWED ? null : p.status;
    this.statusControl.setValue(status, {emitEvent: false});
  }

  get participant() {
    return this._participant;
  }

  statusControl = new FormControl();
  subscription: Subscription;

  constructor(
    private reviewAPI: CohortReviewService,
    private route: ActivatedRoute,
    private workspaceStorageService: WorkspaceStorageService,
  ) {}

  ngOnInit() {
    this.workspaceStorageService.activeWorkspace$.subscribe((workspace) => {
      this.cdrId = parseInt(workspace.cdrVersionId, 10);
    });
    this.workspaceStorageService.reloadIfNew(
      this.route.snapshot.parent.params['ns'],
      this.route.snapshot.parent.params['wsid']);

    this.subscription = this.statusControl.valueChanges
      .filter(status => validStatuses.includes(status))
      .switchMap(status => this.updateStatus(status))
      .subscribe();
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  updateStatus(status): Observable<ParticipantCohortStatus> {
    const pid = this.participant.id;
    const request = <ModifyCohortStatusRequest>{status};
    const {ns, wsid, cid} = this.route.parent.snapshot.parent.params;
    return this.reviewAPI.updateParticipantCohortStatus(ns, wsid, cid, this.cdrId, pid, request);
  }
}
