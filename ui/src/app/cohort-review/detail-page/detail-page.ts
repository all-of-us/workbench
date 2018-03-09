import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';

import {Participant} from '../participant.model';
import {ReviewStateService} from '../review-state.service';

import {CohortReview} from 'generated';


@Component({
  templateUrl: './detail-page.html',
  styleUrls: ['./detail-page.css']
})
export class DetailPage implements OnInit, OnDestroy {

  sidebarOpen = true;
  participant: Participant;
  subscription: Subscription;

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
}
