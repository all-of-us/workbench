import * as React from 'react';
import { RouteComponentProps, withRouter } from 'react-router-dom';
import * as fp from 'lodash/fp';
import * as querystring from 'querystring-es3';

import { Cohort } from 'generated/fetch';

import { RouteLink } from 'app/components/app-router';
import { Button } from 'app/components/buttons';
import { ActionCardBase } from 'app/components/card';
import { FadeBox } from 'app/components/containers';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { SpinnerOverlay } from 'app/components/spinners';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { cohortsApi } from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {
  reactStyles,
  withCurrentCohort,
  withCurrentWorkspace,
} from 'app/utils';
import { NavigationProps } from 'app/utils/navigation';
import { MatchParams } from 'app/utils/stores';
import { analysisTabName } from 'app/utils/user-apps-utils';
import { withNavigation } from 'app/utils/with-navigation-hoc';
import { WorkspaceData } from 'app/utils/workspace-data';

const styles = reactStyles({
  cohortsHeader: {
    color: colors.primary,
    fontSize: '20px',
    lineHeight: '24px',
    fontWeight: 600,
    marginTop: 0,
  },
  cardArea: {
    display: 'flex',
    alignItems: 'center',
    width: '100%',
  },
  card: {
    marginTop: '0.75rem',
    justifyContent: 'space-between',
    marginRight: '1.5rem',
    padding: '1.125rem 1.125rem 0rem 1.125rem',
    boxShadow: '0 0 0 0',
  },
  cardName: {
    fontSize: '18px',
    fontWeight: 600,
    lineHeight: '22px',
    color: colors.primary,
    wordBreak: 'break-word',
    textOverflow: 'ellipsis',
    overflow: 'hidden',
    display: '-webkit-box',
    WebkitLineClamp: 3,
    WebkitBoxOrient: 'vertical',
  },
  cardDescription: {
    marginTop: '0.75rem',
    textOverflow: 'ellipsis',
    overflow: 'hidden',
    display: '-webkit-box',
    WebkitLineClamp: 4,
    WebkitBoxOrient: 'vertical',
  },
  cardButton: {
    margin: '1.5rem 0',
    height: '3rem',
  },
});

const actionCards = [
  {
    title: 'Create another Cohort',
    description: 'Create another cohort for your analysis.',
    action: 'newCohort',
  },
  {
    title: 'Create Review Sets',
    description: `The review set feature allows you to select a subset of your cohort to review
       participants row-level data and add notes and annotations.`,
    action: 'review',
  },
  {
    title: 'Create a Dataset',
    description:
      'Create an analysis ready dataset that can be exported to notebooks.',
    action: 'dataSet',
  },
];

interface Props
  extends WithSpinnerOverlayProps,
    NavigationProps,
    RouteComponentProps<MatchParams> {
  cohort: Cohort;
  workspace: WorkspaceData;
}

interface State {
  cohort: Cohort;
  cohortLoading: boolean;
}

export const CohortActions = fp.flow(
  withCurrentCohort(),
  withCurrentWorkspace(),
  withNavigation,
  withRouter
)(
  class extends React.Component<Props, State> {
    constructor(props: any) {
      super(props);
      this.state = { cohort: undefined, cohortLoading: false };
    }

    componentDidMount(): void {
      this.props.hideSpinner();
      const {
        cohort,
        match: {
          params: { cid },
        },
      } = this.props;
      if (cohort && +cid === cohort.id) {
        this.setState({ cohort });
      } else {
        this.setState({ cohortLoading: true });
        const { namespace, id } = this.props.workspace;
        cohortsApi()
          .getCohort(namespace, id, +this.props.match.params.cid)
          .then((c) => {
            if (c) {
              this.setState({ cohort: c, cohortLoading: false });
            } else {
              this.props.navigate([
                'workspaces',
                namespace,
                id,
                'data',
                'cohorts',
              ]);
            }
          });
      }
    }

    getNavigationPath(action: string): string {
      const { cohort } = this.state;
      const { namespace, id } = this.props.workspace;
      let url = `/workspaces/${namespace}/${id}/`;
      let queryParams: any = null;

      switch (action) {
        case 'cohort':
          url += 'data/cohorts/build';
          queryParams = { cohortId: cohort.id };
          break;
        case 'review':
          url += `data/cohorts/${cohort.id}/reviews`;
          break;
        case 'notebook':
          url += analysisTabName;
          break;
        case 'dataSet':
          url += 'data/data-sets';
          break;
        case 'newCohort':
          url += 'data/cohorts/build';
      }
      if (queryParams) {
        url += '?' + querystring.stringify(queryParams);
      }
      return url;
    }

    render() {
      const { cohort, cohortLoading } = this.state;
      return (
        <FadeBox
          style={{ margin: 'auto', marginTop: '1.5rem', width: '95.7%' }}
        >
          {cohortLoading && <SpinnerOverlay />}
          {cohort && (
            <React.Fragment>
              <h3 style={styles.cohortsHeader}>Cohort Saved Successfully</h3>
              <div style={{ marginTop: '0.375rem' }}>
                The cohort
                <RouteLink
                  style={{ color: colors.accent, margin: '0 4px' }}
                  path={this.getNavigationPath('cohort')}
                >
                  {cohort.name}
                </RouteLink>
                has been saved.
              </div>
              <h3 style={{ ...styles.cohortsHeader, marginTop: '2.25rem' }}>
                What Next?
              </h3>
              <div style={styles.cardArea}>
                {actionCards.map((card, i) => {
                  return (
                    <ActionCardBase key={i} style={styles.card}>
                      <FlexColumn style={{ alignItems: 'flex-start' }}>
                        <FlexRow style={{ alignItems: 'flex-start' }}>
                          <div style={styles.cardName}>{card.title}</div>
                        </FlexRow>
                        <div style={styles.cardDescription}>
                          {card.description}
                        </div>
                      </FlexColumn>
                      <div>
                        <Button
                          type='primary'
                          style={styles.cardButton}
                          path={this.getNavigationPath(card.action)}
                        >
                          {card.title}
                        </Button>
                      </div>
                    </ActionCardBase>
                  );
                })}
              </div>
            </React.Fragment>
          )}
        </FadeBox>
      );
    }
  }
);
