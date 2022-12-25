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
