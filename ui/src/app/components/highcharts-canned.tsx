import * as React from 'react';

export function getCanned() {
  // http://jsfiddle.net/Moonbird_IT/30Luzmya/2/
  return {
    chart: {
      type: 'column',
    },
    legend: {
      layout: 'vertical',
      align: 'right',
      verticalAlign: 'middle',
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
