import * as React from 'react';
import { RouteComponentProps, withRouter } from 'react-router-dom';
import * as fp from 'lodash/fp';

import { Profile, WorkspaceResponseListResponse } from 'generated/fetch';

import { StyledExternalLink } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { Header } from 'app/components/headers';
import { ClrIcon } from 'app/components/icons';
import { MigrationModal } from 'app/components/migration/migration-modal';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { VwbCreateWorkspaceModal } from 'app/pages/workspace/vwb-create-workspace-modal';
import { profileApi, workspacesApi } from 'app/services/swagger-fetch-clients';
import colors, { addOpacity } from 'app/styles/colors';
import { reactStyles, withUserProfile } from 'app/utils';
import { fetchWithSystemErrorHandler } from 'app/utils/errors';
import { NavigationProps } from 'app/utils/navigation';
import { serverConfigStore } from 'app/utils/stores';
import { withNavigation } from 'app/utils/with-navigation-hoc';
import { supportUrls } from 'app/utils/zendesk';

import { ActiveWorkspaces } from './active-workspaces';
import { LegacyWorkbenchEndedBanner } from './legacy-workbench-ended-banner';
import { QuickTourAndVideos } from './quick-tour-and-videos';
// import { VwbBanner } from './vwb-banner';
import { VwbMigrationBanner } from './vwb-migration-banner';

export const styles = reactStyles({
  pageWrapper: {
    margin: 0,
    maxWidth: 'none',
    padding: '0.6rem 0.5rem 1.5rem',
  },
  heading: {
    color: colors.primary,
    fontSize: '1.7rem',
    fontWeight: 600,
    lineHeight: 1.15,
    marginBottom: '0.2rem',
    marginTop: 0,
  },
  subtitle: {
    color: addOpacity(colors.dark, 0.75).toString(),
    fontSize: '1rem',
    fontStyle: 'italic',
    marginBottom: '1rem',
  },
  contentGrid: {
    alignItems: 'flex-start',
    gap: '1.5rem',
    marginTop: '1.5rem',
  },
  leftColumn: {
    flex: 2,
    gap: '1.5rem',
    minWidth: 0,
  },
  rightColumn: {
    flex: 1,
    gap: '1.5rem',
    minWidth: 320,
  },
  panel: {
    backgroundColor: colors.white,
    border: `1px solid ${addOpacity(colors.dark, 0.12).toString()}`,
    borderRadius: '0.35rem',
    boxShadow: `0 1px 2px ${addOpacity(colors.dark, 0.08).toString()}`,
    padding: '1.1rem 1.3rem',
  },
  panelTitle: {
    color: colors.primary,
    fontSize: '1.3rem',
    fontWeight: 600,
    margin: 0,
  },
  tutorialFrame: {
    border: 0,
    borderRadius: '0.25rem',
    marginTop: '0.8rem',
    width: '100%',
  },
  spotlightPanel: {
    backgroundColor: addOpacity(colors.secondary, 0.2).toString(),
    borderColor: addOpacity(colors.secondary, 0.45).toString(),
  },
  spotlightCard: {
    backgroundColor: colors.white,
    border: `1px solid ${addOpacity(colors.dark, 0.12).toString()}`,
    borderRadius: '0.3rem',
    marginTop: '0.75rem',
    padding: '0.9rem 1rem',
  },
  resourceRow: {
    alignItems: 'center',
    borderTop: `1px solid ${addOpacity(colors.dark, 0.15).toString()}`,
    gap: '0.75rem',
    padding: '0.6rem 0',
  },
  resourceIcon: {
    alignItems: 'center',
    backgroundColor: addOpacity(colors.secondary, 0.18).toString(),
    borderRadius: '50%',
    display: 'flex',
    height: '2rem',
    justifyContent: 'center',
    width: '2rem',
  },
  calendarFrame: {
    border: '1px solid #777',
    borderRadius: '0.25rem',
    marginTop: '0.8rem',
    width: '100%',
  },
});

const HomepageHeader = () => {
  return (
    <FlexColumn>
      <Header style={styles.heading}>
        Welcome to your Researcher Workbench
      </Header>
      <div style={styles.subtitle}>
        Everything you need to manage your research work
      </div>
    </FlexColumn>
  );
};

const SpotlightPanel = () => {
  return (
    <div style={{ ...styles.panel, ...styles.spotlightPanel }}>
      <h2 style={styles.panelTitle}>Announcements</h2>
      <div style={styles.spotlightCard}>
        <div
          style={{
            color: addOpacity(colors.dark, 0.7).toString(),
            fontSize: '0.95rem',
            marginBottom: '0.45rem',
          }}
        >
          Oct 20, 2024
        </div>
        <div
          style={{
            color: colors.primary,
            fontSize: '1rem',
            fontWeight: 600,
            marginBottom: '0.55rem',
          }}
        >
          Genomic Data Release
        </div>
        <div
          style={{
            color: addOpacity(colors.dark, 0.8).toString(),
            fontSize: '0.88rem',
            lineHeight: 1.4,
          }}
        >
          The latest Genomic Data Release V# is now available, featuring
          expanded datasets and improved quality metrics.
        </div>
        <StyledExternalLink
          href={supportUrls.helpCenter}
          target='_blank'
          rel='noopener noreferrer'
          style={{
            color: colors.accent,
            display: 'inline-block',
            marginTop: '0.65rem',
          }}
        >
          Learn more
          <ClrIcon shape='arrow' size={12} style={{ marginLeft: '0.3rem' }} />
        </StyledExternalLink>
      </div>
    </div>
  );
};

const ResourcesPanel = () => {
  const resources = [
    {
      title: 'Data Dictionary',
      subtitle: 'Explore available datasets and variables',
      href: supportUrls.dataDictionary,
      icon: 'search',
    },
    {
      title: 'Getting Started Guide',
      subtitle: 'Step-by-step instructions for new users',
      href: supportUrls.gettingStarted,
      icon: 'book',
    },
    {
      title: 'New User Orientation',
      subtitle: 'Onboard to the All of Us Researcher Workbench',
      href: supportUrls.videos,
      icon: 'users',
    },
    {
      title: 'Training Modules',
      subtitle:
        'training.researchallofus.org (sign in with your Workbench username)',
      href: 'https://training.researchallofus.org',
      icon: 'form',
    },
  ];

  return (
    <div style={styles.panel}>
      <h2 style={styles.panelTitle}>Resources</h2>
      <FlexColumn style={{ marginTop: '0.65rem' }}>
        {resources.map((resource) => (
          <StyledExternalLink
            key={resource.title}
            href={resource.href}
            target='_blank'
            rel='noopener noreferrer'
            style={{ color: 'inherit', textDecoration: 'none' }}
            aria-label={`Open ${resource.title}`}
          >
            <FlexRow style={styles.resourceRow}>
              <div style={styles.resourceIcon}>
                <ClrIcon
                  shape={resource.icon}
                  size={16}
                  style={{ color: colors.accent }}
                />
              </div>
              <FlexColumn>
                <div
                  style={{
                    color: colors.dark,
                    fontSize: '0.95rem',
                    fontWeight: 600,
                    marginBottom: '0.15rem',
                  }}
                >
                  {resource.title}
                </div>
                <div
                  style={{
                    color: addOpacity(colors.dark, 0.75).toString(),
                    fontSize: '0.8rem',
                    lineHeight: 1.35,
                  }}
                >
                  {resource.subtitle}
                </div>
              </FlexColumn>
            </FlexRow>
          </StyledExternalLink>
        ))}
      </FlexColumn>
    </div>
  );
};

const TutorialVideoPanel = () => {
  return (
    <div style={styles.panel}>
      <h2 style={styles.panelTitle}>Registration and Access Tutorial</h2>
      <iframe
        title='Registration and Access Tutorial'
        style={styles.tutorialFrame}
        width='100%'
        height='460'
        src='https://www.youtube.com/embed/744gVoEQS6k?rel=0&modestbranding=1'
        allow='accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share'
        referrerPolicy='strict-origin-when-cross-origin'
        allowFullScreen
      />
    </div>
  );
};

const EventCalendarPanel = () => {
  const calendarUrl =
    'https://calendar.google.com/calendar/embed?height=600&wkst=1' +
    '&ctz=America%2FChicago&showPrint=0' +
    '&src=c2FtYW50aGFsLnN0ZXdhcnRAdnVtYy5vcmc' +
    '&src=MjEydGJ2bWY2a3VpZXRpcXUxdGVxZGpjdmtAZ3JvdXAuY2FsZW5kYXIuZ29vZ2xlLmNvbQ' +
    '&color=%23039be5&color=%233f51b5';

  return (
    <div style={styles.panel}>
      <h2 style={styles.panelTitle}>Event Calendar</h2>
      <iframe
        title='User Support Hub Event Calendar'
        style={styles.calendarFrame}
        width='100%'
        height='360'
        src={calendarUrl}
      />
    </div>
  );
};

interface Props
  extends WithSpinnerOverlayProps,
    NavigationProps,
    RouteComponentProps {
  profileState: {
    profile: Profile;
    reload: Function;
  };
}

interface State {
  firstVisit: boolean;
  firstVisitTraining: boolean;
  userWorkspacesResponse: WorkspaceResponseListResponse;
  showMigrationModal: boolean;
  showCreateWorkspaceModal: boolean;
}

export const Homepage = fp.flow(
  withUserProfile(),
  withNavigation,
  withRouter
)(
  class extends React.Component<Props, State> {
    private pageId = 'homepage';
    private timer: NodeJS.Timer;

    constructor(props: Props) {
      super(props);
      this.state = {
        firstVisit: undefined,
        firstVisitTraining: true,
        userWorkspacesResponse: undefined,
        showMigrationModal: false,
        showCreateWorkspaceModal: false,
      };
    }

    closeCreateWorkspaceModal = () => {
      this.setState({ showCreateWorkspaceModal: false });
    };

    componentDidMount() {
      this.props.hideSpinner();
      this.fetchWorkspaces();
      this.callProfile();
    }

    componentDidUpdate(prevProps) {
      const {
        profileState: { profile },
      } = this.props;
      if (!fp.isEqual(prevProps.profileState.profile, profile)) {
        this.callProfile();
      }
    }

    componentWillUnmount() {
      clearTimeout(this.timer);
    }

    setFirstVisit() {
      this.setState({ firstVisit: true });
      profileApi().updatePageVisits({ page: this.pageId });
    }

    async callProfile() {
      const {
        profileState: { profile, reload },
      } = this.props;

      if (fp.isEmpty(profile)) {
        setTimeout(() => {
          reload();
        }, 10000);
      } else {
        if (profile.pageVisits) {
          if (!profile.pageVisits.some((v) => v.page === this.pageId)) {
            this.setFirstVisit();
          }
          if (profile.pageVisits.some((v) => v.page === 'moodle')) {
            this.setState({ firstVisitTraining: false });
          }
        } else {
          // page visits is null; is first visit
          this.setFirstVisit();
        }
      }
    }

    async fetchWorkspaces() {
      return fetchWithSystemErrorHandler(() =>
        workspacesApi().getWorkspaces()
      ).then((response) => this.setState({ userWorkspacesResponse: response }));
    }

    closeMigrationModal = () => {
      this.setState({ showMigrationModal: false });
    };

    render() {
      const {
        profileState: {
          profile: { migrationTestingGroup },
        },
      } = this.props;
      const { firstVisit, userWorkspacesResponse } = this.state;
      const { enableVWBHomepageBanner, restrictLegacyAccess } =
        serverConfigStore.get().config;
      const workspaces = userWorkspacesResponse?.items || [];

      return (
        <React.Fragment>
          <FlexColumn style={styles.pageWrapper}>
            <HomepageHeader />
            {enableVWBHomepageBanner &&
              (restrictLegacyAccess && !migrationTestingGroup ? (
                <LegacyWorkbenchEndedBanner />
              ) : (
                <VwbMigrationBanner />
              ))}

            <FlexRow style={styles.contentGrid}>
              <FlexColumn style={styles.leftColumn}>
                <ActiveWorkspaces workspaces={workspaces} />
                <TutorialVideoPanel />
              </FlexColumn>

              <FlexColumn style={styles.rightColumn}>
                <SpotlightPanel />
                <ResourcesPanel />
                <EventCalendarPanel />
              </FlexColumn>
            </FlexRow>
          </FlexColumn>
          <QuickTourAndVideos showQuickTourInitially={firstVisit} />
          {this.state.showMigrationModal && (
            <MigrationModal onClose={this.closeMigrationModal} />
          )}
          {this.state.showCreateWorkspaceModal && (
            <VwbCreateWorkspaceModal onClose={this.closeCreateWorkspaceModal} />
          )}
        </React.Fragment>
      );
    }
  }
);
