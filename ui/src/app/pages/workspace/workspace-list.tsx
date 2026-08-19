import * as React from 'react';
import RSelect from 'react-select';
import * as fp from 'lodash/fp';

import {
  Profile,
  VwbWorkspace,
  WorkspaceAccessLevel,
  WorkspaceRecoveryStatus,
} from 'generated/fetch';

import { environment } from 'environments/environment';
import { AlertDanger } from 'app/components/alert';
import { Button } from 'app/components/buttons';
import { FadeBox } from 'app/components/containers';
import { FlexRow } from 'app/components/flex';
import { ListPageHeader, SmallHeader } from 'app/components/headers';
import { ClrIcon, NewWindowIcon } from 'app/components/icons';
import { Spinner } from 'app/components/spinners';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { VwbWorkspaces } from 'app/pages/homepage/rw2-workspaces';
import { WorkspaceCard } from 'app/pages/workspace/workspace-card';
import { workspacesApi } from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles, withUserProfile } from 'app/utils';
import { hasTierAccess } from 'app/utils/access-tiers';
import { convertAPIError } from 'app/utils/errors';
import { serverConfigStore } from 'app/utils/stores';
import { WorkspacePermissions } from 'app/utils/workspace-permissions';

import { NewWorkspaceButton } from './new-workspace-button';
import { VwbImportantBanner } from './vwb-important-banner';

type VwbWorkspaceCardModel = VwbWorkspace & {
  role?: 'OWNER' | 'WRITER' | 'READER';
  dataCollection?: string;
  lastChanged?: string;
  createdBy?: string;
};

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
  vwbWorkspaces: VwbWorkspaceCardModel[];
  workspaceList: WorkspacePermissions[];
  filterLevels: WorkspaceAccessLevel[] | null;
  errorText: string;
  firstSignIn: Date;
  workspaceViewFilter: 'all' | 'verily' | 'legacy';
}

export const WorkspaceList = fp.flow(withUserProfile())(
  class extends React.Component<WorkspaceListProps, State> {
    private timer: NodeJS.Timer;

    constructor(props) {
      super(props);
      this.state = {
        workspacesLoading: true,
        vwbWorkspaces: [],
        workspaceList: [],
        filterLevels: null,
        errorText: '',
        firstSignIn: undefined,
        workspaceViewFilter: 'all',
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
        const [legacyResponse, vwbResponse] = await Promise.all([
          workspacesApi().getWorkspaces(),
          workspacesApi()
            .getVwbWorkspaces()
            .catch(() => ({ items: [] })),
        ]);

        const workspacesReceived = legacyResponse.items;
        const resolvedVwbWorkspaces = (vwbResponse.items ??
          []) as VwbWorkspaceCardModel[];

        workspacesReceived.sort((a, b) =>
          a.workspace.name.localeCompare(b.workspace.name)
        );

        this.setState({
          vwbWorkspaces: resolvedVwbWorkspaces,
          workspaceList: workspacesReceived.map(
            (w) => new WorkspacePermissions(w)
          ),
          workspacesLoading: false,
        });
      } catch (e) {
        const response = await convertAPIError(e);
        this.setState({
          errorText: response.message,
          workspacesLoading: false,
        });
      }
    }

    render() {
      const {
        errorText,
        filterLevels,
        workspaceList,
        vwbWorkspaces,
        workspacesLoading,
        workspaceViewFilter,
      } = this.state;

      const { profile } = this.props.profileState;
      const enableVwbMigration =
        serverConfigStore.get().config.enableVwbMigration;

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

      const archivalRecoveryStates = [
        'NOT_STARTED',
        'REQUESTED',
        'RECOVERING',
        'FAILED',
        'RECOVERED',
      ];

      const legacyWorkspaces = filteredList;

      // All legacy workspaces shown; section visibility is controlled by workspaceViewFilter
      const filteredLegacyWorkspaces = legacyWorkspaces;

      const showVerilySection =
        workspaceViewFilter === 'all' || workspaceViewFilter === 'verily';
      const showLegacySection =
        workspaceViewFilter === 'all' || workspaceViewFilter === 'legacy';

      return (
        <>
          <div style={styles.fadeBox}>
            <VwbImportantBanner
              title='The Workspaces migration has ended'
              message={
                'The original Researcher Workbench is no longer available for active use. ' +
                'Existing workspaces have been archived and can be recovered through the ' +
                'workspace recovery process.'
              }
            />
          </div>
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

                <div style={{ marginLeft: '0.75em', paddingRight: '0.5em' }}>
                  Show
                </div>

                <FlexRow style={{ gap: '0.4em' }}>
                  <Button
                    type={
                      workspaceViewFilter === 'all' ? 'primary' : 'secondary'
                    }
                    onClick={() =>
                      this.setState({ workspaceViewFilter: 'all' })
                    }
                    style={{ height: '2.25rem' }}
                  >
                    All
                  </Button>
                  <Button
                    type={
                      workspaceViewFilter === 'verily' ? 'primary' : 'secondary'
                    }
                    onClick={() =>
                      this.setState({ workspaceViewFilter: 'verily' })
                    }
                    style={{ height: '2.25rem' }}
                  >
                    Workspaces
                  </Button>
                  <Button
                    type={
                      workspaceViewFilter === 'legacy' ? 'primary' : 'secondary'
                    }
                    onClick={() =>
                      this.setState({ workspaceViewFilter: 'legacy' })
                    }
                    style={{ height: '2.25rem' }}
                  >
                    Legacy Workspaces
                  </Button>
                </FlexRow>

                {/* RIGHT SIDE */}
                <div
                  style={{
                    marginLeft: 'auto',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '12px',
                  }}
                >
                  <Button
                    type='primary'
                    onClick={() =>
                      window.open(
                        `${environment.vwbUiUrl}/workspaces`,
                        '_blank'
                      )
                    }
                    style={{ height: '2.25rem' }}
                  >
                    Open Researcher Workbench <NewWindowIcon />
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
              {workspacesLoading ? (
                <div style={{ textAlign: 'center' }}>
                  <Spinner style={{ margin: '2rem auto' }} />
                </div>
              ) : (
                <div
                  style={{
                    display: 'flex',
                    marginTop: '2rem',
                    flexWrap: 'wrap',
                  }}
                >
                  {showVerilySection && (
                    <div style={{ width: '100%' }}>
                      <VwbWorkspaces
                        loading={workspacesLoading}
                        workspaces={vwbWorkspaces}
                        currentUsername={profile.username}
                      />
                    </div>
                  )}

                  {showLegacySection && legacyWorkspaces.length > 0 && (
                    <>
                      <div
                        style={{
                          width: '100%',
                          marginTop: '24px',
                          marginBottom: '12px',
                        }}
                      >
                        <SmallHeader>Legacy Workspaces</SmallHeader>
                      </div>

                      <div
                        style={{
                          width: '100%',
                          background: '#FFF8E7',
                          border: '1px solid #F5C842',
                          borderRadius: '6px',
                          padding: '12px',
                          marginBottom: '16px',
                          color: colors.dark,
                          fontSize: '13px',
                        }}
                      >
                        Workspaces may be migrated or in archival flow. Use the
                        status badges and filters to find what you need.
                        Deleting a migrated or recovered workspace reduces
                        legacy costs and does not affect the matching Researcher
                        Workbench workspace.
                      </div>

                      {profile.migrationTestingGroup && enableVwbMigration && (
                        <NewWorkspaceButton />
                      )}

                      {filteredLegacyWorkspaces.map((wp) => {
                        const recoveryState = wp.workspace.recoveryState;
                        const isMigrationFinished =
                          wp.workspace.migrationState === 'FINISHED';
                        const isInArchivalFlow =
                          (recoveryState &&
                            archivalRecoveryStates.includes(recoveryState)) ||
                          (!recoveryState && !isMigrationFinished);
                        const isMigrated =
                          isMigrationFinished && !isInArchivalFlow;
                        const isOwner =
                          wp.accessLevel === WorkspaceAccessLevel.OWNER;
                        const isCreator =
                          wp.workspace.creatorUser?.userName?.toLowerCase() ===
                          profile.username?.toLowerCase();
                        const nonMigratedTierAccessDisabled =
                          !hasTierAccess(
                            profile,
                            wp.workspace.accessTierShortName
                          ) || !profile.migrationTestingGroup;

                        const archivalFlowTierAccessDisabled =
                          !hasTierAccess(
                            profile,
                            wp.workspace.accessTierShortName
                          ) ||
                          wp.accessLevel !== WorkspaceAccessLevel.OWNER ||
                          wp.workspace.recoveryState !==
                            WorkspaceRecoveryStatus.NOT_STARTED;

                        const tierAccessDisabled = isMigrated
                          ? false
                          : isInArchivalFlow
                          ? archivalFlowTierAccessDisabled
                          : nonMigratedTierAccessDisabled;

                        return (
                          <WorkspaceCard
                            key={`${wp.workspace.namespace}-${wp.workspace.terraName}-legacy`}
                            workspace={wp.workspace}
                            accessLevel={wp.accessLevel}
                            reload={() => this.reloadWorkspaces()}
                            tierAccessDisabled={tierAccessDisabled}
                            isMigratedView={false}
                            disableOpenAction={isMigrated}
                            showDeleteAction={
                              recoveryState !==
                              WorkspaceRecoveryStatus.RECOVERING
                            }
                            canDeleteAction={isOwner || isCreator}
                          />
                        );
                      })}

                      {filteredLegacyWorkspaces.length === 0 && (
                        <div style={{ width: '100%', marginBottom: '16px' }}>
                          No legacy workspaces match this filter.
                        </div>
                      )}
                    </>
                  )}
                </div>
              )}
            </div>
          </FadeBox>
        </>
      );
    }
  }
);
