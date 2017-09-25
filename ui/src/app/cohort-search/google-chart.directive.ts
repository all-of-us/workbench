import {Directive, ElementRef, Input, OnInit} from '@angular/core';

declare var google: any;
declare var googleLoaded: any;

@Directive({
  selector: '[appGoogleChart]'
})
export class GoogleChartDirective implements OnInit {

  public _element: any;
  @Input('chartType') public chartType: string;
  @Input('chartOptions') public chartOptions: Object;
  @Input('chartData') public chartData: Object;

  constructor(public element: ElementRef) {
    this._element = this.element.nativeElement;
  }

  ngOnInit() {
    google.charts.load('current', {'packages': ['corechart']});
    this.drawGraph(this.chartOptions, this.chartType, this.chartData, this._element);
  }

  drawGraph (chartOptions, chartType, chartData, ele) {

    google.charts.setOnLoadCallback(drawChart);

    function drawChart() {
      let wrapper;
      wrapper = new google.visualization.ChartWrapper({
        chartType: chartType,
        dataTable: chartData ,
        options: chartOptions || {},
        containerId: ele.id
      });

      wrapper.draw();
    }
  }
}
