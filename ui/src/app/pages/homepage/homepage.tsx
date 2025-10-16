import * as React from 'react';
import { RouteComponentProps, withRouter } from 'react-router-dom';
import * as fp from 'lodash/fp';
import { faUpRightFromSquare } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { Profile, WorkspaceResponseListResponse } from 'generated/fetch';

import { environment } from 'environments/environment';
import {
  Button,
  StyledExternalLink,
  StyledRouterLink,
} from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { Header, SemiBoldHeader, SmallHeader } from 'app/components/headers';
import { ClrIcon } from 'app/components/icons';
import { CustomBulletList, CustomBulletListItem } from 'app/components/lists';
import { AoU } from 'app/components/text-wrappers';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { RecentResources } from 'app/pages/homepage/recent-resources';
import { RecentWorkspaces } from 'app/pages/homepage/recent-workspaces';
import { profileApi, workspacesApi } from 'app/services/swagger-fetch-clients';
import colors, { addOpacity } from 'app/styles/colors';
import { reactStyles, withUserProfile } from 'app/utils';
import { AnalyticsTracker } from 'app/utils/analytics';
import { fetchWithSystemErrorHandler } from 'app/utils/errors';
import { NavigationProps, useNavigation } from 'app/utils/navigation';
import { withNavigation } from 'app/utils/with-navigation-hoc';
import { supportUrls } from 'app/utils/zendesk';

import { QuickTourAndVideos } from './quick-tour-and-videos';

export const styles = reactStyles({
  bottomBanner: {
    width: '100%',
    display: 'flex',
    backgroundColor: colors.primary,
    paddingLeft: '5.25rem',
    alignItems: 'center',
    marginTop: '3rem',
  },
  bottomLinks: {
    color: colors.white,
    fontSize: '.75rem',
    height: '1.5rem',
    marginLeft: '3.75rem',
    fontWeight: 400,
  },
  contentWrapperLeft: {
    paddingLeft: '3%',
    width: '40%',
  },
  contentWrapperRight: {
    justifyContent: 'space-between',
    width: '60%',
  },
  fadeBox: {
    margin: '1.5rem 0 0 3%',
    width: '95%',
    padding: '0 0.15rem',
  },
  logo: {
    height: '5.25rem',
    width: '10.5rem',
    lineHeight: '85px',
  },
  mainHeader: {
    color: colors.primary,
    fontSize: 28,
    fontWeight: 400,
    letterSpacing: 'normal',
  },
  pageWrapper: {
    marginLeft: '-1.5rem',
    marginRight: '-0.9rem',
    justifyContent: 'space-between',
    fontSize: '1.2em',
  },
  welcomeMessageIcon: {
    height: '3.375rem',
    width: '4.125rem',
  },
});

const VWB_USER_SUPPORT_HUB_URL =
  'https://support.researchallofus.org/hc/en-us/articles/39985865987732-What-to-expect-during-the-Researcher-Workbench-Migration';

const WelcomeHeader = () => {
  return (
    <FlexRow
      style={{
        background: colors.banner,
        borderRadius: '0.5rem',
        margin: '1.5rem 3% 0 3%',
        padding: '1.5rem',
      }}
    >
      <FlexColumn style={{ color: colors.primary, width: '50%' }}>
        <Header
          style={{
            fontWeight: 600,
            fontSize: '32px',
            lineHeight: '40px',
            margin: 0,
          }}
        >
          A more powerful way to work with <AoU /> data
        </Header>
        <SmallHeader style={{ marginTop: '1rem' }}>
          The new{' '}
          <span style={{ fontWeight: 700 }}>
            {' '}
            <AoU /> Researcher experience, built on the{' '}
            <StyledExternalLink
              href={environment.vwbUiUrl}
              style={{ color: colors.primary, textDecoration: 'underline' }}
              target='_blank'
            >
              Verily Platform
            </StyledExternalLink>
          </span>{' '}
          offers expanded data exploration and analysis tools, an updated
          interface, and access to enhanced cloud capabilities.
          <ul style={{ paddingLeft: '1.5rem' }}>
            <li>
              You can explore the updated Workbench now and get early access to
              new tools
            </li>
            <li>
              In the coming months, researchers will gradually transition to the
              new Workbench
            </li>
            <li>
              Until then, the legacy environment remains available for you to
              use
            </li>
            <li>
              For details on the update and transition timeline, visit the{' '}
              <StyledExternalLink
                href={VWB_USER_SUPPORT_HUB_URL}
                style={{ color: colors.primary, textDecoration: 'underline' }}
                target='_blank'
              >
                User Support Hub
              </StyledExternalLink>
            </li>
          </ul>
        </SmallHeader>
        <Button style={{ height: '45px', width: '300px' }}>
          <a
            href={environment.vwbUiUrl}
            target='_blank'
            style={{ color: colors.white }}
          >
            Explore the Verily Platform
            <FontAwesomeIcon
              icon={faUpRightFromSquare}
              title='Explore the Verily Platform'
              style={{ marginLeft: '0.5rem' }}
            />
          </a>
        </Button>
      </FlexColumn>
      <FlexColumn style={{ marginLeft: '1.5rem', width: '45%' }}>
        Carousel ...
      </FlexColumn>
    </FlexRow>
  );
};

const Workspaces = ({ onChange }: { onChange: () => void }) => {
  const [navigate] = useNavigation();

  return (
    <FlexColumn>
      <FlexRow
        style={{ justifyContent: 'space-between', alignItems: 'center' }}
      >
        <FlexRow style={{ alignItems: 'center' }}>
          <SemiBoldHeader style={{ marginTop: '0px' }}>
            Workspaces
          </SemiBoldHeader>
          <ClrIcon
            aria-label='Create Workspace'
            shape='plus-circle'
            size={30}
            className={'is-solid'}
            style={{
              color: colors.accent,
              marginLeft: '1.5rem',
              cursor: 'pointer',
            }}
            onClick={() => {
              AnalyticsTracker.Workspaces.OpenCreatePage();
              navigate(['workspaces', 'build']);
            }}
          />
        </FlexRow>
        <span
          style={{
            alignSelf: 'flex-end',
            color: colors.accent,
            cursor: 'pointer',
          }}
          onClick={() => navigate(['workspaces'])}
        >
          See all workspaces
        </span>
      </FlexRow>
      <RecentWorkspaces {...{ onChange }} />
    </FlexColumn>
  );
};

const GettingStarted = () => {
  return (
    <div
      data-test-id='getting-started'
      style={{
        backgroundColor: addOpacity(colors.primary, 0.1).toString(),
        color: colors.primary,
        borderRadius: 10,
        margin: '2em 0em',
      }}
    >
      <div style={{ margin: '1em 2em' }}>
        <h2 style={{ fontWeight: 600, marginTop: 0 }}>
          Here are some tips to get you started:
        </h2>
        <CustomBulletList>
          <CustomBulletListItem bullet='→'>
            Create a{' '}
            <StyledExternalLink
              href='https://support.google.com/chrome/answer/2364824'
              target='_blank'
            >
              Chrome Profile
            </StyledExternalLink>{' '}
            with your <AoU /> Researcher Workbench Google account. This will
            keep your workbench browser sessions isolated from your other Google
            accounts.
          </CustomBulletListItem>
          <CustomBulletListItem bullet='→'>
            Check out{' '}
            <StyledRouterLink path='/library'>
              Featured Workspaces
            </StyledRouterLink>{' '}
            from the left hand panel to browse through example workspaces.
          </CustomBulletListItem>
          <CustomBulletListItem bullet='→'>
            Browse through our{' '}
            <StyledExternalLink href={supportUrls.helpCenter} target='_blank'>
              support materials
            </StyledExternalLink>{' '}
            and forum topics.
          </CustomBulletListItem>
        </CustomBulletList>
      </div>
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
      };
    }

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

    userHasWorkspaces(): boolean {
      const { userWorkspacesResponse } = this.state;
      return userWorkspacesResponse?.items?.length > 0;
    }

    render() {
      const { firstVisit, userWorkspacesResponse } = this.state;

      return (
        <React.Fragment>
          <FlexColumn style={styles.pageWrapper}>
            <WelcomeHeader />
            <div style={styles.fadeBox}>
              {/* The elements inside this fadeBox will be changed as part of ongoing homepage redesign work */}
              <FlexColumn style={{ justifyContent: 'flex-start' }}>
                <Workspaces onChange={() => this.fetchWorkspaces()} />
                {userWorkspacesResponse &&
                  (this.userHasWorkspaces() ? (
                    <RecentResources
                      workspaces={userWorkspacesResponse.items}
                    />
                  ) : (
                    <GettingStarted />
                  ))}
              </FlexColumn>
            </div>
          </FlexColumn>
          <QuickTourAndVideos showQuickTourInitially={firstVisit} />
        </React.Fragment>
      );
    }
  }
);
