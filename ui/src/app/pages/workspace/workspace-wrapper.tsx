import * as React from 'react';
import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { faXmark } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { Workspace } from 'generated/fetch';

import { StyledExternalLink } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { HelpSidebar } from 'app/components/help-sidebar';
import { Spinner } from 'app/components/spinners';
import { WorkspaceNavBar } from 'app/pages/workspace/workspace-nav-bar';
import { WorkspaceRoutes } from 'app/routing/workspace-app-routing';
import { workspacesApi } from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import { AccessTierShortNames } from 'app/utils/access-tiers';
import { LeoRuntimeInitializer } from 'app/utils/leo-runtime-initializer';
import {
  currentWorkspaceStore,
  NavigateFn,
  nextWorkspaceWarmupStore,
  useNavigation,
} from 'app/utils/navigation';
import {
  MatchParams,
  routeDataStore,
  runtimeDiskStore,
  runtimeStore,
  userAppsStore,
  useStore,
} from 'app/utils/stores';
import { maybeStartPollingForUserApps } from 'app/utils/user-apps-utils';
import { zendeskBaseUrl } from 'app/utils/zendesk';

const styles = reactStyles({
  bannerNotification: {
    padding: 16,
    marginTop: 16,
    marginLeft: '3rem',
    width: 'calc(100% - 7.5rem)',
    boxSizing: 'border-box',
    borderWidth: '1px',
    borderStyle: 'solid',
    borderRadius: '5px',
    color: colors.primary,
    fontFamily: 'Montserrat',
    letterSpacing: 0,
    lineHeight: '22px',
    borderColor: colors.accent,
    backgroundColor: colorWithWhiteness(colors.accent, 0.85),
  },
});

// Workspaces created before this date use multi-region buckets which incur additional costs compared to
// single-region buckets.
const MULTI_REGION_BUCKET_END_DATE = new Date(2022, 5, 15); // June 15th, 2022. Months are 0-indexed.

const BannerNotification = ({ children }) => {
  return (
    <div style={styles.bannerNotification}>
      <FlexRow style={{ justifyContent: 'space-between' }}>{children}</FlexRow>
    </div>
  );
};

interface NotificationProps {
  onCancel: () => void;
}
const NewCtNotification = (props: NotificationProps) => {
  const dataPathsLink = (
    <StyledExternalLink
      target='_blank'
      href={`${zendeskBaseUrl()}/articles/4616869437204-Controlled-CDR-Directory`}
    >
      Genomic data paths
    </StyledExternalLink>
  );
  const dataFilesLink = (
    <StyledExternalLink
      target='_blank'
      href={`${zendeskBaseUrl()}/articles/4614687617556-How-the-All-of-Us-Genomic-data-are-organized`}
    >
      genomic data formatting
    </StyledExternalLink>
  );
  const cohortBuilderLink = (
    <StyledExternalLink
      target='_blank'
      href={`${zendeskBaseUrl()}/articles/360039585591-Selecting-participants-using-the-Cohort-Builder-tool`}
    >
      how to build a cohort
    </StyledExternalLink>
  );
  const supportHubLink = (
    <StyledExternalLink target='_blank' href={zendeskBaseUrl()}>
      User Support Hub
    </StyledExternalLink>
  );

  return (
    <BannerNotification>
      <FlexColumn>
        <div>
          These resources will get you started using the genomic data:{' '}
          {dataPathsLink}, {dataFilesLink}, and {cohortBuilderLink}.
        </div>
        <div>
          You can always learn more in the {supportHubLink} section on Genomics.
        </div>
      </FlexColumn>
      <FontAwesomeIcon
        icon={faXmark}
        style={{ fontSize: '21px', marginLeft: '1em' }}
        onClick={() => props.onCancel()}
      />
    </BannerNotification>
  );
};

const MultiRegionWorkspaceNotification = () => {
  return (
    <BannerNotification>
      <FlexColumn>
        <div>
          Workspaces created before{' '}
          {MULTI_REGION_BUCKET_END_DATE.toLocaleDateString()} use multi-region
          buckets, which incur additional costs. To prevent this, we recommend
          duplicating the workspace (using single-region buckets) to save costs.
          Contact support for assistance.
        </div>
      </FlexColumn>
    </BannerNotification>
  );
};

interface UpdateRuntimeStoresProps {
  workspaceNamespace: string;
  pollAborter: AbortController;
  setPollAborter: (ac: AbortController) => void;
}
const updateRuntimeStores = async ({
  workspaceNamespace,
  pollAborter,
  setPollAborter,
}: UpdateRuntimeStoresProps) => {
  runtimeDiskStore.set({
    workspaceNamespace,
    gcePersistentDisk: undefined,
  });
  runtimeStore.set({
    workspaceNamespace,
    runtime: undefined,
    runtimeLoaded: false,
  });
  pollAborter.abort();
  const newPollAborter = new AbortController();
  setPollAborter(newPollAborter);

  try {
    await LeoRuntimeInitializer.initialize({
      workspaceNamespace,
      pollAbortSignal: newPollAborter.signal,
      maxCreateCount: 0,
      maxResumeCount: 0,
    });
  } catch {
    // Ignore InitialRuntimeNotFoundError.
    // Ignore ExceededActionCountError. This is thrown when the runtime doesn't exist, or
    // isn't started. Both of these scenarios are expected, since we don't want to do any lazy
    // initialization here.
    // Also ignore LeoRuntimeInitializationAbortedError - this is expected when navigating
    // away from a page during a poll.
    // Ideally, we would handle or log errors except the ones listed above.
  }
};

interface GetWorkspaceProps {
  wsid: string;
  setWorkspace: (w: Workspace) => void;
  navigate: NavigateFn;
}
const getWorkspaceAndUpdateStores = async ({
  workspaceNamespace,
  wsid,
  setWorkspace,
  navigate,
  pollAborter,
  setPollAborter,
}: GetWorkspaceProps & UpdateRuntimeStoresProps) => {
  try {
    const { workspace, accessLevel } = await workspacesApi().getWorkspace(
      workspaceNamespace,
      wsid
    );
    currentWorkspaceStore.next({
      ...workspace,
      accessLevel,
    });
    updateRuntimeStores({
      workspaceNamespace: workspace.namespace,
      pollAborter,
      setPollAborter,
    });
    setWorkspace(workspace);
  } catch (ex) {
    if (ex.status === 403) {
      navigate(['/workspaces']);
    }
  }
};

export const WorkspaceWrapper = ({ hideSpinner }) => {
  const params = useParams<MatchParams>();
  const { ns, wsid } = params;
  const routeData = useStore(routeDataStore);
  const [navigate] = useNavigation();

  const [pollAborter, setPollAborter] = useState(new AbortController());
  const [workspace, setWorkspace] = useState<Workspace>(undefined);
  const [showNewCtNotification, setShowNewCtNotification] = useState(false);

  useEffect(() => {
    hideSpinner();
    return () => {
      const { timeoutID } = userAppsStore.get();
      if (timeoutID) {
        clearTimeout(timeoutID);
      }
    };
  }, []);

  useEffect(() => {
    // abort if the routing is invalid (though this should never happen)
    if (!ns || !wsid) {
      return;
    }

    maybeStartPollingForUserApps(ns);
    workspacesApi().updateRecentWorkspaces(ns, wsid);

    // avoid a redundant fetch if we can find the workspace in the currentWorkspaceStore or the
    // nextWorkspaceWarmupStore (as a result of creation or duplication)

    const wsInCurrentStore = currentWorkspaceStore.getValue();
    if (
      wsInCurrentStore &&
      // can't use ?. here because we don't want to match against undefined
      wsInCurrentStore.namespace === ns &&
      wsInCurrentStore.id === wsid
    ) {
      updateRuntimeStores({
        workspaceNamespace: ns,
        pollAborter,
        setPollAborter,
      });
      setWorkspace(wsInCurrentStore);
    } else {
      // if the current workspace store is set to something besides where the routing indicates, it's invalid
      currentWorkspaceStore.next(null);

      const wsInNextStore = nextWorkspaceWarmupStore.getValue();
      nextWorkspaceWarmupStore.next(undefined);
      if (
        wsInNextStore &&
        // can't use ?. here because we don't want to match against undefined
        wsInNextStore.namespace === ns &&
        wsInNextStore.id === wsid
      ) {
        currentWorkspaceStore.next(wsInNextStore);
        updateRuntimeStores({
          workspaceNamespace: ns,
          pollAborter,
          setPollAborter,
        });
        setWorkspace(wsInNextStore);
      } else {
        getWorkspaceAndUpdateStores({
          workspaceNamespace: ns,
          wsid,
          setWorkspace,
          navigate,
          pollAborter,
          setPollAborter,
        });
      }
    }
  }, [ns, wsid]);

  useEffect(() => {
    const isControlled =
      workspace?.accessTierShortName === AccessTierShortNames.Controlled;
    const tenMinutesAgo = Date.now() - 10 * 60 * 1000; // arbitrary notion of 'new' workspace
    const isNew = workspace?.creationTime > tenMinutesAgo;
    setShowNewCtNotification(isControlled && isNew);
  }, [workspace]);

  const showMultiRegionWorkspaceNotification =
    workspace &&
    new Date(workspace.creationTime) < MULTI_REGION_BUCKET_END_DATE;

  return (
    <>
      {workspace ? (
        <>
          {!routeData.minimizeChrome && (
            <WorkspaceNavBar tabPath={routeData.workspaceNavBarTab} />
          )}
          {showNewCtNotification && (
            <NewCtNotification
              onCancel={() => setShowNewCtNotification(false)}
            />
          )}
          {showMultiRegionWorkspaceNotification && (
            <MultiRegionWorkspaceNotification />
          )}
          <HelpSidebar pageKey={routeData.pageKey} />
          <div
            style={{
              marginRight: '45px',
              height: !routeData.contentFullHeightOverride ? 'auto' : '100%',
            }}
          >
            <WorkspaceRoutes />
          </div>
        </>
      ) : (
        <div
          style={{
            display: 'flex',
            height: '100%',
            width: '100%',
            justifyContent: 'center',
            alignItems: 'center',
          }}
        >
          <Spinner title='loading workspaces spinner' />
        </div>
      )}
    </>
  );
};
