import * as React from 'react';
import { useState } from 'react';
import {
  faGear,
  faPlay,
  faRocket,
  faTrashCan,
} from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { AppStatus, UserAppEnvironment, Workspace } from 'generated/fetch';

import { AppStatusIcon } from 'app/components/app-status-icon';
import { Clickable } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { withErrorModal } from 'app/components/modals';
import { TooltipTrigger } from 'app/components/popups';
import { RuntimeStatusIcon } from 'app/components/runtime-status-icon';
import { leoProxyApi } from 'app/services/notebooks-swagger-fetch-clients';
import { appsApi } from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import { cond, reactStyles } from 'app/utils';
import { setSidebarActiveIconStore } from 'app/utils/navigation';
import {
  isActionable,
  RuntimeStatusRequest,
  useRuntimeStatus,
} from 'app/utils/runtime-utils';
import { runtimeStore, useStore } from 'app/utils/stores';
import {
  createUserApp,
  getUserApps,
  pauseUserApp,
  resumeUserApp,
} from 'app/utils/user-apps-utils';

import { AppLogo } from './app-logo';
import { AppsPanelButton } from './apps-panel-button';
import { NewNotebookButton } from './new-notebook-button';
import { PauseResumeButton } from './pause-resume-button';
import { RuntimeCost } from './runtime-cost';
import {
  canCreateApp,
  canDeleteApp,
  defaultRStudioConfig,
  fromRuntimeStatus,
  fromUserAppStatus,
  fromUserAppStatusWithFallback,
  UIAppType,
} from './utils';

const styles = reactStyles({
  expandedAppContainer: {
    background: colors.white,
    marginLeft: '1em',
    marginBottom: '1em',
    padding: '1em',
  },
  enabledTrashButton: {
    alignSelf: 'center',
    marginLeft: 'auto',
    marginRight: '1em',
  },
  disabledTrashButton: {
    alignSelf: 'center',
    marginLeft: 'auto',
    marginRight: '1em',
    color: colors.disabled,
    cursor: 'not-allowed',
  },
});

const SettingsButton = (props: { onClick: Function; disabled?: boolean }) => {
  const { onClick, disabled } = props;
  return (
    <AppsPanelButton
      {...{ onClick, disabled }}
      icon={faGear}
      buttonText='Settings'
    />
  );
};

const PauseRuntimeButton = (props: { workspace: Workspace }) => {
  const {
    workspace: { namespace, googleProject },
  } = props;

  const [status, setRuntimeStatus] = useRuntimeStatus(namespace, googleProject);

  return (
    <PauseResumeButton
      externalStatus={fromRuntimeStatus(status)}
      onPause={() => setRuntimeStatus(RuntimeStatusRequest.Stop)}
      onResume={() => setRuntimeStatus(RuntimeStatusRequest.Start)}
    />
  );
};

const JupyterButtonRow = (props: {
  workspace: Workspace;
  onClickRuntimeConf: Function;
}) => {
  const { workspace, onClickRuntimeConf } = props;
  return (
    <FlexRow>
      <SettingsButton onClick={onClickRuntimeConf} />
      <PauseRuntimeButton {...{ workspace }} />
      <NewNotebookButton {...{ workspace }} />
    </FlexRow>
  );
};

const PauseUserAppButton = (props: {
  userApp: UserAppEnvironment;
  workspaceNamespace: string;
}) => {
  const { googleProject, appName, status } = props.userApp || {};

  return (
    <PauseResumeButton
      externalStatus={fromUserAppStatus(status)}
      onPause={() =>
        pauseUserApp(googleProject, appName, props.workspaceNamespace)
      }
      onResume={() =>
        resumeUserApp(googleProject, appName, props.workspaceNamespace)
      }
    />
  );
};

// TODO generalize as UserAppButtonRow?
const CromwellButtonRow = (props: {
  userApp: UserAppEnvironment;
  workspaceNamespace: string;
}) => {
  const { userApp, workspaceNamespace } = props;

  return (
    <FlexRow>
      <SettingsButton
        onClick={() => setSidebarActiveIconStore.next('cromwellConfig')}
        data-test-id='Cromwell-settings-button'
      />
      <PauseUserAppButton {...{ userApp, workspaceNamespace }} />
    </FlexRow>
  );
};

const RStudioButtonRow = (props: {
  userApp: UserAppEnvironment;
  workspaceNamespace: string;
}) => {
  const { userApp, workspaceNamespace } = props;
  const [creating, setCreating] = useState(false);

  const onClickCreate = withErrorModal(
    {
      title: 'Error Creating RStudio Environment',
      message:
        'Please wait a few minutes and try to create your RStudio Environment again.',
      onDismiss: () => setCreating(false),
    },
    async () => {
      setCreating(true);
      await createUserApp(workspaceNamespace, defaultRStudioConfig);
    }
  );

  const onClickLaunch = withErrorModal(
    {
      title: 'Error Opening RStudio Environment',
      message: 'Please try again.',
    },
    async () => {
      await leoProxyApi().setCookie(userApp.googleProject, userApp.appName, {
        credentials: 'include',
      });
      window.open(userApp.proxyUrls.rstudio, '_blank').focus();
    }
  );

  const createButtonDisabled = creating || !canCreateApp(userApp);
  const launchButtonDisabled = userApp?.status !== AppStatus.RUNNING;

  return (
    <FlexRow>
      <TooltipTrigger
        disabled={!createButtonDisabled}
        content='An RStudio app exists or is being created'
      >
        {/* tooltip trigger needs a div for some reason */}
        <div>
          <AppsPanelButton
            disabled={createButtonDisabled}
            onClick={onClickCreate}
            icon={faPlay}
            buttonText={creating ? 'Creating' : 'Create'}
            data-test-id='RStudio-create-button'
          />
        </div>
      </TooltipTrigger>
      <PauseUserAppButton {...{ userApp, workspaceNamespace }} />
      <TooltipTrigger
        disabled={!launchButtonDisabled}
        content='Environment must be running to launch RStudio'
      >
        {/* tooltip trigger needs a div for some reason */}
        <div>
          <AppsPanelButton
            onClick={onClickLaunch}
            disabled={launchButtonDisabled}
            icon={faRocket}
            buttonText='Launch'
            data-test-id='RStudio-launch-button'
          />
        </div>
      </TooltipTrigger>
    </FlexRow>
  );
};

interface ExpandedAppProps {
  appType: UIAppType;
  initialUserAppInfo: UserAppEnvironment;
  workspace: Workspace;
  onClickRuntimeConf: Function;
  onClickDeleteRuntime: Function;
}
export const ExpandedApp = (props: ExpandedAppProps) => {
  const { runtime } = useStore(runtimeStore);
  const {
    appType,
    initialUserAppInfo,
    workspace,
    onClickRuntimeConf,
    onClickDeleteRuntime,
  } = props;
  const [deletingApp, setDeletingApp] = useState(false);

  const trashEnabled =
    appType === UIAppType.JUPYTER
      ? isActionable(runtime?.status)
      : !deletingApp && canDeleteApp(initialUserAppInfo);

  // TODO allow configuration
  const deleteDiskWithUserApp = true;

  const onClickDelete =
    appType === UIAppType.JUPYTER
      ? onClickDeleteRuntime
      : () => {
          setDeletingApp(true);
          appsApi()
            .deleteApp(
              workspace.namespace,
              initialUserAppInfo.appName,
              deleteDiskWithUserApp
            )
            .then(() => getUserApps(workspace.namespace));
        };
  return (
    <FlexColumn
      style={styles.expandedAppContainer}
      data-test-id={`${appType}-expanded`}
    >
      <FlexRow>
        <div>
          <AppLogo
            {...{ appType }}
            style={{ marginRight: '1em', padding: '1rem' }}
          />
        </div>
        {appType === UIAppType.JUPYTER && (
          <RuntimeStatusIcon
            style={{ alignSelf: 'center', marginRight: '0.5em' }}
            workspaceNamespace={workspace.namespace}
          />
        )}
        {
          // TODO: support Cromwell + other User Apps
          appType === UIAppType.JUPYTER && <RuntimeCost />
        }
        <Clickable
          disabled={!trashEnabled}
          style={
            trashEnabled
              ? styles.enabledTrashButton
              : styles.disabledTrashButton
          }
          onClick={onClickDelete}
          data-test-id={`${appType}-delete-button`}
          propagateDataTestId
        >
          <FontAwesomeIcon icon={faTrashCan} />
        </Clickable>
      </FlexRow>
      {appType === UIAppType.JUPYTER ? (
        <JupyterButtonRow {...{ workspace, onClickRuntimeConf }} />
      ) : (
        <FlexColumn>
          {/* TODO: keep status updated internally */}
          <FlexRow style={{ justifyContent: 'center' }}>
            status: {fromUserAppStatusWithFallback(initialUserAppInfo?.status)}{' '}
            <AppStatusIcon
              style={{ alignSelf: 'center', margin: '0 0.5em' }}
              appStatus={initialUserAppInfo?.status}
              userSuspended={false}
            />
          </FlexRow>
          {cond(
            [
              appType === UIAppType.CROMWELL,
              () => (
                <CromwellButtonRow
                  userApp={initialUserAppInfo}
                  workspaceNamespace={workspace.namespace}
                />
              ),
            ],
            [
              appType === UIAppType.RSTUDIO,
              () => (
                <RStudioButtonRow
                  userApp={initialUserAppInfo}
                  workspaceNamespace={workspace.namespace}
                />
              ),
            ],
            () => null
          )}
        </FlexColumn>
      )}
    </FlexColumn>
  );
};
