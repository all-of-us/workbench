import * as React from 'react';
import { useState } from 'react';
import { faGear, faPlay, faTrashCan } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { UserAppEnvironment, Workspace } from 'generated/fetch';

import { Clickable } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { withErrorModal } from 'app/components/modals';
import { TooltipTrigger } from 'app/components/popups';
import { RuntimeStatusIcon } from 'app/components/runtime-status-icon';
import { leoAppsApi } from 'app/services/notebooks-swagger-fetch-clients';
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
    width: 'fit-content',
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

const PauseUserAppButton = (props: { userApp: UserAppEnvironment }) => {
  const { googleProject, appName, status } = props.userApp || {};

  return (
    <PauseResumeButton
      externalStatus={fromUserAppStatus(status)}
      onPause={() => leoAppsApi().stopApp(googleProject, appName)}
      onResume={() => leoAppsApi().startApp(googleProject, appName)}
    />
  );
};

// TODO generalize as UserAppButtonRow?
const CromwellButtonRow = (props: {
  userApp: UserAppEnvironment;
  workspaceNamespace: string;
}) => {
  const { userApp } = props;

  return (
    <FlexRow>
      <TooltipTrigger
        disabled={false}
        content='Support for configuring Cromwell is not yet available'
      >
        {/* tooltip trigger needs a div for some reason */}
        <div>
          <SettingsButton disabled={true} onClick={() => {}} />
        </div>
      </TooltipTrigger>
      <PauseUserAppButton {...{ userApp }} />
      <TooltipTrigger
        disabled={canCreateApp(userApp)}
        content='A Cromwell app exists or is being created'
      >
        {/* tooltip trigger needs a div for some reason */}
        <div>
          <AppsPanelButton
            disabled={!canCreateApp(userApp)}
            onClick={() => setSidebarActiveIconStore.next('cromwellConfig')}
            icon={faPlay}
            buttonText='Launch'
          />
        </div>
      </TooltipTrigger>
    </FlexRow>
  );
};

const RStudioButtonRow = (props: {
  userApp: UserAppEnvironment;
  workspaceNamespace: string;
}) => {
  const { userApp, workspaceNamespace } = props;
  const [launching, setLaunching] = useState(false);

  const onClickLaunch = withErrorModal(
    {
      title: 'Error Creating RStudio Environment',
      message: 'Please refresh the page.',
    },
    async () => {
      setLaunching(true);
      await appsApi().createApp(workspaceNamespace, defaultRStudioConfig);
    }
  );

  const launchButtonDisabled = launching || !canCreateApp(userApp);

  return (
    <FlexRow>
      <TooltipTrigger
        disabled={false}
        content='Support for configuring RStudio is not yet available'
      >
        {/* tooltip trigger needs a div for some reason */}
        <div>
          <SettingsButton
            disabled={true}
            onClick={() => {}}
            data-test-id='rstudio-settings-button'
          />
        </div>
      </TooltipTrigger>
      <PauseUserAppButton {...{ userApp }} />
      <TooltipTrigger
        disabled={!launchButtonDisabled}
        content='An RStudio app exists or is being created'
      >
        {/* tooltip trigger needs a div for some reason */}
        <div>
          <AppsPanelButton
            disabled={launchButtonDisabled}
            onClick={onClickLaunch}
            icon={faPlay}
            buttonText={launching ? 'Launching' : 'Launch'}
            data-test-id='rstudio-launch-button'
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
          appsApi().deleteApp(
            workspace.namespace,
            initialUserAppInfo.appName,
            deleteDiskWithUserApp
          );
        };
  return (
    <FlexColumn
      style={styles.expandedAppContainer}
      data-test-id={`${appType}-expanded`}
    >
      <FlexRow>
        <div>
          <AppLogo {...{ appType }} style={{ marginRight: '1em' }} />
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
          data-test-id={`delete-${appType}`}
          style={
            trashEnabled
              ? styles.enabledTrashButton
              : styles.disabledTrashButton
          }
          onClick={onClickDelete}
          data-test-id={`${appType}-delete-button`}
        >
          <FontAwesomeIcon icon={faTrashCan} />
        </Clickable>
      </FlexRow>
      {appType === UIAppType.JUPYTER ? (
        <JupyterButtonRow {...{ workspace, onClickRuntimeConf }} />
      ) : (
        <FlexColumn style={{ alignItems: 'center' }}>
          {/* TODO: keep status updated internally */}
          <div>
            status: {fromUserAppStatusWithFallback(initialUserAppInfo?.status)}{' '}
            (refresh to update)
          </div>
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
