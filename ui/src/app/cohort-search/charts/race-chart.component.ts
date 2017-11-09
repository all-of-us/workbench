import {Component, Input} from '@angular/core';

@Component({
  selector: 'app-race-chart',
  template: `
    <p>
      race-chart works!
    </p>
  `,
  styles: []
})
export class RaceChartComponent {
  @Input() data;
}
