import * as React from 'react';
import { useEffect, useState } from 'react';
import * as highCharts from 'highcharts';
import highchartsFunnel from 'highcharts/modules/funnel';
import HighchartsReact from 'highcharts-react-official';

import {
  AgeType,
  CohortDefinition,
  GenderSexRaceOrEthType,
} from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { SpinnerOverlay } from 'app/components/spinners';
import { searchRequestStore } from 'app/pages/data/cohort/search-state.service';
import { getChartObj } from 'app/pages/data/cohort/utils';
import { cohortBuilderApi } from 'app/services/swagger-fetch-clients';
import { withCurrentWorkspace } from 'app/utils';
import { WorkspaceData } from 'app/utils/workspace-data';

highchartsFunnel(highCharts);
highCharts.setOptions({
  lang: {
    decimalPoint: '.',
    thousandsSep: ',',
  },
});

export const FunnelPlotTest = withCurrentWorkspace()(
  ({
    onExit,
    cohortDefinition,
    totalCount,
    workspace: { namespace, id },
  }: {
    onExit: () => void;
    cohortDefinition: CohortDefinition;
    totalCount: number;
    workspace: WorkspaceData;
  }) => {
    const [loading, setLoading] = useState(true);
    const [options, setOptions] = useState(null);

    const getAllCounts = async () => {
      let funnelGroups;
      const cohortDefinitionStore = searchRequestStore.getValue();
      // Condition is intentionally false, using mock data below until nulls in request are fixed
      if (cohortDefinitionStore.includes.length < 0) {
        const counts = await Promise.all(
          cohortDefinitionStore.includes.slice(1).map((group, index) => {
            const searchRequest = {
              ...cohortDefinitionStore,
              includes: cohortDefinitionStore.includes.slice(index),
            };
            return cohortBuilderApi().findDemoChartInfo(
              namespace,
              id,
              GenderSexRaceOrEthType.RACE.toString(),
              AgeType.AGEATCDR.toString(),
              searchRequest as CohortDefinition
            );
          })
        );
        funnelGroups = [
          [cohortDefinitionStore.includes[0].name, totalCount],
          ...counts.map((count, index) => [
            cohortDefinitionStore.includes[index + 1].name,
            count,
          ]),
        ];
      } else {
        funnelGroups = [
          ['Group 1', 86731],
          ['Group 2', 59273],
          ['Group 3', 41462],
          ['Group 4', 35465],
        ];
      }
      const chartOptions = {
        chart: {
          backgroundColor: 'none',
          height: 700,
          type: 'funnel',
        },
        credits: {
          enabled: false,
        },
        title: {
          text: '',
        },
        legend: {
          enabled: false,
        },
        plotOptions: {
          series: {
            dataLabels: {
              enabled: true,
              format: '<b>{point.name}</b> ({point.y:,.0f})',
              softConnector: true,
            },
            center: ['40%', '50%'],
            neckWidth: '30%',
            neckHeight: '25%',
            width: '80%',
          },
        },
        series: [
          {
            name: 'Participant Count',
            data: funnelGroups,
            tooltip: {
              pointFormat:
                '<span style="color:{point.color}">\u25CF </span><b> {point.y}</b>',
            },
          },
        ],
      };
      setOptions(chartOptions);
      setLoading(false);
    };

    useEffect(() => {
      if (searchRequestStore.getValue()) {
        getAllCounts();
      }
    }, [cohortDefinition]);

    // const getChartOptions = () => {
    //   const { categories, data } = getCategoriesAndData();
    //   const height = Math.max(categories.length * 30, 200);
    //   const chartOptions = {
    //     chart: {
    //       height,
    //       type: 'funnel',
    //     },
    //     credits: {
    //       enabled: false,
    //     },
    //     title: {
    //       text: '',
    //     },
    //     legend: {
    //       enabled: false,
    //     },
    //     plotOptions: {
    //       series: {
    //         dataLabels: {
    //           enabled: true,
    //           format: '<b>{point.name}</b> ({point.y:,.0f})',
    //           softConnector: true,
    //         },
    //         center: ['40%', '50%'],
    //         neckWidth: '30%',
    //         neckHeight: '25%',
    //         width: '80%',
    //       },
    //       bar: {
    //         groupPadding: 0,
    //         pointPadding: 0.1,
    //       },
    //     },
    //     series: [
    //       {
    //         name: 'Participant Count',
    //         data,
    //         tooltip: {
    //           pointFormat:
    //             '<span style="color:{point.color}">\u25CF </span><b> {point.y}</b>',
    //         },
    //       },
    //     ],
    //   };
    //   setOptions(chartOptions);
    // };

    // const getCategoriesAndData = () => {
    //   const chartData = data.reduce(
    //     (acc, { count, name }) => {
    //       if (!acc.categories.includes(name)) {
    //         acc.categories.push(name);
    //       }
    //       const index = acc.data.findIndex((d) => d.name === name);
    //       if (index > -1) {
    //         acc.data[index].y += count;
    //       } else {
    //         acc.data.push({
    //           y: count,
    //           name,
    //           color: colors.chartColors[acc.data.length],
    //         });
    //       }
    //       return acc;
    //     },
    //     { categories: [], data: [] }
    //   );
    //   return {
    //     categories: chartData.categories.sort((a, b) => (a > b ? 1 : -1)),
    //     data: chartData.data.sort((a, b) => (a.name > b.name ? 1 : -1)),
    //   };
    // };

    return (
      <div style={{ height: 750 }}>
        <div style={{ height: '1.25rem', width: '100%' }}>
          <Button
            type='link'
            onClick={() => onExit()}
            style={{
              height: '1.25rem',
              fontSize: '12px',
              float: 'right',
            }}
            disabled={loading}
          >
            Hide Funnel
          </Button>
        </div>
        {loading ? (
          <SpinnerOverlay />
        ) : (
          <HighchartsReact
            highcharts={highCharts}
            options={options}
            callback={getChartObj}
          />
        )}
      </div>
    );
  }
);
