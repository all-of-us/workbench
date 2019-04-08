import {Component, Input} from '@angular/core';
import {cohortReviewApi} from 'app/services/swagger-fetch-clients';
import {WorkspaceData} from 'app/services/workspace-storage.service';
import {ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {currentCohortStore} from 'app/utils/navigation';
import * as React from 'react';
import {SpinnerOverlay} from '../../components/spinners';

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
  domain: string;
  workspace: WorkspaceData;
}

export interface ParticipantsChartsState {
  data: any;
  loading: boolean;
  options: any;
}

export const ParticipantsCharts = withCurrentWorkspace()(
  class extends React.Component<ParticipantsChartsProps, ParticipantsChartsState>  {
    constructor(props: ParticipantsChartsProps) {
      super(props);
      this.state = {
        data: null,
        loading: true,
        options: null,
      };
    }

    componentDidMount() {
      const {domain, workspace: {cdrVersionId, id, namespace}} = this.props;
      const cohort = currentCohortStore.getValue();
      cohortReviewApi().getCohortChartData(namespace, id, cohort.id, +cdrVersionId, domain, 10)
        .then(resp => {
          const data = resp.items.map(item => {
            const percentCount = ((item.count / resp.count) * 100);
            return {...item, percentCount};
          });
          this.setState({data, loading: false});
        });
    }

    render() {
      const {domain} = this.props;
      const {data, loading} = this.state;
      return <React.Fragment>
        <style>{css}</style>
        {data && <div className='container chart-width page-break'>
          <div className='domain-title'>Top 10 {domain}s</div>
          <div className='graph-border'>
            {data.map((item, i) => (
              <div key={i} className='data-container row'>
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
        </div>}
        {loading && <SpinnerOverlay />}
      </React.Fragment>;
    }
  }
);

@Component({
  selector: 'app-participants-charts',
  template: '<div #root></div>'

})
export class ParticipantsChartsComponent extends ReactWrapperBase {

  @Input('domain') domain: ParticipantsChartsProps['domain'];
  constructor() {
    super(ParticipantsCharts, ['domain']);
  }
}

