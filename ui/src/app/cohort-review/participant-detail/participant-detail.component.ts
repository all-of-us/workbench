import {Component, OnDestroy} from '@angular/core';

import {Participant} from '../participant.model';
import {ReviewStateService} from '../review-state.service';

@Component({
  selector: 'app-participant-detail',
  templateUrl: './participant-detail.component.html',
  styleUrls: ['./participant-detail.component.css']
})
export class ParticipantDetailComponent implements OnDestroy {
  participant: Participant = Participant.makeRandom(123);
  constructor(private state: ReviewStateService) {}
  openSideBar() { this.state.sidebarOpen.next(true); }
  ngOnDestroy() { this.state.sidebarOpen.next(false); }
}
