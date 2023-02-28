import * as React from 'react';
import * as highCharts from 'highcharts';
import HighchartsReact from 'highcharts-react-official';
import { SelectButton } from 'primereact/selectbutton';
import {
  faChartBar,
  faChartColumn,
  faChartLine,
} from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { Button, Clickable } from 'app/components/buttons';
import { FadeBox } from 'app/components/containers';
import { ClrIcon } from 'app/components/icons';
import { TextInput } from 'app/components/inputs';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import { withSpinnerOverlay } from 'app/components/with-spinner-overlay';
import { getChartObj } from 'app/pages/data/cohort/utils';
import colors, { colorWithWhiteness } from 'app/styles/colors';

const { useEffect, useState } = React;

enum ChartType {
  Bar = 'Bar',
  Column = 'Column',
  Line = 'Line',
}

interface ChartConfig {
  id: number;
  chartType: ChartType;
  name: string;
  xAxis: string;
  yAxis: string;
  zAxis?: string;
}

const columnOptions = (config: ChartConfig) => ({
  chart: {
    type: config.chartType.toLowerCase(),
  },
  title: {
    align: 'left',
    text: config.name,
  },
  accessibility: {
    announceNewData: {
      enabled: true,
    },
  },
  xAxis: {
    type: 'category',
  },
  yAxis: {
    title: {
      text: 'Total percent market share',
    },
  },
  legend: {
    enabled: false,
  },
  plotOptions: {
    series: {
      borderWidth: 0,
      dataLabels: {
        enabled: true,
        format: '{point.y:.1f}%',
      },
    },
  },

  tooltip: {
    headerFormat: '<span style="font-size:11px">{series.name}</span><br>',
    pointFormat:
      '<span style="color:{point.color}">{point.name}</span>: <b>{point.y:.2f}%</b> of total<br/>',
  },

  series: [
    {
      name: 'Browsers',
      colorByPoint: true,
      data: [
        {
          name: 'Chrome',
          y: 63.06,
          drilldown: 'Chrome',
        },
        {
          name: 'Safari',
          y: 19.84,
          drilldown: 'Safari',
        },
        {
          name: 'Firefox',
          y: 4.18,
          drilldown: 'Firefox',
        },
        {
          name: 'Edge',
          y: 4.12,
          drilldown: 'Edge',
        },
        {
          name: 'Opera',
          y: 2.33,
          drilldown: 'Opera',
        },
        {
          name: 'Internet Explorer',
          y: 0.45,
          drilldown: 'Internet Explorer',
        },
        {
          name: 'Other',
          y: 1.582,
          drilldown: null,
        },
      ],
    },
  ],
  drilldown: {
    breadcrumbs: {
      position: {
        align: 'right',
      },
    },
    series: [
      {
        name: 'Chrome',
        id: 'Chrome',
        data: [
          ['v65.0', 0.1],
          ['v64.0', 1.3],
          ['v63.0', 53.02],
          ['v62.0', 1.4],
          ['v61.0', 0.88],
          ['v60.0', 0.56],
          ['v59.0', 0.45],
          ['v58.0', 0.49],
          ['v57.0', 0.32],
          ['v56.0', 0.29],
          ['v55.0', 0.79],
          ['v54.0', 0.18],
          ['v51.0', 0.13],
          ['v49.0', 2.16],
          ['v48.0', 0.13],
          ['v47.0', 0.11],
          ['v43.0', 0.17],
          ['v29.0', 0.26],
        ],
      },
      {
        name: 'Firefox',
        id: 'Firefox',
        data: [
          ['v58.0', 1.02],
          ['v57.0', 7.36],
          ['v56.0', 0.35],
          ['v55.0', 0.11],
          ['v54.0', 0.1],
          ['v52.0', 0.95],
          ['v51.0', 0.15],
          ['v50.0', 0.1],
          ['v48.0', 0.31],
          ['v47.0', 0.12],
        ],
      },
      {
        name: 'Internet Explorer',
        id: 'Internet Explorer',
        data: [
          ['v11.0', 6.2],
          ['v10.0', 0.29],
          ['v9.0', 0.27],
          ['v8.0', 0.47],
        ],
      },
      {
        name: 'Safari',
        id: 'Safari',
        data: [
          ['v11.0', 3.39],
          ['v10.1', 0.96],
          ['v10.0', 0.36],
          ['v9.1', 0.54],
          ['v9.0', 0.13],
          ['v5.1', 0.2],
        ],
      },
      {
        name: 'Edge',
        id: 'Edge',
        data: [
          ['v16', 2.6],
          ['v15', 0.92],
          ['v14', 0.4],
          ['v13', 0.1],
        ],
      },
      {
        name: 'Opera',
        id: 'Opera',
        data: [
          ['v50.0', 0.96],
          ['v49.0', 0.82],
          ['v12.1', 0.14],
        ],
      },
    ],
  },
});

const CreateChartModal = ({
  close,
  submit,
}: {
  close: () => void;
  submit: (config: ChartConfig) => void;
}) => {
  const [formState, setFormState] = useState({
    id: 0,
    chartType: ChartType.Bar,
    name: '',
    xAxis: '',
    yAxis: '',
    zAxis: '',
  });
  return (
    <Modal onRequestClose={() => close()}>
      <ModalTitle
        style={{
          color: '#302973',
          fontSize: '1.35rem',
          fontWeight: 200,
        }}
      >
        Create New Chart
      </ModalTitle>
      <ModalBody style={{ lineHeight: '1.5rem' }}>
        <TextInput
          placeholder='CHART NAME'
          onChange={(value) =>
            setFormState((prevState) => ({ ...prevState, name: value }))
          }
          onBlur={() => {}}
        />
        <h4 style={{ color: '#302973', marginTop: '0.75rem' }}>Chart Type</h4>
        <SelectButton
          style={{ marginTop: '0.25rem' }}
          options={Object.values(ChartType)}
          value={formState.chartType}
          onChange={(e) =>
            setFormState((prevState) => ({ ...prevState, chartType: e.value }))
          }
        />
      </ModalBody>
      <ModalFooter>
        <Button
          style={{
            color: '#000000',
            fontWeight: 'bold',
            marginRight: '1.5rem',
            padding: '0.375rem 0.75rem',
            letterSpacing: '1.25px',
          }}
          type='link'
          onClick={() => close()}
        >
          CANCEL
        </Button>
        <Button
          style={{
            background: '#302C71',
            marginLeft: '0.75rem',
          }}
          type='primary'
          onClick={() => submit(formState)}
        >
          CREATE CHART
        </Button>
      </ModalFooter>
    </Modal>
  );
};

const ChartIcon = ({ chartType }) => {
  switch (chartType) {
    case ChartType.Bar:
      return (
        <FontAwesomeIcon
          title='Bar Chart'
          style={{ color: colors.primary, marginLeft: '0.25rem' }}
          icon={faChartBar}
        />
      );
    case ChartType.Column:
      return (
        <FontAwesomeIcon
          title='Column Chart'
          style={{ color: colors.primary, marginLeft: '0.25rem' }}
          icon={faChartColumn}
        />
      );
    case ChartType.Line:
      return (
        <FontAwesomeIcon
          title='Line Chart'
          style={{ color: colors.primary, marginLeft: '0.25rem' }}
          icon={faChartLine}
        />
      );
  }
};

export const DataExplorer = withSpinnerOverlay()(({ hideSpinner }) => {
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [chartList, setChartList] = useState([]);
  const [selectedChart, setSelectedChart] = useState<ChartConfig>();

  useEffect(() => {
    hideSpinner();
  }, []);

  return (
    <>
      <FadeBox style={{ margin: 'auto', paddingTop: '1.5rem', width: '95.7%' }}>
        <div style={{ display: 'flex' }}>
          <div
            style={{
              border: `1px solid ${colorWithWhiteness(colors.black, 0.8)}`,
              borderRadius: '3px',
              flex: '0 0 20%',
              marginBottom: '0.375rem',
            }}
          >
            <div
              style={{
                borderBottom: `1px solid ${colorWithWhiteness(
                  colors.black,
                  0.8
                )}`,
                color: colors.primary,
                fontSize: '16px',
                fontWeight: 600,
                padding: '0.75rem',
              }}
            >
              Charts
              <Clickable
                style={{ display: 'inline-block', marginLeft: '0.75rem' }}
                onClick={() => setShowCreateModal(true)}
              >
                <ClrIcon shape='plus-circle' class='is-solid' size={18} />
              </Clickable>
            </div>
            <div style={{ padding: '0.5rem' }}>
              {chartList.map((chartItem, index) => (
                <div key={index} style={{ marginBottom: '0.25rem' }}>
                  <Clickable onClick={() => setSelectedChart(chartItem)}>
                    {chartItem.name}
                    <ChartIcon chartType={chartItem.chartType} />
                  </Clickable>
                </div>
              ))}
            </div>
          </div>
          <div style={{ flex: '0 0 80%', marginLeft: '0.375rem' }}>
            {selectedChart && (
              <HighchartsReact
                highcharts={highCharts}
                options={columnOptions(selectedChart)}
                callback={getChartObj}
              />
            )}
          </div>
        </div>
      </FadeBox>
      {showCreateModal && (
        <CreateChartModal
          close={() => setShowCreateModal(false)}
          submit={(newChart) => {
            setChartList((prevState) => [...prevState, newChart]);
            setShowCreateModal(false);
          }}
        />
      )}
    </>
  );
});
