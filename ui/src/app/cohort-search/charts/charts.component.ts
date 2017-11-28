import {Component, Input} from '@angular/core';
import {List, Map} from 'immutable';

type Datum = Map<string, any>;
type Data = List<Datum>;

const decodeGender = datum =>
  datum.update('gender', code => ({
      'M': 'Male',
      'F': 'Female'
  }[code]));

const race = (datum: Datum): string => datum.get('race', 'Unknown');
const range = (datum: Datum): string => datum.get('ageRange', 'Unknown');
const gender = (datum: Datum): string => datum.get('gender', 'Unknown');
const count = (datum: Datum): number => datum.get('count', 0);
const sumData = (data: Data): number => data.reduce((s, d) => s + count(d), 0);
const genderAndAge = (datum: Datum): string => `${gender(datum)} ${range(datum)}`;

const _genderChart = (data) => {
  const counts = data.groupBy(gender).map(sumData);
  return [{
    type: 'bar',
    orientation: 'h',
    marker: {
      color: ['#6870C4', '#6FEAD9'],
    },
    y: ['Male', 'Female'],
    x: [counts.get('Male', 0), counts.get('Female', 0)]
  }];
};

const _comboChart = (data) => {
  const _trace = {
    type: 'bar',
    orientation: 'h',
    y: [
      'Male > 65',
      'Male 45-64',
      'Male 19-44',
      'Male 0-18',
      'Female > 65',
      'Female 45-64',
      'Female 19-44',
      'Female 0-18',
    ]
  };

  const _getCounts = (_data) => {
    const counts = _data
      .groupBy(genderAndAge)
      .map(val => val.getIn([0, 'count']));
    return _trace.y.map(key => counts.get(key, 0));
  };

  return data
    .groupBy(race)
    .map((val, key) => ({
      ..._trace,
      name: key,
      x: _getCounts(val),
    }))
    .valueSeq()
    .toArray();
};

@Component({
  selector: 'app-charts',
  template: `
    <div class="box">
      <div
        *ngIf="!(genderData || combinationData)"
        [style.padding]="'0.5rem'"
        [style.text-align]="'center'">
        <em>No data to display yet.</em>
      </div>

      <app-plotly
        *ngIf="genderData"
        id="genderChart"
        [data]="genderData"
        [layout]="genderLayout">
      </app-plotly>

      <app-plotly
        *ngIf="combinationData"
        id="combinationChart"
        [data]="combinationData"
        [layout]="combinationLayout">
      </app-plotly>
    </div>
  `,
  styles: [`
    .box {
      border: 1px solid #d7d7d7;
    }
  `],
})
export class ChartsComponent {
  private _data: Data;
  private genderData;
  private combinationData;

  /* tslint:disable-next-line:no-unused-variable */
  private readonly genderLayout = {
    title: 'Results By Gender',
    showLegend: false,
  };

  /* tslint:disable-next-line:no-unused-variable */
  private readonly combinationLayout = {
    title: 'Results by Gender, Age, and Race',
    barmode: 'stack',
    margin: {
      l: 90,
    },
    legend: {
      x: 1,
      y: 1,
      font: {
        family: 'sans-serif',
        size: 12,
        color: '#000'
      },
      bgcolor: '#E2E2E2',
      bordercolor: '#FFFFFF',
      borderwidth: 2
    },
  };

  @Input()
  set data(rawData: Data) {
    this._data = rawData;

    if (rawData.isEmpty()) {
      this.genderData = null;
      this.combinationData = null;
      return;
    }

    // Preprocess Codes into Human Readable Strings
    const cleanData = rawData.map(decodeGender);

    // Generate the charts
    this.genderData = _genderChart(cleanData);
    this.combinationData = _comboChart(cleanData);
  }

  get data() {
    return this._data;
  }
}
