import {Component, Input} from '@angular/core';
import {List, Map} from 'immutable';


@Component({
  selector: 'app-combo-chart',
  templateUrl: './combo-chart.component.html',
  styleUrls: ['./combo-chart.component.css'],
})
export class ComboChartComponent {
  /*
   * TODO - this maps gender codes to human readable representations.  We
   * probably need to either grab that repro from the DB or generate a complete
   * list of possible values and include mappings for each one, not just the
   * binary basics.
   */
  readonly codeMap = {
    'M': 'Male',
    'F': 'Female',
  };

  readonly axis = {
    x: {
      show: true,
      label: 'Participant Share By Race',
      showLabel: true,
      percentFormatter: tick => `${tick}%`
    },
    y: {
      show: true,
    }
  };

  private _raw;
  private _data: any = [];

  /**
   * Toggle between normalized and stacked implementations.
   */
  @Input() mode: 'normalized' | 'stacked' = 'normalized';

  /**
   * Transforms the raw data from a List of Maps to a structure with the type signature:
   *   { name: string; series: { name: string; value: number}[]; }[]
   */
  @Input() set data(raw) {
    this._raw = raw;

    this._data = raw
      .map(datum => datum.update('gender', code => this.codeMap[code]))
      .groupBy(datum => `${datum.get('gender', 'Unknown')} ${datum.get('ageRange', 'Unknown')}`)
      .map(group => group
        .map(datum => ({
          name: datum.get('race', 'Unknown'),
          value: datum.get('count', 0)
        }))
        .toArray()
      )
      .map((series, name) => ({series, name}))
      .valueSeq()
      .sort((a, b) => a.name > b.name ? 1 : -1)
      .toArray();

    window.dispatchEvent(new Event('resize'));
  }

  get data() {
    return this._data;
  }

  /**
   * Returns the minimum height of the container as a number representing
   * pixels.  The min height is calculated as 100 * the number of bars with a
   * positive value or else just 100.
   */
  get minHeight() {
    return Math.max(this.data.length * 40, 200);
  }
}
