import * as React from 'react';
import RSelect from 'react-select';
import * as fp from 'lodash/fp';

import { Profile, WorkspaceAccessLevel } from 'generated/fetch';

import { environment } from 'environments/environment';
import { CommonToggle } from 'app/components/admin/common-toggle';
import { AlertDanger } from 'app/components/alert';
import { Button } from 'app/components/buttons';
import { FadeBox } from 'app/components/containers';
import { FlexRow } from 'app/components/flex';
import { ListPageHeader, SmallHeader } from 'app/components/headers';
import { ClrIcon, NewWindowIcon } from 'app/components/icons';
import { Spinner } from 'app/components/spinners';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { WorkspaceCard } from 'app/pages/workspace/workspace-card';
import { workspacesApi } from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles, withUserProfile } from 'app/utils';
import { hasTierAccess } from 'app/utils/access-tiers';
import { convertAPIError } from 'app/utils/errors';
import { serverConfigStore } from 'app/utils/stores';
import { WorkspacePermissions } from 'app/utils/workspace-permissions';

import { NewWorkspaceButton } from './new-workspace-button';

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
  banner: {
    background: colorWithWhiteness(colors.warning, 0.85),
    borderRadius: 6,
    padding: '16px 20px',
    marginBottom: 20,
    alignItems: 'center',
  },
  text: {
    flex: 1,
    fontWeight: 500,
    fontSize: '13px',
    color: colors.dark,
  },
  icon: {
    marginRight: 10,
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
  showMigrated: boolean;
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
        showMigrated: false,
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
      const {
        errorText,
        filterLevels,
        workspaceList,
        workspacesLoading,
        showMigrated,
      } = this.state;

      const { profile } = this.props.profileState;

      const enableVwbMigration =
        serverConfigStore.get().config.enableVwbMigration;
      const migrationTestingGroup = profile?.migrationTestingGroup;

      const filters = [
        { label: 'Owner', value: ['OWNER'] },
        { label: 'Writer', value: ['WRITER'] },
        { label: 'Reader', value: ['READER'] },
        { label: 'All', value: null },
      ];

      const defaultFilter = filters.find((f) => f.label === 'All');

      const filteredList = workspaceList.filter(
        ({ accessLevel }) => !filterLevels || filterLevels.includes(accessLevel)
      );

      const migratedWorkspaces = filteredList.filter(
        (wp) => wp.workspace.migrationState === 'FINISHED'
      );

      const nonMigratedWorkspaces = filteredList.filter(
        (wp) => wp.workspace.migrationState !== 'FINISHED'
      );

      return (
        <FadeBox style={styles.fadeBox}>
          <div id='workspaces-list' style={{ padding: '0 1.5rem' }}>
            <ListPageHeader>Workspaces</ListPageHeader>

            {/* FILTER ROW */}
            <FlexRow style={{ marginTop: '0.5em', alignItems: 'center' }}>
              <div style={{ paddingRight: '0.75em' }}>Filter by</div>

              <RSelect
                aria-label='Access level filter selector'
                options={filters}
                defaultValue={defaultFilter}
                onChange={({ value }) =>
                  this.setState({
                    filterLevels: value
                      ? value.map((l) => WorkspaceAccessLevel[l])
                      : null,
                  })
                }
                styles={{
                  control: (base) => ({
                    ...base,
                    width: '100px',
                  }),
                }}
              />

              {/* RIGHT SIDE */}
              <div
                style={{
                  marginLeft: 'auto',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '12px',
                }}
              >
                {enableVwbMigration && migrationTestingGroup && (
                  <FlexRow style={{ alignItems: 'center', gap: '6px' }}>
                    <CommonToggle
                      name='show-migrated'
                      checked={showMigrated}
                      onToggle={() =>
                        this.setState((prev) => ({
                          showMigrated: !prev.showMigrated,
                        }))
                      }
                    />
                  </FlexRow>
                )}

                <Button
                  type='primary'
                  onClick={() =>
                    window.open(`${environment.vwbUiUrl}/workspaces`, '_blank')
                  }
                  style={{ height: '2.25rem' }}
                >
                  Open Verily Workbench <NewWindowIcon />
                </Button>
              </div>
            </FlexRow>

            {/* ERROR */}
            {errorText && (
              <AlertDanger>
                <ClrIcon shape='exclamation-circle' />
                {errorText}
              </AlertDanger>
            )}

            {/* CARDS */}
            <div style={styles.cardArea}>
              {workspacesLoading ? (
                <Spinner style={{ margin: '2rem auto' }} />
              ) : (
                <div
                  style={{
                    display: 'flex',
                    marginTop: '2rem',
                    flexWrap: 'wrap',
                  }}
                >
                  {showMigrated ? (
                    <>
                      {/* MIGRATED */}
                      <div style={{ width: '100%', marginBottom: '12px' }}>
                        <SmallHeader>Migrated Workspaces</SmallHeader>
                      </div>

                      {migratedWorkspaces.map((wp) => (
                        <WorkspaceCard
                          key={wp.workspace.namespace + '-m'}
                          workspace={wp.workspace}
                          accessLevel={wp.accessLevel}
                          reload={() => this.reloadWorkspaces()}
                          tierAccessDisabled={
                            !hasTierAccess(
                              profile,
                              wp.workspace.accessTierShortName
                            )
                          }
                          isMigratedView={showMigrated}
                        />
                      ))}

                      {/* LEGACY */}
                      <div style={{ width: '100%', marginTop: '16px' }}>
                        <div style={{ marginBottom: '12px' }}>
                          <SmallHeader>Legacy Workspaces</SmallHeader>
                        </div>
                        <FlexRow style={styles.banner}>
                          <ClrIcon
                            shape='exclamation-triangle'
                            size={16}
                            style={styles.icon}
                          />

                          <div style={styles.text}>
                            The following workspaces have already been migrated.
                            You’re still being billed for these workspaces. To
                            avoid duplicate charges, we recommend deleting them
                            when you’re finished.
                          </div>
                        </FlexRow>
                      </div>
                      <NewWorkspaceButton />
                      {filteredList.map((wp) => (
                        <WorkspaceCard
                          key={wp.workspace.namespace + '-l'}
                          workspace={wp.workspace}
                          accessLevel={wp.accessLevel}
                          reload={() => this.reloadWorkspaces()}
                          tierAccessDisabled={
                            !hasTierAccess(
                              profile,
                              wp.workspace.accessTierShortName
                            )
                          }
                          isMigratedView={false}
                        />
                      ))}
                    </>
                  ) : (
                    <>
                      {enableVwbMigration && migrationTestingGroup && (
                        <div style={{ width: '100%', marginBottom: '12px' }}>
                          <SmallHeader>Non-migrated Workspaces</SmallHeader>
                        </div>
                      )}
                      <NewWorkspaceButton />
                      {nonMigratedWorkspaces.map((wp) => (
                        <WorkspaceCard
                          key={wp.workspace.namespace}
                          workspace={wp.workspace}
                          accessLevel={wp.accessLevel}
                          reload={() => this.reloadWorkspaces()}
                          tierAccessDisabled={
                            !hasTierAccess(
                              profile,
                              wp.workspace.accessTierShortName
                            )
                          }
                          isMigratedView={false}
                        />
                      ))}
                    </>
                  )}
                </div>
              )}
            </div>
          </div>
        </FadeBox>
      );
    }
  }
);
