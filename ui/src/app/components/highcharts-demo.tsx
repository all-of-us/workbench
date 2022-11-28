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
  Cohort,
  CohortDefinition,
  DemoChartInfo,
  GenderOrSexType,
} from 'generated/fetch';

import { getChartObj } from 'app/cohort-search/utils';
import {
  cohortBuilderApi,
  cohortsApi,
} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import { MatchParams } from 'app/utils/stores';
import { WorkspaceData } from 'app/utils/workspace-data';

import { WithSpinnerOverlayProps } from './with-spinner-overlay';

export interface Props
  extends WithSpinnerOverlayProps,
    RouteComponentProps<MatchParams> {
  cdrVersionTiersResponse: CdrVersionTiersResponse;
  cohort: Cohort;
  workspace: WorkspaceData;
}

interface State {
  demoChartData: DemoChartInfo[];
  options: any;
}

export const DemoChart = fp.flow(withRouter)(
  class extends React.Component<Props, State> {
    constructor(props: Props) {
      super(props);
      this.state = { demoChartData: null, options: null };
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
      this.setState({ demoChartData: demoChartData.items });
      this.getChartOptions();
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
      return { categories, series };
    }

    render() {
      const { options } = this.state;
      return (
        <div>
          <div style={{ minHeight: 200 }}>
            {options && (
              <HighchartsReact
                highcharts={highCharts}
                options={options}
                callback={getChartObj}
              />
            )}
          </div>
          <div style={{ minHeight: 200 }}>
            {options && (
              <HighchartsReact
                highcharts={highCharts}
                options={options}
                callback={getChartObj}
              />
            )}
          </div>
        </div>
      );
    }
  }
);
