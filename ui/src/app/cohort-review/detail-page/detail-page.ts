import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {Subscription} from 'rxjs/Subscription';

import {Participant} from 'app/cohort-review/participant.model';

@Component({
  templateUrl: './detail-page.html',
  styleUrls: ['./detail-page.css']
})
export class DetailPage implements OnInit, OnDestroy {

  sidebarOpen = true;
  participant: Participant;
  subscription: Subscription;
  participantId: number;
  constructor(private route: ActivatedRoute) {}

  ngOnInit() {
    this.subscription = this.route.data.subscribe(({participant, annotations}) => {
      participant.annotations = annotations;
      this.participant = participant;
    });
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  get angleDir() {
    return this.sidebarOpen ? 'right' : 'left';
  }

  toggleSidebar() {
    this.sidebarOpen = !this.sidebarOpen;
  }
  getNavigatedParticipantId(id) {
    if (id) {
      this.participantId = id;
    }
  }
}
