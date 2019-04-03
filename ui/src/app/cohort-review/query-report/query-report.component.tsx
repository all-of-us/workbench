import {cohortReviewStore} from 'app/cohort-review/review-state.service';
import {cdrVersionsApi} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {Cohort, CohortReview} from 'generated/fetch';
import {List} from 'immutable';
import * as React from 'react';
import {Observable} from 'rxjs/Observable';

interface QueryReportProps {
  cohort: Cohort;
  review: CohortReview;
  backToReview: Function;
}

export class QueryReport extends React.Component<QueryReportProps> {
  cdrDetails: any;
  data:  Observable<List<any>>;

  constructor(props: QueryReportProps) {
    super(props);
    this.state = {};
  }

  componentDidMount() {
    const {review} = this.props;
    cdrVersionsApi().getCdrVersions().then(resp => {
      this.cdrDetails = resp.items.find(
        v => v.cdrVersionId === review.cdrVersionId.toString()
      );
    });
  }

  getDemoChartData(d) {
    if (d) {
      this.data = d.toJS();
    }
  }

  render() {
    const {cohort, review} = this.props;
    return <div className='report-background'>
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
                    {review.cohortName}
                  </div>
                  <div className='query-title'>
                    Created By
                  </div>
                  <div className='query-content'>
                    {cohort.creator}
                  </div>
                </div>
                <div className='col-sm-6 col-xs-6'>
                  <div className='query-title'>
                    Date created
                  </div>
                  <div className='query-content'>
                    {cohort.creationTime}
                  </div>
                  <div className='query-title'>
                    Dataset
                  </div>
                  <div className='query-content'>
                    {this.cdrDetails.name}
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
    </div>;
  }
}
