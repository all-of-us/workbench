import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {Subscription} from 'rxjs/Subscription';

import {Participant} from '../participant.model';
import {ReviewStateService} from '../review-state.service';

@Component({
  selector: 'app-participant-detail',
  templateUrl: './participant-detail.component.html',
  styleUrls: ['./participant-detail.component.css']
})
export class ParticipantDetailComponent implements OnInit, OnDestroy {
  participant: Participant;
  private subscription: Subscription;

  constructor(
    private state: ReviewStateService,
    private route: ActivatedRoute,
  ) {}

  ngOnInit() {
    this.subscription = this.state.participant$
      .merge(this.route.data.pluck('participant'))
      .subscribe(participant => this.participant = <Participant>participant);
  }

  ngOnDestroy() {
    this.state.sidebarOpen.next(false);
    this.state.participant.next(null);
    this.subscription.unsubscribe();
  }

  openSideBar() {
    this.state.sidebarOpen.next(true);
  }
}
