// For DT-32 demonstration plots
// original file from dolbeew/highcharts-demo -- src/app/components/highcharts-new-gallery.tsx
// 1. create new route - routing - workspace-app-routing.tsx
import * as React from 'react';
import { RouteComponentProps } from 'react-router-dom';
import { cloneDeep } from 'lodash';
import * as highCharts from 'highcharts';
import HighchartsReact from 'highcharts-react-official';

import { ChartData, Domain } from 'generated/fetch';

import { getChartObj } from 'app/cohort-search/utils';
import { chartBuilderApi } from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import { reactStyles, withCurrentWorkspace } from 'app/utils';
import { MatchParams } from 'app/utils/stores';
import { WorkspaceData } from 'app/utils/workspace-data';

import {
  getCannedCategoryCounts,
  getCannedCategoryCountsByAgeBin,
} from './highcharts-canned';
import {
  Category,
  getAvailableCategories,
  getChartCategoryCounts,
  getChartCategoryCountsByAgeBin,
} from './highcharts-utils';

const css = `
  .stats-left-padding {
    padding-left: 6rem;
  }
  @media print{
    .doNotPrint{
      display:none !important;
      -webkit-print-color-adjust: exact;
    }
    .page-break {
      page-break-inside:auto;
    }
    .stats-left-padding {
      padding-left: 2rem;
    }
  }
`;
const styles = reactStyles({
  backBtn: {
    padding: 0,
    border: 0,
    fontSize: '14px',
    color: colors.accent,
    background: 'transparent',
    cursor: 'pointer',
  },
  container: {
    width: '100%',
    marginLeft: 'auto',
    marginRight: 'auto',
    // paddingLeft: '0.5rem',
    // paddingRight: '0.5rem',
  },
  row: {
    display: 'flex',
    flexWrap: 'wrap',
    // marginRight: '-.5rem',
    // marginLeft: '-.5rem',
  },
  col: {
    position: 'relative',
    minHeight: '1px',
    width: '100%',
    paddingLeft: '0.5rem',
    paddingRight: '0.5rem',
  },
  reportBackground: {
    backgroundColor: colors.white,
    paddingTop: '1rem',
    marginTop: '0.5rem',
  },
  chartTitle: {
    marginLeft: '0.4rem',
    paddingBottom: '0.5rem',
    fontSize: '16px',
    fontWeight: 600,
    color: colors.primary,
    lineHeight: '22px',
  },
});

export interface ChartProps extends RouteComponentProps<MatchParams> {
  domain?: string;
  cohortId?: any;
  workspace: WorkspaceData;
}

export interface ChartState {
  data: any;
  loading: boolean;
  options: any;
  chartType: any;
  newChartData: ChartData[];
  chartPopPyramid: any;
  chartsGenderRaceByAgeMap: {};
  categoryNames: {};
  categoryValues: {};
  categoryCountsRow: any;
  categoryCountsWidths: any;
  categoryCountsAgeBinRow: any;
  categoryCountsAgeBinWidths: any;
}

function getSeperatorDiv(seperatorText) {
  return (
    <div style={styles.row}>
      <div
        style={{
          ...styles.col,
          flex: '0 0 100%',
          maxWidth: '100%',
        }}
      >
        <hr style={{ ...styles.col, color: '#FF0000' }}></hr>
        <div style={styles.chartTitle}>
          <h1>{seperatorText}</h1>
        </div>
        <hr style={{ ...styles.col, color: '#FF00FF' }}></hr>
      </div>
    </div>
  );
}

export const Chart = withCurrentWorkspace()(
  class extends React.Component<ChartProps, ChartState> {
    constructor(props: ChartProps) {
      super(props);
      this.state = {
        data: null,
        loading: true,
        options: null,
        chartType: null,
        newChartData: null,
        chartPopPyramid: null,
        chartsGenderRaceByAgeMap: {},
        categoryNames: {
          gender: 'Gender',
          race: 'Race',
          ethnicity: 'Ethnicity',
          ageBin: 'Age Range',
          conceptName: 'Concept Name',
        },
        categoryValues: {},
        categoryCountsRow: null,
        categoryCountsWidths: null,
        categoryCountsAgeBinRow: null,
        categoryCountsAgeBinWidths: null,
      };
    }

    async componentDidMount() {
      // this.getChartDataOld();
      await this.doCharts();
    }

    async componentDidUpdate(prevProps: Readonly<ChartProps>) {
      const { domain } = this.props;

      if (domain && domain !== prevProps.domain) {
        this.setState({ loading: true });
        await this.doCharts();
      }
    }

    async doCharts() {
      // get new chart data
      const newChartData = await this.fetchChartData().then((result) => {
        return result.items;
      });

      // get available categories and their valueMap
      getAvailableCategories(newChartData);

      const cats = [
        Category.AgeBin,
        Category.Gender,
        Category.SexAtBirth,
        Category.Race,
        Category.Ethnicity,
      ];
      const { domain } = this.props;
      // demographics counts charts
      const cRow = [];
      if (domain) {
        Object.values(cats).forEach((category) => {
          cRow.push(
            getChartCategoryCounts(newChartData, Domain[domain], category)
          );
        });
      } else {
        Object.values(cats).forEach((category) => {
          cRow.push(
            getChartCategoryCounts(newChartData, Domain.PERSON, category)
          );
        });
      }
      const cWidths = await this.getChartWidths(cRow);
      this.setState({
        categoryCountsRow: cRow,
        categoryCountsWidths: cWidths,
      });
      this.setState({
        categoryCountsRow: cRow,
        categoryCountsWidths: cWidths,
      });
      // demographics counts-distribution-AgeBins charts
      const catsNoAgeBin = cats.filter(
        (category) => category !== Category.AgeBin
      );

      const cRow2 = [];
      if (domain) {
        Object.values(catsNoAgeBin).forEach((category) => {
          cRow2.push(
            getChartCategoryCountsByAgeBin(
              newChartData,
              Domain[domain],
              category
            )
          );
        });
      } else {
        Object.values(catsNoAgeBin).forEach((category) => {
          cRow2.push(
            getChartCategoryCountsByAgeBin(
              newChartData,
              Domain.PERSON,
              category
            )
          );
        });
      }

      const cWidths2 = await this.getChartWidths(cRow2);
      this.setState({
        categoryCountsAgeBinRow: cRow2,
        categoryCountsAgeBinWidths: cWidths2,
      });

      await this.getChartDataOld();
    }

    async fetchChartData() {
      const {
        domain,
        cohortId,
        workspace: { id, namespace },
      } = this.props;

      return chartBuilderApi().getChartData(namespace, id, +cohortId, domain);
    }

    async getChartDataOld() {
      const {
        domain,
        cohortId,
        workspace: { id, namespace },
      } = this.props;

      const newChartData = await chartBuilderApi().getChartData(
        namespace,
        id,
        +cohortId,
        domain
      );
      this.setState({
        newChartData: newChartData.items,
        chartType: domain ? domain + ' Chart' : 'Demographics Chart',
      });

      const { categories, seriesGenderMap, genderHelper } =
        this.getGenderByRaceChartData();

      // change y values for Female to be negative for Population Chart
      // make a copy without changing the original
      const F = cloneDeep(seriesGenderMap.F);
      F.data.reduce((accum, rec) => {
        rec.y = -rec.y;
        accum.push(rec);
        return accum;
      }, []);
      this.setState({
        chartPopPyramid: this.getGenderRaceByAgeChart(
          categories,
          [F, seriesGenderMap.M],
          true
        ),
      });
      // after pyramid plot
      const chartsGenderRaceByAgeMap = {};
      for (const key of Object.keys(genderHelper)) {
        chartsGenderRaceByAgeMap[key] = this.getGenderRaceByAgeChart(
          categories,
          [seriesGenderMap[genderHelper[key].genderKey]]
        );
      }
      this.setState({ chartsGenderRaceByAgeMap, loading: false });
    }

    getGenderByRaceChartData() {
      const { newChartData } = this.state;
      const categories = Array.from(
        new Set(newChartData.map((dat) => dat.ageBin))
      ).sort((a, b) => (a > b ? 1 : -1));

      const races = Array.from(
        new Set(newChartData.map((dat) => dat.race))
      ).sort((a, b) => (a > b ? -1 : 1));
      const raceColorMap = {};
      races.map((r, i) => (raceColorMap[r] = colors.chartColors[i]));

      const getComboKey = (record) => {
        return `${record.gender} ${record.ageBin} ${record.race}`;
      };

      const ageGenderRaceHelper = {};
      const genderHelper = {};
      const seriesHelper = newChartData
        .reduce((accum, record) => {
          const key = getComboKey(record);
          if (!genderHelper[record.gender]) {
            genderHelper[record.gender] = {
              genderCount: record.count,
              genderKey: record.gender.startsWith('Not')
                ? 'O'
                : record.gender[0],
            };
          } else {
            genderHelper[record.gender].genderCount += record.count;
          }
          const index = categories.indexOf(record.ageBin);
          if (!ageGenderRaceHelper[key]) {
            ageGenderRaceHelper[key] = {
              x: index,
              raceCount: record.count,
              gender: record.gender,
              race: record.race,
            };
            accum.push(ageGenderRaceHelper[key]);
          } else {
            ageGenderRaceHelper[key].raceCount += record.count;
          }
          return accum;
        }, [])
        .sort((a, b) => a.x - b.x || a.race.localeCompare(b.race));

      const series = seriesHelper.reduce((accum, rec) => {
        const gender = rec.gender;
        rec.genderCount = genderHelper[rec.gender].genderCount;
        rec.y = (100.0 * rec.raceCount) / rec.genderCount;
        rec.color = raceColorMap[rec.race];

        const index = accum.findIndex((d) => d.name === gender);
        if (index === -1) {
          accum.push({ name: rec.gender, data: [rec] });
        } else {
          accum[index].data.push(rec);
        }
        return accum;
      }, []);

      const seriesGenderMap = series.reduce((accum, rec) => {
        const genderKey = rec.name.startsWith('Not') ? 'O' : rec.name[0];
        accum[genderKey] = rec;
        return accum;
      }, {});

      return { categories, seriesGenderMap, races, genderHelper };
    }

    getGenderRaceByAgeChart(ageCategories, genderSeries, isAxesInverted?) {
      const xAxis = [this.getXAxis(ageCategories, false, 'Age group')];
      // pop pyramid plot of the 2-genderSeries
      if (genderSeries.length === 2) {
        xAxis.push(this.getXAxis(ageCategories, true, '', 0));
      }
      const yAxis = [this.getYAxis()];

      return {
        chart: {
          type: 'column',
          inverted: !!isAxesInverted,
        },
        title: {
          text: '',
        },
        xAxis: xAxis,
        yAxis: yAxis,
        legend: {
          enabled: false,
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
              this.series.name +
              ' (' +
              this.point.category +
              ')' +
              ', race: ' +
              this.point.race +
              '</b><br/>' +
              'count: ' +
              Math.abs(this.point.raceCount) +
              ' / ' +
              Math.abs(this.point.genderCount) +
              ' (' +
              this.series.name +
              ')' +
              '</b><br/>' +
              'percent: ' +
              highCharts.numberFormat(Math.abs(this.point.y), 2) +
              '%'
            );
          },
        },
        series: genderSeries,
      };
    }

    private getYAxis() {
      return {
        title: {
          text: '',
        },
        labels: {
          formatter: function () {
            return Math.abs(this.value) + '%';
          },
        },
      };
    }
    private getXAxis(ageCategories, rightSide, titleText?, linkedToVal?) {
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

    async getChartWidths(chartRow) {
      let totalBars = 0;
      chartRow.forEach(
        (chart) => (totalBars += chart.xAxis[0].categories.length + 1)
      );
      const chartWidths = [];
      chartRow.forEach((chart) => {
        chartWidths.push(
          ((chart.xAxis[0].categories.length + 1) * 100) / totalBars
        );
      });

      return chartWidths;
    }

    render() {
      const { chartPopPyramid, chartsGenderRaceByAgeMap } = this.state;
      const swapped = cloneDeep(chartsGenderRaceByAgeMap);
      // change chart.inverted:true
      Object.keys(swapped).map((key) => {
        swapped[key].chart.inverted = true;
      });
      const { domain } = this.props;
      const { categoryCountsRow, categoryCountsWidths } = this.state;
      const { categoryCountsAgeBinRow, categoryCountsAgeBinWidths } =
        this.state;

      return (
        <React.Fragment>
          <style>{css}</style>
          <div style={{ ...styles.container, margin: 0 }}>
            {getSeperatorDiv('Canned Frequency counts')}
            <div style={styles.row}>
              <div
                style={{
                  ...styles.col,
                  flex: '0 0 30%',
                  maxWidth: '30%',
                }}
              >
                <HighchartsReact
                  highcharts={highCharts}
                  options={getCannedCategoryCounts()}
                  callback={getChartObj}
                />
              </div>
              <div
                style={{
                  ...styles.col,
                  flex: '0 0 30%',
                  maxWidth: '30%',
                }}
              >
                <HighchartsReact
                  highcharts={highCharts}
                  options={getCannedCategoryCountsByAgeBin()}
                  callback={getChartObj}
                />
              </div>
            </div>
            {getSeperatorDiv(
              'Demographics Frequency counts - one category (' + domain + ')'
            )}
            <div style={styles.row}>
              {categoryCountsRow &&
                Object.values(categoryCountsRow).map((value, index) => (
                  <div
                    key={index}
                    style={{
                      ...styles.col,
                      flex: '0 0 ' + categoryCountsWidths[index] + '%',
                      maxWidth: categoryCountsWidths[index] + '%',
                    }}
                  >
                    <HighchartsReact
                      highcharts={highCharts}
                      options={value}
                      callback={getChartObj}
                    />
                  </div>
                ))}
            </div>
            {getSeperatorDiv(
              'Demographics Frequency/ counts Distribution by Age Range - one category (' +
                domain +
                ')'
            )}
            <div style={styles.row}>
              {categoryCountsAgeBinRow &&
                Object.values(categoryCountsAgeBinRow).map((value2, index2) => (
                  <div
                    key={index2}
                    style={{
                      ...styles.col,
                      flex: '0 0 ' + categoryCountsAgeBinWidths[index2] + '%',
                      maxWidth: categoryCountsAgeBinWidths[index2] + '%',
                    }}
                  >
                    <HighchartsReact
                      highcharts={highCharts}
                      options={value2}
                      callback={getChartObj}
                    />
                  </div>
                ))}
            </div>
            <div style={styles.row}>
              {chartsGenderRaceByAgeMap &&
                Object.keys(chartsGenderRaceByAgeMap).map((key, index) => (
                  <div
                    key={index}
                    style={{
                      ...styles.col,
                      flex: '0 0 33%',
                      maxWidth: '33%',
                    }}
                  >
                    <div>
                      <span style={styles.chartTitle}>{key}</span>
                    </div>
                    <HighchartsReact
                      highcharts={highCharts}
                      options={chartsGenderRaceByAgeMap[key]}
                      callback={getChartObj}
                    />
                  </div>
                ))}
            </div>
            <hr></hr>
            <div style={styles.row}>
              {swapped &&
                Object.keys(swapped).map((key, index) => (
                  <div
                    key={index}
                    style={{
                      ...styles.col,
                      flex: '0 0 33%',
                      maxWidth: '33%',
                    }}
                  >
                    <div>
                      <span style={styles.chartTitle}>{key}</span>
                    </div>
                    <HighchartsReact
                      highcharts={highCharts}
                      options={swapped[key]}
                      callback={getChartObj}
                    />
                  </div>
                ))}
            </div>
            <hr></hr>
            <div style={styles.row}>
              <div
                style={{
                  ...styles.col,
                  flex: '0 0 100%',
                  maxWidth: '100%',
                }}
              >
                {chartPopPyramid && (
                  <HighchartsReact
                    highcharts={highCharts}
                    options={chartPopPyramid}
                    callback={getChartObj}
                  />
                )}
              </div>
            </div>
          </div>
        </React.Fragment>
      );
    }
  }
);
