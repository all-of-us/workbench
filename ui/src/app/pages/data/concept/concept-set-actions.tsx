import * as React from 'react';
import { RouteComponentProps, withRouter } from 'react-router-dom';
import * as fp from 'lodash/fp';

import { ConceptSet } from 'generated/fetch';

import { RouteLink } from 'app/components/app-router';
import { Button } from 'app/components/buttons';
import { ActionCardBase } from 'app/components/card';
import { FadeBox } from 'app/components/containers';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { SpinnerOverlay } from 'app/components/spinners';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { conceptSetsApi } from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {
  reactStyles,
  withCurrentConceptSet,
  withCurrentWorkspace,
} from 'app/utils';
import { NOTEBOOKS_TAB_NAME } from 'app/utils/constants';
import { conceptSetUpdating, NavigationProps } from 'app/utils/navigation';
import { MatchParams } from 'app/utils/stores';
import { withNavigation } from 'app/utils/with-navigation-hoc';
import { WorkspaceData } from 'app/utils/workspace-data';

const styles = reactStyles({
  conceptSetsHeader: {
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
    title: 'Create another Concept Set',
    description:
      'Create another concept set for the same or a different domain.',
    action: 'newConceptSet',
  },
  {
    title: 'Create a Dataset',
    description:
      'Create an analysis ready dataset that can be exported to notebooks.',
    action: 'dataSet',
  },
];

interface State {
  conceptSet: ConceptSet;
  conceptSetLoading: boolean;
}

interface Props
  extends WithSpinnerOverlayProps,
    NavigationProps,
    RouteComponentProps<MatchParams> {
  conceptSet: ConceptSet;
  workspace: WorkspaceData;
}

export const ConceptSetActions = fp.flow(
  withCurrentConceptSet(),
  withCurrentWorkspace(),
  withNavigation,
  withRouter
)(
  class extends React.Component<Props, State> {
    constructor(props: any) {
      super(props);
      this.state = {
        conceptSet: undefined,
        conceptSetLoading: false,
      };
    }

    componentDidMount(): void {
      this.props.hideSpinner();
      conceptSetUpdating.next(false);
      const {
        conceptSet,
        match: {
          params: { csid },
        },
      } = this.props;
      if (conceptSet && +csid === conceptSet.id) {
        this.setState({ conceptSet });
      } else {
        this.setState({ conceptSetLoading: true });
        const { namespace, id } = this.props.workspace;
        conceptSetsApi()
          .getConceptSet(namespace, id, +csid)
          .then((cs) => {
            if (cs) {
              this.setState({ conceptSet: cs, conceptSetLoading: false });
            } else {
              this.props.navigate([
                'workspaces',
                namespace,
                id,
                'data',
                'concepts',
              ]);
            }
          });
      }
    }

    getNavigationPath(action: string): string {
      const { namespace, id } = this.props.workspace;
      const { conceptSet } = this.state;
      let url = `/workspaces/${namespace}/${id}/`;
      switch (action) {
        case 'conceptSet':
          url += `data/concepts/sets/${conceptSet.id}`;
          break;
        case 'newConceptSet':
          url += 'data/concepts';
          break;
        case 'notebook':
          url += NOTEBOOKS_TAB_NAME;
          break;
        case 'dataSet':
          url += 'data/data-sets';
          break;
      }
      return url;
    }

    render() {
      const { conceptSet, conceptSetLoading } = this.state;
      return (
        <FadeBox
          style={{ margin: 'auto', marginTop: '1.5rem', width: '95.7%' }}
        >
          {conceptSetLoading && <SpinnerOverlay />}
          {conceptSet && (
            <React.Fragment>
              <h3 style={styles.conceptSetsHeader}>
                Concept Set Saved Successfully
              </h3>
              <div style={{ marginTop: '0.375rem' }}>
                The concept set
                <RouteLink
                  style={{ color: colors.accent, margin: '0 4px' }}
                  path={this.getNavigationPath('conceptSet')}
                >
                  {conceptSet.name}
                </RouteLink>
                has been saved.
              </div>
              <h3 style={{ ...styles.conceptSetsHeader, marginTop: '2.25rem' }}>
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
