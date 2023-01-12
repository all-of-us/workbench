import ustopo from '@highcharts/map-collection/countries/us/us-all.topo.json';

export function getCannedCategoryCounts() {
  // http://jsfiddle.net/Moonbird_IT/30Luzmya/2/
  return {
    chart: {
      type: 'column',
    },
    title: {
      text: 'Count/Frequency One category',
    },
    legend: {
      layout: 'horizontal',
      align: 'right',
      verticalAlign: 'top',
    },
    xAxis: {
      type: 'category',
    },
    plotOptions: {
      series: {
        grouping: false,
        events: {
          legendItemClick: function () {
            console.log('this object name: ' + this.constructor.name);
            console.log(this);
            const seriesIndex = this.index;
            if (this.visible && this.chart.restIsHidden) {
              for (let i = 0; i < this.chart.series.length; i++) {
                if (this.chart.series[i].index !== seriesIndex) {
                  this.chart.series[i].show();
                }
              }
              this.chart.restIsHidden = false;
            } else {
              for (let i = 0; i < this.chart.series.length; i++) {
                if (this.chart.series[i].index !== seriesIndex) {
                  this.chart.series[i].hide();
                }
              }
              this.show();
              this.chart.restIsHidden = true;
            }
            return false;
          },
        },
      },
    },
    series: [
      {
        name: 'Hispanic or Latino',
        color: 'red',
        data: [{ name: 'Hispanic or Latino', y: 24916, x: 0 }],
      },
      {
        name: 'Not Hispanic or Latino',
        color: 'blue',
        data: [{ name: 'Not Hispanic or Latino', y: 11816, x: 1 }],
      },
      {
        name: 'Prefer Not To Answer',
        color: 'orange',
        data: [{ name: 'Prefer Not To Answer', y: 34400, x: 2 }],
      },
      {
        name: 'Race Ethnicity None Of These',
        color: 'green',
        data: [{ name: 'Race Ethnicity None Of These', y: 12908, x: 3 }],
      },
      {
        name: 'Skip',
        color: 'red',
        data: [{ name: 'Skip', y: 5000, x: 4 }],
      },
    ],
  };
}

export function getCannedCategoryCountsByAgeBin() {
  return {
    chart: {
      type: 'column',
      inverted: false,
    },
    title: {
      text: 'Counts/Frequency distribution<br/><b>One category (Gender)</b> by <b>Age Ranges</b>',
    },
    xAxis: [
      {
        title: {
          text: 'Age Range (y)',
        },
        categories: [
          '20-29',
          '30-39',
          '40-49',
          '50-59',
          '60-69',
          '70-79',
          '80-89',
          '90-99',
        ],
        opposite: false,
        reversed: false,
      },
    ],
    yAxis: [
      {
        title: {
          text: 'Percent(%)',
        },
      },
    ],
    legend: {
      layout: 'horizontal',
      verticalAlign: 'top',
      itemMarginBottom: 5,
      useHTML: true,
      labelFormatter: function () {
        return this.name.slice(0, 15) + (this.name.length > 15 ? '...' : '');
      },
    },
    plotOptions: {
      series: {
        stacking: 'normal',
        events: {
          legendItemClick: function () {
            console.log('this object name: ' + this.constructor.name);
            console.log(this);
            const seriesIndex = this.index;
            if (this.visible && this.chart.restIsHidden) {
              for (let i = 0; i < this.chart.series.length; i++) {
                if (this.chart.series[i].index !== seriesIndex) {
                  this.chart.series[i].show();
                }
              }
              this.chart.restIsHidden = false;
            } else {
              for (let i = 0; i < this.chart.series.length; i++) {
                if (this.chart.series[i].index !== seriesIndex) {
                  this.chart.series[i].hide();
                }
              }
              this.show();
              this.chart.restIsHidden = true;
            }
            return false;
          },
        },
      },
    },
    tooltip: {
      formatter: function () {
        let tip = this.point.ageBin + '(y), ';
        tip += this.point.categoryName + ', <br/>';
        tip += 'counts: (' + this.point.categoryCount;
        tip += '/' + this.point.categoryTotal + ')<br/>';
        return tip + 'percent:' + parseFloat(this.point.y).toFixed(2) + '%';
      },
    },
    series: [
      {
        name: 'Female',
        color: '#AABBFF',
        data: [
          {
            ageBin: '20-29',
            category: 'gender',
            categoryName: 'Female',
            categoryCount: 12252,
            categorySortKey: 'F',
            categoryTotal: 142306,
            x: 0,
            y: 8.609615898135004,
          },
          {
            ageBin: '30-39',
            category: 'gender',
            categoryName: 'Female',
            categoryCount: 22506,
            categorySortKey: 'F',
            categoryTotal: 142306,
            x: 1,
            y: 15.81521509985524,
          },
          {
            ageBin: '40-49',
            category: 'gender',
            categoryName: 'Female',
            categoryCount: 20808,
            categorySortKey: 'F',
            categoryTotal: 142306,
            x: 2,
            y: 14.622011721220469,
          },
          {
            ageBin: '50-59',
            category: 'gender',
            categoryName: 'Female',
            categoryCount: 26570,
            categorySortKey: 'F',
            categoryTotal: 142306,
            x: 3,
            y: 18.67103284471491,
          },
          {
            ageBin: '60-69',
            category: 'gender',
            categoryName: 'Female',
            categoryCount: 30509,
            categorySortKey: 'F',
            categoryTotal: 142306,
            x: 4,
            y: 21.439011707166248,
          },
          {
            ageBin: '70-79',
            category: 'gender',
            categoryName: 'Female',
            categoryCount: 22131,
            categorySortKey: 'F',
            categoryTotal: 142306,
            x: 5,
            y: 15.551698452630248,
          },
          {
            ageBin: '80-89',
            category: 'gender',
            categoryName: 'Female',
            categoryCount: 7113,
            categorySortKey: 'F',
            categoryTotal: 142306,
            x: 6,
            y: 4.998383764563687,
          },
          {
            ageBin: '90-99',
            category: 'gender',
            categoryName: 'Female',
            categoryCount: 417,
            categorySortKey: 'F',
            categoryTotal: 142306,
            x: 7,
            y: 0.29303051171419336,
          },
        ],
      },
      {
        name: 'Male',
        color: '#9999FF',
        data: [
          {
            ageBin: '20-29',
            category: 'gender',
            categoryName: 'Male',
            categoryCount: 7734,
            categorySortKey: 'M',
            categoryTotal: 87098,
            x: 0,
            y: 8.87965280488645,
          },
          {
            ageBin: '30-39',
            category: 'gender',
            categoryName: 'Male',
            categoryCount: 13679,
            categorySortKey: 'M',
            categoryTotal: 87098,
            x: 1,
            y: 15.705297480998416,
          },
          {
            ageBin: '40-49',
            category: 'gender',
            categoryName: 'Male',
            categoryCount: 12928,
            categorySortKey: 'M',
            categoryTotal: 87098,
            x: 2,
            y: 14.843050357069048,
          },
          {
            ageBin: '50-59',
            category: 'gender',
            categoryName: 'Male',
            categoryCount: 16242,
            categorySortKey: 'M',
            categoryTotal: 87098,
            x: 3,
            y: 18.647959769455095,
          },
          {
            ageBin: '60-69',
            category: 'gender',
            categoryName: 'Male',
            categoryCount: 18491,
            categorySortKey: 'M',
            categoryTotal: 87098,
            x: 4,
            y: 21.230108613286184,
          },
          {
            ageBin: '70-79',
            category: 'gender',
            categoryName: 'Male',
            categoryCount: 13401,
            categorySortKey: 'M',
            categoryTotal: 87098,
            x: 5,
            y: 15.386116787985946,
          },
          {
            ageBin: '80-89',
            category: 'gender',
            categoryName: 'Male',
            categoryCount: 4344,
            categorySortKey: 'M',
            categoryTotal: 87098,
            x: 6,
            y: 4.987485361317137,
          },
          {
            ageBin: '90-99',
            category: 'gender',
            categoryName: 'Male',
            categoryCount: 279,
            categorySortKey: 'M',
            categoryTotal: 87098,
            x: 7,
            y: 0.3203288250017222,
          },
        ],
      },
      {
        name: 'Not man only, not woman only, prefer not to answer, or skipped',
        color: '#77BBEE',
        data: [
          {
            ageBin: '20-29',
            category: 'gender',
            categoryName:
              'Not man only, not woman only, prefer not to answer, or skipped',
            categoryCount: 425,
            categorySortKey: 'O',
            categoryTotal: 5119,
            x: 0,
            y: 8.302402813049424,
          },
          {
            ageBin: '30-39',
            category: 'gender',
            categoryName:
              'Not man only, not woman only, prefer not to answer, or skipped',
            categoryCount: 778,
            categorySortKey: 'O',
            categoryTotal: 5119,
            x: 1,
            y: 15.198280914241062,
          },
          {
            ageBin: '40-49',
            category: 'gender',
            categoryName:
              'Not man only, not woman only, prefer not to answer, or skipped',
            categoryCount: 803,
            categorySortKey: 'O',
            categoryTotal: 5119,
            x: 2,
            y: 15.686657550302794,
          },
          {
            ageBin: '50-59',
            category: 'gender',
            categoryName:
              'Not man only, not woman only, prefer not to answer, or skipped',
            categoryCount: 939,
            categorySortKey: 'O',
            categoryTotal: 5119,
            x: 3,
            y: 18.34342645047861,
          },
          {
            ageBin: '60-69',
            category: 'gender',
            categoryName:
              'Not man only, not woman only, prefer not to answer, or skipped',
            categoryCount: 1139,
            categorySortKey: 'O',
            categoryTotal: 5119,
            x: 4,
            y: 22.250439538972454,
          },
          {
            ageBin: '70-79',
            category: 'gender',
            categoryName:
              'Not man only, not woman only, prefer not to answer, or skipped',
            categoryCount: 788,
            categorySortKey: 'O',
            categoryTotal: 5119,
            x: 5,
            y: 15.393631568665755,
          },
          {
            ageBin: '80-89',
            category: 'gender',
            categoryName:
              'Not man only, not woman only, prefer not to answer, or skipped',
            categoryCount: 235,
            categorySortKey: 'O',
            categoryTotal: 5119,
            x: 6,
            y: 4.59074037898027,
          },
          {
            ageBin: '90-99',
            category: 'gender',
            categoryName:
              'Not man only, not woman only, prefer not to answer, or skipped',
            categoryCount: 12,
            categorySortKey: 'O',
            categoryTotal: 5119,
            x: 7,
            y: 0.23442078530963079,
          },
        ],
      },
    ],
  };
}

export function getCannedTopology() {
  // The data is joined to map using value of 'hc-key' property by default.
  // See API docs for 'joinBy' for more info on linking data and map.
  const data = [
    ['us-ma', 10],
    ['us-wa', 11],
    ['us-ca', 12],
    ['us-or', 13],
    ['us-wi', 14],
    ['us-me', 15],
    ['us-mi', 16],
    ['us-nv', 17],
    ['us-nm', 18],
    ['us-co', 19],
    ['us-wy', 20],
    ['us-ks', 21],
    ['us-ne', 22],
    ['us-ok', 23],
    ['us-mo', 24],
    ['us-il', 25],
    ['us-in', 26],
    ['us-vt', 27],
    ['us-ar', 28],
    ['us-tx', 29],
    ['us-ri', 30],
    ['us-al', 31],
    ['us-ms', 32],
    ['us-nc', 33],
    ['us-va', 34],
    ['us-ia', 35],
    ['us-md', 36],
    ['us-de', 37],
    ['us-pa', 38],
    ['us-nj', 39],
    ['us-ny', 40],
    ['us-id', 41],
    ['us-sd', 42],
    ['us-ct', 43],
    ['us-nh', 44],
    ['us-ky', 45],
    ['us-oh', 46],
    ['us-tn', 47],
    ['us-wv', 48],
    ['us-dc', 49],
    ['us-la', 50],
    ['us-fl', 51],
    ['us-ga', 52],
    ['us-sc', 53],
    ['us-mn', 54],
    ['us-mt', 55],
    ['us-nd', 56],
    ['us-az', 57],
    ['us-ut', 58],
    ['us-hi', 59],
    ['us-ak', 60],
    ['gu-3605', 61],
    ['mp-ti', 62],
    ['mp-sa', 63],
    ['mp-ro', 64],
    ['as-6515', 65],
    ['as-6514', 66],
    ['pr-3614', 67],
    ['vi-3617', 68],
    ['vi-6398', 69],
    ['vi-6399', 70],
  ];

  // Create the chart
  return {
    chart: {
      map: ustopo,
    },
    title: {
      text: 'Highcharts Maps basic demo',
    },

    subtitle: {
      text:
        'Source map: <a href="http://code.highcharts.com/mapdata/countries/us/custom/us-all-territories.topo.json">' +
        'United States of America with Territories</a>',
    },

    mapNavigation: {
      enabled: true,
      buttonOptions: {
        verticalAlign: 'bottom',
      },
    },

    colorAxis: {
      min: 0,
    },

    series: [
      {
        data: data,
        joinBy: ['hc-key'],
        name: 'Random data',
        states: {
          hover: {
            color: '#BADA55',
          },
        },
        dataLabels: {
          enabled: true,
          format: '{point.properties.hc-a2}',
        },
      },
    ],
  };
}

export function getCannedTreemap() {
  // https://codepen.io/pen
  const color = ['#75E492', '#e987cf', '#75C6E4', '#9375E4', '#e49375'];
  return {
    accessibility: {
      screenReaderSection: {
        beforeChartFormat:
          '<{headingTagName}>{chartTitle}</{headingTagName}><div>{typeDescription}</div><div>{chartSubtitle}</div><div>{chartLongdesc}</div>',
      },
      description: 'The demo below visualizes...',
    },
    series: [
      {
        name: 'Condition and Drugs',
        type: 'treemap',
        allowDrillToNode: true,
        dataLabels: {
          enabled: false,
        },
        levelIsConstant: false,
        levels: [
          {
            level: 1,
            dataLabels: {
              enabled: true,
              style: {
                textOutline: false,
              },
            },
            borderWidth: 3,
          },
        ],
        drilldown: {
          series: [
            {
              id: 'Condition and Drugs',
            },
          ],
        },
        data: [
          {
            id: 'Essential hypertension',
            name: 'Essential hypertension',
            value: 378148,
            color: color[0],
          },
          {
            id: '1-1',
            name: '2 ML ondansetron 2 MG/ML Injection',
            value: 177678,
            parent: 'Essential hypertension',
          },
          {
            id: '1-2',
            name: 'calcium chloride 0.0014 MEQ/ML / potassium chloride 0.004 MEQ/ML / sodium chloride 0.103 MEQ/ML / sodium lactate 0.028 MEQ/ML Injectable Solution"',
            value: 7871,
            parent: 'Essential hypertension',
          },
          {
            id: '1-3',
            name: '1000 ML sodium chloride 9 MG/ML Injection',
            value: 108016,
            parent: 'Essential hypertension',
          },
          {
            id: '1-4',
            name: '2 ML fentanyl 0.05 MG/ML Injection',
            value: 20031,
            parent: 'Essential hypertension',
          },
          {
            id: '1-5',
            name: 'tetanus toxoid, reduced diphtheria toxoid, and acellular pertussis vaccine, adsorbed',
            value: 64552,
            parent: 'Essential hypertension',
          },
          {
            id: 'Cough',
            name: 'Cough',
            value: 131918,
            color: color[1],
          },
          {
            id: '2-1',
            name: 'sodium chloride 9 MG/ML Injectable Solution',
            value: 15466,
            parent: 'Cough',
          },
          {
            id: '2-2',
            name: '2 ML ondansetron 2 MG/ML Injection',
            value: 50469,
            parent: 'Cough',
          },
          {
            id: '2-3',
            name: 'calcium chloride 0.0014 MEQ/ML / potassium chloride 0.004 MEQ/ML / sodium chloride 0.103 MEQ/ML / sodium lactate 0.028 MEQ/ML Injectable Solution',
            value: 65983,
            parent: 'Cough',
          },
          {
            id: 'Headache',
            name: 'Headache',
            value: 36156,
            color: color[2],
          },
          {
            id: '3-1',
            name: 'acetaminophen 325 MG Oral Tablet',
            value: 10897,
            parent: 'Headache',
          },
          {
            id: '3-2',
            name: '2 ML fentanyl 0.05 MG/ML Injection',
            value: 9602,
            parent: 'Headache',
          },
          {
            id: '3-4',
            name: 'ondansetron 2 MG/ML Injectable Solution',
            value: 15657,
            parent: 'Headache',
          },
          {
            id: 'Chest pain',
            name: 'Chest pain',
            value: 337071,
            color: color[3],
          },
          {
            id: '4-1',
            name: 'acetaminophen',
            value: 247708,
            parent: 'Chest pain',
          },
          {
            id: '4-2',
            name: 'sodium chloride 9 MG/ML Injectable Solution',
            value: 58931,
            parent: 'Chest pain',
          },
          {
            id: '4-3',
            name: '2 ML ondansetron 2 MG/ML Injection',
            value: 30432,
            parent: 'Chest pain',
          },
        ],
      },
    ],
    title: {
      text: 'Participants taking one of overall-top 10 drugs for each of overall-top 10 conditions',
    },
    tooltip: {
      useHTML: true,
      pointFormat: '<b>{point.name}</b>: Counts <b>{point.value}</b>',
    },
  };
}

export function getCannedHeatmap() {
  const getPointCategoryName = function (point, dimension) {
    const series = point.series,
      isY = dimension === 'y',
      axis = series[isY ? 'yAxis' : 'xAxis'];
    return axis.categories[point[isY ? 'y' : 'x']];
  };

  return {
    chart: {
      type: 'heatmap',
      marginTop: 40,
      marginBottom: 40,
      plotBorderWidth: 1,
    },

    title: {
      text: 'Sales per employee per weekday',
    },

    xAxis: {
      categories: [
        'Alexander',
        'Marie',
        'Maximilian',
        'Sophia',
        'Lukas',
        'Maria',
        'Leon',
        'Anna',
        'Tim',
        'Laura',
      ],
    },
    yAxis: {
      categories: ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday'],
      title: null,
      reversed: true,
    },

    accessibility: {
      point: {
        descriptionFormatter: function (point) {
          const ix = point.index + 1,
            xName = getPointCategoryName(point, 'x'),
            yName = getPointCategoryName(point, 'y'),
            val = point.value;
          return ix + '. ' + xName + ' sales ' + yName + ', ' + val + '.';
        },
      },
    },
    colorAxis: {
      min: 0,
      minColor: '#75C6E4',
      maxColor: '#E987CF',
    },

    legend: {
      align: 'right',
      layout: 'vertical',
      margin: 0,
      verticalAlign: 'top',
      y: 25,
      symbolHeight: 300,
    },

    tooltip: {
      formatter: function () {
        return (
          '<b>' +
          getPointCategoryName(this.point, 'x') +
          '</b> sold <br><b>' +
          this.point.value +
          '</b> items on <br><b>' +
          getPointCategoryName(this.point, 'y') +
          '</b>'
        );
      },
    },

    series: [
      {
        name: 'Sales per employee',
        borderWidth: 1,
        data: [
          [0, 0, 10],
          [0, 1, 19],
          [0, 2, 8],
          [0, 3, 24],
          [0, 4, 67],
          [1, 0, 92],
          [1, 1, 58],
          [1, 2, 78],
          [1, 3, 117],
          [1, 4, 48],
          [2, 0, 35],
          [2, 1, 15],
          [2, 2, 123],
          [2, 3, 64],
          [2, 4, 52],
          [3, 0, 72],
          [3, 1, 132],
          [3, 2, 114],
          [3, 3, 19],
          [3, 4, 16],
          [4, 0, 38],
          [4, 1, 5],
          [4, 2, 8],
          [4, 3, 117],
          [4, 4, 115],
          [5, 0, 88],
          [5, 1, 32],
          [5, 2, 12],
          [5, 3, 6],
          [5, 4, 120],
          [6, 0, 13],
          [6, 1, 44],
          [6, 2, 88],
          [6, 3, 98],
          [6, 4, 96],
          [7, 0, 31],
          [7, 1, 1],
          [7, 2, 82],
          [7, 3, 32],
          [7, 4, 30],
          [8, 0, 85],
          [8, 1, 97],
          [8, 2, 123],
          [8, 3, 64],
          [8, 4, 84],
          [9, 0, 47],
          [9, 1, 114],
          [9, 2, 31],
          [9, 3, 48],
          [9, 4, 91],
        ],
        dataLabels: {
          enabled: true,
          color: '#000000',
        },
      },
    ],
  };
}
