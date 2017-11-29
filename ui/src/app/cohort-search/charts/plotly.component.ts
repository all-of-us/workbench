import {
  Component,
  ElementRef,
  HostListener,
  Input,
  OnChanges,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import * as Plotly from 'plotly.js';

@Component({
  selector: 'app-plotly',
  template: `
  <div #chartContainer
    [style.padding]="'0.5rem'"
    [style.margin]="'0.5rem'">
    <div #chart></div>
  </div>
  `,
})
export class PlotlyComponent implements OnChanges {
  @Input() data: Plotly.Data;
  @Input() layout: Plotly.Layout;
  @Input() configuration?: Partial<Plotly.Config> = {
    modeBarButtonsToRemove: [
      'sendDataToCloud',
      'select2d',
      'lasso2d',
    ],
    displaylogo: false,
  };

  @ViewChild('chartContainer') chartContainer: ElementRef;
  @ViewChild('chart') chart: ElementRef;
  get element() { return this.chart.nativeElement; }

  @HostListener('window:resize', ['$event'])
  onResize(event) {
    // Debug Canary console log
    // console.log(`Window width: ${event.target.innerWidth}`);
    Plotly.Plots.resize(this.element);
  }

  ngOnChanges(changes: SimpleChanges) {
    const data = changes.data && changes.data.currentValue || this.data;
    const layout = changes.layout && changes.layout.currentValue || this.layout;
    const configuration = changes.configuration
      && changes.configuration.currentValue
      || this.configuration;

    const {width, height} = this.chartContainer.nativeElement.getBoundingClientRect();

    layout.width = width;
    layout.height = height;

    Plotly.newPlot(this.element, data, layout, configuration);
    Plotly.Plots.resize(this.element);
  }
}
