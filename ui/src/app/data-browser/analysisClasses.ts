

export interface IAnalysis {
  analysis_id: number;
  analysis_name: string;
  stratum_name: string[];
  stratum: string[];
  results: IAnalysisResult[]; // = new AnalysisResult ;
  status: string;
  chartType: string;
  dataType: string;
  redraw: boolean;
  hcChartOptions(): any;

}

export class Analysis implements IAnalysis {
  analysis_id: number;
  analysis_name: string;
  stratum_name: string[];
  stratum: string[];
  results: IAnalysisResult[];
  chartType: string;
  dataType: string;
  status: string;
  decadeBin: string[];
  redraw: boolean;
  notAlphabetical = [400, 500, 600, 700, 800]
  // Ojbect that defines the colors for the analysis results graphs
  /* Example for Number of person by Gender
  {
    stratum_index: 0,
    stratum_colors: {
      8507: '#hexcolor',
      8532: '#hexcolor',
      0: '#othercolor'},
    }
  }
  */
  colors: any;


  //raceCat: Array<string>;

  constructor(obj?: any) {
    if (obj && obj.analysis_id) {
      this.analysis_id = obj.analysis_id;
    }
    else {
      this.analysis_id = null
    }
    this.analysis_name = obj && obj.analysis_name || null;
    this.results = obj && obj.results || [];
    this.status = null;
    this.stratum = [];
    this.stratum_name = [];
    this.chartType = obj && obj.chartType || 'pie';
    this.dataType = obj && obj.dataType || 'counts';
    this.redraw = false;
    this.colors = {
      stratum_index: -1, // Use last stratum index as the color index
      stratum_colors: {
        8507: '#4f67b9',
        8532: '#fe8f8f',
        0: '#666'
      },
    };

    if (obj && obj.stratum_1_name) {
      this.stratum_name.push(obj.stratum_1_name);
    }
    if (obj && obj.stratum_2_name) {
      this.stratum_name.push(obj.stratum_2_name);
    }
    if (obj && obj.stratum_3_name) {
      this.stratum_name.push(obj.stratum_3_name);
    }
    if (obj && obj.stratum_4_name) {
      this.stratum_name.push(obj.stratum_4_name);
    }
    if (obj && obj.stratum_5_name) {
      this.stratum_name.push(obj.stratum_5_name);
    }
  }


  hcChartOptions(): any {
    // //
    let chartOptions = {

      chart: {

        type: this.chartType,

      },
      credits: {
        enabled: false
      },
      tooltip: {
        pointFormat: this.hcPointFormat()
      },

      title: {
        text: this.analysis_name
      },
      subtitle: {


      },
      plotOptions: {
        series: {
          animation: {
            duration:350,
          },//this.seriesAnimation(), // turns off animation if this.seriesAnimation() is uncommented
          maxPointWidth: 45, //125,
        },
        pie: {
          // size: 260,
          dataLabels: {
            enabled: true,
            distance: -50,
            format: '{point.name} <br> Count: {point.y}'
          },
          showInLegend: true
        },
        column: {
          shadow: false,
          colorByPoint: this.colorByPoint(),
          // pointPadding: -1,
          groupPadding: 0,
          dataLabels: this.rotation()
        }
      },
      yAxis: {

      },
      xAxis: {
        categories: this.makeCountCategories(),
        type: 'category',
        labels: {
          style: {
            whiteSpace: 'nowrap',
          }
        }

      },
      zAxis: {

      },
      legend: {
        enabled: this.seriesLegend()
      },
      series: this.hcSeries(),
      colorByPoint: true
    }
    return chartOptions;

  }//end of hcChartOptions ()

  hcPointFormat() {
    if (this.analysis_id == 12) {
      let pointFormat = '{series.name}<br>count: {point.y}';
      return pointFormat
    }
    else if (this.chartType == "column") {
      let pointFormat = 'count: {point.y}';
      return pointFormat
    }
    else if (this.chartType == "pie") {
      let pointFormat = '<b>{point.percentage:.1f}%</b><br>count: {point.y}'
      return pointFormat
    }

  }

  hcSeries() {
    if (this.dataType === 'counts') {
      return this.makeCountSeries();
    }
  }

  makeCountCategories() {
    // For two stratum , get the unique cats
    // //
    if (this.stratum_name.length == 2) {
      return this.makeTwoStratumCountCategories();
    }

    let catArray = []
    if (this.analysis_id == 3) {
      for (let i = 0; i < this.results.length; i++) {
        catArray.push(this.results[i].stratum_name[0])
      }
      catArray = catArray.sort((a, b) => a - b);
      return catArray
    }
  }

  makeTwoStratumCountCategories() {
    let catArray = []
    // Use First stratum as category in two stratum analysis

    for (let b of this.results) {
      // If item not in array, push it
      if (catArray.indexOf(b.stratum_name[0]) == -1) {
        catArray.push(b.stratum_name[0]);
      }
    }



    return catArray

  }

  makeTwoStratumSeries() {
    // If analysis has two stratum like race and ethnicity for example,
    // use this
    let chartSeries = [];
    let ethArray = []
    let data = []
    // Get distinct category list
    let raceCat = this.makeCountCategories();
    // for each category item push 3eth counts in to the data array in the series object
    // make a series object for each eth

    //make an unique array of eths

    for (let i = 0; i < this.results.length; i++) {
      ethArray.push(this.results[i].stratum_name[1]);
    }
    var unique = ethArray.filter(function(itm, i, a) {
      return i == a.indexOf(itm);
    });


    ethArray = unique;


    var chartCategories = []

    for (let x = 0; x < ethArray.length; x++) {
      chartCategories.push({ name: ethArray[x], data: [] })
    }
    for (let b = 0; b < raceCat.length; b++) {
      for (let x = 0; x < chartCategories.length; x++) {
        for (let i = 0; i < this.results.length; i++) {

          if (chartCategories[x].name === this.results[i].stratum_name[1] && raceCat[b] === this.results[i].stratum_name[0]) {

            chartCategories[x].data.push(this.results[i].count_value)

          }
        }
      }
    }
    // //
    return chartCategories

  }
  makeCountSeries() {

    // //
    if (this.stratum_name.length == 2) {

      return this.makeTwoStratumSeries();
    }

    let chartSeries = [{ data: [] }];
    for (let i = 0; i < this.results.length; i++) {
      var name;
      var last_stratum_index = this.results[i].stratum.length - 1;
      let color_stratum_index = this.colors.stratum_index;
      if (color_stratum_index == -1 || color_stratum_index > last_stratum_index) {
        color_stratum_index = last_stratum_index;
      }
      var color_stratum_value = this.results[i].stratum[color_stratum_index];
      name = this.results[i].stratum_name[last_stratum_index];
      let color = this.colors.stratum_colors[color_stratum_value];
      if (name == 0) {
        name = 'Other'
      }
      else if (name == null) {
        name = 'Other'
      }
      /* No work for column
      let thisSeries = {color: color, data: { name: name, y: this.results[i].count_value } };
      chartSeries.push(thisSeries);
      */
      // chartSeries[0].color = color;

      chartSeries[0].data.push({ name: name, y: this.results[i].count_value, color: color })

    }

    ////
    //defalut sort
    if (this.analysis_id == 2) {
      chartSeries[0].data.sort(function(a, b): number {
        var nameA = a.name.toLowerCase(), nameB = b.name.toLowerCase()
        if (nameA < nameB) //sort string ascending
          return -1
        if (nameA > nameB)
          return 1
        return 0 //default return value (no sorting)
      })
      // return chartSeries;
    } else {

      // TODO: make numaric sort desending
      chartSeries[0].data = chartSeries[0].data.sort((a, b) => a.name - b.name);
    }
    return chartSeries;




  }

  nameLength(name) {
    if (name.length > 35) {
      return name.substring(0, 35) + "..."
    } else {
      return name
    }
  }

  colorByPoint() {
    // this.analysis_id ==12 is a grouping of columns... which is why we don't need it to have colorbypoint enabled.
    if (this.analysis_id == 12) {
      return false;
    } else {
      return true;
    }
  }
  seriesLegend() {
    // this.analysis_id ==4 is a grouping of columns... which is why we need series legend
    if (this.analysis_id == 4) {
      return true;
    } else {
      return false;
    }
  }

  seriesAnimation() {
    if ([1, 2, 3, 4, 12].indexOf(this.analysis_id) !== -1) {
      return true
    } else {
      return false
    }
  }
  rotation() {
    ////
    if (this.results.length > 0) {
      return {
        enabled: true,
        rotation: -90,
        align: 'right',
        y: 10, // y: value offsets dataLabels in pixels.

        style: {
          'fontWeight': 'thinner',
          "fontSize": "15px",
          "textOutline": "1.75px black",
          "color": "white"
        }
      }
    } else {
      return {
        enabled: true,
        rotation: 0,
        align: 'center',
        y: 30, // y: value offsets dataLabels in pixels.

        style: {
          'fontWeight': 'thinner',
          "fontSize": "15px",
          "textOutline": "1.75px black",
          "color": "white",
        }
      }
    }
  }



}

export interface IAnalysisResult {
  analysis_id: number;
  stratum_name: string[];
  stratum: string[];
  count_value: number;

}
export class AnalysisResult {
  analysis_id: number;
  stratum_name: string[];
  stratum: string[];
  count_value: number;

  constructor(obj?: any) {
    this.analysis_id = obj && obj.analysis_id || null;
    this.count_value = obj && obj.count_value || 0;
    this.stratum = [];
    this.stratum_name = [];
    if (obj && obj.stratum_1) {
      this.stratum.push(obj.stratum_1);
      if (obj.stratum_1_name) {
        this.stratum_name.push(obj.stratum_1_name);
      }
      else {
        this.stratum_name.push(obj.stratum_1);
      }
    }
    if (obj && obj.stratum_2) {
      this.stratum.push(obj.stratum_2);
      this.stratum_name.push(obj.stratum_2_name);
    }
    if (obj && obj.stratum_3) {
      this.stratum.push(obj.stratum_3);
      this.stratum_name.push(obj.stratum_3_name);
    }
    if (obj && obj.stratum_4) {
      this.stratum.push(obj.stratum_4);
      this.stratum_name.push(obj.stratum_4_name);
    }
    if (obj && obj.stratum_5) {
      this.stratum.push(obj.stratum_5);
      this.stratum_name.push(obj.stratum_5_name);
    }

  }

}

export class AnalysisSection {
  section_id: number;
  name: string; // No spaces , used in path
  title: string;
  analyses: number[]; // array of analyses to run for this section concepts
}
