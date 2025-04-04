import * as React from 'react';
import { useState } from 'react';
import {
  faGear,
  faRocket,
  faTrashCan,
} from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { AppStatus, UserAppEnvironment, Workspace } from 'generated/fetch';

import { cond, switchCase } from '@terra-ui-packages/core-utils';
import { AppStatusIndicator } from 'app/components/app-status-indicator';
import { DeleteCromwellConfirmationModal } from 'app/components/apps-panel/delete-cromwell-modal';
import { Clickable } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import {
  cromwellConfigIconId,
  rstudioConfigIconId,
  sasConfigIconId,
  SidebarIconId,
} from 'app/components/help-sidebar-icons';
import { TooltipTrigger } from 'app/components/popups';
import { RuntimeStatusIndicator } from 'app/components/runtime-status-indicator';
import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import {
  currentWorkspaceStore,
  sidebarActiveIconStore,
  useNavigation,
} from 'app/utils/navigation';
import { useRuntimeStatus } from 'app/utils/runtime-hooks';
import {
  canDeleteRuntime,
  RuntimeStatusRequest,
} from 'app/utils/runtime-utils';
import { runtimeStore, serverConfigStore, useStore } from 'app/utils/stores';
import { BILLING_ACCOUNT_DISABLED_TOOLTIP } from 'app/utils/strings';
import {
  openAppInIframe,
  pauseUserApp,
  resumeUserApp,
} from 'app/utils/user-apps-utils';
import { isValidBilling } from 'app/utils/workspace-utils';

import { AppBanner } from './app-banner';
import { AppsPanelButton } from './apps-panel-button';
import { NewJupyterNotebookButton } from './new-jupyter-notebook-button';
import { PauseResumeButton } from './pause-resume-button';
import { RuntimeCost } from './runtime-cost';
import {
  canDeleteApp,
  fromRuntimeStatus,
  fromUserAppStatus,
  fromUserAppStatusWithFallback,
  openConfigPanelForUIApp,
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
  const { workspace } = props;
  const { namespace, googleProject } = workspace;

  const [runtimeStatus, setRuntimeStatusRequest] = useRuntimeStatus(
    namespace,
    googleProject
  );

  return (
    <PauseResumeButton
      disabled={!isValidBilling(workspace)}
      externalStatus={fromRuntimeStatus(runtimeStatus)}
      onPause={() => setRuntimeStatusRequest(RuntimeStatusRequest.Stop)}
      onResume={() => setRuntimeStatusRequest(RuntimeStatusRequest.Start)}
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
      <NewJupyterNotebookButton {...{ workspace }} />
    </FlexRow>
  );
};

const PauseUserAppButton = (props: {
  userApp: UserAppEnvironment;
  workspaceNamespace: string;
}) => {
  const { googleProject, appName, status } = props.userApp || {};
  const { config } = useStore(serverConfigStore);

  return (
    <PauseResumeButton
      externalStatus={fromUserAppStatus(status)}
      onPause={() =>
        pauseUserApp(googleProject, appName, props.workspaceNamespace)
      }
      onResume={() =>
        resumeUserApp(googleProject, appName, props.workspaceNamespace)
      }
      disabled={!config.enableGKEAppPausing}
      disabledTooltip={
        !config.enableGKEAppPausing &&
        'Pause and resume are not currently available.'
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
        onClick={() => openConfigPanelForUIApp(UIAppType.CROMWELL)}
        data-test-id='Cromwell-settings-button'
      />
      <PauseUserAppButton {...{ userApp, workspaceNamespace }} />
    </FlexRow>
  );
};

const RStudioButtonRow = (props: {
  userApp: UserAppEnvironment;
  billingAccountDisabled: boolean;
}) => {
  const { userApp, billingAccountDisabled } = props;
  const [navigate] = useNavigation();
  const { namespace, terraName } = currentWorkspaceStore.getValue();
  const onClickLaunch = async () => {
    openAppInIframe(namespace, terraName, userApp, navigate);
    sidebarActiveIconStore.next(null);
  };
  const launchButtonDisabled =
    billingAccountDisabled || userApp?.status !== AppStatus.RUNNING;
  let tooltip;
  if (billingAccountDisabled) {
    tooltip = BILLING_ACCOUNT_DISABLED_TOOLTIP;
  } else if (userApp?.status !== AppStatus.RUNNING) {
    tooltip = 'Environment must be running to launch RStudio';
  }
  return (
    <FlexRow>
      <SettingsButton
        onClick={() => openConfigPanelForUIApp(UIAppType.RSTUDIO)}
      />
      <PauseUserAppButton userApp={userApp} workspaceNamespace={namespace} />
      <TooltipTrigger disabled={!launchButtonDisabled} content={tooltip}>
        {/* tooltip trigger needs a div for some reason */}
        <div>
          <AppsPanelButton
            onClick={onClickLaunch}
            disabled={launchButtonDisabled}
            icon={faRocket}
            buttonText='Open RStudio'
            data-test-id='open-RStudio-button'
          />
        </div>
      </TooltipTrigger>
    </FlexRow>
  );
};

const SASButtonRow = (props: {
  userApp: UserAppEnvironment;
  billingAccountDisabled: boolean;
}) => {
  const { userApp } = props;
  const [navigate] = useNavigation();
  const { namespace, terraName } = currentWorkspaceStore.getValue();

  const onClickLaunch = async () => {
    openAppInIframe(namespace, terraName, userApp, navigate);
    sidebarActiveIconStore.next(null);
  };

  const launchButtonDisabled = userApp?.status !== AppStatus.RUNNING;

  return (
    <FlexRow>
      <SettingsButton onClick={() => openConfigPanelForUIApp(UIAppType.SAS)} />
      <PauseUserAppButton userApp={userApp} workspaceNamespace={namespace} />
      <TooltipTrigger
        disabled={!launchButtonDisabled}
        content='Environment must be running to launch SAS'
      >
        {/* tooltip trigger needs a div for some reason */}
        <div>
          <AppsPanelButton
            onClick={onClickLaunch}
            disabled={launchButtonDisabled}
            icon={faRocket}
            buttonText='Open SAS'
            data-test-id='open-SAS-button'
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
  onClickDeleteGkeApp: (sidebarIcon: SidebarIconId) => void;
}
export const ExpandedApp = (props: ExpandedAppProps) => {
  const { runtime } = useStore(runtimeStore);
  const {
    appType,
    initialUserAppInfo,
    workspace,
    onClickRuntimeConf,
    onClickDeleteRuntime,
    onClickDeleteGkeApp,
  } = props;
  const [showCromwellDeleteModal, setShowCromwellDeleteModal] = useState(false);

  const trashEnabled =
    appType === UIAppType.JUPYTER
      ? canDeleteRuntime(runtime?.status)
      : canDeleteApp(initialUserAppInfo);

  const billingAccountDisabled = !isValidBilling(workspace);

  const onClickDelete = switchCase(
    appType,
    [UIAppType.JUPYTER, () => onClickDeleteRuntime],
    [UIAppType.CROMWELL, () => () => setShowCromwellDeleteModal(true)],
    [UIAppType.RSTUDIO, () => () => onClickDeleteGkeApp(rstudioConfigIconId)],
    [UIAppType.SAS, () => () => onClickDeleteGkeApp(sasConfigIconId)]
  );

  return (
    <FlexColumn
      style={styles.expandedAppContainer}
      data-test-id={`${appType}-expanded`}
    >
      <FlexRow>
        <div>
          <AppBanner
            {...{ appType }}
            style={{ marginRight: '1em', padding: '1rem' }}
          />
        </div>
        {appType === UIAppType.JUPYTER && (
          <RuntimeStatusIndicator
            style={{ alignSelf: 'center', marginRight: '0.5em' }}
            workspaceNamespace={workspace.namespace}
          />
        )}
        {
          // TODO: support Cromwell + other User Apps
          appType === UIAppType.JUPYTER && <RuntimeCost />
        }
        <TooltipTrigger
          disabled={trashEnabled}
          content='Your application is not in a deleteable state.'
        >
          <Clickable
            aria-label={`Delete ${appType} Environment`}
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
        </TooltipTrigger>
      </FlexRow>
      {appType === UIAppType.JUPYTER ? (
        <JupyterButtonRow {...{ workspace, onClickRuntimeConf }} />
      ) : (
        <FlexColumn>
          <FlexRow style={{ justifyContent: 'center' }}>
            Status: {fromUserAppStatusWithFallback(initialUserAppInfo?.status)}{' '}
            <AppStatusIndicator
              style={{ alignSelf: 'center', margin: '0 0.5em' }}
              appStatus={initialUserAppInfo?.status}
              userSuspended={false}
            />
          </FlexRow>
          {cond<React.ReactNode>(
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
                  {...{ billingAccountDisabled }}
                  userApp={initialUserAppInfo}
                />
              ),
            ],
            [
              appType === UIAppType.SAS,
              () => (
                <SASButtonRow
                  {...{ billingAccountDisabled }}
                  userApp={initialUserAppInfo}
                />
              ),
            ],

            () => null
          )}
        </FlexColumn>
      )}
      {showCromwellDeleteModal && (
        <DeleteCromwellConfirmationModal
          clickYes={() => {
            setShowCromwellDeleteModal(false);
            onClickDeleteGkeApp(cromwellConfigIconId);
          }}
          clickNo={() => setShowCromwellDeleteModal(false)}
        />
      )}
    </FlexColumn>
  );
};
