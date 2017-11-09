import {Component, Input} from '@angular/core';

@Component({
  selector: 'app-charts',
  template: `
    <div class="box">
      <app-gender-chart
        [data]="genderData">
      </app-gender-chart>

      <app-race-chart
        [data]="raceData">
      </app-race-chart>
    </div>
  `,
  styles: [`.box { border: 1px solid #d7d7d7; }`],
})
export class ChartsComponent {
  @Input() data;
}
