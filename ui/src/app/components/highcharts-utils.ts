import { forEach, forIn } from 'lodash';
import * as highCharts from 'highcharts';

import { ChartData, Domain } from 'generated/fetch';

import hcColors from './highcharts-colors';

export enum Category {
  Gender = 'gender',
  SexAtBirth = 'sexAtBirth',
  Race = 'race',
  Ethnicity = 'ethnicity',
  AgeBin = 'ageBin',
  ConceptName = 'conceptName',
  NumConceptsCoOccur = 'numConceptsCoOccur',
}

const CHART_CATEGORY_KEY_NAME = {
  gender: 'Gender',
  sexAtBirth: 'Sex At Birth',
  race: 'Race',
  ethnicity: 'Ethnicity',
  ageBin: 'Age Range',
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
    ).sort((a, b) => (a > b ? 1 : -1));
  });
  return { categoryNames, categoryValues };
}

export function formattedRank(rank: number) {
  const fr = '00' + String(rank);
  return fr.slice(fr.length - 2);
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
    return `${(formattedRank(record.conceptRank), record.conceptName)}`;
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
  };
}

export function getChartCategoryCounts(
  dataForCharts: Array<ChartData>,
  domain: Domain,
  category: Category
) {
  const categoryProp = category.toString();
  let total = 0;
  const categoryCounts = dataForCharts
    .reduce((accum, record) => {
      const key = record[categoryProp];
      const rec = accum.find((item) => item.categoryName === key);
      // console.log('key:',key,'rec:',rec);
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
    rec.color = hcColors.hcColors[categoryProp][i];
    accum.push({
      name: rec.categoryName,
      color: rec.color,
      data: [rec],
    });
    return accum;
  }, []);
  // console.log('series:', series);

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
  category: Category
) {
  const categories = Array.from(
    new Set(dataForCharts.map((dat) => dat.ageBin))
  ).sort((a, b) => (a > b ? 1 : -1));

  const categoryProp = category.toString();
  const total = {};
  let categoryColorIndex = -1;
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
      // console.log('key:',key,'rec:',rec);
      if (!rec) {
        // 1st data record
        categoryColorIndex += 1;
        const seriesObj = {
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
        accum[key] = seriesObj;
        total[key] = record.count;
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
        total[key] += record.count;
      }
      return accum;
    },
    {} as ReduceReturnType
  );
  // compute %
  // categoryTotal * 100 / sum(categoryTotal)
  Object.values(categoryCounts).reduce((accum, serObj) => {
    serObj.data.reduce((recAccum, rec) => {
      rec.categoryTotal = total[serObj.name];
      rec.x = categories.findIndex((cat) => rec.ageBin === cat);
      rec.y = (rec.categoryCount * 100) / total[serObj.name];
    }, []);
  }, {});
  const series = Object.values(categoryCounts);

  return {
    chart: {
      type: 'column',
      inverted: true,
    },
    title: {
      text: CHART_CATEGORY_KEY_NAME[categoryProp],
    },
    xAxis: [getXAxis(categories, false, 'Age Range(Y)')],
    yAxis: [getYAxis('Percent(%)')],
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
    series: series,
  };
}
