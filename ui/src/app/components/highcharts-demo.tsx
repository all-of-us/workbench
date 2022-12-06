// For DT-32 demonstration plots
// original file from dolbeew/highcharts-demo -- src/app/components/highcharts-demo.tsx
// 1. create new route - routing - workspace-app-routing.tsx
import * as React from 'react';
import { RouteComponentProps, withRouter } from 'react-router-dom';
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
  queryHeader: {
    fontSize: '18px',
    fontWeight: 600,
    color: colors.primary,
    lineHeight: '24px',
  },
  queryTitle: {
    fontSize: '16px',
    fontWeight: 600,
    color: colors.primary,
    lineHeight: '22px',
  },
  queryContent: {
    fontSize: '13px',
    color: colors.primary,
    lineHeight: '30px',
    paddingBottom: '0.6rem',
  },
  containerMargin: {
    margin: 0,
    minWidth: '100%',
  },
  chartTitle: {
    marginLeft: '0.4rem',
    paddingBottom: '0.5rem',
    fontSize: '16px',
    fontWeight: 600,
    color: colors.primary,
    lineHeight: '22px',
  },
  graphBorder: {
    minHeight: '10rem',
    marginLeft: '23%',
    padding: '0.3rem',
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
}

export const DemoChart = fp.flow(withRouter)(
  class extends React.Component<Props, State> {
    constructor(props: Props) {
      super(props);
      this.state = {
        options: null,
        newChartData: null,
        chartPopPyramid: null,
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
      const { categories, seriesGenderMap } = this.getGenderByRaceChartData();
      this.setState({
        chartPopPyramid: this.extracted(categories, [
          seriesGenderMap.Male,
          seriesGenderMap.Female,
        ]),
      });

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
            genderHelper[record.gender] = { genderCount: record.count };
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
        if (gender === 'Male') {
          rec.raceCount = -rec.raceCount;
        }
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
        accum[rec.name] = rec;
        return accum;
      }, {});

      return { categories, seriesGenderMap };
    }

    extracted(ageCategories, genderSeries) {
      const height = Math.max(ageCategories.length * 60, 300);
      const width = height * genderSeries.length;
      const xAxis = [this.getXAxis(ageCategories, false, 'Age group')];
      if (genderSeries.length === 2) {
        xAxis.push(this.getXAxis(ageCategories, true, '', 0));
      }

      return {
        chart: {
          height,
          width,
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
          enabled: true,
        },
        plotOptions: {
          bar: {
            groupPadding: 0,
            pointPadding: 0.1,
          },
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
      const { chartPopPyramid } = this.state;
      return (
        <React.Fragment>
          <style>{css}</style>
          <div style={{ ...styles.container, margin: 0 }}>
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
        </React.Fragment>
      );
    }
  }
);
