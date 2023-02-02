import * as highCharts from 'highcharts';

import { ChartData, Domain } from 'generated/fetch';

import ustopo from '@highcharts/map-collection/countries/us/us-all.topo.json';

import hcColors from './highcharts-colors';

require('highcharts/modules/exporting')(highCharts);

export enum Category {
  Gender = 'gender',
  SexAtBirth = 'sexAtBirth',
  Race = 'race',
  Ethnicity = 'ethnicity',
  AgeBin = 'ageBin',
  StateCode = 'stateCode',
  ConceptName = 'conceptName',
  NumConceptsCoOccur = 'numConceptsCoOccur',
}

const CHART_CATEGORY_KEY_NAME = {
  gender: 'Gender',
  sexAtBirth: 'Sex At Birth',
  race: 'Race',
  ethnicity: 'Ethnicity',
  ageBin: 'Age Range',
  stateCode: 'State Code',
  conceptName: 'Concept Name',
  numConceptsCoOccur: 'Number of Co-occurring Concepts',
};

export function getAvailableCategories(dataForCharts: Array<ChartData>) {
  const categoryNames = {};
  const categoryValues = {};
  Object.keys(CHART_CATEGORY_KEY_NAME).forEach((key) => {
    categoryNames[key] = CHART_CATEGORY_KEY_NAME[key];
    categoryValues[key] = Array.from(
      new Set(dataForCharts.map((dat) => dat[key]))
    ).sort((a, b) => (a > b ? -1 : 1));
  });
  return { categoryNames, categoryValues };
}

export function formattedRank(rank: number) {
  // const fr = '00' + String(rank);
  // return fr.slice(fr.length - 2);
  return String(rank);
}

function getCategorySortKey(
  domain: Domain,
  categoryProp: string,
  record: ChartData
) {
  const key = record[categoryProp];
  if (domain === Domain.PERSON) {
    if (categoryProp === 'gender') {
      return key.startsWith('Not') ? 'O' : key[0];
    } else {
      return key;
    }
  } else {
    return formattedRank(record.conceptRank) + ':' + record.conceptName;
  }
}

function getXAxis(categories, rightSide, titleText?, linkedToVal?) {
  return {
    title: {
      text: titleText ? titleText : '',
    },
    categories: categories,
    opposite: rightSide,
    reversed: false,
    linkedTo: linkedToVal,
  };
}

function getYAxis(titleText?) {
  return {
    title: {
      text: titleText ? titleText : '',
    },
    labels: {
      formatter: function () {
        return Math.abs(this.value) + '%';
      },
    },
    reversedStacks: true,
  };
}

export function getChartCategoryCounts(
  dataForCharts: Array<ChartData>,
  domain: Domain,
  category: Category,
  categoryVals: []
) {
  const categoryProp = category.toString();
  let total = 0;
  const categoryCounts = dataForCharts
    .reduce((accum, record) => {
      const key = record[categoryProp];
      const rec = accum.find((item) => item.categoryName === key);
      if (!rec) {
        accum.push({
          category: categoryProp,
          categoryName: key,
          categoryCount: record.count,
          categorySortKey: getCategorySortKey(domain, categoryProp, record),
        });
      } else {
        rec.categoryCount += record.count;
      }
      total += record.count;
      return accum;
    }, [])
    .sort(
      (a, b) => a.x - b.x || a.categorySortKey.localeCompare(b.categorySortKey)
    );

  // compute %
  // categoryTotal * 100 / sum(categoryTotal)
  const categories = [];

  const series = categoryCounts.reduce((accum, rec, i) => {
    categories.push(rec.categoryName);
    rec.x = i;
    rec.y = (100 * rec.categoryCount) / total;
    rec.total = total;
    const colorIndex = categoryVals.findIndex(
      (value) => value === rec.categoryName
    );
    rec.color = hcColors.hcColors[categoryProp][colorIndex];
    accum.push({
      name: rec.categoryName,
      color: rec.color,
      data: [rec],
    });
    return accum;
  }, []);

  return {
    chart: {
      type: 'column',
      inverted: false,
    },
    title: {
      text: CHART_CATEGORY_KEY_NAME[categoryProp],
    },
    xAxis: [getXAxis(categories, false, '', 0)],
    yAxis: [getYAxis()],
    legend: {
      layout: 'horizontal',
      // align: 'right',
      verticalAlign: 'top',
      itemMarginBottom: 5,
      useHTML: true,
      labelFormatter: function () {
        return this.name.slice(0, 15) + (this.name.length > 15 ? '...' : '');
      },
    },
    plotOptions: {
      series: {
        stacking: 'normal', // 'normal',
        events: {
          legendItemClick: function () {
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
        return (
          '<b>' +
          CHART_CATEGORY_KEY_NAME[categoryProp] +
          ' (' +
          this.point.category +
          ')' +
          '</b><br/>' +
          'count: ' +
          Math.abs(this.point.categoryCount) +
          ' / ' +
          Math.abs(total) +
          '</b><br/>' +
          'percent: ' +
          highCharts.numberFormat(Math.abs(this.point.y), 2) +
          '%'
        );
      },
    },
    series: series,
  };
}

export function getChartCategoryCountsByAgeBin(
  dataForCharts: Array<ChartData>,
  domain: Domain,
  category: Category,
  categoryVals: []
) {
  const categories = Array.from(
    new Set(dataForCharts.map((dat) => dat.ageBin))
  ).sort((a, b) => (a > b ? 1 : -1));

  const categoryProp = category.toString();
  const categoryTotal = {};
  interface ReduceReturnType {
    id: number; // 👈️ 👈️ 👈️ no longer optional
    name: string;
    color: string;
    sortKey: any;
    data: [any];
  }

  const categoryCounts = dataForCharts.reduce<ReduceReturnType>(
    (accum, record) => {
      const key = record[categoryProp];
      const ageBin = record.ageBin;
      const rec = accum[key]; // accum['Female']
      const categoryColorIndex = categoryVals.findIndex(
        (value) => value === key
      );
      if (!rec) {
        // 1st data record
        accum[key] = {
          name: key,
          color: hcColors.hcColors[categoryProp][categoryColorIndex],
          sortKey: getCategorySortKey(domain, categoryProp, record),
          data: [
            {
              ageBin: record.ageBin,
              category: categoryProp,
              categoryName: key,
              categoryCount: record.count,
            },
          ],
        };
        categoryTotal[key] = record.count;
      } else {
        const dataRec = accum[key].data.find((item) => item.ageBin === ageBin);
        if (!dataRec) {
          // next data record
          accum[key].data.push({
            ageBin: record.ageBin,
            category: categoryProp,
            categoryName: key,
            categoryCount: record.count,
          });
        } else {
          dataRec.categoryCount += record.count;
        }
        categoryTotal[key] += record.count;
      }
      return accum;
    },
    {} as ReduceReturnType
  );
  // compute %
  // categoryCount * 100 / sum(categoryTotal)
  const sumCategoryTotal = Object.keys(categoryTotal).reduce((accum, key) => {
    return accum + categoryTotal[key];
  }, 0);
  const series = Object.values(categoryCounts)
    .reduce((accum, serObj) => {
      serObj.data.reduce((recAccum, rec) => {
        rec.categoryTotal = categoryTotal[serObj.name];
        rec.sumCategoryTotal = sumCategoryTotal;
        rec.x = categories.findIndex((cat) => rec.ageBin === cat);
        rec.y = (rec.categoryCount * 100) / sumCategoryTotal;
      }, []);
      accum.push(serObj);
      return accum;
    }, [])
    .sort((a, b) => a.sortKey === b.sortKey);
  // set reversedStacks:false on yaxis
  const yaxis = getYAxis('Percent(%)');
  yaxis.reversedStacks = false;

  return {
    chart: {
      type: 'column',
      inverted: true,
    },
    title: {
      text: CHART_CATEGORY_KEY_NAME[categoryProp],
    },
    xAxis: [getXAxis(categories, false, 'Age Range(Y)')],
    yAxis: [yaxis],
    legend: {
      layout: 'horizontal',
      // align: 'right',
      verticalAlign: 'top',
      itemMarginBottom: 5,
      useHTML: true,
      labelFormatter: function () {
        return this.name.slice(0, 15) + (this.name.length > 15 ? '...' : '');
      },
    },
    plotOptions: {
      series: {
        stacking: 'normal', // 'normal',
        events: {
          legendItemClick: function () {
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
        tip += '/' + this.point.sumCategoryTotal + ')<br/>';
        return tip + 'percent:' + parseFloat(this.point.y).toFixed(2) + '%';
      },
    },
    series: series,
  };
}

export function getChartCategoryCountsByConceptRank(
  dataForCharts: Array<ChartData>,
  domain: Domain,
  category: Category,
  categoryVals: []
) {
  // for x-axis instead of ageBin use [conceptRank,conceptName]
  const categories = Array.from(
    new Set(dataForCharts.map((dat) => dat.conceptRank + ':' + dat.conceptName))
  ).sort((a, b) =>
    +a.substring(0, a.indexOf(':')) > +b.substring(0, b.indexOf(':')) ? 1 : -1
  );
  // use counts for categoryProperty (gender, race,...)
  const categoryProp = category.toString();
  const categoryTotal = {};
  interface ReduceReturnType {
    id: number; // 👈️ 👈️ 👈️ no longer optional
    name: string;
    color: string;
    sortKey: any;
    data: [any];
  }

  const categoryCounts = dataForCharts.reduce<ReduceReturnType>(
    (accum, record) => {
      const key = record[categoryProp];
      const conceptRankName =
        formattedRank(record.conceptRank) + ':' + record.conceptName;
      const rec = accum[key]; // accum['Female']
      const categoryColorIndex = categoryVals.findIndex(
        (value) => value === key
      );
      if (!rec) {
        // 1st data record
        const seriesObj = {
          name: key,
          color: hcColors.hcColors[categoryProp][categoryColorIndex],
          sortKey: getCategorySortKey(domain, categoryProp, record),
          data: [
            {
              conceptRankName:
                formattedRank(record.conceptRank) + ':' + record.conceptName,
              conceptRank: record.conceptRank,
              conceptName: record.conceptName,
              category: categoryProp,
              categoryName: key,
              categoryCount: record.count,
            },
          ],
        };
        accum[key] = seriesObj;
        categoryTotal[key] = record.count;
      } else {
        const dataRec = accum[key].data.find(
          (item) =>
            formattedRank(item.conceptRank) + ':' + item.conceptName ===
            conceptRankName
        );
        if (!dataRec) {
          // next data record
          accum[key].data.push({
            conceptRankName:
              formattedRank(record.conceptRank) + ':' + record.conceptName,
            conceptRank: record.conceptRank,
            conceptName: record.conceptName,
            category: categoryProp,
            categoryName: key,
            categoryCount: record.count,
          });
        } else {
          dataRec.categoryCount += record.count;
        }
        categoryTotal[key] += record.count;
      }
      return accum;
    },
    {} as ReduceReturnType
  );
  // compute %
  // categoryCount * 100 / sum(categoryTotal)
  const sumCategoryTotal = Object.keys(categoryTotal).reduce((accum, key) => {
    return accum + categoryTotal[key];
  }, 0);
  const series = Object.values(categoryCounts)
    .reduce((accum, serObj) => {
      serObj.data.reduce((recAccum, rec) => {
        rec.categoryTotal = categoryTotal[serObj.name];
        rec.sumCategoryTotal = sumCategoryTotal;
        rec.x = categories.findIndex((cat) => rec.conceptRankName === cat);
        rec.y = (rec.categoryCount * 100) / sumCategoryTotal;
      }, []);
      accum.push(serObj);
      return accum;
    }, [])
    .sort((a, b) => (a.conceptRank > b.conceptRank ? -1 : 1));
  // set reversedStacks:false on yaxis
  const yaxis = getYAxis('Percent(%)');
  yaxis.reversedStacks = false;

  return {
    chart: {
      type: 'column',
      inverted: true,
    },
    title: {
      text: CHART_CATEGORY_KEY_NAME[categoryProp],
    },
    xAxis: [getXAxis(categories, false, 'Ranked Concept Name')],
    yAxis: [yaxis],
    legend: {
      layout: 'horizontal',
      // align: 'right',
      verticalAlign: 'top',
      itemMarginBottom: 5,
      useHTML: true,
      labelFormatter: function () {
        return this.name.slice(0, 15) + (this.name.length > 15 ? '...' : '');
      },
    },
    plotOptions: {
      series: {
        stacking: 'normal', // 'normal',
        events: {
          legendItemClick: function () {
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
        let tip = this.point.conceptRank + ': ' + this.point.conceptName;
        tip += this.point.categoryName + ', <br/>';
        tip += 'counts: (' + this.point.categoryCount;
        tip += '/' + this.point.sumCategoryTotal + ')<br/>';
        return tip + 'percent:' + parseFloat(this.point.y).toFixed(2) + '%';
      },
    },
    series: series,
  };
}

export function getChartMapParticipantCounts(
  dataForCharts: Array<ChartData>,
  domain: Domain,
  category: Category
) {
  const categoryProp = category.toString();
  let total = 0;
  const categoryCounts = dataForCharts
    .reduce((accum, record) => {
      const key =
        record[categoryProp] !== null ? record[categoryProp] : 'UNKNOWN';
      const rec = accum.find((item) => item.categoryName === key);
      if (!rec) {
        accum.push({
          category: categoryProp,
          categoryName: key,
          categoryCount: record.count,
          categorySortKey: key,
        });
      } else {
        rec.categoryCount += record.count;
      }
      total += record.count;
      return accum;
    }, [])
    .sort(
      (a, b) => a.x - b.x || a.categorySortKey.localeCompare(b.categorySortKey)
    );

  const seriesName = 'Participant count';
  const series = categoryCounts.reduce((accum, rec, i) => {
    rec.code = rec.categoryName;
    rec.value = rec.categoryCount;
    rec.valueFraction = (100 * rec.categoryCount) / total;
    rec.total = total;

    const seriesObj = accum.find((item) => item.name === seriesName);
    if (!seriesObj) {
      accum.push({
        name: seriesName,
        data: [rec],
        joinBy: ['hc-a2', 'code'],
        states: {
          hover: {
            color: '#BADA55',
          },
        },
        dataLabels: {
          enabled: true,
          format: '{point.properties.hc-a2}',
        },
      });
    } else {
      seriesObj.data.push(rec);
    }
    return accum;
  }, []);

  return {
    chart: {
      map: ustopo,
    },
    colors: [
      'rgba(19,64,117,0.125)',
      'rgba(19,64,117,0.25)',
      'rgba(19,64,117,0.375)',
      'rgba(19,64,117,0.5)',
      'rgba(19,64,117,0.625)',
      'rgba(19,64,117,0.75)',
      'rgba(19,64,117,0.875)',
      'rgba(19,64,117,1.0)',
    ],
    title: {
      text: seriesName,
    },
    mapNavigation: {
      enabled: true,
      buttonOptions: {
        verticalAlign: 'top',
      },
    },
    legend: {
      title: {
        text: 'Number of Participants',
        style: {
          color:
            // theme
            (highCharts.defaultOptions &&
              highCharts.defaultOptions.legend &&
              highCharts.defaultOptions.legend.title &&
              highCharts.defaultOptions.legend.title.style &&
              highCharts.defaultOptions.legend.title.style.color) ||
            'black',
        },
      },
      align: 'left',
      verticalAlign: 'bottom',
      floating: true,
      layout: 'vertical',
      valueDecimals: 0,
      backgroundColor:
        // theme
        (highCharts.defaultOptions &&
          highCharts.defaultOptions.legend &&
          highCharts.defaultOptions.legend.backgroundColor) ||
        'rgba(255, 255, 255, 0.85)',
      symbolRadius: 0,
      symbolHeight: 14,
    },
    colorAxis: {
      dataClasses: [
        {
          to: 100,
        },
        {
          from: 100,
          to: 200,
        },
        {
          from: 200,
          to: 500,
        },
        {
          from: 500,
          to: 1000,
        },
        {
          from: 1000,
          to: 50000,
        },
        {
          from: 5000,
          to: 10000,
        },
        {
          from: 10000,
          to: 20000,
        },
        {
          from: 20000,
        },
      ],
    },
    series: series,
  };
}

export function getChartMapBubbleParticipantCounts(
  dataForCharts: Array<ChartData>,
  domain: Domain,
  category: Category
) {
  const categoryProp = category.toString();
  let total = 0;
  const categoryCounts = dataForCharts
    .reduce((accum, record) => {
      const key =
        record[categoryProp] !== null ? record[categoryProp] : 'UNKNOWN';
      const rec = accum.find((item) => item.categoryName === key);
      if (!rec) {
        accum.push({
          category: categoryProp,
          categoryName: key,
          categoryCount: record.count,
          categorySortKey: key,
        });
      } else {
        rec.categoryCount += record.count;
      }
      total += record.count;
      return accum;
    }, [])
    .sort(
      (a, b) => a.x - b.x || a.categorySortKey.localeCompare(b.categorySortKey)
    );

  // find min-max of counts:
  const minCount = categoryCounts.reduce(
    (min, item) => (item.categoryCount < min ? item.categoryCount : min),
    20
  );
  const maxCount = categoryCounts.reduce(
    (max, item) => (item.categoryCount > max ? item.categoryCount : max),
    20
  );

  console.log(minCount, maxCount);
  const seriesName = 'Participant count';
  const series = categoryCounts.reduce((accum, rec, i) => {
    rec.code = rec.categoryName;
    rec.value = rec.categoryCount;
    rec.valueFraction = (100 * rec.categoryCount) / total;
    rec.total = total;
    // for bubble?
    rec.z = rec.categoryCount;

    const seriesObj = accum.find((item) => item.name === seriesName);
    if (!seriesObj) {
      accum.push(
        {
          name: 'States',
          color: '#FFB0E0',
          enableMouseTracking: false,
        },
        {
          type: 'mapbubble',
          maxSize: '12%',
          name: seriesName,
          data: [rec],
          joinBy: ['hc-a2', 'code'],
          states: {
            hover: {
              color: '#BADA55',
            },
          },
          dataLabels: {
            enabled: true,
            format: '{point.properties.hc-a2}',
          },
        }
      );
    } else {
      seriesObj.data.push(rec);
    }
    return accum;
  }, []);

  return {
    chart: {
      map: ustopo,
    },
    colors: [
      'rgba(19,64,117,0.125)',
      'rgba(19,64,117,0.25)',
      'rgba(19,64,117,0.375)',
      'rgba(19,64,117,0.5)',
      'rgba(19,64,117,0.625)',
      'rgba(19,64,117,0.75)',
      'rgba(19,64,117,0.875)',
      'rgba(19,64,117,1.0)',
    ],
    title: {
      text: seriesName,
    },
    mapNavigation: {
      enabled: true,
      buttonOptions: {
        verticalAlign: 'top',
      },
    },
    legend: {
      title: {
        text: 'Number of Participants',
        style: {
          color:
            // theme
            (highCharts.defaultOptions &&
              highCharts.defaultOptions.legend &&
              highCharts.defaultOptions.legend.title &&
              highCharts.defaultOptions.legend.title.style &&
              highCharts.defaultOptions.legend.title.style.color) ||
            'black',
        },
      },
      align: 'left',
      verticalAlign: 'bottom',
      floating: true,
      layout: 'vertical',
      valueDecimals: 0,
      backgroundColor:
        // theme
        (highCharts.defaultOptions &&
          highCharts.defaultOptions.legend &&
          highCharts.defaultOptions.legend.backgroundColor) ||
        'rgba(255, 255, 255, 0.85)',
      symbolRadius: 0,
      symbolHeight: 14,
    },
    colorAxis: {
      dataClasses: [
        {
          to: 100,
        },
        {
          from: 100,
          to: 200,
        },
        {
          from: 200,
          to: 500,
        },
        {
          from: 500,
          to: 1000,
        },
        {
          from: 1000,
          to: 50000,
        },
        {
          from: 5000,
          to: 10000,
        },
        {
          from: 10000,
          to: 20000,
        },
        {
          from: 20000,
        },
      ],
    },
    series: series,
  };
}
