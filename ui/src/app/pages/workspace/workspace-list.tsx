import * as React from 'react';
import RSelect from 'react-select';
import * as fp from 'lodash/fp';

import { Profile, WorkspaceAccessLevel } from 'generated/fetch';

import { AlertDanger } from 'app/components/alert';
import { FadeBox } from 'app/components/containers';
import { FlexRow } from 'app/components/flex';
import { ListPageHeader } from 'app/components/headers';
import { ClrIcon } from 'app/components/icons';
import { Spinner } from 'app/components/spinners';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { NewWorkspaceButton } from 'app/pages/workspace/new-workspace-button';
import { WorkspaceCard } from 'app/pages/workspace/workspace-card';
import { workspacesApi } from 'app/services/swagger-fetch-clients';
import { reactStyles, withUserProfile } from 'app/utils';
import { hasTierAccess } from 'app/utils/access-tiers';
import { convertAPIError } from 'app/utils/errors';
import { WorkspacePermissions } from 'app/utils/workspace-permissions';

const styles = reactStyles({
  fadeBox: {
    margin: '1.5rem auto 0 auto',
    width: '97.5%',
    padding: '0 1.5rem',
  },
  cardArea: {
    display: 'flex',
    justifyContent: 'flex-start',
    flexWrap: 'wrap',
  },
});

interface WorkspaceListProps extends WithSpinnerOverlayProps {
  profileState: { profile: Profile; reload: Function; updateCache: Function };
}

interface State {
  workspacesLoading: boolean;
  workspaceList: WorkspacePermissions[];
  filterLevels: WorkspaceAccessLevel[] | null;
  errorText: string;
  firstSignIn: Date;
}

export const WorkspaceList = fp.flow(withUserProfile())(
  class extends React.Component<WorkspaceListProps, State> {
    private timer: NodeJS.Timer;

    constructor(props) {
      super(props);
      this.state = {
        workspacesLoading: true,
        workspaceList: [],
        filterLevels: null,
        errorText: '',
        firstSignIn: undefined,
      };
    }

    componentDidMount() {
      this.props.hideSpinner();
      this.reloadWorkspaces();
    }

    componentWillUnmount() {
      clearTimeout(this.timer);
    }

    async reloadWorkspaces() {
      this.setState({ workspacesLoading: true });
      try {
        const workspacesReceived = (await workspacesApi().getWorkspaces())
          .items;
        workspacesReceived.sort((a, b) =>
          a.workspace.name.localeCompare(b.workspace.name)
        );
        this.setState({
          workspaceList: workspacesReceived.map(
            (w) => new WorkspacePermissions(w)
          ),
          workspacesLoading: false,
        });
      } catch (e) {
        const response = await convertAPIError(e);
        this.setState({ errorText: response.message });
      }
    }

    render() {
      const { errorText, filterLevels, workspaceList, workspacesLoading } =
        this.state;

      const { profile } = this.props.profileState;

      // Maps each "Filter by" dropdown element to a set of access levels to display.
      const filters = [
        { label: 'Owner', value: ['OWNER'] },
        { label: 'Writer', value: ['WRITER'] },
        { label: 'Reader', value: ['READER'] },
        { label: 'All', value: null },
      ];
      const defaultFilter = filters.find((f) => f.label === 'All');

      return (
        <React.Fragment>
          <FadeBox style={styles.fadeBox}>
            <div id='workspaces-list' style={{ padding: '0 1.5rem' }}>
              <ListPageHeader>Workspaces</ListPageHeader>
              <FlexRow style={{ marginTop: '0.5em' }}>
                <div style={{ margin: '0', padding: '0.5em 0.75em 0 0' }}>
                  Filter by
                </div>
                <RSelect
                  data-test-id='access-level-filter'
                  options={filters}
                  defaultValue={defaultFilter}
                  onChange={({ value }) =>
                    this.setState({ filterLevels: value })
                  }
                  styles={{
                    control: (baseStyles) => ({
                      ...baseStyles,
                      width: '100px',
                    }),
                  }}
                />
              </FlexRow>
              {errorText && (
                <AlertDanger>
                  <ClrIcon shape='exclamation-circle' />
                  {errorText}
                </AlertDanger>
              )}
              <div style={styles.cardArea}>
                {workspacesLoading ? (
                  <Spinner style={{ margin: '2.25rem auto' }} />
                ) : (
                  <div
                    style={{
                      display: 'flex',
                      marginTop: '2.25rem',
                      flexWrap: 'wrap',
                    }}
                  >
                    <NewWorkspaceButton />
                    {workspaceList
                      .filter(
                        ({ accessLevel }) =>
                          !filterLevels || filterLevels.includes(accessLevel)
                      )
                      .map((wp) => {
                        return (
                          <WorkspaceCard
                            key={wp.workspace.namespace}
                            workspace={wp.workspace}
                            accessLevel={wp.accessLevel}
                            aws={wp.workspace.aws}
                            reload={() => this.reloadWorkspaces()}
                            tierAccessDisabled={
                              !hasTierAccess(
                                profile,
                                wp.workspace.accessTierShortName
                              )
                            }
                          />
                        );
                      })}
                  </div>
                )}
              </div>
            </div>
          </FadeBox>
        </React.Fragment>
      );
    }
  }
);
