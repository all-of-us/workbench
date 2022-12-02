// For DT-32 demonstration plots
// original file from dolbeew/highcharts-demo -- src/app/components/highcharts-demo.tsx
// 1. create new route - routing - workspace-app-routing.tsx
import * as React from 'react';
import { RouteComponentProps, withRouter } from 'react-router-dom';
import * as fp from 'lodash/fp';
import * as highCharts from 'highcharts';
import HighchartsReact from 'highcharts-react-official';

import {
  AgeType,
  CdrVersionTiersResponse,
  ChartData,
  Cohort,
  CohortDefinition,
  DemoChartInfo,
  GenderOrSexType,
} from 'generated/fetch';

import { getChartObj } from 'app/cohort-search/utils';
import {
  chartBuilderApi,
  cohortBuilderApi,
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
  demoChartData: DemoChartInfo[];
  newChartData: ChartData[];
  options: any;
  newOptions: any;
}

export const DemoChart = fp.flow(withRouter)(
  class extends React.Component<Props, State> {
    constructor(props: Props) {
      super(props);
      this.state = {
        demoChartData: null,
        options: null,
        newChartData: null,
        newOptions: null,
      };
    }

    async componentDidMount(): Promise<void> {
      const { hideSpinner } = this.props;
      // call ot get cohort
      const cohortDefinition = await this.getCohort();
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
      const demoChartData = await cohortBuilderApi().findDemoChartInfo(
        ns,
        wsid,
        GenderOrSexType[GenderOrSexType.GENDER],
        AgeType[AgeType.AGE],
        cohortDefinition
      );
      const newChartData = await chartBuilderApi().getChartData(ns, wsid, 1);
      this.setState({
        demoChartData: demoChartData.items,
        newChartData: newChartData.items,
      });
      this.getChartOptions();
      this.getNewChartOptions();
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

    getChartOptions() {
      const normalized = 'normalized';
      const { categories, series } = this.getCategoriesAndSeries();
      console.log('categories:', categories);
      console.log('series:', series);

      const height = Math.max(categories.length * 30, 200);
      const options = {
        chart: {
          height,
          type: 'bar',
        },
        credits: {
          enabled: false,
        },
        title: {
          text: '',
        },
        xAxis: {
          categories,
          tickLength: 0,
          tickPixelInterval: 50,
        },
        yAxis: {
          labels: {
            format: '{value}' + (normalized ? '%' : ''),
          },
          min: 0,
          title: {
            text: '',
          },
        },
        colors: colors.chartColors,
        legend: {
          enabled: false,
        },
        plotOptions: {
          bar: {
            groupPadding: 0,
            pointPadding: 0.1,
          },
          series: {
            stacking: normalized ? 'percent' : 'normal',
          },
        },
        series,
      };
      this.setState({ options });
    }

    getCategoriesAndSeries() {
      const { demoChartData } = this.state;
      const codeMap = {
        M: 'Male',
        F: 'Female',
        'No matching concept': 'Unknown',
      };
      const getKey = (dat) => {
        const gender = !!codeMap[dat.name] ? codeMap[dat.name] : dat.name;
        return `${gender} ${dat.ageRange || 'Unknown'}`;
      };
      const categories = demoChartData
        ?.reduce((acc, datum) => {
          const key = getKey(datum);
          if (!acc.includes(key)) {
            acc.push(key);
          }
          return acc;
        }, [])
        .sort((a, b) => (a > b ? 1 : -1));
      const series = demoChartData
        .reduce((acc, datum) => {
          const key = getKey(datum);
          const obj = { x: categories.indexOf(key), y: datum.count };
          const index = acc.findIndex((d) => d.name === datum.race);
          if (index === -1) {
            acc.push({ name: datum.race, data: [obj] });
          } else {
            acc[index].data.push(obj);
          }
          return acc;
        }, [])
        .sort((a, b) => (a.name < b.name ? 1 : -1));
      console.log('categories:', categories);
      console.log('series:', series);

      return { categories, series };
    }

    getNewChartOptions() {
      const normalized = '';
      const { categories, series } = this.getCategoriesAndSeries2();

      const height = Math.max(categories.length * 30, 600);
      const newOptions = {
        chart: {
          height,
          type: 'bar',
        },
        title: {
          text: 'Population pyramid for CDR',
        },
        accessibility: {
          point: {
            valueDescriptionFormat: '{index}. Age {xDescription} Race {race}',
          },
        },
        xAxis: [
          {
            categories: categories,
            reversed: false,
            labels: {
              step: 1,
            },
            accessibility: {
              description: 'Age (male)',
            },
          },
          {
            // mirror axis on right side
            opposite: true,
            reversed: false,
            categories: categories,
            linkedTo: 0,
            labels: {
              step: 1,
            },
            accessibility: {
              description: 'Age (female)',
            },
          },
        ],
        yAxis: {
          title: {
            text: null,
          },
          labels: {
            formatter: function () {
              return Math.abs(this.value);
            },
          },
          accessibility: {
            description: 'Percentage population',
            rangeDescription: 'Range: 0 to 5%',
          },
        },
        colors: colors.chartColors,
        legend: {
          enabled: false,
        },
        plotOptions: {
          bar: {
            groupPadding: 0,
            pointPadding: 0.1,
          },
          series: {
            stacking: normalized ? 'percent' : 'normal',
          },
        },
        tooltip: {
          formatter: function () {
            return (
              '<b>' +
              this.series.name +
              ', age ' +
              this.point.category +
              ', race ' + this.point.race +
              '</b><br/>' +
              'Population: ' +
              highCharts.numberFormat(Math.abs(this.point.y), 1)
            );
          },
        },
        series,
      };
      this.setState({ newOptions });
    }

    getCategoriesAndSeries2() {
      const categories = ['20-24', '25-29', '30-34', '35-40', '40-45'];
      const series = [
        {
          name: 'Male',
          data: [
            { x: 0, y: -25, race:'W' },
            { x: 0, y: -15, race:'B' },
            { x: 0, y: -10, race:'A' },
            { x: 0, y: -5, race:'U' },
            { x: 1, y: -15, race:'W' },
            { x: 1, y: -9, race:'B' },
            { x: 1, y: -7, race:'A' },
            { x: 1, y: -1, race:'U' },
          ],
        },
        {
          name: 'Female',
          data: [50, 40, 30, 20, 10],
        },
      ];

      return { categories, series };
    }

    render() {
      const { options, newOptions } = this.state;
      return (
        <React.Fragment>
          <style>{css}</style>
          <div style={{ ...styles.container, margin: 0 }}>
            <div>
              <div style={{ minHeight: 200, width: 800 }}>
                {options && (
                  <HighchartsReact
                    highcharts={highCharts}
                    options={options}
                    callback={getChartObj}
                  />
                )}
              </div>
              <div style={{ minHeight: 200, width: 800 }}>
                {newOptions && (
                  <HighchartsReact
                    highcharts={highCharts}
                    options={newOptions}
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
