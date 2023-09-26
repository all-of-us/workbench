import * as React from 'react';
import * as HighCharts from 'highcharts';
import HighchartsReact from 'highcharts-react-official';

import { CloudStorageTraffic, TimeSeriesPoint } from 'generated/fetch';

import moment from 'moment';

const customOptions: HighCharts.Options = {
  time: {
    useUTC: false,
  },
  lang: {
    decimalPoint: '.',
    thousandsSep: ',',
  },
};

const getHighChartsReactOptions = (receivedBytes: TimeSeriesPoint[]) => ({
  animation: false,
  chart: {
    animation: false,
    height: '150px',
  },
  credits: {
    enabled: false,
  },
  legend: {
    enabled: false,
  },
  title: {
    text: undefined,
  },
  tooltip: {
    xDateFormat: '%A, %b %e, %H:%M',
    valueDecimals: 0,
  },
  xAxis: {
    min: moment().subtract(6, 'hours').valueOf(),
    max: moment().valueOf(),
    title: {
      enabled: false,
    },
    type: 'datetime',
    zoomEnabled: false,
  },
  yAxis: {
    title: {
      enabled: false,
    },
    zoomEnabled: false,
  },
  series: [
    {
      data: receivedBytes?.map((x) => [x.timestamp, x.value]),
      lineWidth: 0.5,
      name: 'GCS received bytes',
    },
  ],
});

interface Props {
  cloudStorageTraffic: CloudStorageTraffic;
}
export const CloudStorageTrafficChart = ({ cloudStorageTraffic }: Props) => {
  HighCharts.setOptions(customOptions);
  return (
    <div>
      <h2>Cloud Storage Traffic</h2>
      <div>
        Cloud Storage <i>received_bytes_count</i> over the past 6 hours.
      </div>
      <div style={{ width: '500px', zIndex: 1001 }}>
        <HighchartsReact
          highcharts={HighCharts}
          options={getHighChartsReactOptions(
            cloudStorageTraffic?.receivedBytes
          )}
        />
      </div>
    </div>
  );
};
