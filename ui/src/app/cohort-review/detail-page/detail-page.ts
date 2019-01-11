import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {Subscription} from 'rxjs/Subscription';

import {Participant} from '../participant.model';
import {ReviewStateService} from '../review-state.service';

@Component({
  templateUrl: './detail-page.html',
  styleUrls: ['./detail-page.css']
})
export class DetailPage implements OnInit, OnDestroy {

  sidebarOpen = true;
  participant: Participant;
  subscription: Subscription;
  participantId: number;
  sidebarTransition: boolean;
  constructor(private route: ActivatedRoute, private state: ReviewStateService) {}

  ngOnInit() {
    const {annotationDefinitions} = this.route.snapshot.data;
    this.state.annotationDefinitions.next(annotationDefinitions);
    this.subscription = this.route.data.subscribe(({participant, annotations}) => {
      participant.annotations = annotations;
      this.participant = participant;
    });
    const element = document.getElementById('review-sidebar-content');
    element.addEventListener('transitionend', () => {
      this.sidebarTransition = !this.sidebarTransition;
    }, false);
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
    const element = document.getElementById('review-sidebar-content');
    element.removeEventListener('transitionend', () => {
      this.sidebarTransition = !this.sidebarTransition;
    }, false);
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
