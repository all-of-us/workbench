import * as React from 'react';
import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import * as fp from 'lodash/fp';
import { faXmark } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { StyledExternalLink } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { HelpSidebar } from 'app/components/help-sidebar';
import { Spinner } from 'app/components/spinners';
import { WorkspaceNavBar } from 'app/pages/workspace/workspace-nav-bar';
import { WorkspaceRoutes } from 'app/routing/workspace-app-routing';
import { workspacesApi } from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles, withCurrentWorkspace } from 'app/utils';
import { AccessTierShortNames } from 'app/utils/access-tiers';
import { reportError } from 'app/utils/errors';
import {
  ExceededActionCountError,
  InitialRuntimeNotFoundError,
  LeoRuntimeInitializationAbortedError,
  LeoRuntimeInitializer,
} from 'app/utils/leo-runtime-initializer';
import {
  currentWorkspaceStore,
  nextWorkspaceWarmupStore,
  useNavigation,
} from 'app/utils/navigation';
import {
  diskStore,
  MatchParams,
  routeDataStore,
  runtimeStore,
  useStore,
} from 'app/utils/stores';

const styles = reactStyles({
  newCtNotification: {
    padding: 16,
    marginTop: 16,
    marginLeft: '2rem',
    width: 'calc(100% - 5rem)',
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

interface NotificationProps {
  isNewCT: boolean;
  onCancel: () => void;
}
const MaybeNewCtNotification = (props: NotificationProps) => {
  const dataPaths =
    'https://aousupporthelp.zendesk.com/hc/en-us/articles/4616869437204-Controlled-CDR-Directory';
  const dataFiles =
    'https://aousupporthelp.zendesk.com/hc/en-us/articles/4614687617556-How-the-All-of-Us-Genomic-data-are-organized';
  const cohortBuilder =
    'https://aousupporthelp.zendesk.com/hc/en-us/articles/360039585591-Selecting-participants-using-the-Cohort-Builder-tool';

  const dataPathsLink = (
    <StyledExternalLink href={dataPaths}>This resource</StyledExternalLink>
  );
  const dataFilesLink = (
    <StyledExternalLink href={dataFiles}>this resource</StyledExternalLink>
  );
  const cohortBuilderLink = (
    <StyledExternalLink href={cohortBuilder}>this resource</StyledExternalLink>
  );

  return (
    props.isNewCT && (
      <div style={styles.newCtNotification}>
        <FlexRow>
          <FlexColumn>
            <div>
              These resources will get you started using the genomic data.
            </div>
            <div>
              {dataPathsLink} provides full genomic data paths, {dataFilesLink}{' '}
              provides information about genomic data formatting, and{' '}
              {cohortBuilderLink} describes how to build a cohort.
            </div>
          </FlexColumn>
          <FontAwesomeIcon
            icon={faXmark}
            style={{ fontSize: '21px', marginLeft: '1em' }}
            onClick={() => props.onCancel()}
          />
        </FlexRow>
      </div>
    )
  );
};

export const WorkspaceWrapper = fp.flow(withCurrentWorkspace())(
  ({ workspace, hideSpinner }) => {
    useEffect(() => hideSpinner(), []);
    const routeData = useStore(routeDataStore);
    const [navigate] = useNavigation();

    const [pollAborter, setPollAborter] = useState(new AbortController());
    const params = useParams<MatchParams>();
    const { ns, wsid } = params;

    const [showNewCTNotification, setShowNewCTNotification] = useState(false);

    useEffect(() => {
      const updateStores = async (namespace) => {
        diskStore.set({
          workspaceNamespace: namespace,
          persistentDisk: undefined,
        });
        runtimeStore.set({
          workspaceNamespace: namespace,
          runtime: undefined,
          runtimeLoaded: false,
        });
        pollAborter.abort();
        const newPollAborter = new AbortController();
        setPollAborter(newPollAborter);

        try {
          await LeoRuntimeInitializer.initialize({
            workspaceNamespace: namespace,
            pollAbortSignal: newPollAborter.signal,
            maxCreateCount: 0,
            maxResumeCount: 0,
          });
        } catch (e) {
          // Ignore ExceededActionCountError. This is thrown when the runtime doesn't exist, or
          // isn't started. Both of these scenarios are expected, since we don't want to do any lazy
          // initialization here.
          // Also ignore LeoRuntimeInitializationAbortedError - this is expected when navigating
          // away from a page during a poll.
          if (
            !(
              e instanceof InitialRuntimeNotFoundError ||
              e instanceof ExceededActionCountError ||
              e instanceof LeoRuntimeInitializationAbortedError
            )
          ) {
            // Ideally, we would have some top-level error messaging here.
            reportError(e);
          }
        }
      };
      const getWorkspaceAndUpdateStores = async (namespace, id) => {
        try {
          // No destructuring because otherwise it shadows the workspace in props
          const wsResponse = await workspacesApi().getWorkspace(namespace, id);
          currentWorkspaceStore.next({
            ...wsResponse.workspace,
            accessLevel: wsResponse.accessLevel,
          });
          updateStores(wsResponse.workspace.namespace);
        } catch (ex) {
          if (ex.status === 403) {
            navigate(['/workspaces']);
          }
        }
      };

      if (
        !currentWorkspaceStore.getValue() ||
        currentWorkspaceStore.getValue().namespace !== ns ||
        currentWorkspaceStore.getValue().id !== wsid
      ) {
        currentWorkspaceStore.next(null);
        // In a handful of situations - namely on workspace creation/clone,
        // the application will preload the next workspace to avoid a redundant
        // refetch here.
        const nextWs = nextWorkspaceWarmupStore.getValue();
        nextWorkspaceWarmupStore.next(undefined);
        if (nextWs && nextWs.namespace === ns && nextWs.id === wsid) {
          currentWorkspaceStore.next(nextWs);
          updateStores(ns);
        } else {
          getWorkspaceAndUpdateStores(ns, wsid);
        }
      }
    }, [ns, wsid]);

    useEffect(() => {
      workspacesApi().updateRecentWorkspaces(ns, wsid);
    }, [ns, wsid]);

    useEffect(() => {
      const isControlled =
        workspace?.accessTierShortName === AccessTierShortNames.Controlled;
      const tenMinutesAgo = Date.now() - 10 * 60 * 1000; // arbitrary notion of 'new' workspace
      const isNew = workspace?.creationTime > tenMinutesAgo;
      setShowNewCTNotification(isControlled && isNew);
    }, [workspace]);

    return (
      <>
        {workspace ? (
          <>
            {!routeData.minimizeChrome && (
              <WorkspaceNavBar tabPath={routeData.workspaceNavBarTab} />
            )}
            <MaybeNewCtNotification
              isNewCT={showNewCTNotification}
              onCancel={() => setShowNewCTNotification(false)}
            />
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
            <Spinner />
          </div>
        )}
      </>
    );
  }
);
