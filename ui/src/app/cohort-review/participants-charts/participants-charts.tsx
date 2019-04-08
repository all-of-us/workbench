import {Component, Input} from '@angular/core';
import {ReactWrapperBase} from 'app/utils';
import * as React from 'react';

const css = `
.data-container {
  display: -webkit-box;
}

.data-blue {
  background-color: #216FB4;
  color: white;
  font-size: 10px;
  text-align: end;
  padding-right:0.2rem;
  font-weight: bold;
}

.light-grey {
  background-color: #CCCCCC;
  display: -webkit-box;
}

.divider {
  border-right: 1px solid black;
}

.data-bar-container {
  padding-left: 1rem;
  padding-top: 0.5rem;
  border-left: 1px solid black;
}

.data-heading {
  padding-top: 0.5rem;
  width: 16rem;
  font-size: 10px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  text-align: end;
  padding-right: 0.5rem;
}

.data-percent {
  padding-top: 0.5rem;
  white-space: nowrap;
  font-size: 10px;
  font-weight: bold;
  color: #4a4a4a;
}

.count {
  padding-left: 0.2rem;
  font-size: 10px;
  font-weight: bold;
  color: #4a4a4a;
}

.btn-outline {
  pointer-events: none;
}

.container-margin {
  margin: 0;
  min-width: 100%;
}

.button-padding {
  padding-top: 1rem;
}

.button-padding-zero {
  padding-top: 0rem;
}

.chart-width {
  margin: 0;
  padding-top: 1rem;
  padding-bottom: 1rem;
}

.btn-disable {
  cursor: not-allowed;
  opacity: 0.5;
}

.btn-disable button {
  pointer-events: none;
}

.btn-outline {
  pointer-events: none;
}

.graph-border {
  padding: 0.3rem;
}

.chart-heading {
  text-align: center;
  color: #4A4A4A;
  font-size: 12px;
  font-weight: bold;
  white-space: nowrap;
}

.domain-title {
  margin-top: 2rem;
  padding-bottom: 0.5rem;
  font-size: 16px;
  font-weight: 600;
  color: #262262;
  line-height: 22px;
}

@media print{
  .graph-border {
    padding: 2rem;
    page-break-inside:avoid;
  }
  .page-break {
    page-break-inside: avoid;
  }

}
`;

export interface ParticipantsChartsProps {
  data: any;
  totalCount: number;
  domain: string;
}

export class ParticipantsCharts extends React.Component<ParticipantsChartsProps, {options: any}>  {
  constructor(props: ParticipantsChartsProps) {
    super(props);
    this.state = {options: null};
  }

  componentDidMount() {
    const {data, totalCount} = this.props;
    if (data) {
      data.forEach(itemCount => {
        const percentCount = ((itemCount.count / totalCount) * 100);
        Object.assign(itemCount, {percentCount: percentCount});
      });
    }
  }

  render() {
    const {data, domain} = this.props;
    return <React.Fragment>
      <style>{css}</style>
      <div className='container chart-width page-break'>
        <div className='domain-title'>Top 10 {domain}s</div>
        <div className='graph-border'>
          {data.map(item => (
            <div className='data-container row'>
              {item.name.length >= 40 &&
                <div className='data-heading col-sm-4 col-xs-4 col-lg-4 col-xl-4'>
                  {item.name}
                </div>
              }
              {item.name.length < 40 &&
                <div className='col-sm-3 col-lg-4 col-xs-4 col-xl-4 data-heading'>
                  {item.name}
                </div>
              }
              <div className='data-bar-container col-sm-7 col-xs-7 col-lg-7 col-xl-7'>
                <div className='light-grey'>
                  <div className='data-blue'
                    style={{height: '24px', width: `${item.percentCount}%`}}>
                    {item.percentCount >= 90 && <span>{item.count}</span>}
                  </div>
                  <div className='count'
                    style={{height: '24px', width: `${item.percentCount}%`}}>
                    {item.percentCount < 90 && <span>{item.count}</span>}
                  </div>
                </div>
              </div>
              <div className='data-percent'>
                {item.percentCount} % of Cohort
              </div>
            </div>
          ))}
        </div>
      </div>
    </React.Fragment>;
  }
}

@Component({
  selector: 'app-participants-charts',
  template: '<div #root></div>'

})
export class ParticipantsChartsComponent extends ReactWrapperBase {

  @Input('data') data: ParticipantsChartsProps['data'];
  @Input('totalCount') totalCount: ParticipantsChartsProps['totalCount'];
  @Input('domain') domain: ParticipantsChartsProps['domain'];
  constructor() {
    super(ParticipantsCharts, ['data', 'totalCount', 'domain']);
  }
}

