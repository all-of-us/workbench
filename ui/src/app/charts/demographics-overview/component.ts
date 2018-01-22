import {Component, Input, ViewChild} from '@angular/core';
import * as d3 from 'd3';

interface Datum {
  race: string;
  ageRange: string;
  gender: string;
  count: number;
}

function decode(value) {
  return {
    'M': 'Male',
    'F': 'Female',
  }[value];
}

@Component({
  selector: 'app-demographics-overview-chart',
  template: `
    <div *ngIf="data">
      <app-chart-container #container [options]="options">
      </app-chart-container>
    </div>
  `
})
export default class DemographicsOverviewChartComponent {
  readonly options = {
    margin: {top: 20, right: 20, bottom: 30, left: 40},
    width: 600,
  };

  @ViewChild('container') container;

  /* Demographics Overview Data shape and functions */
  @Input() set data(incoming: Datum[]) {
    if (incoming) {
      this._data = incoming;
      setTimeout(() => this.drawChart(), 0);
    }
  }
  private _data: Datum[] = [];
  get data()      { return this._data; }
  get races()     { return this._dataProperty('race'); }
  get genders()   { return this._dataProperty('gender'); }
  get ageRanges() { return this._dataProperty('ageRange'); }

  private _dataProperty(prop) {
    return Array.from(new Set(this.data.map(d => d[prop])));
  }

  /**
   * .stacked() - this function will take .data and roll it up into the stack
   * format expected by D3's stack charting tools
   */
  stacked() {
    /* Cross product genders x ageRanges */
    const ageRangesByGender = [];
    this.genders.forEach(gender => {
      this.ageRanges.forEach(range => {
        ageRangesByGender.push([gender, range]);
      });
    });

    /* Generate an empty set of counts */
    const initCounts = () =>
      this.races.reduce((counter, race) => {
        counter[race] = 0; return counter;
      }, {});

    /* Roll up the data by gender and ageRange */
    return ageRangesByGender.map(([gender, ageRange]) => {
      const counts = initCounts();
      let total = 0;
      this.data
        .filter(datum => datum.ageRange === ageRange)
        .filter(datum => datum.gender === gender)
        .forEach(datum => {
          total += datum.count;
          counts[datum.race] += datum.count;
        });
      const key = `${decode(gender)} ${ageRange}`;
      return {key, total, ...counts};
    });
  }

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
      .range(["#98abc5", "#8a89a6", "#7b6888", "#6b486b", "#a05d56", "#d0743c", "#ff8c00"]);
  }

  drawChart() {
    const {x, y, z} = this;
    const {chart, width, height} = this.container;
    const data = this.stacked();
    const keys = this.races;

    data.sort(function(a, b) { return b.total - a.total; });
    x.domain(data.map(function(d) { return d.key; }));
    y.domain([0, d3.max(data, function(d) { return d.total; })]).nice();
    z.domain(keys);

    chart
      .append("g")
      .selectAll("g")
      .data(d3.stack().keys(keys)(data))
      .enter().append("g")
        .attr("fill", function(d) { return z(d.key); })
      .selectAll("rect")
      .data(function(d) { return d; })
      .enter().append("rect")
        .attr("x", function(d) { return x(d.data.key); })
        .attr("y", function(d) { return y(d[1]); })
        .attr("height", function(d) { return y(d[0]) - y(d[1]); })
        .attr("width", x.bandwidth());

    chart
      .append("g")
        .attr("class", "axis")
        .attr("transform", "translate(0," + height + ")")
        .call(d3.axisBottom(x));

    chart
      .append("g")
        .attr("class", "axis")
        .call(d3.axisLeft(y).ticks(null, "s"))
      .append("text")
        .attr("x", 2)
        .attr("y", y(y.ticks().pop()) + 0.5)
        .attr("dy", "0.32em")
        .attr("fill", "#000")
        .attr("font-weight", "bold")
        .attr("text-anchor", "start")
        .text("Population");

    const legend = chart.append("g")
        .attr("font-family", "sans-serif")
        .attr("font-size", 10)
        .attr("text-anchor", "end")
      .selectAll("g")
      .data(keys.slice().reverse())
      .enter().append("g")
        .attr("transform", function(d, i) { return "translate(0," + i * 20 + ")"; });

    legend
      .append("rect")
        .attr("x", width - 19)
        .attr("width", 19)
        .attr("height", 19)
        .attr("fill", z);

    legend
      .append("text")
        .attr("x", width - 24)
        .attr("y", 9.5)
        .attr("dy", "0.32em")
        .text(function(d) { return d; });
  }
}
