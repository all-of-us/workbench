import * as moment from 'moment';

import {CohortStatus, ParticipantCohortStatus} from 'generated';

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

export class Participant implements ParticipantCohortStatus {
  participantId: number;
  status: CohortStatus;

  birthDatetime: number;
  dob: any; /* moment.js object */

  /* Potentially each of these should be an Enum */
  gender: string;
  race: string;
  ethnicity: string;

  /* Aliasing participantId */
  get id() { return this.participantId; }
  set id(val: number) { this.participantId = val; }

  /* Computed properties */
  get formattedStatusText() {
    return {
      [CohortStatus.EXCLUDED]: 'Excluded',
      [CohortStatus.INCLUDED]: 'Included',
      [CohortStatus.NEEDSFURTHERREVIEW]: 'Undecided',
      [CohortStatus.NOTREVIEWED]: 'Unreviewed',
    }[this.status];
  }

  get statusLabelClass() {
    if (this.status === CohortStatus.INCLUDED) {
      return {'label-success': true};
    } else if (this.status === CohortStatus.EXCLUDED) {
      return {'label-warning': true};
    } else {
      return {'label-info': true};
    }
  }

  constructor(obj?: ParticipantCohortStatus) {
    if (obj) {
      const {participantId, status, gender, race, ethnicity, birthDatetime} = obj;
      this.id = participantId;
      this.status = status;
      this.gender = gender;
      this.race = race;
      this.ethnicity = ethnicity;
      this.birthDatetime = birthDatetime;
      this.dob = moment(birthDatetime);
    }
  }

  static fromStatus(obj: ParticipantCohortStatus): Participant {
    return new Participant(obj);
  }

  static makeRandom(id: number): Participant {
    const p = new Participant();
    p.id = id;
    p.dob = randomDate();
    p.gender = choice('MF');
    p.race = choice('ABCDEF');
    p.ethnicity = choice('XYZ');
    p.status = choice(statusOpts);
    return p;
  }

  static makeRandomFromExisting(obj: ParticipantCohortStatus): Participant {
    const p = Participant.makeRandom(obj.participantId);
    p.status = obj.status;
    return p;
  }
}
