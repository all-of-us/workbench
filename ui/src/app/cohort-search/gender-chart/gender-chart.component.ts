import {ChangeDetectionStrategy, Component, Input} from '@angular/core';
import {List, Map} from 'immutable';


@Component({
  selector: 'app-gender-chart',
  templateUrl: './gender-chart.component.html',
  styleUrls: ['./gender-chart.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GenderChartComponent {
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

  /*
   * TODO - see the above note on this.codeMap
   */
  readonly defaults = {
    Male: 0,
    Female: 0,
    Unknown: 0
  };

  readonly axis = {
    x: {
      show: true,
      label: '# Participants',
      showLabel: true,
    },
    y: {
      show: true,
      label: 'Gender',
      showLabel: true,
    }
  };

  private _raw: any;
  private _data: any = [];

  /**
   * Transforms the raw chart data from a List of Maps to a list of {name,
   * value} objects suitable for consumption by ngx-charts-bar-horizontal.
   * Attaches the raw data to the component for debugging purposes.
   */
  @Input() set data(raw) {
    this._raw = raw;
    this._data = raw
      .map(datum => datum.update('gender', code => this.codeMap[code]))
      .groupBy(datum => datum.get('gender', 'Unknown'))
      .map(data => data.reduce((s, d) => s + d.get('count', 0), 0))
      .toMap()
      .mergeWith((old, _) => old, this.defaults)
      .map((value, name) => ({name, value}))
      .valueSeq()
      .toArray();

    /*
     * This is a kind of workaround - the way ngx-charts detects how large it
     * should be (to be responsive) only appears to detect changes in the
     * parent container on a window resize event
     */
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
    const numberBars = this.data.reduce((count, obj) => {
      if (obj.value > 0) { count += 1; }
      return count;
    }, 0);

    return Math.max(numberBars * 100, 200);
  }
}
