import {Component, ChangeDetectionStrategy, Input} from '@angular/core';
import {Set} from 'immutable';


@Component({
  selector: 'app-charts',
  template: `
    <div class="box">
      <app-gender-chart [data]="genderCounts">
      </app-gender-chart>
    </div>
    <div>&nbsp;</div>
    <div class="box">
      <app-race-chart [data]="raceCounts">
      </app-race-chart>
    </div>
  `,
  styles: [`.box { border: 1px solid #d7d7d7; }`],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ChartsComponent {
  /*
   * NOTE (jms) PROVISIONAL
   * The purpose of this component is to generate the chart data from the
   * aggregated subject set by iterating that set in a single pass; the
   * actual counting code and type mappings, however, are NOT finished and
   * should be considered provisional.
   */

  private _subjects: Set<string>;

  @Input()
  set subjects(maybeSubjects) {
    const subjectSet = !maybeSubjects ? Set([]) : maybeSubjects;

    const genderCounts = {};
    const raceCounts = {};

    subjectSet.forEach(subject => {
      let [uid, gender, race] = subject.split(',');
      gender = this.genderCodeMap[gender] || 'Unknown';
      race = this.raceCodeMap[race] || 'Unknown';

      /* Check for new keys */
      if (!genderCounts[gender]) {
        genderCounts[gender] = 0;
      }
      if (!raceCounts[race]) {
        raceCounts[race] = 0;
      }

      /* Update the counts */
      genderCounts[gender] += 1;
      raceCounts[race] += 1;
    });
    this.genderCounts = genderCounts;
    this.raceCounts = raceCounts;
  }

  get subjects() {
    return this._subjects;
  }

  private genderCounts;
  private raceCounts;

  readonly genderCodeMap = {
    '1': 'Male',
    '2': 'Female',
  };

  readonly raceCodeMap = {
    '1': 'Caucasian',
    '2': 'African American'
  };
}
