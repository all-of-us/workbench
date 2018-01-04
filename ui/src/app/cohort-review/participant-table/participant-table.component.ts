import {Component} from '@angular/core';

import {Participant} from '../participant.model';

const dummy_data = () =>
  Array.from(new Array(100).keys())
    .map(i => Participant.makeRandom(i));

@Component({
  selector: 'app-participant-table',
  templateUrl: './participant-table.component.html',
})
export class ParticipantTableComponent {

  // private loading = false;

  DUMMY_DATA: Participant[] = dummy_data();
  total = this.DUMMY_DATA.length;

  refresh() {}
}
