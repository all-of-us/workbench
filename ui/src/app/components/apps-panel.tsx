import * as React from 'react';
import { useEffect, useState } from 'react';
import {
  faGear,
  faPause,
  faPlay,
  faPlusCircle,
  faTrashCan,
} from '@fortawesome/free-solid-svg-icons';
import { faSyncAlt } from '@fortawesome/free-solid-svg-icons/faSyncAlt';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { BillingStatus, RuntimeStatus, Workspace } from 'generated/fetch';

import { Clickable, CloseButton } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { DisabledPanel } from 'app/components/runtime-configuration-panel/disabled-panel';
import { RuntimeStatusIcon } from 'app/components/runtime-status-icon';
import { NewNotebookModal } from 'app/pages/analysis/new-notebook-modal';
import { dropNotebookFileSuffix } from 'app/pages/analysis/util';
import { notebooksApi } from 'app/services/swagger-fetch-clients';
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
    marginLeft: '1em',
    marginBottom: '1em',
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
    color: colors.primary,
    fontFamily: 'Montserrat',
    fontWeight: 'bold',
    letterSpacing: 0,
    lineHeight: '15px',
  },
  disabledButtonText: {
    color: colors.secondary,
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
  closeButton: { marginLeft: 'auto', alignSelf: 'center' },
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

  return (
    <img
      src={logos.get(props.appType)}
      alt={props.appType.toString()}
      style={{ marginRight: '1em' }}
    />
  );
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

  if (!isVisible(runtime?.status)) {
    return null;
  }

  const analysisConfig = toAnalysisConfig(runtime, persistentDisk);
  const runningCost = formatUsd(machineRunningCost(analysisConfig));
  const storageCost = formatUsd(machineStorageCost(analysisConfig));

  // display running cost or stopped (storage) cost
  // Error and Deleted statuses are not included because they're not "visible" [isVisible() = false]
  const text: string = switchCase(
    runtime.status,
    // TODO: is it appropriate to assume full running cost in all these cases?
    [RuntimeStatus.Creating, () => `${runtime.status} ${runningCost} / hr`],
    [RuntimeStatus.Running, () => `${runtime.status} ${runningCost} / hr`],
    [RuntimeStatus.Updating, () => `${runtime.status} ${runningCost} / hr`],
    [RuntimeStatus.Deleting, () => `${runtime.status} ${runningCost} / hr`],
    [RuntimeStatus.Stopping, () => `Pausing ${runningCost} / hr`],
    [RuntimeStatus.Starting, () => `Resuming ${runningCost} / hr`],
    [RuntimeStatus.Stopped, () => `Paused ${storageCost} / hr`],
    [RuntimeStatus.Unknown, () => runtime.status]
  );

  return (
    <div data-test-id='runtime-cost' style={{ alignSelf: 'center' }}>
      {text}
    </div>
  );
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
        () => {
          setPausing(true);
          setRuntimeStatus(RuntimeStatusRequest.Stop);
        },
      ],
      [
        RuntimeStatus.Stopped,
        () => {
          setResuming(true);
          setRuntimeStatus(RuntimeStatusRequest.Start);
        },
      ]
    );

  const [icon, buttonText, enabled] = cond(
    [
      pausing || status === RuntimeStatus.Stopping,
      () => [faSyncAlt, 'Pausing', false],
    ],
    [
      resuming || status === RuntimeStatus.Starting,
      () => [faSyncAlt, 'Resuming', false],
    ],
    [status === RuntimeStatus.Stopped, () => [faPlay, 'Resume', true]],
    [status === RuntimeStatus.Running, () => [faPause, 'Pause', true]],
    // choose a (disabled) default to show for other states
    () => [faPause, 'Pause', false]
  );

  return (
    <Clickable
      disabled={!enabled}
      style={{ padding: '0.5em' }}
      onClick={toggleRuntimeStatus}
    >
      <FlexColumn style={enabled ? styles.button : styles.disabledButton}>
        <FontAwesomeIcon {...{ icon }} style={styles.buttonIcon} />
        <div style={enabled ? styles.buttonText : styles.disabledButtonText}>
          {buttonText}
        </div>
      </FlexColumn>
    </Clickable>
  );
};

const RuntimeOpenButton = (props: {
  workspace: Workspace;
  onClose: Function;
}) => {
  const { workspace, onClose } = props;

  const [showModal, setShowModal] = useState(false);
  const [notebookNameList, setNotebookNameList] = useState<string[]>([]);

  useEffect(() => {
    notebooksApi()
      .getNoteBookList(workspace.namespace, workspace.id)
      .then((nbl) =>
        setNotebookNameList(nbl.map((fd) => dropNotebookFileSuffix(fd.name)))
      );
  }, []);

  return (
    <>
      {showModal && (
        <NewNotebookModal
          {...{ workspace }}
          existingNameList={notebookNameList}
          onClose={() => {
            setShowModal(false);
          }}
          onCreate={() => {
            setShowModal(false);
            // close the AppsPanel on notebook creation
            onClose();
          }}
        />
      )}
      <Clickable
        style={{ padding: '0.5em' }}
        onClick={() => setShowModal(true)}
      >
        <FlexColumn style={styles.button}>
          <FontAwesomeIcon icon={faPlusCircle} style={styles.buttonIcon} />
          <div style={styles.buttonText}>Create New</div>
        </FlexColumn>
      </Clickable>
    </>
  );
};

const ExpandedApp = (props: {
  appType: UIAppType;
  workspace: Workspace;
  onClose: Function;
  onClickRuntimeConf: Function;
  onClickDeleteRuntime: Function;
}) => {
  const { runtime } = useStore(runtimeStore);
  const {
    appType,
    workspace,
    onClose,
    onClickRuntimeConf,
    onClickDeleteRuntime,
  } = props;
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
          <RuntimeOpenButton {...{ workspace, onClose }} />
        </FlexRow>
      </FlexColumn>
    )
  );
};

export const AppsPanel = (props: {
  workspace: Workspace;
  onClose: Function;
  onClickRuntimeConf: Function;
  onClickDeleteRuntime: Function;
}) => {
  const { onClose } = props;
  const { runtime } = useStore(runtimeStore);
  const {
    config: { enableGkeApp },
  } = useStore(serverConfigStore);

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

  const showExpanded = (appType: UIAppType): boolean =>
    userExpandedApps.includes(appType) ||
    appStates.find((s) => s.appType === appType)?.expand;

  const showInActiveSection = (appType: UIAppType): boolean =>
    appsToDisplay.includes(appType) &&
    showExpanded(appType) &&
    !userExpandedApps.includes(appType);
  const showActiveSection = appStates
    .map((s) => s.appType)
    .some(showInActiveSection);

  const showInAvailableSection = (appType: UIAppType): boolean =>
    appsToDisplay.includes(appType) &&
    (userExpandedApps.includes(appType) || !showExpanded(appType));
  const showAvailableSection = appStates
    .map((s) => s.appType)
    .some(showInAvailableSection);

  const ActiveApps = () => (
    <FlexColumn>
      <FlexRow>
        <h3 style={styles.header}>Active applications</h3>
        {showActiveSection && (
          <CloseButton {...{ onClose }} style={styles.closeButton} />
        )}
      </FlexRow>
      {appsToDisplay.map((appType) => {
        return showInActiveSection(appType) ? (
          <ExpandedApp {...{ ...props, appType }} key={appType} />
        ) : null;
      })}
    </FlexColumn>
  );

  const AvailableApps = () => (
    <FlexColumn>
      <FlexRow>
        <h3 style={styles.header}>Launch other applications</h3>
        {!showActiveSection && (
          <CloseButton {...{ onClose }} style={styles.closeButton} />
        )}
      </FlexRow>
      {appsToDisplay.map((appType) => {
        return showInAvailableSection(appType) ? (
          showExpanded(appType) ? (
            <ExpandedApp {...{ ...props, appType }} key={appType} />
          ) : (
            <AvailableApp
              {...{ appType }}
              key={appType}
              onClick={() => addToExpandedApps(appType)}
            />
          )
        ) : null;
      })}
    </FlexColumn>
  );

  return props.workspace.billingStatus === BillingStatus.INACTIVE ? (
    <DisabledPanel />
  ) : (
    <div>
      {runtime?.status && showActiveSection && <ActiveApps />}
      {runtime?.status && showAvailableSection && <AvailableApps />}
    </div>
  );
};
