import * as React from 'react';
import { useEffect, useState } from 'react';
import {
  faGear,
  faPause,
  faPlay,
  faTrashCan,
} from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { AppType, UserAppEnvironment, Workspace } from 'generated/fetch';

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
import { canDeleteApp, defaultCromwellConfig, UIAppType } from './utils';

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
const CromwellButtonRow = (props: { workspaceNamespace: string }) => {
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
      <AppsPanelButton
        onClick={() => {
          appsApi().createApp(props.workspaceNamespace, defaultCromwellConfig);
        }}
        icon={faPlay}
        buttonText='Launch'
      />
    </FlexRow>
  );
};

const TempCromwellDebugInfo = (props: { userApp: UserAppEnvironment }) => {
  const { appName, status } = props.userApp;
  return (
    <div>
      {appName} | {status}
    </div>
  );
};

interface ExpandedAppProps {
  appType: UIAppType;
  workspace: Workspace;
  onClickRuntimeConf: Function;
  onClickDeleteRuntime: Function;
}
export const ExpandedApp = (props: ExpandedAppProps) => {
  const { runtime } = useStore(runtimeStore);
  const { appType, workspace, onClickRuntimeConf, onClickDeleteRuntime } =
    props;

  // not used for Jupyter
  const [userApp, setUserApp] = useState<UserAppEnvironment>();
  useEffect(() => {
    if (appType !== UIAppType.JUPYTER) {
      appsApi()
        .listAppsInWorkspace(workspace.namespace)
        .then((result) => {
          console.log(result);
          result
            .filter((env) => env.appType === AppType.CROMWELL) // TODO choose the right app instead of hardcode
            .map(setUserApp);
        });
    }
  }, []);

  const trashEnabled =
    appType === UIAppType.JUPYTER
      ? isActionable(runtime?.status)
      : canDeleteApp(userApp);
  const onClickDelete =
    appType === UIAppType.JUPYTER
      ? onClickDeleteRuntime
      : () => appsApi().deleteApp(workspace.namespace, userApp.appName, true);

  return (
    <FlexColumn style={styles.expandedAppContainer}>
      <FlexRow>
        <div>
          <AppLogo {...{ appType }} style={{ marginRight: '1em' }} />
        </div>
        {
          // TODO: support Cromwell + other User Apps
          appType === UIAppType.JUPYTER && (
            <RuntimeStatusIcon
              style={{ alignSelf: 'center', marginRight: '0.5em' }}
              workspaceNamespace={workspace.namespace}
            />
          )
        }
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
        <>
          {/* TODO: generalize to other User Apps */}
          <CromwellButtonRow workspaceNamespace={workspace.namespace} />
          {userApp && <TempCromwellDebugInfo {...{ userApp }} />}
        </>
      )}
    </FlexColumn>
  );
};
