import ustopo from "@highcharts/map-collection/countries/us/us-all.topo.json";

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
      bar: {
        grouping: false,
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
  //const topology = require('assets/json/us-all-territories.topo.json');

  // Prepare demo data. The data is joined to map using value of 'hc-key'
  // property by default. See API docs for 'joinBy' for more info on linking
  // data and map.
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
    chart:{
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
