

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
  redraw: boolean;
  notAlphabetical = [400, 500, 600, 700, 800];
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

  static analysisClone(obj:any) {
    let a = new Analysis();
    a.analysis_id = obj.analysis_id;
    a.analysis_name =  obj.analysis_name;
    a.results =  obj.results ;
    a.status = obj.status;
    a.stratum = [];
    a.stratum_name = [];
    a.chartType =  obj.chartType ;
    a.dataType =obj.dataType ;
    a.redraw = false;
    a.colors = obj.colors;
    /* Don't copy stratum */
    /*
    if (obj.stratum && obj.stratum.length)
    {
        for (const n of obj.stratum) {
            a.stratum.push(n);
        }
    }*/

    if (obj.stratum_name && obj.stratum_name.length)
    {
        for (const n2 of obj.stratum_name) {
            a.stratum_name.push(n2);
        }
    }
    return a;
  }

  constructor(obj?: any) {
    if (obj && obj.analysisId) {
      this.analysis_id = obj.analysisId;
    }
    else {
      this.analysis_id = null;
    }
    this.analysis_name = obj && obj.analysisName || null;
    this.results = obj && obj.results || [];
    this.status = null;
    this.stratum = [];
    this.stratum_name = [];
    this.chartType = obj && obj.chartType || 'pie';
    if (this.analysis_id === 3000 || this.analysis_id === 3102) { this.chartType = 'column'; }
    else { this.chartType = 'pie'; }
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

    if (obj && obj.stratum1Name) {
      this.stratum_name.push(obj.stratum1Name);
    }
    if (obj && obj.stratum2Name) {
      this.stratum_name.push(obj.stratum2Name);
    }
    if (obj && obj.stratum3Name) {
      this.stratum_name.push(obj.stratum3Name);
    }
    if (obj && obj.stratum4Name) {
      this.stratum_name.push(obj.stratum4Name);
    }
    if (obj && obj.stratum5Name) {
      this.stratum_name.push(obj.stratum5Name);
    }
  }



  hcChartOptions(): any {
    // //
    const chartOptions = {

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
            duration: 350,
          }, //this.seriesAnimation(), // turns off animation if this.seriesAnimation() is uncommented
          maxPointWidth: 45, //125,
        },
        pie: {
          // size: 260,
          dataLabels: {
            enabled: true,
            distance: -50,
            format: '{point.name} <br> Count: {point.y}'
          },
          showInLegend: true,
            colorByPoint: false
        },
        column: {
          shadow: false,
          colorByPoint: false,
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
      colorByPoint: false
    };
    return chartOptions;

  }//end of hcChartOptions ()

  hcPointFormat() {
    if (this.analysis_id == 12) {
      const pointFormat = '{series.name}<br>count: {point.y}';
      return pointFormat;
    }
    else if (this.chartType == 'column') {
      const pointFormat = 'count: {point.y}';
      return pointFormat;
    }
    else if (this.chartType == 'pie') {
      const pointFormat = '<b>{point.percentage:.1f}%</b><br>count: {point.y}';
      return pointFormat;
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

    let catArray = [];
    if (this.analysis_id == 3) {
      for (let i = 0; i < this.results.length; i++) {
        catArray.push(this.results[i].stratum_name[0]);
      }
      catArray = catArray.sort((a, b) => a - b);
      return catArray;
    }
  }

  makeTwoStratumCountCategories() {
    const catArray = [];
    // Use First stratum as category in two stratum analysis

    for (const b of this.results) {
      // If item not in array, push it
      if (catArray.indexOf(b.stratum_name[0]) == -1) {
        catArray.push(b.stratum_name[0]);
      }
    }



    return catArray;

  }

  makeTwoStratumSeries() {
    // If analysis has two stratum like race and ethnicity for example,
    // use this
    const chartSeries = [];
    let ethArray = [];
    const data = [];
    // Get distinct category list
    const raceCat = this.makeCountCategories();
    // for each category item push 3eth counts in to the data array in the series object
    // make a series object for each eth

    //make an unique array of eths

    for (let i = 0; i < this.results.length; i++) {
      ethArray.push(this.results[i].stratum_name[1]);
    }
    const unique = ethArray.filter(function(itm, i, a) {
      return i == a.indexOf(itm);
    });


    ethArray = unique;


    const chartCategories = [];

    for (let x = 0; x < ethArray.length; x++) {
      chartCategories.push({ name: ethArray[x], data: [] });
    }
    for (let b = 0; b < raceCat.length; b++) {
      for (let x = 0; x < chartCategories.length; x++) {
        for (let i = 0; i < this.results.length; i++) {

          if (chartCategories[x].name === this.results[i].stratum_name[1] && raceCat[b] === this.results[i].stratum_name[0]) {

            chartCategories[x].data.push(this.results[i].count_value);

          }
        }
      }
    }
    // //
    return chartCategories;

  }
  makeCountSeries() {

    // //
    if (this.stratum_name.length == 2) {

      return this.makeTwoStratumSeries();
    }

    const chartSeries = [{ data: [] }];
    for (let i = 0; i < this.results.length; i++) {
      let name;
      const last_stratum_index = this.results[i].stratum.length - 1;
      let color_stratum_index = this.colors.stratum_index;
      if (color_stratum_index == -1 || color_stratum_index > last_stratum_index) {
        color_stratum_index = last_stratum_index;
      }
      const color_stratum_value = this.results[i].stratum[color_stratum_index];
      name = this.results[i].stratum_name[last_stratum_index];
      const color = this.colors.stratum_colors[color_stratum_value];
      if (name == 0) {
        name = 'Other';
      }
      else if (name == null) {
        name = 'Other';
      }
      /* No work for column
      let thisSeries = {color: color, data: { name: name, y: this.results[i].count_value } };
      chartSeries.push(thisSeries);
      */
      // chartSeries[0].color = color;

      chartSeries[0].data.push({ name: name, y: this.results[i].count_value, color:'red'});

    }

    ////
    //defalut sort
    if (this.analysis_id == 2) {
      chartSeries[0].data.sort(function(a, b): number {
        const nameA = a.name.toLowerCase(), nameB = b.name.toLowerCase();
        if (nameA < nameB) //sort string ascending
          return -1;
        if (nameA > nameB)
          return 1;
        return 0; //default return value (no sorting)
      });
      // return chartSeries;
    } else {

      // TODO: make numaric sort desending
      chartSeries[0].data = chartSeries[0].data.sort((a, b) => a.name - b.name);
    }
    return chartSeries;




  }

  nameLength(name) {
    if (name.length > 35) {
      return name.substring(0, 35) + '...';
    } else {
      return name;
    }
  }

  colorByPoint() {
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
      return true;
    } else {
      return false;
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
          'fontSize': '15px',
          'textOutline': '1.75px black',
          'color': 'white'
        }
      };
    } else {
      return {
        enabled: true,
        rotation: 0,
        align: 'center',
        y: 30, // y: value offsets dataLabels in pixels.

        style: {
          'fontWeight': 'thinner',
          'fontSize': '15px',
          'textOutline': '1.75px black',
          'color': 'white',
        }
      };
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

  static clone(obj:any) {
    let r = new AnalysisResult();
    r.analysis_id = obj && obj.analysis_id || null;
    r.count_value = obj && obj.count_value || 0;
    r.stratum = obj.stratum;
    r.stratum_name = obj.stratum_name;
    return r;

  }
  constructor(obj?: any) {
    this.analysis_id = obj && obj.analysisId || null;
    this.count_value = obj && obj.countValue || 0;
    this.stratum = [];
    this.stratum_name = [];

    if (obj && obj.stratum1) {
      this.stratum.push(obj.stratum1);
      if (obj.stratum1Name) {
        this.stratum_name.push(obj.stratum1Name);
      }
      else {
        this.stratum_name.push(obj.stratum1);
      }
    }
    if (obj && obj.stratum2) {
      this.stratum.push(obj.stratum2);
      this.stratum_name.push(obj.stratum2Name);
    }
    if (obj && obj.stratum3) {
      this.stratum.push(obj.stratum3);
      this.stratum_name.push(obj.stratum3Name);
    }
    if (obj && obj.stratum_4) {
      this.stratum.push(obj.stratum4);
      this.stratum_name.push(obj.stratum4Name);
    }
    if (obj && obj.stratum5) {
      this.stratum.push(obj.stratum5);
      this.stratum_name.push(obj.stratum5Name);
    }

  }

}


export class AnalysisSection {
  section_id: number;
  name: string; // No spaces , used in path
  title: string;
  analyses: number[]; // array of analyses to run for this section concepts
}
