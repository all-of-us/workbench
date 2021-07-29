import * as fp from 'lodash/fp';
import * as React from 'react';

import {Button} from 'app/components/buttons';
import {Modal, ModalFooter, ModalTitle} from 'app/components/modals';
import {SpinnerOverlay} from 'app/components/spinners';
import {WithSpinnerOverlayProps} from 'app/components/with-spinner-overlay';
import {CreateReviewModal} from 'app/pages/data/cohort-review/create-review-modal';
import {queryResultSizeStore, visitsFilterOptions} from 'app/services/review-state.service';
import {cohortBuilderApi, cohortReviewApi, cohortsApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {reactStyles, withCurrentWorkspace, withUrlParams} from 'app/utils';
import {
  currentCohortReviewStore,
  NavigationProps
} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {Cohort, CriteriaType, Domain, ReviewStatus, SortOrder, WorkspaceAccessLevel} from 'generated/fetch';
import {withNavigation} from '../../../utils/navigation-wrapper';

const styles = reactStyles({
  title: {
    color: colors.primary,
    fontSize: '0.9rem',
    fontWeight: 200,
  },
});

interface Props extends WithSpinnerOverlayProps, NavigationProps {
  workspace: WorkspaceData;
  urlParams: any;
}

interface State {
  reviewPresent: boolean;
  cohort: Cohort;
  readonly: boolean;
}

export const CohortReview = fp.flow(
  withCurrentWorkspace(),
  withUrlParams(),
  withNavigation
)(
  class extends React.Component<Props, State> {
    constructor(props: any) {
      super(props);
      this.state = {
        reviewPresent: undefined,
        cohort: undefined,
        readonly: false
      };
    }

    componentDidMount(): void {
      this.props.hideSpinner();
      this.loadCohort();
    }

    loadCohort() {
      const {ns, wsid, cid} = this.props.urlParams;
      const {accessLevel, cdrVersionId} = this.props.workspace;
      this.setState({readonly: accessLevel === WorkspaceAccessLevel.READER});

      if (!cid) {
        return;
      }

      cohortReviewApi().getParticipantCohortStatuses(ns, wsid, cid, +cdrVersionId, {
        page: 0,
        pageSize: 25,
        sortOrder: SortOrder.Asc,
        filters: {items: []}
      }).then(resp => {
        const {cohortReview, queryResultSize} = resp;
        currentCohortReviewStore.next(cohortReview);
        queryResultSizeStore.next(queryResultSize);
        const reviewPresent = cohortReview.reviewStatus !== ReviewStatus.NONE;
        this.setState({reviewPresent});
        if (reviewPresent) {
          this.props.navigate(['workspaces', ns, wsid, 'data', 'cohorts', cid, 'review', 'participants']);
        }
      });
      cohortsApi().getCohort(ns, wsid, cid).then(cohort => this.setState({cohort}));
      if (!visitsFilterOptions.getValue()) {
        cohortBuilderApi().findCriteriaBy(
          ns, wsid, Domain[Domain.VISIT], CriteriaType[CriteriaType.VISIT]
        ).then(response => {
          visitsFilterOptions.next([
            {value: null, label: 'Any'},
            ...response.items.map(option => {
              return {value: option.name, label: option.name};
            })
          ]);
        });
      }
    }

    componentDidUpdate(prevProps: Readonly<Props>) {
      if (prevProps.urlParams.ns !== this.props.urlParams.ns
        || prevProps.urlParams.wsid !== this.props.urlParams.wsid
        || prevProps.urlParams.cid !== this.props.urlParams.cid) {
        this.loadCohort();
      }
    }

    reviewCreated = () => {
      this.setState({reviewPresent: true});
    }

    goBack = () => {
      history.back();
    }

    render() {
      const {cohort, readonly, reviewPresent} = this.state;
      const loading = !cohort || reviewPresent === undefined;
      const ableToReview = !!cohort && reviewPresent === false && !readonly;
      const unableToReview = !!cohort && reviewPresent === false && readonly;
      return <React.Fragment>
        {loading ? <SpinnerOverlay/>
          : <React.Fragment>
            {ableToReview && <CreateReviewModal canceled={() => this.goBack()} cohort={cohort} created={() => this.reviewCreated()}/>}
            {unableToReview && <Modal onRequestClose={() => this.goBack()}>
                <ModalTitle style={styles.title}>Users with read-only access cannot create cohort reviews</ModalTitle>
                <ModalFooter>
                    <Button style={{}} type='primary' onClick={() => this.goBack()}>Return to cohorts</Button>
                </ModalFooter>
            </Modal>}
          </React.Fragment>}
      </React.Fragment>;
    }
  }
);


