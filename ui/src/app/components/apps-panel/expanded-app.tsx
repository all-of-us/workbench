import * as React from 'react';
import { faGear, faTrashCan } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { Workspace } from 'generated/fetch';

import { Clickable } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
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

const RuntimeSettingsButton = (props: { onClickRuntimeConf: Function }) => (
  <AppsPanelButton
    onClick={props.onClickRuntimeConf}
    icon={faGear}
    buttonText='Settings'
  />
);

export const ExpandedApp = (props: {
  appType: UIAppType;
  workspace: Workspace;
  onClickRuntimeConf: Function;
  onClickDeleteRuntime: Function;
}) => {
  const { runtime } = useStore(runtimeStore);
  const { appType, workspace, onClickRuntimeConf, onClickDeleteRuntime } =
    props;
  return (
    // first iteration: hard-code Jupyter only
    appType === UIAppType.JUPYTER && (
      <FlexColumn style={styles.expandedAppContainer}>
        <FlexRow>
          <div>
            <AppLogo {...{ appType }} style={{ marginRight: '1em' }} />
          </div>
          <RuntimeStatusIcon
            style={{ alignSelf: 'center', marginRight: '0.5em' }}
            workspaceNamespace={workspace.namespace}
          />
          <RuntimeCost />
          <Clickable
            style={
              isActionable(runtime?.status)
                ? styles.enabledTrashButton
                : styles.disabledTrashButton
            }
            onClick={onClickDeleteRuntime}
          >
            <FontAwesomeIcon icon={faTrashCan} />
          </Clickable>
        </FlexRow>
        <FlexRow>
          <RuntimeSettingsButton {...{ onClickRuntimeConf }} />
          <RuntimeStateButton {...{ workspace }} />
          <NewNotebookButton {...{ workspace }} />
        </FlexRow>
      </FlexColumn>
    )
  );
};
