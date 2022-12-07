// For DT-32 demonstration plots
// original file from dolbeew/highcharts-demo -- src/app/components/highcharts-demo.tsx
// 1. create new route - routing - workspace-app-routing.tsx
import * as React from 'react';
import { RouteComponentProps, withRouter } from 'react-router-dom';
import { forEach } from 'lodash';
import * as fp from 'lodash/fp';
import * as highCharts from 'highcharts';
import HighchartsReact from 'highcharts-react-official';

import {
  CdrVersionTiersResponse,
  ChartData,
  Cohort,
  CohortDefinition,
} from 'generated/fetch';

import { getChartObj } from 'app/cohort-search/utils';
import {
  chartBuilderApi,
  cohortsApi,
} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import { MatchParams } from 'app/utils/stores';
import { WorkspaceData } from 'app/utils/workspace-data';

import { WithSpinnerOverlayProps } from './with-spinner-overlay';

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
    paddingLeft: '0.5rem',
    paddingRight: '0.5rem',
  },
  row: {
    display: 'flex',
    flexWrap: 'wrap',
    marginRight: '-.5rem',
    marginLeft: '-.5rem',
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
export interface Props
  extends WithSpinnerOverlayProps,
    RouteComponentProps<MatchParams> {
  cdrVersionTiersResponse: CdrVersionTiersResponse;
  cohort: Cohort;
  workspace: WorkspaceData;
}

interface State {
  newChartData: ChartData[];
  options: any;
  chartPopPyramid: any;
  chartPopPyramidM: any;
  chartPopPyramidF: any;
  chartPopPyramidO: any;
  chartsGenderRaceByAgeMap: {};
}

export const DemoChart = fp.flow(withRouter)(
  class extends React.Component<Props, State> {
    constructor(props: Props) {
      super(props);
      this.state = {
        options: null,
        newChartData: null,
        chartPopPyramid: null,
        chartPopPyramidM: null,
        chartPopPyramidF: null,
        chartPopPyramidO: null,
        chartsGenderRaceByAgeMap: {},
      };
    }

    async componentDidMount(): Promise<void> {
      const { hideSpinner } = this.props;
      // call ot get cohort
      // const cohortDefinition = await this.getCohort();
      // all api to get chart data
      const { ns, wsid } = this.props.match.params;
      // const [demoChartInfo, ethnicityInfo, participantCount] = await Promise.all([
      //   cohortBuilderApi().findDemoChartInfo(
      //     ns,
      //     wsid,
      //     GenderOrSexType[GenderOrSexType.GENDER],
      //     AgeType[AgeType.AGE],
      //     cohortDefinition
      //   ),
      // cohortBuilderApi().findEthnicityInfo(ns, wsid, cohortDefinition),
      // cohortBuilderApi().countParticipants(ns, wsid, cohortDefinition),
      //    ]);
      // const demoChartData = await cohortBuilderApi().findDemoChartInfo(
      //   ns,
      //   wsid,
      //   GenderOrSexType[GenderOrSexType.GENDER],
      //   AgeType[AgeType.AGE],
      //   cohortDefinition
      // );
      const newChartData = await chartBuilderApi().getChartData(ns, wsid, 1);
      this.setState({
        newChartData: newChartData.items,
      });
      const { categories, seriesGenderMap, races, genderHelper } =
        this.getGenderByRaceChartData();
      // change y values for Male to be negative for Population Chart
      const M = JSON.parse(JSON.stringify(seriesGenderMap.M));
      M.data.reduce((accum, rec) => {
        rec.y = -rec.y;
        accum.push(rec);
        return accum;
      }, []);
      this.setState({
        chartPopPyramid: this.extracted(
          categories,
          [M, seriesGenderMap.F],
          races
        ),
      });
      // after pyramid plot
      const chartsGenderRaceByAgeMap = {};
      for (const key of Object.keys(genderHelper)) {
        chartsGenderRaceByAgeMap[key] = this.extracted(
          categories,
          seriesGenderMap[genderHelper[key].genderKey],
          races
        );
      }
      console.log('chartsGenderRaceByAgeMap', chartsGenderRaceByAgeMap);
      this.setState({ chartsGenderRaceByAgeMap });

      this.setState({
        chartPopPyramidM: this.extracted(
          categories,
          [seriesGenderMap.M],
          races
        ),
      });
      this.setState({
        chartPopPyramidF: this.extracted(
          categories,
          [seriesGenderMap.F],
          races
        ),
      });
      this.setState({
        chartPopPyramidO: this.extracted(
          categories,
          [seriesGenderMap.O],
          races
        ),
      });
      const m = this.state.chartPopPyramidM;
      console.log(m.name);
      hideSpinner();
    }

    async getCohort() {
      const { ns, wsid, cid } = this.props.match.params;
      let cohortDef: CohortDefinition;
      await cohortsApi()
        .getCohort(ns, wsid, +cid)
        .then(async (cohortResponse) => {
          cohortDef = JSON.parse(cohortResponse.criteria);
        });
      return cohortDef;
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
        .sort((a, b) => a.x - b.x);

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

    extracted(ageCategories, genderSeries, races) {
      const height = Math.max(ageCategories.length * 60, 300);
      // const width = height * genderSeries.length;
      const xAxis = [this.getXAxis(ageCategories, false, 'Age group')];
      if (genderSeries.length === 2) {
        xAxis.push(this.getXAxis(ageCategories, true, '', 0));
      } else if (genderSeries.length === 3) {
        xAxis.push(this.getXAxis(ageCategories, false, '', 0));
        xAxis.push(this.getXAxis(ageCategories, true, '', 0));
      }

      return {
        chart: {
          height,
          type: 'bar',
        },
        title: {
          text: '',
        },
        accessibility: {
          point: {
            valueDescriptionFormat:
              '{index} Age {xDescription} Race {race} Count {raceCount} GenderCount {genderCount} Percent {y}',
          },
        },
        xAxis: xAxis,
        yAxis: {
          title: {
            text: '',
          },
          labels: {
            formatter: function () {
              return Math.abs(this.value) + '%';
            },
          },
          accessibility: {
            description: 'Percentage population',
            rangeDescription: 'Range: 0 to 5%',
          },
        },
        legend: {
          align: 'center',
          verticalAlign: 'middle',
          itemMarginBottom: 10,
          x: -15,
          y: 0,
          labelFormatter: function () {
            return races[2];
          },
        },
        plotOptions: {
          bar: {
            groupPadding: 0,
            pointPadding: 0.1,
          },
          showInLegend: true,
          series: {
            stacking: 'normal', // 'normal',
          },
        },
        tooltip: {
          formatter: function () {
            return (
              '<b>' +
              this.series.name +
              ', age ' +
              this.point.category +
              ', race ' +
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

    render() {
      const {
        chartPopPyramid,
        chartPopPyramidM,
        chartPopPyramidF,
        chartPopPyramidO,
        chartsGenderRaceByAgeMap,
      } = this.state;
      const chartMap = JSON.parse(JSON.stringify(chartsGenderRaceByAgeMap));
      return (
        <React.Fragment>
          <style>{css}</style>
          <div style={{ ...styles.container, margin: 0 }}>
            <div>
              <span style={styles.chartTitle}>Population Pyramid</span>
            </div>
            <div style={{ minHeight: 200 }}>
              {chartPopPyramid && (
                <HighchartsReact
                  highcharts={highCharts}
                  options={chartPopPyramid}
                  callback={getChartObj}
                />
              )}
            </div>
          </div>
          <div style={{ ...styles.container, margin: 0 }}>
            <div>
              <span style={styles.chartTitle}>
                Race by gender over age groups
              </span>
            </div>
            <div style={styles.row}>
              <div
                style={{
                  ...styles.col,
                  flex: '0 0 33%',
                  maxWidth: '33%',
                }}
              >
                <div>
                  <span style={styles.chartTitle}>Male</span>
                </div>
                {chartPopPyramidM && (
                  <HighchartsReact
                    highcharts={highCharts}
                    options={chartPopPyramidM}
                    callback={getChartObj}
                  />
                )}
              </div>
              <div
                style={{
                  ...styles.col,
                  flex: '0 0 33%',
                  maxWidth: '33%',
                }}
              >
                <div>
                  <span style={styles.chartTitle}>Male</span>
                </div>
                {chartPopPyramidF && (
                  <HighchartsReact
                    highcharts={highCharts}
                    options={chartPopPyramidF}
                    callback={getChartObj}
                  />
                )}
              </div>
              <div
                style={{
                  ...styles.col,
                  flex: '0 0 33%',
                  maxWidth: '33%',
                }}
              >
                <div>
                  <span style={styles.chartTitle}>Not man only,...</span>
                </div>
                {chartPopPyramidO && (
                  <HighchartsReact
                    highcharts={highCharts}
                    options={chartPopPyramidO}
                    callback={getChartObj}
                  />
                )}
              </div>
            </div>
          </div>
          <div style={{ ...styles.container, margin: 0 }}>
            <div>
              <span style={styles.chartTitle}>
                Race by gender over age groups
              </span>
            </div>
            <div style={styles.row}>
              {Object.keys(chartMap).map((key) => (
                <div
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
                    options={chartMap[key]}
                    callback={getChartObj}
                  />
                </div>
              ))}
            </div>
          </div>
        </React.Fragment>
      );
    }
  }
);
