import {Component, Input, ChangeDetectionStrategy} from '@angular/core';
import {List, Map} from 'immutable';

const decoder = code => ({M: 'Male', F: 'Female'}[code]);
const decodeGender = datum => datum.update('gender', decoder);
const keepOld = (old, _) => old;

const baseLine = {
  gender: Map({Male: 0, Female: 0}),
  race: Map({}),
  ageRange: Map({}),
};

@Component({
  selector: 'app-charts',
  template: `
    <div class="box">
      <div appGoogleChart
        id="genderChart"
        [chartType]="'BarChart'"
        [dataTable]="genderData"
        [options]="genderOpts">
      </div>
      <div appGoogleChart
        id="raceChart"
        [chartType]="'PieChart'"
        [dataTable]="raceData"
        [options]="raceOpts">
      </div>
    </div>
  `,
  styles: [`
    .box {
      border: 1px solid #d7d7d7;
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChartsComponent {
  private rawData: List<Map<string, any>>;

  private ageData;
  private genderData;
  private raceData;

  private readonly genderOpts = {
    title: 'Results By Gender',
    chartArea: {width: '80%'},
    isStacked: true,
    legend: { position: 'none' },
    hAxis: {
      title: 'Total Count',
      minValue: 0,
      width: '100%',
      height: '300'
    },
  };

  private readonly raceOpts = {
    title: 'Results By Race',
    chartArea: {width: '80%'},
    width: '100%',
    height: '300'
  };

  @Input()
  set data(rawData) {
    const processKey = key =>
      rawData
        .map(decodeGender)
        .groupBy(datum => datum.get(key))
        .map(data => data.reduce((s, d) => s + d.get('count'), 0))
        .toMap()
        .mergeWith(keepOld, baseLine[key])
        .entrySeq()
        .toArray();

    this.rawData = rawData;
    this.genderData = [
      ['Gender', 'Count'],
      ...processKey('gender')
    ];

    this.raceData = [
      ['Race', 'Count'],
      ...processKey('race')
    ];

    this.ageData = processKey('ageRange');
  }

  get data() {
    return this.rawData;
  }
}
