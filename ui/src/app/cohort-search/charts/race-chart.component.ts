import {Component, ChangeDetectionStrategy, Input} from '@angular/core';


@Component({
  selector: 'app-race-chart',
  template: `
    <app-google-chart
      [containerId]="'race_chart'"
      [chartType]="'PieChart'"
      [data]="data"
      [options]="options"
    ></app-google-chart>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RaceChartComponent {

  private _data: any;

  @Input()
  set data(counts: object) {
    const header = ['Race', 'Count Per'];

    /* Default Case or no value provided */
    if (!Object.keys(counts).length) {
      this._data = [ header, ['Unknown', 0]];
      return;
    }

    /* Normal case with values */
    const newData = [];
    for (const key of Object.keys(counts)) {
      newData.push([key, counts[key]]);
    }
    this._data = [header, ...newData];
  }

  get data() {
    return this._data;
  }

  readonly options  = {
    title: 'Results By Race',
    chartArea: {width: '80%'},
    width: '100%',
    height: '300'
  };
}
