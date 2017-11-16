import {Component, Input, ChangeDetectionStrategy} from '@angular/core';
import {List, Map, Set} from 'immutable';

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

const genderDataTable = (cleanData: Data): any[] => {
  const headers = ['Gender', 'Count', {role: 'style'}];
  const defaults = {Male: 0, Female: 0};
  const colors = {Male: '#6870C4', Female: '#6FEAD9'};

  const data = cleanData
    .groupBy(gender)
    .map(sumData)
    .toMap()  // Makes typescript happy; otherwise unnecessary
    .mergeWith((old, _) => old, defaults)
    .map((_count, _gender) => [_gender, _count, colors[_gender]])
    .valueSeq()
    .toArray();

  return [headers, ...data];
};

const combinationDataTable = (cleanData: Data): any[] => {
  let raceSet = Set(cleanData.map(race)).toArray().sort();

  raceSet = raceSet.length > 0
    ? raceSet
    : ['Black or African American', 'White', 'Unknown'];

  const headers = ['Gender-Age', ...raceSet];

  let data = cleanData
    .groupBy(genderAndAge)
    .sort()
    .map(group => group
      .groupBy(race)
      .map(sumData)
      .sort()
      .valueSeq()
      .toArray()
    )
    .map((val, key) => [key, ...val])
    .valueSeq()
    .toArray();

  if (!data.length) {
    data = [
      ['Female 0-18',  0, 0, 0],
      ['Female 19-44', 0, 0, 0],
      ['Female 45-64', 0, 0, 0],
      ['Female > 65',  0, 0, 0],
      ['Male 0-18',    0, 0, 0],
      ['Male 19-44',   0, 0, 0],
      ['Male 45-64',   0, 0, 0],
      ['Male > 65',    0, 0, 0],
    ];
  }

  return [headers, ...data];
};


@Component({
  selector: 'app-charts',
  template: `
    <div class="box">

      <div appGoogleChart
        [style.padding]="'0.5rem'"
        id="genderChart"
        [chartType]="'BarChart'"
        [dataTable]="genderData"
        [options]="genderOpts">
      </div>

      <div appGoogleChart
        [style.padding]="'0.5rem'"
        id="combinationChart"
        [chartType]="'BarChart'"
        [dataTable]="combinationData"
        [options]="combinationOpts">
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
  private rawData: Data;

  private genderData: any[];
  private readonly genderOpts = {
    title: 'Results By Gender',
    chartArea: {width: '80%'},
    legend: { position: 'none' },
    hAxis: {
      title: 'Total Count',
      minValue: 0,
      width: '100%',
      height: '300'
    },
  };

  private combinationData: any[];
  private readonly combinationOpts = {
    title: 'Results by Gender, Age, and Race',
    chartArea: {width: '75%'},
    bar: {groupWidth: '75%'},
    legend: {position: 'top', maxlines: 3},
    hAxis: {
      title: 'Total Count',
      minValue: 0,
      width: '100%',
      height: '300',
    },
    isStacked: 'percent',
  };

  @Input()
  set data(rawData: Data) {
    this.rawData = rawData;
    // Preprocess Codes into Human Readable Strings
    const cleanData = rawData.map(decodeGender);
    // Process the data into charts
    this.genderData = genderDataTable(cleanData);
    this.combinationData = combinationDataTable(cleanData);
  }

  get data() {
    return this.rawData;
  }
}
