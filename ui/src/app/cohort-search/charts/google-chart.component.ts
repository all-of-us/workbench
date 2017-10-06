/**
 * ChartComponent: generates a Google Chart
 */
import {
  Component, ChangeDetectionStrategy,
  Input, OnChanges, SimpleChanges
} from '@angular/core';

declare var google: any;


@Component({
  selector: 'app-google-chart',
  template: `<div id={{containerId}}></div>`,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class GoogleChartComponent implements OnChanges {
  @Input() containerId: string;
  @Input() chartType: string;
  @Input() data;
  @Input() options: object;

  draw({chartType, dataTable, options, containerId}) {
    google.charts.setOnLoadCallback(() => {
      const wrapper = new google.visualization.ChartWrapper({
          chartType, dataTable, options, containerId
      });
      wrapper.draw();
    });
  }

  ngOnChanges(changes: SimpleChanges) {
    const chart = {
      chartType: this.chartType,
      dataTable: this.data,
      options: this.options,
      containerId: this.containerId
    };

    for (const prop of Object.keys(changes)) {
      chart[prop] = changes[prop].currentValue;
    }
    this.draw(chart);
  }
}
