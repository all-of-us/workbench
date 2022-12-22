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
  for (const key in CHART_CATEGORY_KEY_NAME) {
    categoryNames[key] = CHART_CATEGORY_KEY_NAME[key];
    categoryValues[key] = Array.from(
      new Set(dataForCharts.map((dat) => dat[key]))
    ).sort((a, b) => (a > b ? 1 : -1));
  }
  console.log('categoryNames:', categoryNames);
  console.log('categoryValueMap:', categoryValues);
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

export function getChartCategoryCounts(
  dataForCharts: Array<ChartData>,
  domain: Domain,
  category: Category
) {
  const categoryProp = category.toString();
  let total = 0;
  let categoryCounts = [];
  categoryCounts = dataForCharts
    .reduce((accum, record) => {
      const key = record[categoryProp];
      const rec = accum.find((rec) => rec.categoryName === key);
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
  console.log('categoryCounts:', categoryCounts);

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
  console.log('series:', series);

  const chart = {
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
  return chart;
}

function getXAxis(ageCategories, rightSide, titleText?, linkedToVal?) {
  return {
    title: {
      text: titleText ? titleText : '',
    },
    categories: ageCategories,
    opposite: rightSide,
    reversed: false,
    labels: {
      step: 1,
    },
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
