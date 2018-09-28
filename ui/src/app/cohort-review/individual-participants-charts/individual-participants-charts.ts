import {Component, Input, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {Subscription} from 'rxjs/Subscription';

import {ReviewStatus} from 'generated';
import {ReviewStateService} from '../review-state.service';
import {Set} from "immutable";
import {CohortSearchActions, CohortSearchState, getParticipantData} from "../../cohort-search/redux";
import {NgRedux} from "@angular-redux/store";

@Component({
  selector: 'app-individual-participants-charts',
  templateUrl: './individual-participants-charts.html',
  styleUrls: ['./individual-participants-charts.css']
})
export class IndividualParticipantsCharts implements OnInit {
  selected = Set<number>();
  options: any;
  subscription: Subscription;
  participantsId: any;
  conditionData;
  procedureData;
  drugData

  constructor(
    private state: ReviewStateService,
    private route: ActivatedRoute,
    private router: Router,
    private actions: CohortSearchActions,
    private ngRedux: NgRedux<CohortSearchState>,
  ) {}

  ngOnInit() {
    const {ns, wsid, cid} = this.route.parent.snapshot.params;
    const cdrid = +(this.route.parent.snapshot.data.workspace.cdrVersionId);
    this.subscription = this.route.data.map(({participant}) => participant)
      .subscribe(participants =>{
        this.participantsId = participants.participantId;
      })
    const limit = 5;
    this.actions.fetchIndividualParticipantsData(ns, wsid, cid, cdrid, this.participantsId,'CONDITION', limit);
    this.actions.fetchIndividualParticipantsData(ns, wsid, cid, cdrid, this.participantsId,'PROCEDURE', limit);
    this.actions.fetchIndividualParticipantsData(ns, wsid, cid, cdrid, this.participantsId,'DRUG', limit);

    const getConditionsParticipantsDomainData = this.ngRedux
      .select(getParticipantData('CONDITION'))
      .filter(domain => !!domain)
      .subscribe(loading => {
        this.conditionData = loading
        console.log(this.conditionData);
      });
    this.subscription = getConditionsParticipantsDomainData;

    const getProcedureParticipantsDomainData = this.ngRedux
      .select(getParticipantData('PROCEDURE'))
      .filter(domain => !!domain)
      .subscribe(loading => {
        this.procedureData = loading
      });
    this.subscription = getProcedureParticipantsDomainData;

    const getDrugParticipantsDomainData = this.ngRedux
      .select(getParticipantData('DRUG'))
      .filter(domain => !!domain)
      .subscribe(loading => {
        this.drugData = loading
      });
    this.subscription = getDrugParticipantsDomainData;

  }
}
