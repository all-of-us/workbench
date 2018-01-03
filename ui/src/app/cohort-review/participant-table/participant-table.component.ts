import {
  Component,
  EventEmitter,
  OnDestroy,
  OnInit,
  Output
} from '@angular/core';

import {
  CohortReviewService,
  CohortStatus,
  ParticipantCohortStatus,
} from 'generated';

/* Pick a random element from an array */
const choice = (arr) => {
  const index = Math.floor(Math.random() * arr.length);
  return arr[index];
};

const start = new Date(1960, 0, 1);
const end = new Date();
const statusOpts = [
  CohortStatus.INCLUDED,
  CohortStatus.EXCLUDED,
  CohortStatus.NEEDSFURTHERREVIEW,
  CohortStatus.NOTREVIEWED,
];

const randomDate = () =>
  new Date(start.getTime() + Math.random() * (end.getTime() - start.getTime()));

const dummy_data = () => Array.from(new Array(100).keys()).map(
  (i) => ({
    id: i,
    dob: randomDate(),
    gender: choice('MF'),
    race: choice('ABCDEF'),
    ethnicity: choice('XYZ'),
    status: choice(statusOpts),
  }));

@Component({
  selector: 'app-participant-table',
  templateUrl: './participant-table.component.html',
})
export class ParticipantTableComponent implements OnInit {
  // @Output() onSelection = new EventEmitter<ParticipantCohortStatus>();

  private loading = false;
  DUMMY_DATA = dummy_data();
  total = this.DUMMY_DATA.length;

  constructor() {}

  ngOnInit() {
  }

  refresh() {
  }

  statusText(stat: CohortStatus): string {
    return {
      [CohortStatus.EXCLUDED]: 'Excluded',
      [CohortStatus.INCLUDED]: 'Included',
      [CohortStatus.NEEDSFURTHERREVIEW]: 'Undecided',
      [CohortStatus.NOTREVIEWED]: 'Unreviewed',
    }[stat];
  }

  statusClass(stat: CohortStatus): object {
    if (stat === CohortStatus.INCLUDED) {
      return {'label-success': true};
    } else if (stat === CohortStatus.EXCLUDED) {
      return {'label-warning': true};
    } else {
      return {'label-info': true};
    }
  }

  onSelect(selected: ParticipantCohortStatus): void {
    // this.onSelection.emit(selected);
    // this.state.participant.next(selected);
  }
}
