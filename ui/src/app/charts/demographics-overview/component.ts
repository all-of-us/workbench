import {Component, Input, ViewChild} from '@angular/core';
import * as d3 from 'd3';

import ChartInfoContainer from './model';


@Component({
  selector: 'app-demographics-overview-chart',
  template: `
    <div *ngIf='data'>
      <app-chart-container #container [options]='options'>
      </app-chart-container>
    </div>
  `
})
export default class DemographicsOverviewChartComponent {
  readonly d3 = d3;  /* for console debugging */

  readonly options = {
    margin: {top: 20, right: 20, bottom: 30, left: 40},
    width: 600,
  };

  @ViewChild('container') container;

  /* Demographics Overview Data shape and functions */
  private _data: ChartInfoContainer;

  get data() {
    return this._data;
  }

  @Input() set data(incoming) {
    if (incoming) {
      this._data = new ChartInfoContainer(incoming);
      setTimeout(() => this.drawChart(), 0);
    }
  }

  /* D3 specific scaling functions */
  get x() {
    return d3.scaleBand()
      .rangeRound([0, this.container.width])
      .paddingInner(0.05)
      .align(0.1);
  }

  get y() {
    return d3.scaleLinear()
      .rangeRound([this.container.height, 0]);
  }

  get z() {
    return d3.scaleOrdinal()
      .range(['#98abc5', '#8a89a6', '#7b6888', '#6b486b', '#a05d56', '#d0743c', '#ff8c00']);
  }

  drawChart() {
    const {x, y, z} = this;
    const {chart, width, height} = this.container;

    const keys = this.data.races;
    const data = this.data.asStack();

    const stack = d3.stack()
      .keys(keys)
      .order(d3.stackOrderDescending);

    const series = stack(data);

    data.sort(function(a, b) { return b.genderAgeRange < a.genderAgeRange ? 1 : -1; });
    x.domain(data.map(function(d) { return d.genderAgeRange; }));
    y.domain([0, d3.max(data, function(d) { return d.total; })]).nice();

    const zkeys = d3
      .entries(this.data.withTotals('race'))
      .sort(entry => entry.value)
      .map(entry => entry.key);

    z.domain(zkeys);

    chart
      .append('g')
      .selectAll('g')
      .data(series)
      .enter().append('g')
        .attr('fill', function(d) { return z(d.key); })
      .selectAll('rect')
      .data(function(d) { return d; })
      .enter().append('rect')
        .attr('x', function(d) { return x(d.data.genderAgeRange); })
        .attr('y', function(d) { return y(d[1]); })
        .attr('height', function(d) { return y(d[0]) - y(d[1]); })
        .attr('width', x.bandwidth());

    chart
      .append('g')
        .attr('class', 'axis')
        .attr('transform', 'translate(0,' + height + ')')
        .call(d3.axisBottom(x));

    chart
      .append('g')
        .attr('class', 'axis')
        .call(d3.axisLeft(y).ticks(null, 's'))
      .append('text')
        .attr('x', 2)
        .attr('y', y(y.ticks().pop()) + 0.5)
        .attr('dy', '0.32em')
        .attr('fill', '#000')
        .attr('font-weight', 'bold')
        .attr('text-anchor', 'start')
        .text('Population');

    const legend = chart.append('g')
        .attr('font-family', 'sans-serif')
        .attr('font-size', 10)
        .attr('text-anchor', 'end')
      .selectAll('g')
      .data(keys.slice().reverse())
      .enter().append('g')
        .attr('transform', function(d, i) { return 'translate(0,' + i * 20 + ')'; });

    legend
      .append('rect')
        .attr('x', width - 19)
        .attr('width', 19)
        .attr('height', 19)
        .attr('fill', z);

    legend
      .append('text')
        .attr('x', width - 24)
        .attr('y', 9.5)
        .attr('dy', '0.32em')
        .text(function(d) { return d; });
  }
}
