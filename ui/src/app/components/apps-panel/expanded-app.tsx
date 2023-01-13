import * as React from 'react';
import { faGear, faTrashCan } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { Workspace } from 'generated/fetch';

import { Clickable } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { TooltipTrigger } from 'app/components/popups';
import { RuntimeStatusIcon } from 'app/components/runtime-status-icon';
import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import { isActionable } from 'app/utils/runtime-utils';
import { runtimeStore, useStore } from 'app/utils/stores';

import { AppLogo } from './app-logo';
import { AppsPanelButton } from './apps-panel-button';
import { NewNotebookButton } from './new-notebook-button';
import { RuntimeCost } from './runtime-cost';
import { RuntimeStateButton } from './runtime-state-button';
import { UIAppType } from './utils';

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

const JupyterAppButtonRow = (props: {
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

const UserAppButtonRow = () => (
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
    <AppsPanelButton onClick={() => {}} icon={faGear} buttonText='TODO' />
    <AppsPanelButton onClick={() => {}} icon={faGear} buttonText='TODO' />
  </FlexRow>
);

export const ExpandedApp = (props: {
  appType: UIAppType;
  workspace: Workspace;
  onClickRuntimeConf: Function;
  onClickDeleteRuntime: Function;
  onClickDeleteAppEnvironment?: Function; // TODO
}) => {
  const { runtime } = useStore(runtimeStore);
  const {
    appType,
    workspace,
    onClickRuntimeConf,
    onClickDeleteRuntime,
    onClickDeleteAppEnvironment,
  } = props;
  const trashEnabled =
    appType === UIAppType.JUPYTER ? isActionable(runtime?.status) : true; // TODO
  const onClickDelete =
    appType === UIAppType.JUPYTER
      ? onClickDeleteRuntime
      : onClickDeleteAppEnvironment;
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
        <JupyterAppButtonRow {...{ workspace, onClickRuntimeConf }} />
      ) : (
        <UserAppButtonRow />
      )}
    </FlexColumn>
  );
};
