import {Component, Input} from '@angular/core';
import * as highCharts from 'highcharts';
import HighchartsReact from 'highcharts-react-official';
import * as React from 'react';

import {getChartObj} from 'app/cohort-search/utils';
import {ReactWrapperBase} from 'app/utils';

interface Props {
  mode: string;
  data: any;
}

interface State {
  options: any;
}

export class ComboChart extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {options: null};
  }

  componentDidMount(): void {
    this.getChartOptions();
  }

  componentDidUpdate(prevProps: any): void {
    if (prevProps.mode !== this.props.mode) {
      this.getChartOptions();
    }
  }

  getChartOptions() {
    const {mode} = this.props;
    const normalized = mode === 'normalized';
    const {categories, series} = this.getCategoriesAndSeries();
    const options = {
      chart: {
        height: 250,
        type: 'bar'
      },
      credits: {
        enabled: false
      },
      title: {
        text: ''
      },
      xAxis: {
        categories,
        tickLength: 0,
        tickPixelInterval: 50
      },
      yAxis: {
        labels: {
          format: '{value}' + (normalized ? '%' : '')
        },
        min: 0,
        title: {
          text: ''
        }
      },
      colors: ['#adcded', '#aae3f5', '#a27ea8', '#7aa3e5', '#a8385d'],
      legend: {
        enabled: false
      },
      plotOptions: {
        bar: {
          groupPadding: 0,
          pointPadding: 0.1,
        },
        series: {
          stacking: (normalized ? 'percent' : 'normal')
        }
      },
      series
    };
    this.setState({options});
  }

  getCategoriesAndSeries() {
    // const {data} = this.props;

    // TODO remove mocked data after testing and before merging
    /* Criteria: Sex assigned at birth -> Unknown */
    const data = [{"gender":"Female","race":"Asian","ageRange":"45-64","count":2},{"gender":"Female","race":"Asian","ageRange":"> 65","count":1},{"gender":"Female","race":"Black or African American","ageRange":"19-44","count":6},{"gender":"Female","race":"Black or African American","ageRange":"45-64","count":24},{"gender":"Female","race":"Black or African American","ageRange":"> 65","count":5},{"gender":"Female","race":"More than one population","ageRange":"19-44","count":1},{"gender":"Female","race":"More than one population","ageRange":"45-64","count":1},{"gender":"Female","race":"More than one population","ageRange":"> 65","count":1},{"gender":"Female","race":"None of these","ageRange":"19-44","count":1},{"gender":"Female","race":"None of these","ageRange":"> 65","count":2},{"gender":"Female","race":"Skip","ageRange":"19-44","count":2},{"gender":"Female","race":"Skip","ageRange":"45-64","count":8},{"gender":"Female","race":"Unknown","ageRange":"19-44","count":6},{"gender":"Female","race":"Unknown","ageRange":"45-64","count":5},{"gender":"Female","race":"Unknown","ageRange":"> 65","count":3},{"gender":"Female","race":"White","ageRange":"19-44","count":8},{"gender":"Female","race":"White","ageRange":"45-64","count":19},{"gender":"Female","race":"White","ageRange":"> 65","count":21},{"gender":"Male","race":"Another single population","ageRange":"45-64","count":1},{"gender":"Male","race":"Asian","ageRange":"> 65","count":1},{"gender":"Male","race":"Black or African American","ageRange":"19-44","count":2},{"gender":"Male","race":"Black or African American","ageRange":"45-64","count":28},{"gender":"Male","race":"Black or African American","ageRange":"> 65","count":11},{"gender":"Male","race":"More than one population","ageRange":"19-44","count":1},{"gender":"Male","race":"More than one population","ageRange":"45-64","count":1},{"gender":"Male","race":"More than one population","ageRange":"> 65","count":1},{"gender":"Male","race":"None of these","ageRange":"45-64","count":1},{"gender":"Male","race":"Skip","ageRange":"19-44","count":1},{"gender":"Male","race":"Skip","ageRange":"45-64","count":1},{"gender":"Male","race":"Skip","ageRange":"> 65","count":6},{"gender":"Male","race":"Unknown","ageRange":"19-44","count":7},{"gender":"Male","race":"Unknown","ageRange":"45-64","count":8},{"gender":"Male","race":"Unknown","ageRange":"> 65","count":3},{"gender":"Male","race":"White","ageRange":"19-44","count":4},{"gender":"Male","race":"White","ageRange":"45-64","count":15},{"gender":"Male","race":"White","ageRange":"> 65","count":9},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"Another single population","ageRange":"45-64","count":1},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"Asian","ageRange":"19-44","count":1},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"Black or African American","ageRange":"19-44","count":1},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"Black or African American","ageRange":"45-64","count":7},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"Black or African American","ageRange":"> 65","count":1},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"I prefer not to answer","ageRange":"45-64","count":1},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"None of these","ageRange":"19-44","count":1},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"Skip","ageRange":"19-44","count":3},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"Skip","ageRange":"45-64","count":5},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"Skip","ageRange":"> 65","count":6},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"White","ageRange":"45-64","count":4},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"White","ageRange":"> 65","count":3},{"gender":"Unknown","race":"Unknown","ageRange":"0-18","count":6},{"gender":"Unknown","race":"Unknown","ageRange":"19-44","count":7363},{"gender":"Unknown","race":"Unknown","ageRange":"45-64","count":6589},{"gender":"Unknown","race":"Unknown","ageRange":"> 65","count":3989}];

    /* Criteria: Female & Pregnant */
    // const data = [{"gender":"Female","race":"Another single population","ageRange":"19-44","count":34},{"gender":"Female","race":"Another single population","ageRange":"45-64","count":3},{"gender":"Female","race":"Asian","ageRange":"19-44","count":97},{"gender":"Female","race":"Asian","ageRange":"45-64","count":3},{"gender":"Female","race":"Black or African American","ageRange":"19-44","count":532},{"gender":"Female","race":"Black or African American","ageRange":"45-64","count":23},{"gender":"Female","race":"Black or African American","ageRange":"> 65","count":2},{"gender":"Female","race":"I prefer not to answer","ageRange":"19-44","count":16},{"gender":"Female","race":"I prefer not to answer","ageRange":"45-64","count":1},{"gender":"Female","race":"More than one population","ageRange":"19-44","count":74},{"gender":"Female","race":"More than one population","ageRange":"45-64","count":1},{"gender":"Female","race":"None of these","ageRange":"19-44","count":22},{"gender":"Female","race":"None of these","ageRange":"45-64","count":1},{"gender":"Female","race":"Skip","ageRange":"19-44","count":8},{"gender":"Female","race":"Skip","ageRange":"45-64","count":3},{"gender":"Female","race":"Unknown","ageRange":"0-18","count":1},{"gender":"Female","race":"Unknown","ageRange":"19-44","count":1891},{"gender":"Female","race":"Unknown","ageRange":"45-64","count":33},{"gender":"Female","race":"White","ageRange":"19-44","count":875},{"gender":"Female","race":"White","ageRange":"45-64","count":19},{"gender":"Female","race":"White","ageRange":"> 65","count":1}];

    /* Criteria: Female */
    // const data = [{"gender":"Female","race":"Another single population","ageRange":"19-44","count":477},{"gender":"Female","race":"Another single population","ageRange":"45-64","count":258},{"gender":"Female","race":"Another single population","ageRange":"> 65","count":130},{"gender":"Female","race":"Asian","ageRange":"0-18","count":1},{"gender":"Female","race":"Asian","ageRange":"19-44","count":2603},{"gender":"Female","race":"Asian","ageRange":"45-64","count":1332},{"gender":"Female","race":"Asian","ageRange":"> 65","count":621},{"gender":"Female","race":"Black or African American","ageRange":"0-18","count":9},{"gender":"Female","race":"Black or African American","ageRange":"19-44","count":9306},{"gender":"Female","race":"Black or African American","ageRange":"45-64","count":12615},{"gender":"Female","race":"Black or African American","ageRange":"> 65","count":4581},{"gender":"Female","race":"I prefer not to answer","ageRange":"0-18","count":1},{"gender":"Female","race":"I prefer not to answer","ageRange":"19-44","count":359},{"gender":"Female","race":"I prefer not to answer","ageRange":"45-64","count":323},{"gender":"Female","race":"I prefer not to answer","ageRange":"> 65","count":156},{"gender":"Female","race":"More than one population","ageRange":"0-18","count":2},{"gender":"Female","race":"More than one population","ageRange":"19-44","count":1547},{"gender":"Female","race":"More than one population","ageRange":"45-64","count":753},{"gender":"Female","race":"More than one population","ageRange":"> 65","count":343},{"gender":"Female","race":"None of these","ageRange":"19-44","count":471},{"gender":"Female","race":"None of these","ageRange":"45-64","count":556},{"gender":"Female","race":"None of these","ageRange":"> 65","count":335},{"gender":"Female","race":"Skip","ageRange":"19-44","count":179},{"gender":"Female","race":"Skip","ageRange":"45-64","count":327},{"gender":"Female","race":"Skip","ageRange":"> 65","count":205},{"gender":"Female","race":"Unknown","ageRange":"0-18","count":19},{"gender":"Female","race":"Unknown","ageRange":"19-44","count":12406},{"gender":"Female","race":"Unknown","ageRange":"45-64","count":9364},{"gender":"Female","race":"Unknown","ageRange":"> 65","count":3370},{"gender":"Female","race":"White","ageRange":"0-18","count":15},{"gender":"Female","race":"White","ageRange":"19-44","count":24103},{"gender":"Female","race":"White","ageRange":"45-64","count":26336},{"gender":"Female","race":"White","ageRange":"> 65","count":23322}];

    /* New data after api fix: Sex assigned at birth -> Unknown (throws a Highcharts error in browser console, I think becuase the Genders aren't sorted) */
    // const data = [{"gender":"Male","race":"Another single population","ageRange":"0-18","count":0},{"gender":"Male","race":"Another single population","ageRange":"19-44","count":0},{"gender":"Male","race":"Another single population","ageRange":"45-64","count":1},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"Another single population","ageRange":"> 65","count":0},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"Another single population","ageRange":"0-18","count":0},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"Another single population","ageRange":"19-44","count":0},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"Another single population","ageRange":"45-64","count":1},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"Another single population","ageRange":"> 65","count":0},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"Another single population","ageRange":"0-18","count":0},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"Another single population","ageRange":"19-44","count":0},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"Another single population","ageRange":"45-64","count":1},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"Another single population","ageRange":"> 65","count":0},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"Another single population","ageRange":"0-18","count":0},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"Another single population","ageRange":"19-44","count":0},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"Another single population","ageRange":"45-64","count":1},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"Another single population","ageRange":"> 65","count":0},{"gender":"Female","race":"Asian","ageRange":"0-18","count":0},{"gender":"Female","race":"Asian","ageRange":"19-44","count":0},{"gender":"Female","race":"Asian","ageRange":"45-64","count":2},{"gender":"Female","race":"Asian","ageRange":"> 65","count":1},{"gender":"Male","race":"Asian","ageRange":"0-18","count":0},{"gender":"Male","race":"Asian","ageRange":"19-44","count":0},{"gender":"Male","race":"Asian","ageRange":"45-64","count":0},{"gender":"Male","race":"Asian","ageRange":"> 65","count":1},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"Asian","ageRange":"0-18","count":0},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"Asian","ageRange":"19-44","count":1},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"Asian","ageRange":"45-64","count":0},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"Asian","ageRange":"> 65","count":0},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"Asian","ageRange":"0-18","count":0},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"Asian","ageRange":"19-44","count":1},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"Asian","ageRange":"45-64","count":0},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"Asian","ageRange":"> 65","count":0},{"gender":"Female","race":"Black or African American","ageRange":"0-18","count":0},{"gender":"Female","race":"Black or African American","ageRange":"19-44","count":6},{"gender":"Female","race":"Black or African American","ageRange":"45-64","count":24},{"gender":"Female","race":"Black or African American","ageRange":"> 65","count":5},{"gender":"Male","race":"Black or African American","ageRange":"0-18","count":0},{"gender":"Male","race":"Black or African American","ageRange":"19-44","count":2},{"gender":"Male","race":"Black or African American","ageRange":"45-64","count":28},{"gender":"Male","race":"Black or African American","ageRange":"> 65","count":11},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"Black or African American","ageRange":"0-18","count":0},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"Black or African American","ageRange":"19-44","count":1},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"Black or African American","ageRange":"45-64","count":7},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"Black or African American","ageRange":"> 65","count":1},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"Black or African American","ageRange":"0-18","count":0},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"Black or African American","ageRange":"19-44","count":0},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"Black or African American","ageRange":"45-64","count":0},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"Black or African American","ageRange":"> 65","count":1},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"I prefer not to answer","ageRange":"0-18","count":0},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"I prefer not to answer","ageRange":"19-44","count":0},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"I prefer not to answer","ageRange":"45-64","count":1},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"I prefer not to answer","ageRange":"> 65","count":0},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"I prefer not to answer","ageRange":"0-18","count":0},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"I prefer not to answer","ageRange":"19-44","count":0},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"I prefer not to answer","ageRange":"45-64","count":1},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"I prefer not to answer","ageRange":"> 65","count":0},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"I prefer not to answer","ageRange":"0-18","count":0},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"I prefer not to answer","ageRange":"19-44","count":0},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"I prefer not to answer","ageRange":"45-64","count":1},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"I prefer not to answer","ageRange":"> 65","count":0},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"I prefer not to answer","ageRange":"0-18","count":0},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"I prefer not to answer","ageRange":"19-44","count":0},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"I prefer not to answer","ageRange":"45-64","count":1},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"I prefer not to answer","ageRange":"> 65","count":0},{"gender":"Female","race":"More than one population","ageRange":"0-18","count":0},{"gender":"Female","race":"More than one population","ageRange":"19-44","count":1},{"gender":"Female","race":"More than one population","ageRange":"45-64","count":1},{"gender":"Female","race":"More than one population","ageRange":"> 65","count":1},{"gender":"Male","race":"More than one population","ageRange":"0-18","count":0},{"gender":"Male","race":"More than one population","ageRange":"19-44","count":1},{"gender":"Male","race":"More than one population","ageRange":"45-64","count":1},{"gender":"Male","race":"More than one population","ageRange":"> 65","count":1},{"gender":"Male","race":"More than one population","ageRange":"0-18","count":0},{"gender":"Male","race":"More than one population","ageRange":"19-44","count":0},{"gender":"Male","race":"More than one population","ageRange":"45-64","count":0},{"gender":"Male","race":"More than one population","ageRange":"> 65","count":1},{"gender":"Male","race":"More than one population","ageRange":"0-18","count":0},{"gender":"Male","race":"More than one population","ageRange":"19-44","count":0},{"gender":"Male","race":"More than one population","ageRange":"45-64","count":0},{"gender":"Male","race":"More than one population","ageRange":"> 65","count":1},{"gender":"Female","race":"None of these","ageRange":"0-18","count":0},{"gender":"Female","race":"None of these","ageRange":"19-44","count":1},{"gender":"Female","race":"None of these","ageRange":"45-64","count":0},{"gender":"Female","race":"None of these","ageRange":"> 65","count":2},{"gender":"Male","race":"None of these","ageRange":"0-18","count":0},{"gender":"Male","race":"None of these","ageRange":"19-44","count":0},{"gender":"Male","race":"None of these","ageRange":"45-64","count":1},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"None of these","ageRange":"> 65","count":0},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"None of these","ageRange":"0-18","count":0},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"None of these","ageRange":"19-44","count":1},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"None of these","ageRange":"45-64","count":0},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"None of these","ageRange":"> 65","count":0},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"None of these","ageRange":"0-18","count":0},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"None of these","ageRange":"19-44","count":1},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"None of these","ageRange":"45-64","count":0},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"None of these","ageRange":"> 65","count":0},{"gender":"Female","race":"Skip","ageRange":"0-18","count":0},{"gender":"Female","race":"Skip","ageRange":"19-44","count":2},{"gender":"Female","race":"Skip","ageRange":"45-64","count":8},{"gender":"Male","race":"Skip","ageRange":"> 65","count":0},{"gender":"Male","race":"Skip","ageRange":"0-18","count":0},{"gender":"Male","race":"Skip","ageRange":"19-44","count":1},{"gender":"Male","race":"Skip","ageRange":"45-64","count":1},{"gender":"Male","race":"Skip","ageRange":"> 65","count":6},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"Skip","ageRange":"0-18","count":0},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"Skip","ageRange":"19-44","count":3},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"Skip","ageRange":"45-64","count":5},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"Skip","ageRange":"> 65","count":6},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"Skip","ageRange":"0-18","count":0},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"Skip","ageRange":"19-44","count":0},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"Skip","ageRange":"45-64","count":0},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"Skip","ageRange":"> 65","count":6},{"gender":"Female","race":"Unknown","ageRange":"0-18","count":0},{"gender":"Female","race":"Unknown","ageRange":"19-44","count":6},{"gender":"Female","race":"Unknown","ageRange":"45-64","count":5},{"gender":"Female","race":"Unknown","ageRange":"> 65","count":3},{"gender":"Male","race":"Unknown","ageRange":"0-18","count":0},{"gender":"Male","race":"Unknown","ageRange":"19-44","count":7},{"gender":"Male","race":"Unknown","ageRange":"45-64","count":8},{"gender":"Male","race":"Unknown","ageRange":"> 65","count":3},{"gender":"Unknown","race":"Unknown","ageRange":"0-18","count":6},{"gender":"Unknown","race":"Unknown","ageRange":"19-44","count":7363},{"gender":"Unknown","race":"Unknown","ageRange":"45-64","count":6589},{"gender":"Unknown","race":"Unknown","ageRange":"> 65","count":3989},{"gender":"Unknown","race":"Unknown","ageRange":"0-18","count":0},{"gender":"Unknown","race":"Unknown","ageRange":"19-44","count":0},{"gender":"Unknown","race":"Unknown","ageRange":"45-64","count":0},{"gender":"Unknown","race":"Unknown","ageRange":"> 65","count":3989},{"gender":"Female","race":"White","ageRange":"0-18","count":0},{"gender":"Female","race":"White","ageRange":"19-44","count":8},{"gender":"Female","race":"White","ageRange":"45-64","count":19},{"gender":"Female","race":"White","ageRange":"> 65","count":21},{"gender":"Male","race":"White","ageRange":"0-18","count":0},{"gender":"Male","race":"White","ageRange":"19-44","count":4},{"gender":"Male","race":"White","ageRange":"45-64","count":15},{"gender":"Male","race":"White","ageRange":"> 65","count":9},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"White","ageRange":"0-18","count":0},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"White","ageRange":"19-44","count":0},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"White","ageRange":"45-64","count":4},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"White","ageRange":"> 65","count":3},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"White","ageRange":"0-18","count":0},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"White","ageRange":"19-44","count":0},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"White","ageRange":"45-64","count":0},{"gender":"Not man only, not woman only, prefer not to answer, or skipped","race":"White","ageRange":"> 65","count":3}];
    const codeMap = {
      'M': 'Male',
      'F': 'Female',
      'No matching concept': 'Unknown'
    };
    const getKey = (dat) => {
      const gender = !!codeMap[dat.gender] ? codeMap[dat.gender] : dat.gender;
      return `${gender} ${dat.ageRange || 'Unknown'}`;
    };
    const categories = data.reduce((acc, datum) => {
      const key = getKey(datum);
      if (!acc.includes(key)) {
        acc.push(key);
      }
      return acc;
    }, []).sort((a, b) => a > b ? 1 : -1);
    const series = data.reduce((acc, datum) => {
      const key = getKey(datum);
      const obj = {x: categories.indexOf(key), y: datum.count};
      const index = acc.findIndex(d => d.name === datum.race);
      if (index === -1) {
        acc.push({name: datum.race, data: [obj]});
      } else {
        acc[index].data.push(obj);
      }
      return acc;
    }, []).sort((a, b) => a['name'] < b['name'] ? 1 : -1);
    return {categories, series};
  }

  render() {
    const {options} = this.state;
    return <div style={{minHeight: 200}}>
      {options && <HighchartsReact highcharts={highCharts} options={options} callback={getChartObj} />}
    </div>;
  }
}

@Component ({
  selector: 'app-combo-chart',
  template: '<div #root></div>'
})
export class ComboChartComponent extends ReactWrapperBase {
  @Input('mode') mode: Props['mode'];
  @Input('data') data: Props['data'];
  constructor() {
    super(ComboChart, ['mode', 'data']);
  }
}

