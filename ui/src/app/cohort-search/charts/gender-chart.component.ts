import {Component, Input} from '@angular/core';

@Component({
  selector: 'app-gender-chart',
  template: `
    <p>
      gender-chart works!
    </p>
  `,
  styles: []
})
export class GenderChartComponent {
  @Input() data;
}
