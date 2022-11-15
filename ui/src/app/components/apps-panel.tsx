import * as React from 'react';
import { useEffect, useState } from 'react';
import {
  faGear,
  faPause,
  faPlay,
  faRocket,
  faTrashCan,
} from '@fortawesome/free-solid-svg-icons';
import { faSyncAlt } from '@fortawesome/free-solid-svg-icons/faSyncAlt';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { BillingStatus, RuntimeStatus, Workspace } from 'generated/fetch';

import { Clickable } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { DisabledPanel } from 'app/components/runtime-configuration-panel/disabled-panel';
import { RuntimeStatusIcon } from 'app/components/runtime-status-icon';
import colors from 'app/styles/colors';
import { cond, reactStyles, switchCase } from 'app/utils';
import { machineRunningCost, machineStorageCost } from 'app/utils/machines';
import { formatUsd } from 'app/utils/numbers';
import {
  isActionable,
  isVisible,
  RuntimeStatusRequest,
  toAnalysisConfig,
  useRuntimeStatus,
} from 'app/utils/runtime-utils';
import {
  diskStore,
  runtimeStore,
  serverConfigStore,
  useStore,
} from 'app/utils/stores';
import jupyterLogo from 'assets/images/jupyter.png';
import rStudioLogo from 'assets/images/RStudio.png';

const styles = reactStyles({
  expandedAppContainer: {
    background: colors.white,
    margin: '1em',
    padding: '1em',
    width: 'fit-content',
  },
  button: {
    opacity: 1,
    boxSizing: 'border-box',
    height: 69,
    width: 108,
    border: '1px solid #979797',
    borderRadius: 9,
    alignItems: 'center',
  },
  disabledButton: {
    opacity: 0.46,
    cursor: 'not-allowed',
    boxSizing: 'border-box',
    height: 69,
    width: 108,
    border: '1px solid #979797',
    borderRadius: 9,
    alignItems: 'center',
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
  buttonText: {
    color: '#4D72AA', // TODO fix
    fontFamily: 'Montserrat',
    fontWeight: 'bold',
    letterSpacing: 0,
    lineHeight: '15px',
  },
  buttonIcon: {
    height: 23,
    padding: '0.7em',
  },
  header: {
    color: colors.primary,
    height: 19,
    fontFamily: 'Montserrat',
    fontSize: 16,
    fontWeight: 600,
    letterSpacing: 0,
    lineHeight: '19px',
    margin: '1em',
  },
  availableApp: {
    background: colors.white,
    marginLeft: '1em',
    marginBottom: '1em',
    justifyContent: 'center',
  },
});

// Eventually we will need to align this with the API's AppType
enum UIAppType {
  JUPYTER = 'Jupyter',
  RSTUDIO = 'RStudio',
}

const AppLogo = (props: { appType: UIAppType }) => {
  const logos = new Map([
    [UIAppType.JUPYTER, jupyterLogo],
    [UIAppType.RSTUDIO, rStudioLogo],
  ]);

  return <img src={logos.get(props.appType)} style={{ marginRight: '1em' }} />;
};

const AvailableApp = (props: { appType: UIAppType; onClick: Function }) => {
  const { appType, onClick } = props;
  return (
    <Clickable {...{ onClick }}>
      <FlexRow style={styles.availableApp}>
        <AppLogo {...{ appType }} />
      </FlexRow>
    </Clickable>
  );
};

const RuntimeCost = () => {
  const { runtime } = useStore(runtimeStore);
  const { persistentDisk } = useStore(diskStore);

  if (!runtime) {
    return null;
  }

  const analysisConfig = toAnalysisConfig(runtime, persistentDisk);
  const runningCost = formatUsd(machineRunningCost(analysisConfig));
  const storageCost = formatUsd(machineStorageCost(analysisConfig));

  // display running cost or stopped (storage) cost
  switch (runtime.status) {
    // TODO: is it appropriate to assume full running cost in all these cases?
    case RuntimeStatus.Creating:
    case RuntimeStatus.Running:
    case RuntimeStatus.Updating:
    case RuntimeStatus.Deleting:
      return (
        <div style={{ alignSelf: 'center' }}>
          {runtime.status} {runningCost} / hr
        </div>
      );
    case RuntimeStatus.Stopping:
      return (
        <div style={{ alignSelf: 'center' }}>Pausing {runningCost} / hr</div>
      );
    case RuntimeStatus.Starting:
      return (
        <div style={{ alignSelf: 'center' }}>Resuming {runningCost} / hr</div>
      );
    case RuntimeStatus.Stopped:
      return (
        <div style={{ alignSelf: 'center' }}>Paused {storageCost} / hr</div>
      );
    case RuntimeStatus.Unknown:
      return <div style={{ alignSelf: 'center' }}>Unknown</div>;
    case RuntimeStatus.Error:
      return <div style={{ alignSelf: 'center' }}>Error</div>;
    case RuntimeStatus.Deleted:
    default:
      return <div style={{ alignSelf: 'center' }}>Not Running</div>;
  }
};

const RuntimeSettingsButton = (props: { onClickRuntimeConf: Function }) => (
  <Clickable style={{ padding: '0.5em' }} onClick={props.onClickRuntimeConf}>
    <FlexColumn style={styles.button}>
      <FontAwesomeIcon icon={faGear} style={styles.buttonIcon} />
      <div style={styles.buttonText}>Settings</div>
    </FlexColumn>
  </Clickable>
);

const RuntimeStateButton = (props: { workspace: Workspace }) => {
  const {
    workspace: { namespace, googleProject },
  } = props;

  const [status, setRuntimeStatus] = useRuntimeStatus(namespace, googleProject);

  // transition states, so we don't have to wait for polling
  const [pausing, setPausing] = useState(false);
  const [resuming, setResuming] = useState(false);

  // when polling updates the status value, clear our transition states
  useEffect(() => {
    if (pausing) {
      setPausing(false);
    }
    if (resuming) {
      setResuming(false);
    }
  }, [status]);

  // transition from RUNNING to STOPPED, or STOPPED to RUNNING
  const toggleRuntimeStatus = () =>
    switchCase(
      status,
      [
        RuntimeStatus.Running,
        async () => {
          setPausing(true);
          await setRuntimeStatus(RuntimeStatusRequest.Stop);
        },
      ],
      [
        RuntimeStatus.Stopped,
        async () => {
          setResuming(true);
          await setRuntimeStatus(RuntimeStatusRequest.Start);
        },
      ]
    );

  const [icon, buttonText] = cond(
    [
      pausing || status === RuntimeStatus.Stopping,
      () => [faSyncAlt, 'Pausing'],
    ],
    [
      resuming || status === RuntimeStatus.Starting,
      () => [faSyncAlt, 'Resuming'],
    ],
    [status === RuntimeStatus.Stopped, () => [faPlay, 'Resume']],
    [status === RuntimeStatus.Running, () => [faPause, 'Pause']],
    // choose this button's most common function as the (disabled) default text to show for other states
    () => [faPause, 'Pause']
  );

  const disabled =
    pausing ||
    resuming ||
    (status !== RuntimeStatus.Running && status !== RuntimeStatus.Stopped);

  return (
    <Clickable
      {...{ disabled }}
      style={{ padding: '0.5em' }}
      onClick={toggleRuntimeStatus}
    >
      <FlexColumn style={disabled ? styles.disabledButton : styles.button}>
        <FontAwesomeIcon {...{ icon }} style={styles.buttonIcon} />
        <div style={styles.buttonText}>{buttonText}</div>
      </FlexColumn>
    </Clickable>
  );
};

const RuntimeOpenButton = (props: { disabled: boolean }) => {
  const { disabled } = props;
  return (
    <Clickable {...{ disabled }} style={{ padding: '0.5em' }}>
      <FlexColumn style={disabled ? styles.disabledButton : styles.button}>
        <FontAwesomeIcon icon={faRocket} style={styles.buttonIcon} />
        <div style={styles.buttonText}>Open</div>
      </FlexColumn>
    </Clickable>
  );
};

const ExpandedApp = (props: {
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
            <AppLogo {...{ appType }} />
          </div>
          <RuntimeStatusIcon
            style={{ alignSelf: 'center', marginRight: '0.5em' }}
            workspaceNamespace={workspace.namespace}
          />
          <RuntimeCost />
          <Clickable
            disabled={!isActionable(runtime?.status)}
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
          <RuntimeOpenButton disabled={true} />
        </FlexRow>
      </FlexColumn>
    )
  );
};

export const AppsPanel = (props: {
  workspace: Workspace;
  onClickRuntimeConf: Function;
  onClickDeleteRuntime: Function;
}) => {
  const { runtime } = useStore(runtimeStore);
  const {
    config: { enableGkeApp },
  } = useStore(serverConfigStore);

  // TODO not sure if this behavior is right

  // which app(s) have the user explicitly expanded by clicking?
  const [userExpandedApps, setUserExpandedApps] = useState([]);
  const addToExpandedApps = (appType: UIAppType) =>
    setUserExpandedApps([...userExpandedApps, appType]);

  // environments will see only Jupyter until we are ready to launch apps (enableGkeApp = true)
  // in display order
  const appsToDisplay = enableGkeApp
    ? [UIAppType.JUPYTER, UIAppType.RSTUDIO]
    : [UIAppType.JUPYTER];

  const appStates = [
    { appType: UIAppType.JUPYTER, expand: isVisible(runtime?.status) },
    // RStudio is not implemented yet
    { appType: UIAppType.RSTUDIO, expand: false },
  ];

  const shouldExpand = (appType: UIAppType): boolean =>
    userExpandedApps.includes(appType) ||
    appStates.find((s) => s.appType === appType)?.expand;

  const showActive = appStates.some(
    (s) => appsToDisplay.includes(s.appType) && shouldExpand(s.appType)
  );
  const showAvailable = appStates.some(
    (s) => appsToDisplay.includes(s.appType) && !shouldExpand(s.appType)
  );

  const ActiveApps = () => (
    <div>
      <h3 style={styles.header}>Active applications</h3>
      {appsToDisplay.map((appType) => {
        return shouldExpand(appType) ? (
          <ExpandedApp {...{ ...props, appType }} key={appType} />
        ) : null;
      })}
    </div>
  );

  const AvailableApps = () => (
    <FlexColumn>
      <h3 style={styles.header}>Launch other applications</h3>
      {appsToDisplay.map((appType) => {
        return shouldExpand(appType) ? null : (
          <AvailableApp
            {...{ appType }}
            key={appType}
            onClick={() => addToExpandedApps(appType)}
          />
        );
      })}
    </FlexColumn>
  );

  return props.workspace.billingStatus === BillingStatus.INACTIVE ? (
    <DisabledPanel />
  ) : (
    <div>
      {runtime?.status && showActive && <ActiveApps />}
      {runtime?.status && showAvailable && <AvailableApps />}
    </div>
  );
};
