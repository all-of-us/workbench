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
import {hasNewValidProps, reactStyles, withCurrentWorkspace} from 'app/utils';
import {
  currentCohortReviewStore,
  NavigationProps
} from 'app/utils/navigation';
import { MatchParams } from 'app/utils/stores';
import {withNavigation} from 'app/utils/with-navigation-hoc';
import {WorkspaceData} from 'app/utils/workspace-data';
import {Cohort, CriteriaType, Domain, ReviewStatus, SortOrder, WorkspaceAccessLevel} from 'generated/fetch';
import { RouteComponentProps, withRouter } from 'react-router-dom';

const styles = reactStyles({
  title: {
    color: colors.primary,
    fontSize: '0.9rem',
    fontWeight: 200,
  },
});

interface Props extends WithSpinnerOverlayProps, NavigationProps, RouteComponentProps<MatchParams> {
  workspace: WorkspaceData;
}

interface State {
  reviewPresent: boolean;
  cohort: Cohort;
}

export const CohortReview = fp.flow(
  withCurrentWorkspace(),
  withNavigation,
  withRouter
)(
  class extends React.Component<Props, State> {
    constructor(props: any) {
      super(props);
      this.state = {
        reviewPresent: undefined,
        cohort: undefined,
      };
    }

    get readonly() {
      return this.props.workspace.accessLevel === WorkspaceAccessLevel.READER;
    }

    componentDidMount(): void {
      this.props.hideSpinner();
      this.loadCohort();
    }

    loadCohort() {
      const {ns, wsid, cid} = this.props.match.params;
      const {cdrVersionId} = this.props.workspace;

      if (!cid) {
        return;
      }

      cohortReviewApi().getParticipantCohortStatuses(ns, wsid, +cid, +cdrVersionId, {
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
      cohortsApi().getCohort(ns, wsid, +cid).then(cohort => this.setState({cohort}));
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

    reviewCreated = () => {
      this.setState({reviewPresent: true});
    }

    goBack = () => {
      history.back();
    }

    render() {
      const {cohort, reviewPresent} = this.state;
      const loading = !cohort || reviewPresent === undefined;
      const ableToReview = !!cohort && reviewPresent === false && !this.readonly;
      const unableToReview = !!cohort && reviewPresent === false && this.readonly;
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


