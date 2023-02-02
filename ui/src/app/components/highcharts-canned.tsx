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

import hcColors from './highcharts-colors';
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
  const colors = hcColors.hcColors.gender;
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
        dataOld: [
          {
            id: 'Essential hypertension',
            name: 'Essential hypertension',
            value: 378148,
            color: colors[0],
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
            color: colors[1],
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
            color: colors[2],
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
            color: colors[3],
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
        data: [
          {
            id: '01-Essential hypertension',
            name: 'Essential hypertension',
            value: 28777,
            color: colors[0],
          },
          {
            id: '02-Cough',
            name: 'Cough',
            value: 15918,
            color: colors[1],
          },
          {
            id: '03-Headache',
            name: 'Headache',
            value: 13739,
            color: colors[2],
          },
          {
            id: '04-Depressive disorder',
            name: 'Depressive disorder',
            value: 12152,
            color: colors[3],
          },
          {
            id: '05-Dizziness and giddiness',
            name: 'Dizziness and giddiness',
            value: 11313,
            color: colors[4],
          },
          {
            id: '06-Chest pain',
            name: 'Chest pain',
            value: 10596,
            color: colors[5],
          },
          {
            id: '07-Abdominal pain',
            name: 'Abdominal pain',
            value: 10511,
            color: colors[6],
          },
          {
            id: '08-Hyperlipidemia',
            name: 'Hyperlipidemia',
            value: 9341,
            color: colors[7],
          },
          {
            id: '09-Acute pharyngitis',
            name: 'Acute pharyngitis',
            value: 7708,
            color: colors[8],
          },
          {
            id: '10-Low back pain',
            name: 'Low back pain',
            value: 7699,
            color: colors[9],
          },
          {
            id: '01-01',
            name: 'sodium chloride 9 MG/ML Injectable Solution',
            parent: '01-Essential hypertension',
            value: 9885,
          },
          {
            id: '01-02',
            name: '2 ML ondansetron 2 MG/ML Injection',
            parent: '01-Essential hypertension',
            value: 8505,
          },
          {
            id: '01-03',
            name: 'calcium chloride 0.0014 MEQ/ML / potassium chloride 0.004 MEQ/ML / sodium chloride 0.103 MEQ/ML / sodium lactate 0.028 MEQ/ML Injectable Solution',
            parent: '01-Essential hypertension',
            value: 8435,
          },
          {
            id: '01-04',
            name: 'ondansetron 2 MG/ML Injectable Solution',
            parent: '01-Essential hypertension',
            value: 8166,
          },
          {
            id: '01-05',
            name: '1000 ML sodium chloride 9 MG/ML Injection',
            parent: '01-Essential hypertension',
            value: 7311,
          },
          {
            id: '01-06',
            name: '2 ML fentanyl 0.05 MG/ML Injection',
            parent: '01-Essential hypertension',
            value: 7072,
          },
          {
            id: '01-07',
            name: 'acetaminophen 325 MG Oral Tablet',
            parent: '01-Essential hypertension',
            value: 6490,
          },
          {
            id: '01-08',
            name: 'tetanus toxoid, reduced diphtheria toxoid, and acellular pertussis vaccine, adsorbed',
            parent: '01-Essential hypertension',
            value: 4793,
          },
          {
            id: '01-09',
            name: 'acetaminophen 325 MG / hydrocodone bitartrate 5 MG Oral Tablet',
            parent: '01-Essential hypertension',
            value: 5989,
          },
          {
            id: '01-10',
            name: 'acetaminophen',
            parent: '01-Essential hypertension',
            value: 5473,
          },
          {
            id: '02-01',
            name: 'sodium chloride 9 MG/ML Injectable Solution',
            parent: '02-Cough',
            value: 5310,
          },
          {
            id: '02-02',
            name: '2 ML ondansetron 2 MG/ML Injection',
            parent: '02-Cough',
            value: 4618,
          },
          {
            id: '02-03',
            name: 'calcium chloride 0.0014 MEQ/ML / potassium chloride 0.004 MEQ/ML / sodium chloride 0.103 MEQ/ML / sodium lactate 0.028 MEQ/ML Injectable Solution',
            parent: '02-Cough',
            value: 4698,
          },
          {
            id: '02-04',
            name: 'ondansetron 2 MG/ML Injectable Solution',
            parent: '02-Cough',
            value: 4280,
          },
          {
            id: '02-05',
            name: '1000 ML sodium chloride 9 MG/ML Injection',
            parent: '02-Cough',
            value: 4095,
          },
          {
            id: '02-06',
            name: '2 ML fentanyl 0.05 MG/ML Injection',
            parent: '02-Cough',
            value: 3441,
          },
          {
            id: '02-07',
            name: 'acetaminophen 325 MG Oral Tablet',
            parent: '02-Cough',
            value: 3990,
          },
          {
            id: '02-08',
            name: 'tetanus toxoid, reduced diphtheria toxoid, and acellular pertussis vaccine, adsorbed',
            parent: '02-Cough',
            value: 3167,
          },
          {
            id: '02-09',
            name: 'acetaminophen 325 MG / hydrocodone bitartrate 5 MG Oral Tablet',
            parent: '02-Cough',
            value: 3530,
          },
          {
            id: '02-10',
            name: 'acetaminophen',
            parent: '02-Cough',
            value: 3688,
          },
          {
            id: '03-01',
            name: 'sodium chloride 9 MG/ML Injectable Solution',
            parent: '03-Headache',
            value: 5008,
          },
          {
            id: '03-02',
            name: '2 ML ondansetron 2 MG/ML Injection',
            parent: '03-Headache',
            value: 3817,
          },
          {
            id: '03-03',
            name: 'calcium chloride 0.0014 MEQ/ML / potassium chloride 0.004 MEQ/ML / sodium chloride 0.103 MEQ/ML / sodium lactate 0.028 MEQ/ML Injectable Solution',
            parent: '03-Headache',
            value: 4045,
          },
          {
            id: '03-04',
            name: 'ondansetron 2 MG/ML Injectable Solution',
            parent: '03-Headache',
            value: 4293,
          },
          {
            id: '03-05',
            name: '1000 ML sodium chloride 9 MG/ML Injection',
            parent: '03-Headache',
            value: 3631,
          },
          {
            id: '03-06',
            name: '2 ML fentanyl 0.05 MG/ML Injection',
            parent: '03-Headache',
            value: 2704,
          },
          {
            id: '03-07',
            name: 'acetaminophen 325 MG Oral Tablet',
            parent: '03-Headache',
            value: 3373,
          },
          {
            id: '03-08',
            name: 'tetanus toxoid, reduced diphtheria toxoid, and acellular pertussis vaccine, adsorbed',
            parent: '03-Headache',
            value: 2434,
          },
          {
            id: '03-09',
            name: 'acetaminophen 325 MG / hydrocodone bitartrate 5 MG Oral Tablet',
            parent: '03-Headache',
            value: 3074,
          },
          {
            id: '03-10',
            name: 'acetaminophen',
            parent: '03-Headache',
            value: 3065,
          },
          {
            id: '04-01',
            name: 'sodium chloride 9 MG/ML Injectable Solution',
            parent: '04-Depressive disorder',
            value: 3273,
          },
          {
            id: '04-02',
            name: '2 ML ondansetron 2 MG/ML Injection',
            parent: '04-Depressive disorder',
            value: 5024,
          },
          {
            id: '04-03',
            name: 'calcium chloride 0.0014 MEQ/ML / potassium chloride 0.004 MEQ/ML / sodium chloride 0.103 MEQ/ML / sodium lactate 0.028 MEQ/ML Injectable Solution',
            parent: '04-Depressive disorder',
            value: 3674,
          },
          {
            id: '04-04',
            name: 'ondansetron 2 MG/ML Injectable Solution',
            parent: '04-Depressive disorder',
            value: 2846,
          },
          {
            id: '04-05',
            name: '1000 ML sodium chloride 9 MG/ML Injection',
            parent: '04-Depressive disorder',
            value: 4412,
          },
          {
            id: '04-06',
            name: '2 ML fentanyl 0.05 MG/ML Injection',
            parent: '04-Depressive disorder',
            value: 3625,
          },
          {
            id: '04-07',
            name: 'acetaminophen 325 MG Oral Tablet',
            parent: '04-Depressive disorder',
            value: 3283,
          },
          {
            id: '04-08',
            name: 'tetanus toxoid, reduced diphtheria toxoid, and acellular pertussis vaccine, adsorbed',
            parent: '04-Depressive disorder',
            value: 3364,
          },
          {
            id: '04-09',
            name: 'acetaminophen 325 MG / hydrocodone bitartrate 5 MG Oral Tablet',
            parent: '04-Depressive disorder',
            value: 3130,
          },
          {
            id: '04-10',
            name: 'acetaminophen',
            parent: '04-Depressive disorder',
            value: 2741,
          },
          {
            id: '05-01',
            name: 'sodium chloride 9 MG/ML Injectable Solution',
            parent: '05-Dizziness and giddiness',
            value: 4392,
          },
          {
            id: '05-02',
            name: '2 ML ondansetron 2 MG/ML Injection',
            parent: '05-Dizziness and giddiness',
            value: 3347,
          },
          {
            id: '05-03',
            name: 'calcium chloride 0.0014 MEQ/ML / potassium chloride 0.004 MEQ/ML / sodium chloride 0.103 MEQ/ML / sodium lactate 0.028 MEQ/ML Injectable Solution',
            parent: '05-Dizziness and giddiness',
            value: 3376,
          },
          {
            id: '05-04',
            name: 'ondansetron 2 MG/ML Injectable Solution',
            parent: '05-Dizziness and giddiness',
            value: 3610,
          },
          {
            id: '05-05',
            name: '1000 ML sodium chloride 9 MG/ML Injection',
            parent: '05-Dizziness and giddiness',
            value: 3080,
          },
          {
            id: '05-06',
            name: '2 ML fentanyl 0.05 MG/ML Injection',
            parent: '05-Dizziness and giddiness',
            value: 2379,
          },
          {
            id: '05-07',
            name: 'acetaminophen 325 MG Oral Tablet',
            parent: '05-Dizziness and giddiness',
            value: 2936,
          },
          {
            id: '05-08',
            name: 'tetanus toxoid, reduced diphtheria toxoid, and acellular pertussis vaccine, adsorbed',
            parent: '05-Dizziness and giddiness',
            value: 2109,
          },
          {
            id: '05-09',
            name: 'acetaminophen 325 MG / hydrocodone bitartrate 5 MG Oral Tablet',
            parent: '05-Dizziness and giddiness',
            value: 2441,
          },
          {
            id: '05-10',
            name: 'acetaminophen',
            parent: '05-Dizziness and giddiness',
            value: 2321,
          },
          {
            id: '06-01',
            name: 'sodium chloride 9 MG/ML Injectable Solution',
            parent: '06-Chest pain',
            value: 3990,
          },
          {
            id: '06-02',
            name: '2 ML ondansetron 2 MG/ML Injection',
            parent: '06-Chest pain',
            value: 2875,
          },
          {
            id: '06-03',
            name: 'calcium chloride 0.0014 MEQ/ML / potassium chloride 0.004 MEQ/ML / sodium chloride 0.103 MEQ/ML / sodium lactate 0.028 MEQ/ML Injectable Solution',
            parent: '06-Chest pain',
            value: 2896,
          },
          {
            id: '06-04',
            name: 'ondansetron 2 MG/ML Injectable Solution',
            parent: '06-Chest pain',
            value: 3430,
          },
          {
            id: '06-05',
            name: '1000 ML sodium chloride 9 MG/ML Injection',
            parent: '06-Chest pain',
            value: 2669,
          },
          {
            id: '06-06',
            name: '2 ML fentanyl 0.05 MG/ML Injection',
            parent: '06-Chest pain',
            value: 2167,
          },
          {
            id: '06-07',
            name: 'acetaminophen 325 MG Oral Tablet',
            parent: '06-Chest pain',
            value: 2472,
          },
          {
            id: '06-08',
            name: 'tetanus toxoid, reduced diphtheria toxoid, and acellular pertussis vaccine, adsorbed',
            parent: '06-Chest pain',
            value: 1899,
          },
          {
            id: '06-09',
            name: 'acetaminophen 325 MG / hydrocodone bitartrate 5 MG Oral Tablet',
            parent: '06-Chest pain',
            value: 2124,
          },
          {
            id: '06-10',
            name: 'acetaminophen',
            parent: '06-Chest pain',
            value: 2125,
          },
          {
            id: '07-01',
            name: 'sodium chloride 9 MG/ML Injectable Solution',
            parent: '07-Abdominal pain',
            value: 4216,
          },
          {
            id: '07-02',
            name: '2 ML ondansetron 2 MG/ML Injection',
            parent: '07-Abdominal pain',
            value: 2875,
          },
          {
            id: '07-03',
            name: 'calcium chloride 0.0014 MEQ/ML / potassium chloride 0.004 MEQ/ML / sodium chloride 0.103 MEQ/ML / sodium lactate 0.028 MEQ/ML Injectable Solution',
            parent: '07-Abdominal pain',
            value: 3220,
          },
          {
            id: '07-04',
            name: 'ondansetron 2 MG/ML Injectable Solution',
            parent: '07-Abdominal pain',
            value: 3966,
          },
          {
            id: '07-05',
            name: '1000 ML sodium chloride 9 MG/ML Injection',
            parent: '07-Abdominal pain',
            value: 2686,
          },
          {
            id: '07-06',
            name: '2 ML fentanyl 0.05 MG/ML Injection',
            parent: '07-Abdominal pain',
            value: 2023,
          },
          {
            id: '07-07',
            name: 'acetaminophen 325 MG Oral Tablet',
            parent: '07-Abdominal pain',
            value: 2235,
          },
          {
            id: '07-08',
            name: 'tetanus toxoid, reduced diphtheria toxoid, and acellular pertussis vaccine, adsorbed',
            parent: '07-Abdominal pain',
            value: 1908,
          },
          {
            id: '07-09',
            name: 'acetaminophen 325 MG / hydrocodone bitartrate 5 MG Oral Tablet',
            parent: '07-Abdominal pain',
            value: 2006,
          },
          {
            id: '07-10',
            name: 'acetaminophen',
            parent: '07-Abdominal pain',
            value: 1763,
          },
          {
            id: '08-01',
            name: 'sodium chloride 9 MG/ML Injectable Solution',
            parent: '08-Hyperlipidemia',
            value: 3293,
          },
          {
            id: '08-02',
            name: '2 ML ondansetron 2 MG/ML Injection',
            parent: '08-Hyperlipidemia',
            value: 3064,
          },
          {
            id: '08-03',
            name: 'calcium chloride 0.0014 MEQ/ML / potassium chloride 0.004 MEQ/ML / sodium chloride 0.103 MEQ/ML / sodium lactate 0.028 MEQ/ML Injectable Solution',
            parent: '08-Hyperlipidemia',
            value: 2671,
          },
          {
            id: '08-04',
            name: 'ondansetron 2 MG/ML Injectable Solution',
            parent: '08-Hyperlipidemia',
            value: 2916,
          },
          {
            id: '08-05',
            name: '1000 ML sodium chloride 9 MG/ML Injection',
            parent: '08-Hyperlipidemia',
            value: 2401,
          },
          {
            id: '08-06',
            name: '2 ML fentanyl 0.05 MG/ML Injection',
            parent: '08-Hyperlipidemia',
            value: 2479,
          },
          {
            id: '08-07',
            name: 'acetaminophen 325 MG Oral Tablet',
            parent: '08-Hyperlipidemia',
            value: 1932,
          },
          {
            id: '08-08',
            name: 'tetanus toxoid, reduced diphtheria toxoid, and acellular pertussis vaccine, adsorbed',
            parent: '08-Hyperlipidemia',
            value: 1929,
          },
          {
            id: '08-09',
            name: 'acetaminophen 325 MG / hydrocodone bitartrate 5 MG Oral Tablet',
            parent: '08-Hyperlipidemia',
            value: 1963,
          },
          {
            id: '08-10',
            name: 'acetaminophen',
            parent: '08-Hyperlipidemia',
            value: 1441,
          },
          {
            id: '09-01',
            name: 'sodium chloride 9 MG/ML Injectable Solution',
            parent: '09-Acute pharyngitis',
            value: 2038,
          },
          {
            id: '09-02',
            name: '2 ML ondansetron 2 MG/ML Injection',
            parent: '09-Acute pharyngitis',
            value: 2589,
          },
          {
            id: '09-03',
            name: 'calcium chloride 0.0014 MEQ/ML / potassium chloride 0.004 MEQ/ML / sodium chloride 0.103 MEQ/ML / sodium lactate 0.028 MEQ/ML Injectable Solution',
            parent: '09-Acute pharyngitis',
            value: 2105,
          },
          {
            id: '09-04',
            name: 'ondansetron 2 MG/ML Injectable Solution',
            parent: '09-Acute pharyngitis',
            value: 1692,
          },
          {
            id: '09-05',
            name: '1000 ML sodium chloride 9 MG/ML Injection',
            parent: '09-Acute pharyngitis',
            value: 2377,
          },
          {
            id: '09-06',
            name: '2 ML fentanyl 0.05 MG/ML Injection',
            parent: '09-Acute pharyngitis',
            value: 1577,
          },
          {
            id: '09-07',
            name: 'acetaminophen 325 MG Oral Tablet',
            parent: '09-Acute pharyngitis',
            value: 1763,
          },
          {
            id: '09-08',
            name: 'tetanus toxoid, reduced diphtheria toxoid, and acellular pertussis vaccine, adsorbed',
            parent: '09-Acute pharyngitis',
            value: 2230,
          },
          {
            id: '09-09',
            name: 'acetaminophen 325 MG / hydrocodone bitartrate 5 MG Oral Tablet',
            parent: '09-Acute pharyngitis',
            value: 1847,
          },
          {
            id: '09-10',
            name: 'acetaminophen',
            parent: '09-Acute pharyngitis',
            value: 1879,
          },
          {
            id: '10-01',
            name: 'sodium chloride 9 MG/ML Injectable Solution',
            parent: '10-Low back pain',
            value: 2408,
          },
          {
            id: '10-02',
            name: '2 ML ondansetron 2 MG/ML Injection',
            parent: '10-Low back pain',
            value: 2519,
          },
          {
            id: '10-03',
            name: 'calcium chloride 0.0014 MEQ/ML / potassium chloride 0.004 MEQ/ML / sodium chloride 0.103 MEQ/ML / sodium lactate 0.028 MEQ/ML Injectable Solution',
            parent: '10-Low back pain',
            value: 2357,
          },
          {
            id: '10-04',
            name: 'ondansetron 2 MG/ML Injectable Solution',
            parent: '10-Low back pain',
            value: 2345,
          },
          {
            id: '10-05',
            name: '1000 ML sodium chloride 9 MG/ML Injection',
            parent: '10-Low back pain',
            value: 2115,
          },
          {
            id: '10-06',
            name: '2 ML fentanyl 0.05 MG/ML Injection',
            parent: '10-Low back pain',
            value: 1917,
          },
          {
            id: '10-07',
            name: 'acetaminophen 325 MG Oral Tablet',
            parent: '10-Low back pain',
            value: 1772,
          },
          {
            id: '10-08',
            name: 'tetanus toxoid, reduced diphtheria toxoid, and acellular pertussis vaccine, adsorbed',
            parent: '10-Low back pain',
            value: 1700,
          },
          {
            id: '10-09',
            name: 'acetaminophen 325 MG / hydrocodone bitartrate 5 MG Oral Tablet',
            parent: '10-Low back pain',
            value: 1843,
          },
          {
            id: '10-10',
            name: 'acetaminophen',
            parent: '10-Low back pain',
            value: 1561,
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
      text: 'Top 10 conditions by top 10 Drugs',
    },

    xAxis: {
      categories: [
        '1:Essential hypertension',
        '2:Cough',
        '3:Headache',
        '4:Depressive disorder',
        '5:Dizziness and giddiness',
        '6:Chest pain',
        '7:Abdominal pain',
        '8:Hyperlipidemia',
        '9:Acute pharyngitis',
        '10:Low back pain',
      ],
      title: 'Top 10 Conditions',
    },
    yAxis: {
      categories: [
        '1:sodium chloride 9 MG/ML Injectable Solution',
        '2:2 ML ondansetron 2 MG/ML Injection',
        '3:calcium chloride 0.0014 MEQ/ML / potassium chloride 0.004 MEQ/ML / sodium chloride 0.103 MEQ/ML / sodium lactate 0.028 MEQ/ML Injectable Solution',
        '4:ondansetron 2 MG/ML Injectable Solution',
        '5:1000 ML sodium chloride 9 MG/ML Injection',
        '6:2 ML fentanyl 0.05 MG/ML Injection',
        '7:acetaminophen 325 MG Oral Tablet',
        '8:tetanus toxoid, reduced diphtheria toxoid, and acellular pertussis vaccine, adsorbed',
        '9:acetaminophen 325 MG / hydrocodone bitartrate 5 MG Oral Tablet',
        '10:acetaminophen',
      ],
      title: 'Top 10 Drugs',
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
      // min: 1000,
      // max: 10000,
      // minColor: '#0000FF',
      // maxColor: '#FF0000',
      stops: [
        [0, '#00007F'],
        [0.125, 'blue'],
        [0.25, '#007FFF'],
        [0.375, 'cyan'],
        [0.5, '#7FFF7F'],
        [0.625, 'yellow'],
        [0.75, '#FF7F00'],
        [0.875, 'red'],
        [1, '#7F0000'],
      ],
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
          '</b> -> <br><b>' +
          this.point.value +
          '</b> -> <br><b>' +
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
          [0, 0, 9885],
          [0, 1, 8505],
          [0, 2, 8435],
          [0, 3, 8166],
          [0, 4, 7311],
          [0, 5, 7072],
          [0, 6, 6490],
          [0, 7, 4793],
          [0, 8, 5989],
          [0, 9, 5473],
          [1, 0, 5310],
          [1, 1, 4618],
          [1, 2, 4698],
          [1, 3, 4280],
          [1, 4, 4095],
          [1, 5, 3441],
          [1, 6, 3990],
          [1, 7, 3167],
          [1, 8, 3530],
          [1, 9, 3688],
          [2, 0, 5008],
          [2, 1, 3817],
          [2, 2, 4045],
          [2, 3, 4293],
          [2, 4, 3631],
          [2, 5, 2704],
          [2, 6, 3373],
          [2, 7, 2434],
          [2, 8, 3074],
          [2, 9, 3065],
          [3, 0, 3273],
          [3, 1, 5024],
          [3, 2, 3674],
          [3, 3, 2846],
          [3, 4, 4412],
          [3, 5, 3625],
          [3, 6, 3283],
          [3, 7, 3364],
          [3, 8, 3130],
          [3, 9, 2741],
          [4, 0, 4392],
          [4, 1, 3347],
          [4, 2, 3376],
          [4, 3, 3610],
          [4, 4, 3080],
          [4, 5, 2379],
          [4, 6, 2936],
          [4, 7, 2109],
          [4, 8, 2441],
          [4, 9, 2321],
          [5, 0, 3990],
          [5, 1, 2875],
          [5, 2, 2896],
          [5, 3, 3430],
          [5, 4, 2669],
          [5, 5, 2167],
          [5, 6, 2472],
          [5, 7, 1899],
          [5, 8, 2124],
          [5, 9, 2125],
          [6, 0, 4216],
          [6, 1, 2875],
          [6, 2, 3220],
          [6, 3, 3966],
          [6, 4, 2686],
          [6, 5, 2023],
          [6, 6, 2235],
          [6, 7, 1908],
          [6, 8, 2006],
          [6, 9, 1763],
          [7, 0, 3293],
          [7, 1, 3064],
          [7, 2, 2671],
          [7, 3, 2916],
          [7, 4, 2401],
          [7, 5, 2479],
          [7, 6, 1932],
          [7, 7, 1929],
          [7, 8, 1963],
          [7, 9, 1441],
          [8, 0, 2038],
          [8, 1, 2589],
          [8, 2, 2105],
          [8, 3, 1692],
          [8, 4, 2377],
          [8, 5, 1577],
          [8, 6, 1763],
          [8, 7, 2230],
          [8, 8, 1847],
          [8, 9, 1879],
          [9, 0, 2408],
          [9, 1, 2519],
          [9, 2, 2357],
          [9, 3, 2345],
          [9, 4, 2115],
          [9, 5, 1917],
          [9, 6, 1772],
          [9, 7, 1700],
          [9, 8, 1843],
          [9, 9, 1561],
        ],
        dataLabels: {
          enabled: true,
          color: '#000000',
        },
      },
    ],
  };
}

export function getCannedSankey() {
  return {
    chart: {
      type: 'sankey',
    },
    title: {
      text: 'Highcharts Sankey Diagram',
    },
    accessibility: {
      point: {
        valueDescriptionFormat:
          '{index}. {point.from} to {point.to}, {point.weight}.',
      },
    },
    series: [
      {
        keys: ['from', 'to', 'weight'],
        data: [
          ['Female', 'Hispanic or Latino', 5],
          ['Female', 'Not Hispanic or Latino', 1],
          ['Female', 'Unknown', 1],
          ['Female', 'Skip-2', 1],
          ['Male', 'Hispanic or Latino', 1],
          ['Male', 'Not Hispanic or Latino', 5],
          ['Male', 'Skip-2', 1],
          ['Not a male, not female...', 'Hispanic or Latino', 1],
          ['Not a male, not female...', 'Not Hispanic or Latino', 1],
          ['Not a male, not female...', 'Unknown', 5],
          ['Not a male, not female...', 'Skip-2', 1],
          ['Skip', 'Hispanic or Latino', 1],
          ['Skip', 'Not Hispanic or Latino', 1],
          ['Skip', 'Unknown', 1],
          ['Skip', 'Skip-2', 5],
          ['Hispanic or Latino', 'Asian', 2],
          ['Hispanic or Latino', 'Another Single...', 1],
          ['Hispanic or Latino', 'Black or African', 1],
          ['Hispanic or Latino', 'White', 3],
          ['Not Hispanic or Latino', 'Asian', 1],
          ['Not Hispanic or Latino', 'Another Single...', 3],
          ['Not Hispanic or Latino', 'More than...', 3],
          ['Not Hispanic or Latino', 'Black or African', 3],
          ['Not Hispanic or Latino', 'White', 1],
          ['Unknown', 'Another Single...', 1],
          ['Unknown', 'Black or African', 3],
          ['Unknown', 'White', 1],
          ['Skip-2', 'Asian', 1],
          ['Skip-2', 'Another Single...', 1],
          ['Skip-2', 'Black or African', 2],
          ['Skip-2', 'White', 7],
          ['White', 'Woman', 5],
          ['White', 'Man', 1],
          ['White', 'Other', 3],
          ['Asian', 'Woman', 5],
          ['Asian', 'Man', 1],
          ['Asian', 'Other', 3],
          ['Another Single...', 'Woman', 5],
          ['Another Single...', 'Man', 1],
          ['Another Single...', 'Other', 3],
          ['More than...', 'Woman', 5],
          ['More than...', 'Man', 1],
          ['More than...', 'Other', 3],
          ['Black or African', 'Woman', 5],
          ['Black or African', 'Man', 1],
          ['Black or African', 'Other', 3],
        ],
        type: 'sankey',
        name: 'Sankey demo series',
      },
    ],
  };
}
export function getCannedDependencyWheel() {
  return {
    chart: {
      type: 'dependencywheel',
    },
    title: {
      text: 'Highcharts Dependency wheel Diagram',
    },
    accessibility: {
      point: {
        valueDescriptionFormat:
          '{index}. {point.from} to {point.to}, {point.weight}.',
      },
    },
    series: [
      {
        keys: ['from', 'to', 'weight'],
        data: [
          ['Female', 'Hispanic or Latino', 5],
          ['Female', 'Not Hispanic or Latino', 1],
          ['Female', 'Unknown', 1],
          ['Female', 'Skip-2', 1],
          ['Male', 'Hispanic or Latino', 1],
          ['Male', 'Not Hispanic or Latino', 5],
          ['Male', 'Skip-2', 1],
          ['Not a male, not female...', 'Hispanic or Latino', 1],
          ['Not a male, not female...', 'Not Hispanic or Latino', 1],
          ['Not a male, not female...', 'Unknown', 5],
          ['Not a male, not female...', 'Skip-2', 1],
          ['Skip', 'Hispanic or Latino', 1],
          ['Skip', 'Not Hispanic or Latino', 1],
          ['Skip', 'Unknown', 1],
          ['Skip', 'Skip-2', 5],
          ['Hispanic or Latino', 'Asian', 2],
          ['Hispanic or Latino', 'Another Single...', 1],
          ['Hispanic or Latino', 'Black or African', 1],
          ['Hispanic or Latino', 'White', 3],
          ['Not Hispanic or Latino', 'Asian', 1],
          ['Not Hispanic or Latino', 'Another Single...', 3],
          ['Not Hispanic or Latino', 'More than...', 3],
          ['Not Hispanic or Latino', 'Black or African', 3],
          ['Not Hispanic or Latino', 'White', 1],
          ['Unknown', 'Another Single...', 1],
          ['Unknown', 'Black or African', 3],
          ['Unknown', 'White', 1],
          ['Skip-2', 'Asian', 1],
          ['Skip-2', 'Another Single...', 1],
          ['Skip-2', 'Black or African', 2],
          ['Skip-2', 'White', 7],
          ['White', 'Woman', 5],
          ['White', 'Man', 1],
          ['White', 'Other', 3],
          ['Asian', 'Woman', 5],
          ['Asian', 'Man', 1],
          ['Asian', 'Other', 3],
          ['Another Single...', 'Woman', 5],
          ['Another Single...', 'Man', 1],
          ['Another Single...', 'Other', 3],
          ['More than...', 'Woman', 5],
          ['More than...', 'Man', 1],
          ['More than...', 'Other', 3],
          ['Black or African', 'Woman', 5],
          ['Black or African', 'Man', 1],
          ['Black or African', 'Other', 3],
        ],
        type: 'dependencywheel',
        name: 'Dependency wheel demo',
      },
    ],
  };
}
