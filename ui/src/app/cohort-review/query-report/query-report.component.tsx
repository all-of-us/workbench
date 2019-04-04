import {cohortReviewStore} from 'app/cohort-review/review-state.service';
import {cdrVersionsApi} from 'app/services/swagger-fetch-clients';
import {currentCohortStore} from 'app/utils/navigation';
import {List} from 'immutable';
import * as React from 'react';
import {Observable} from 'rxjs/Observable';

const css = `
  .container {
    min-width: 100%;
  }
  .report-background {
    background-color: white;
    padding-top: 1rem;
    margin-top: 0.5rem;
  }
  .report-background .query-title {
    font-size: 16px;
    font-weight: 600;
    color: #262262;
    line-height: 22px;
  }
  .report-background .query-content {
    font-size: 13px;
    color: black;
    line-height: 30px;
    padding-bottom: 0.6rem;
  }
  .header-border .btn-margin {
    margin-top : 0.7rem;
  }
  .temporal-container {
    padding-left: 1rem;
    margin-top: 1rem;
    margin-bottom: 1rem;
  }
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
`
export class QueryReport extends React.Component<{}, {cdrName: string}> {
  data:  Observable<List<any>>;
  cohort = currentCohortStore.getValue();
  review = cohortReviewStore.getValue();

  constructor(props: any) {
    super(props);
    this.state = {cdrName: null};
  }

  componentDidMount() {
    cdrVersionsApi().getCdrVersions().then(resp => {
      const cdrName = resp.items.find(
        v => v.cdrVersionId === this.review.cdrVersionId.toString()
      ).name;
      this.setState({cdrName});
    });
  }

  getDemoChartData(d) {
    if (d) {
      this.data = d.toJS();
    }
  }

  render() {
    const {cdrName} = this.state;
    return <React.Fragment>
      <style>{css}</style>
      <div className='report-background'>
        <div className='container'>
          <div className='row'>
            <div className='col-sm-6 col-xs-6'>
              <div className='container'>
                <div className='row'>
                  <div className='col-sm-6 col-xs-6'>
                    <div className='query-title'>
                      Cohort Name
                    </div>
                    <div className='query-content'>
                      {this.review.cohortName}
                    </div>
                    <div className='query-title'>
                      Created By
                    </div>
                    <div className='query-content'>
                      {this.cohort.creator}
                    </div>
                  </div>
                  <div className='col-sm-6 col-xs-6'>
                    <div className='query-title'>
                      Date created
                    </div>
                    <div className='query-content'>
                      {this.cohort.creationTime}
                    </div>
                    <div className='query-title'>
                      Dataset
                    </div>
                    <div className='query-content'>
                      {cdrName}
                    </div>
                  </div>
                  <div className='col-sm-12 col-xs-12'>
                    // query-cohort-definition
                  </div>
                </div>
              </div>
            </div>
            <div className=' col-sm-6 col-xs-6 stats-left-padding'>
              // app-descriptive-stats
            </div>
          </div>
        </div>
      // overview-charts
      </div>
    </React.Fragment>;
  }
}
