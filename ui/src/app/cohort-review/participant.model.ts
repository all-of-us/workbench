import {CohortStatus, ParticipantCohortStatus} from 'generated';
import * as moment from 'moment';

export class Participant implements ParticipantCohortStatus {

  /* Participant ID (number) */
  participantId: ParticipantCohortStatus['participantId'];
  get id() { return this.participantId; }
  set id(val: number) { this.participantId = val; }

  /* Status */
  status: CohortStatus;

  get formattedStatusText() {
    return Participant.formatStatusForText(this.status);
  }

  /* Birthdate is a Unix timestamp; we also provide a moment.js object here */
  birthDate: ParticipantCohortStatus['birthDate'];
  private _dob: any; /* Moment.js object */

  get dob() {
    if (!this._dob) {
      this._dob = moment(this.birthDate);
    }
    return this._dob;
  }

  /* Demographic information */
  gender: ParticipantCohortStatus['gender'];
  race: ParticipantCohortStatus['race'];
  ethnicity: ParticipantCohortStatus['ethnicity'];

  /* Constructor & static methods */
  constructor(obj?: ParticipantCohortStatus) {
    if (obj) {
      const {participantId, status, gender, race, ethnicity, birthDate} = obj;
      this.id = participantId;
      this.status = status;
      this.gender = gender;
      this.race = race;
      this.ethnicity = ethnicity;
      this.birthDate = birthDate;
    }
  }

  static fromStatus(obj: ParticipantCohortStatus): Participant {
    return new Participant(obj);
  }

  static formatStatusForText(status: CohortStatus): string {
    return {
      [CohortStatus.EXCLUDED]: 'Excluded',
      [CohortStatus.INCLUDED]: 'Included',
      [CohortStatus.NEEDSFURTHERREVIEW]: 'Needs Further Review',
      [CohortStatus.NOTREVIEWED]: '',
    }[status];
  }
}
