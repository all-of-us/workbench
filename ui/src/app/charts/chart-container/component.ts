import {Component, ElementRef, Input, ViewChild} from '@angular/core';
import * as d3 from 'd3';

interface Margin {
  top: number;
  right: number;
  bottom: number;
  left: number;
}

interface Options {
  margin: Margin;
  width: number;
  height: number;
}

const DEFAULT_OPTIONS = <Options>{
  margin: <Margin>{top: 10, right: 20, bottom: 10, left: 20},
  width: 960,
  height: 500,
};

/**
 * Establishes a configurable container for our D3 charts that follows the
 * "margin convention" a la https://bl.ocks.org/mbostock/3019563
 *
 * Usage:
 *    <app-chart-container #container [options]="opts">
 *    </app-chart-container>
 *
 * Chart selection is container.chart,
 * width and height (w/out margins) are container.width, container.height
 */
@Component({
  selector: 'app-chart-container',
  template: `
    <svg #container
      [attr.width]="options.width"
      [attr.height]="options.height">
      <svg:g #chart [attr.transform]="chartTranslation">
      </svg:g>
    </svg>
  `,
})
export default class ChartContainerComponent {

  /*
   * .gElement and .chart
   * The `gElement` is the raw ElementRef that is used as D3's entry point, the `chart`
   * Provides a handle on the SVGGElement selection that renders the chart inside the margins
   */
  @ViewChild('chart') gElement: ElementRef;
  private _selection: d3.Selection;

  get chart(): d3.Selection<SVGGElement> {
    if (!this._selection) {
      this._selection = d3.select(this.gElement.nativeElement);
    }
    return this._selection;
  }

  /*
   * .svg
   * Provides a handle on the parent SVGElement the contains the chart
   */
  @ViewChild('container') svg: ElementRef;

  /*
   * Option processing
   */
  private _options: Options;
  private readonly _defaultOptions: Options = DEFAULT_OPTIONS;

  get options(): Options {
    return this._options || this._defaultOptions;
  }

  @Input() set options(opts) {
    this._options = {
      ...this._defaultOptions,
      ...opts,
    };
  }

  /*
   * Properties
   */
  get width(): number {
    const {left, right} = this.options.margin;
    return this.options.width - left - right;
  }

  get height(): number {
    const {top, bottom} = this.options.margin;
    return this.options.height - top - bottom;
  }

  get chartTranslation(): string {
    const {top, left} = this.options.margin;
    return `translate(${left}, ${top})`;
  }
}
