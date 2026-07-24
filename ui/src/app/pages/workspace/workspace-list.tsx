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
  workspaceViewFilter: 'all' | 'non-migrated' | 'archival';
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

      const vwbUserFacingIds = new Set(
        vwbWorkspaces
          .map((workspace) => workspace.userFacingId?.toLowerCase())
          .filter(Boolean)
      );

      const archivalRecoveryStates = [
        'NOT_STARTED',
        'REQUESTED',
        'RECOVERING',
        'FAILED',
      ];

      // If a workspace exists in both RW 1.0 and RW 2.0, keep only the RW 2.0 card.
      const filteredLegacyList = filteredList.filter((wp) => {
        // Always keep archival-related items in the legacy archived section.
        if (archivalRecoveryStates.includes(wp.workspace.recoveryState)) {
          return true;
        }

        return (
          !wp.workspace.namespace ||
          !vwbUserFacingIds.has(wp.workspace.namespace.toLowerCase())
        );
      });

      const nonMigratedWorkspaces = filteredLegacyList.filter(
        (wp) =>
          wp.workspace.migrationState !== 'FINISHED' &&
          wp.workspace.recoveryState == null
      );

      const archivedWorkspaces = filteredLegacyList.filter((wp) =>
        archivalRecoveryStates.includes(wp.workspace.recoveryState)
      );

      const showNonMigratedSection = workspaceViewFilter !== 'archival';
      const showArchivedSection = workspaceViewFilter !== 'non-migrated';

      return (
        <>
          <div style={styles.fadeBox}>
            <VwbImportantBanner
              title='The All of Us Workbench migration has ended'
              message={
                'Legacy Researcher Workbench is no longer available for ' +
                'active use. Existing workspaces have been archived and ' +
                'can be recovered through the workspace recovery process.'
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
                      workspaceViewFilter === 'non-migrated'
                        ? 'primary'
                        : 'secondary'
                    }
                    onClick={() =>
                      this.setState({ workspaceViewFilter: 'non-migrated' })
                    }
                    style={{ height: '2.25rem' }}
                  >
                    Non-Migrated
                  </Button>
                  <Button
                    type={
                      workspaceViewFilter === 'archival'
                        ? 'primary'
                        : 'secondary'
                    }
                    onClick={() =>
                      this.setState({ workspaceViewFilter: 'archival' })
                    }
                    style={{ height: '2.25rem' }}
                  >
                    Archival
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
                    Open RW 2.0 <NewWindowIcon />
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
                  <div style={{ width: '100%' }}>
                    <VwbWorkspaces
                      loading={workspacesLoading}
                      workspaces={vwbWorkspaces}
                      currentUsername={profile.username}
                    />
                  </div>

                  {/* NON-MIGRATED (includes waiting-to-archive workspaces) */}
                  {showNonMigratedSection &&
                    nonMigratedWorkspaces.length > 0 && (
                      <>
                        <div
                          style={{
                            width: '100%',
                            marginTop: '24px',
                            marginBottom: '12px',
                          }}
                        >
                          <SmallHeader>Non-Migrated Workspaces</SmallHeader>
                        </div>

                        {profile.migrationTestingGroup &&
                          enableVwbMigration && <NewWorkspaceButton />}

                        {nonMigratedWorkspaces.map((wp) => (
                          <WorkspaceCard
                            key={`${wp.workspace.namespace}-non-migrated`}
                            workspace={wp.workspace}
                            accessLevel={wp.accessLevel}
                            reload={() => this.reloadWorkspaces()}
                            tierAccessDisabled={
                              !hasTierAccess(
                                profile,
                                wp.workspace.accessTierShortName
                              ) || !profile.migrationTestingGroup
                            }
                            isMigratedView={false}
                          />
                        ))}
                      </>
                    )}

                  {/* ARCHIVED (archived/requested/recovering/failed) */}
                  {showArchivedSection && archivedWorkspaces.length > 0 && (
                    <>
                      <div
                        style={{
                          width: '100%',
                          marginTop: '24px',
                          marginBottom: '12px',
                        }}
                      >
                        <SmallHeader>Archived Workspaces</SmallHeader>
                      </div>

                      <div
                        style={{
                          width: '100%',
                          background: '#F5F7FA',
                          border: '1px solid #D8DDE6',
                          borderRadius: '6px',
                          padding: '12px',
                          marginBottom: '16px',
                          color: colors.dark,
                          fontSize: '13px',
                        }}
                      >
                        These workspaces have been archived from Legacy
                        Workbench. Select Recover Workspace to restore them into
                        RW 2.0.
                      </div>

                      {archivedWorkspaces.map((wp) => (
                        <WorkspaceCard
                          key={`${wp.workspace.namespace}-archived`}
                          workspace={wp.workspace}
                          accessLevel={wp.accessLevel}
                          reload={() => this.reloadWorkspaces()}
                          tierAccessDisabled={
                            !hasTierAccess(
                              profile,
                              wp.workspace.accessTierShortName
                            ) ||
                            wp.accessLevel !== WorkspaceAccessLevel.OWNER ||
                            wp.workspace.recoveryState !==
                              WorkspaceRecoveryStatus.NOT_STARTED
                          }
                          isMigratedView={false}
                        />
                      ))}
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
