import * as React from 'react';
import { useState } from 'react';
import {
  faGear,
  faPause,
  faPlay,
  faTrashCan,
} from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { UserAppEnvironment, Workspace } from 'generated/fetch';

import { Clickable } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { TooltipTrigger } from 'app/components/popups';
import { RuntimeStatusIcon } from 'app/components/runtime-status-icon';
import { appsApi } from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import { isActionable } from 'app/utils/runtime-utils';
import { runtimeStore, useStore } from 'app/utils/stores';

import { AppLogo } from './app-logo';
import { AppsPanelButton } from './apps-panel-button';
import { NewNotebookButton } from './new-notebook-button';
import { RuntimeCost } from './runtime-cost';
import { RuntimeStateButton } from './runtime-state-button';
import {
  canCreateApp,
  canDeleteApp,
  defaultCromwellConfig,
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

const JupyterButtonRow = (props: {
  workspace: Workspace;
  onClickRuntimeConf: Function;
}) => {
  const { workspace, onClickRuntimeConf } = props;
  return (
    <FlexRow>
      <SettingsButton onClick={onClickRuntimeConf} />
      <RuntimeStateButton {...{ workspace }} />
      <NewNotebookButton {...{ workspace }} />
    </FlexRow>
  );
};

// TODO generalize as UserAppButtonRow?
const CromwellButtonRow = (props: {
  userApp: UserAppEnvironment;
  workspaceNamespace: string;
}) => {
  const { userApp, workspaceNamespace } = props;
  const [launching, setLaunching] = useState(false);

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
      <TooltipTrigger
        disabled={false}
        // RW-9304
        content='Support for pausing Cromwell is not yet available'
      >
        {/* tooltip trigger needs a div for some reason */}
        <div>
          <AppsPanelButton
            disabled={true}
            onClick={() => {}}
            icon={faPause}
            buttonText='Pause'
          />
        </div>
      </TooltipTrigger>
      <TooltipTrigger
        disabled={!launching && canCreateApp(userApp)}
        content='A Cromwell app exists or is being created'
      >
        {/* tooltip trigger needs a div for some reason */}
        <div>
          <AppsPanelButton
            disabled={launching || !canCreateApp(userApp)}
            onClick={() => {
              setLaunching(true);
              appsApi().createApp(workspaceNamespace, defaultCromwellConfig);
            }}
            icon={faPlay}
            buttonText={launching ? 'Launching' : 'Launch'}
          />
        </div>
      </TooltipTrigger>
    </FlexRow>
  );
};

// TODO: refine and style more like RuntimeStatusIcon
const AppStatus = (props: { userApp: UserAppEnvironment }) => {
  const { status } = props.userApp;
  return (
    <div style={{ alignSelf: 'center', marginRight: '1em' }}>{status}</div>
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
    <FlexColumn style={styles.expandedAppContainer}>
      <FlexRow>
        <div>
          <AppLogo {...{ appType }} style={{ marginRight: '1em' }} />
        </div>
        {appType === UIAppType.JUPYTER ? (
          <RuntimeStatusIcon
            style={{ alignSelf: 'center', marginRight: '0.5em' }}
            workspaceNamespace={workspace.namespace}
          />
        ) : (
          initialUserAppInfo && <AppStatus userApp={initialUserAppInfo} />
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
        >
          <FontAwesomeIcon icon={faTrashCan} />
        </Clickable>
      </FlexRow>
      {appType === UIAppType.JUPYTER ? (
        <JupyterButtonRow {...{ workspace, onClickRuntimeConf }} />
      ) : (
        <FlexColumn style={{ alignItems: 'center' }}>
          {/* TODO: keep status updated internally */}
          <div>(refresh to update status)</div>
          {/* TODO: generalize to other User Apps*/}
          <CromwellButtonRow
            userApp={initialUserAppInfo}
            workspaceNamespace={workspace.namespace}
          />
        </FlexColumn>
      )}
    </FlexColumn>
  );
};
