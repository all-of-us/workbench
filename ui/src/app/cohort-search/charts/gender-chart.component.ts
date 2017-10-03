import {Component, ChangeDetectionStrategy, Input} from '@angular/core';


@Component({
  selector: 'app-gender-chart',
  template: `
    <app-google-chart
      [containerId]="'gender_chart'"
      [chartType]="'BarChart'"
      [data]="data"
      [options]="options"
    ></app-google-chart>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class GenderChartComponent {

  private _data: any;

  @Input()
  set data(counts: object) {
    const header = ['Gender', 'Count', { role: 'style' }];
    /* Default Case or no value provided */
    if (!Object.keys(counts).length) {
      this._data = [header, ['Unknown', 0, 'gray']];
      return;
    }

    /* Normal case with values */
    const newData = [];
    for (const key of Object.keys(counts)) {
      newData.push([key, counts[key], this.colors[key]]);
    }
    this._data = [header, ...newData];
  }

  get data() {
    return this._data;
  }

  readonly colors = {
    Female: 'blue',
    Male: 'red',
    Unknown: 'gray'
  };

  readonly options = {
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
}
