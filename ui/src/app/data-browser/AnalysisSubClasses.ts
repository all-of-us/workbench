import { Analysis, AnalysisResult} from './AnalysisClasses';

export class AnalysisDistResult extends AnalysisResult {
  min_value: number;
  max_value: number;
  avg_value: number;
  stdev_value: number;
  median_value: number;
  p10_value: number;
  p25_value: number;
  p75_value: number;
  p90_value: number;
  colors: any;

  constructor(obj?: any) {
    super(obj);

    this.min_value = obj && obj.min_value || null;
    this.max_value = obj && obj.max_value || null;
    this.avg_value = obj && obj.avg_value || null;
    this.stdev_value = obj && obj.stdev_value || null;
    this.median_value = obj && obj.median_value || null;
    this.p10_value = obj && obj.p10_value || null;
    this.p25_value = obj && obj.p25_value || null;
    this.p75_value = obj && obj.p75_value || null;
    this.p90_value = obj && obj.p90_value || null;


  }
}

export class AnalysisDist extends Analysis {
  results: AnalysisDistResult[]; // = new AnalysisResult ;
  constructor(obj?: any) {
    super(obj);
    this.chartType = 'boxplot';
    this.dataType = 'distribution';
    this.results = [];


  }

  hcChartOptions(): any {

    const chartOptions = {

      chart: {

        type: this.chartType,

      },
      tooltip: {



        shared: false
      },
      credits: {
        enabled: false
      },

      title: {
        text: this.analysis_name
      },
      subtitle: {

      },
      plotOptions: {
        boxplot: {
          colorByPoint: false,

          dataLabels: {
            enabled: false
          },
          showInLegend: false
        },
        series: {
          animation: false
        }
      },
      yAxis: {

      },
      xAxis: {
        categories: this.hcCategories()

      },
      zAxis: {

      },
      legend: {
        enabled: false
      },
      series: this.hcSeries()
    };
    return chartOptions;

  }// end of hcChartOptions()

  hcSeries() {
    if (this.dataType === 'distribution') {

      return this.makeDistSeries();
    }
  }

  hcCategories() {
    if (this.dataType === 'distribution') {
      return this.makeCategories();
    }
  }


  makeCategories() {
    const name = [];
    for (let i = 0; i < this.results.length; i++) {
      name.push(this.results[i].stratum_name[0] + ' [' + this.results[i].stratum_name[1] + '] ');
    }
    return name;

  }


  makeDistSeries() {
    const chartSeries = [{ data: [] }];
    for (let i = 0; i < this.results.length; i++) {
      let name;
      const last_stratum_index = this.results[i].stratum.length - 1;
      let color_stratum_index = this.colors.stratum_index;
      if (color_stratum_index === -1 || color_stratum_index > last_stratum_index) {
        color_stratum_index = last_stratum_index;
      }
      const color_stratum_value = this.results[i].stratum[color_stratum_index];
      name = this.results[i].stratum_name[last_stratum_index];
      const color = this.colors.stratum_colors[color_stratum_value];
      if (!name) {
        name = 'Other';
      } else if (name == null) {
        name = 'Other';
      }


      chartSeries[0].data.push({ low: this.results[i].min_value,
          high: this.results[i].max_value,
          median: this.results[i].median_value,
          q1: this.results[i].p10_value,
          q3: this.results[i].p90_value,
          color: color });
    }
    return chartSeries;
  }

}
