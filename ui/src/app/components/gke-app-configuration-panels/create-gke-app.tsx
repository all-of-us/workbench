import * as React from 'react';
import { CSSProperties } from 'react';
import { Dropdown } from 'primereact/dropdown';

import {
  AppStatus,
  AppType,
  CreateAppRequest,
  Disk,
  UserAppEnvironment,
} from 'generated/fetch';

import { cond } from '@terra-ui-packages/core-utils';
import {
  appMaxDiskSize,
  appMinDiskSize,
  canDeleteApp,
  defaultAppRequest,
  findApp,
  isAppActive,
  toUIAppType,
} from 'app/components/apps-panel/utils';
import { LinkButton } from 'app/components/buttons';
import { DeletePersistentDiskButton } from 'app/components/common-env-conf-panels/delete-persistent-disk-button';
import { EnvironmentInformedActionPanel } from 'app/components/common-env-conf-panels/environment-informed-action-panel';
import { MachineSelector } from 'app/components/common-env-conf-panels/machine-selector';
import { styles } from 'app/components/common-env-conf-panels/styles';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { SidebarIconId } from 'app/components/help-sidebar-icons';
import { ClrIcon } from 'app/components/icons';
import { CheckBox } from 'app/components/inputs';
import { ErrorMessage } from 'app/components/messages';
import { TooltipTrigger } from 'app/components/popups';
import { DiskSizeSelector } from 'app/components/runtime-configuration-panel/disk-size-selector';
import { AnalysisConfig } from 'app/utils/analysis-config';
import { getWholeDaysFromNow } from 'app/utils/dates';
import {
  allMachineTypes,
  AutodeleteDaysThresholds,
  ComputeType,
  DEFAULT_AUTODELETE_THRESHOLD_MINUTES,
  findMachineByName,
  Machine,
} from 'app/utils/machines';
import { sidebarActiveIconStore } from 'app/utils/navigation';
import { ProfileStore, serverConfigStore, useStore } from 'app/utils/stores';
import { oxfordCommaString } from 'app/utils/strings';
import {
  appTypeToString,
  isDiskSizeValid,
  isInteractiveUserApp,
  unattachedDiskExists,
} from 'app/utils/user-apps-utils';
import { WorkspaceData } from 'app/utils/workspace-data';

import { CreateGkeAppButton } from './create-gke-app-button';
import { DisabledCloudComputeProfile } from './disabled-cloud-compute-profile';
import { OpenGkeAppButton } from './open-gke-app-button';

const defaultIntroText =
  'Your analysis environment consists of an application and compute resources. ' +
  'Your cloud environment is unique to this workspace and not shared with other users.';

const toMachine = (createAppRequest: CreateAppRequest): Machine =>
  findMachineByName(createAppRequest?.kubernetesRuntimeConfig.machineType);

const toAnalysisConfig = (
  createAppRequest: CreateAppRequest
): AnalysisConfig => {
  const { persistentDiskRequest, kubernetesRuntimeConfig } = createAppRequest;
  return {
    machine: toMachine(createAppRequest),
    diskConfig: {
      size: persistentDiskRequest.size,
      detachable: true,
      detachableType: persistentDiskRequest.diskType,
      existingDiskName: null,
    },
    numNodes: kubernetesRuntimeConfig.numNodes,
    // defaults
    computeType: ComputeType.Standard,
    dataprocConfig: undefined,
    gpuConfig: undefined,
    detachedDisk: undefined,
    autopauseThreshold: undefined,
  };
};

const otherApps = (
  userApps: UserAppEnvironment[] | undefined,
  thisAppType: AppType
): UserAppEnvironment[] =>
  userApps?.filter((app) => app.appType !== thisAppType) ?? [];

const otherMachineTypes = (
  userApps: UserAppEnvironment[] | undefined,
  thisAppType: AppType
): string[] =>
  Array.from(
    new Set(
      otherApps(userApps, thisAppType)
        .map(
          (app: UserAppEnvironment) => app.kubernetesRuntimeConfig?.machineType
        )
        // filter out undefined values
        .filter((x) => x) ?? []
    )
  );

// if there are other app(s) with a common machine type, return that machine type
// TODO: what should we do if there is more than one (shouldn't happen) - for now, return undefined

const maybeGetOtherMachineType = (
  userApps: UserAppEnvironment[] | undefined,
  thisAppType: AppType
): string | undefined => {
  const otherTypes = otherMachineTypes(userApps, thisAppType);
  return otherTypes.length === 1 ? otherTypes[0] : undefined;
};

export interface CreateGkeAppProps {
  userApps: UserAppEnvironment[];
  appType: AppType;
  onClose: () => void;
  creatorFreeCreditsRemaining: number | null;
  workspace: WorkspaceData;
  profileState: ProfileStore;
  disk: Disk | undefined;
  onClickDeleteGkeApp: (sidebarIcon: SidebarIconId) => void;
  onClickDeleteUnattachedPersistentDisk: () => void;
  introText?: string;
  CostNote?: React.FunctionComponent;
  SupportNote?: React.FunctionComponent;
  CreateAppText?: React.FunctionComponent;
}

type ToOmit =
  | 'appType'
  | 'introText'
  | 'CostNote'
  | 'SupportNote'
  | 'CreateAppText';

// for use by the individual gke app creation components, e.g. CreateCromwell
export type CommonCreateGkeAppProps = Omit<CreateGkeAppProps, ToOmit>;

export const CreateGkeApp = ({
  userApps,
  appType,
  onClose,
  creatorFreeCreditsRemaining,
  workspace,
  profileState,
  disk,
  onClickDeleteGkeApp,
  onClickDeleteUnattachedPersistentDisk,
  introText = defaultIntroText,
  CostNote = () => null,
  SupportNote = () => null,
  CreateAppText = () => null,
}: CreateGkeAppProps) => {
  const {
    config: { enableGKEAppMachineTypeChoice },
  } = useStore(serverConfigStore);
  const { profile } = profileState;
  const { billingStatus } = workspace;

  const defaultCreateRequest = defaultAppRequest[appType];

  const app = findApp(userApps, toUIAppType[appType]);

  // there may or may not be an existing `app` and/or `disk`
  // start with the default config, but override with the existing app and disk configs

  const [createAppRequest, setCreateAppRequest] =
    React.useState<CreateAppRequest>({
      ...defaultCreateRequest,
      kubernetesRuntimeConfig: {
        ...defaultCreateRequest.kubernetesRuntimeConfig,
        machineType:
          // if there is an active app, use its machineType for display
          app?.kubernetesRuntimeConfig.machineType ??
          // otherwise, if there is an active app of a different type, use its machine type to configure this one
          maybeGetOtherMachineType(userApps, appType) ??
          // use the default if neither of these cases apply
          defaultCreateRequest.kubernetesRuntimeConfig.machineType,
      },

      persistentDiskRequest: disk ?? defaultCreateRequest.persistentDiskRequest,
      autodeleteEnabled:
        app?.autodeleteEnabled ?? defaultCreateRequest.autodeleteEnabled,
      autodeleteThreshold:
        app?.autodeleteThreshold ?? defaultCreateRequest.autodeleteThreshold,
    });

  const autodeleteRemainingDays: number = (() => {
    if (app?.autodeleteEnabled && app.dateAccessed && app.autodeleteThreshold) {
      const dateAccessed = new Date(app.dateAccessed);
      const autodeleteDate = new Date(
        dateAccessed.getTime() + app.autodeleteThreshold * 60 * 1000
      );

      return getWholeDaysFromNow(autodeleteDate.getTime());
    }
    return null;
  })();

  const autodeleteToolTip = (
    <div>
      Deleting the environment when not in use will help reduce costs.
      <br />
      Reset Timer: Open the application to reset the timer
      <br />
      Disable Auto-Delete: Uncheck the box if you do not want your app to be
      deleted. Disabling the auto-delete feature of a running environment is not
      currently possible
    </div>
  );

  const showDeleteDiskButton = unattachedDiskExists(app, disk);
  const showDeleteAppButton = canDeleteApp(app);

  const canModifyDiskSize = !app && !unattachedDiskExists(app, disk);

  const disableDiskSizeContent = cond(
    [
      !!app,
      `Disk size cannot be updated because ${toUIAppType[appType]} environment already exists. 
      To make changes, please delete the disk and recreate the environment.`,
    ],
    [
      unattachedDiskExists(app, disk),
      `Cannot modify existing disk. To update the disk size please delete the disk and create a new environment.`,
    ]
  );

  const showErrorBanner =
    createAppRequest && !isDiskSizeValid(createAppRequest);

  // when there is a delete button, FlexRow aligns the open/create button to the right
  // for consistency of location when there is no delete button, we shift it to the right with `margin-left: auto`

  const shiftOpenOrCreateButtonRight =
    !showDeleteDiskButton && !showDeleteAppButton;
  const openOrCreateButtonStyle: CSSProperties = shiftOpenOrCreateButtonRight
    ? {
        marginLeft: 'auto',
      }
    : {};

  const canConfigureMachineType =
    enableGKEAppMachineTypeChoice &&
    !isAppActive(app) &&
    otherApps(userApps, appType).length === 0;

  const machineTypeDisabledText = cond(
    [
      otherApps(userApps, appType).length > 0,
      'Cannot configure the compute profile when there are applications running in the workspace.  ' +
        'You must delete other applications before configuring a new one with a different compute profile.',
    ],
    [
      app?.status === AppStatus.DELETING,
      'Cannot configure the compute profile of an application which is being deleted.  Please wait for deletion to complete.',
    ],
    [
      isAppActive(app),
      'Cannot configure the compute profile of a running application.  ' +
        'Please delete the current application and create a new application with the desired profile.',
    ]
  );

  // TODO determine appropriate machine types for the app type
  const validMachineTypes = allMachineTypes;

  const otherAppTypes: string[] = Object.keys(AppType)
    .filter((k) => AppType[k] !== appType)
    .map((k) => appTypeToString[AppType[k]]);
  // also handles a potential future when we have more than two other app types
  const otherAppsString = oxfordCommaString(otherAppTypes);
  const sharingNote =
    `Your ${appTypeToString[appType]} environment will share CPU and RAM ` +
    `resources with any ${otherAppsString} environments you run in this workspace.`;

  return (
    <FlexColumn
      id={`${appTypeToString[appType]}-configuration-panel`}
      style={{ height: '100%', rowGap: '1rem' }}
    >
      <div>{introText}</div>
      <div style={{ ...styles.controlSection }}>
        <EnvironmentInformedActionPanel
          {...{
            creatorFreeCreditsRemaining,
            profile,
            workspace,
          }}
          analysisConfig={toAnalysisConfig(createAppRequest)}
          appType={toUIAppType[appType]}
          status={app?.status}
        />
        <CostNote />
      </div>
      <div style={{ ...styles.controlSection }}>
        {canConfigureMachineType ? (
          <FlexColumn
            style={{ ...styles.controlSection, padding: 0, rowGap: '1em' }}
          >
            <FlexRow>
              <h3
                style={{
                  ...styles.sectionHeader,
                  ...styles.bold,
                  marginRight: '3rem',
                }}
              >
                Cloud compute profile
              </h3>
              <div style={styles.formGrid2}>
                <MachineSelector
                  {...{ validMachineTypes }}
                  idPrefix={appTypeToString[appType]}
                  disabled={false}
                  selectedMachine={toMachine(createAppRequest)}
                  onChange={(machine: Machine) =>
                    setCreateAppRequest((prevState) => ({
                      ...prevState,
                      kubernetesRuntimeConfig: {
                        ...prevState.kubernetesRuntimeConfig,
                        machineType: machine.name,
                      },
                    }))
                  }
                  machineType={
                    createAppRequest?.kubernetesRuntimeConfig.machineType
                  }
                />
              </div>
            </FlexRow>
            <div>{sharingNote}</div>
          </FlexColumn>
        ) : (
          <DisabledCloudComputeProfile
            {...{ appType, sharingNote }}
            machine={toMachine(createAppRequest)}
            disabledText={machineTypeDisabledText}
          />
        )}
      </div>
      <div style={{ ...styles.controlSection }}>
        <FlexRow
          style={{
            alignItems: 'center',
          }}
        >
          <CheckBox
            aria-label={`gke-autodelete-checkbox`}
            disabled={isAppActive(app)}
            checked={
              createAppRequest.autodeleteEnabled || app?.autodeleteEnabled
            }
            onChange={(autodeleteEnabled) => {
              setCreateAppRequest((prevState) => ({
                ...prevState,
                autodeleteEnabled,
              }));
            }}
            style={{ marginRight: '1rem', zoom: 1.5 }}
          />
          <FlexColumn>
            <label style={styles.label} htmlFor='gke-autodelete-label'>
              Automatically delete application after
            </label>
            <p style={{ marginTop: '0' }}>
              Your persistent disk will not be deleted.
            </p>
          </FlexColumn>
          <TooltipTrigger side='top' content={autodeleteToolTip}>
            <div>
              <ClrIcon
                className='is-solid'
                style={{
                  marginRight: '0.5rem',
                  marginTop: '0.3rem',
                  zoom: 1.2,
                }}
                shape='info-standard'
              />
            </div>
          </TooltipTrigger>
          <FlexColumn>
            <Dropdown
              aria-label={`Auto-deletion time limit`}
              appendTo='self'
              disabled={isAppActive(app) || !createAppRequest.autodeleteEnabled}
              options={AutodeleteDaysThresholds.map((days) => ({
                value: days * 24 * 60,
                label: `Idle for ${days} days`,
              }))}
              value={
                createAppRequest.autodeleteThreshold ||
                DEFAULT_AUTODELETE_THRESHOLD_MINUTES
              }
              onChange={(e) => {
                setCreateAppRequest((prevState) => ({
                  ...prevState,
                  autodeleteThreshold: e.value,
                }));
              }}
              style={{ marginLeft: '1rem' }}
            />
            {autodeleteRemainingDays !== null && (
              <p
                aria-label={`Autodelete remaining days`}
                style={{ marginTop: '0', marginLeft: '1rem' }}
              >
                {autodeleteRemainingDays > 0
                  ? `${autodeleteRemainingDays} days remain until deletion.`
                  : 'App will be deleted within 1 day.'}
              </p>
            )}
          </FlexColumn>
        </FlexRow>
      </div>
      <div style={{ ...styles.controlSection }}>
        <FlexRow>
          <TooltipTrigger
            disabled={canModifyDiskSize}
            content={disableDiskSizeContent}
          >
            <div>
              <DiskSizeSelector
                onChange={(size: number) =>
                  setCreateAppRequest((prevState) => ({
                    ...prevState,
                    persistentDiskRequest: {
                      ...prevState.persistentDiskRequest,
                      size: size,
                    },
                  }))
                }
                disabled={!canModifyDiskSize}
                diskSize={createAppRequest.persistentDiskRequest.size}
                idPrefix={'gke-app'}
              />
            </div>
          </TooltipTrigger>
        </FlexRow>
      </div>
      <SupportNote />
      <FlexRow>
        {showErrorBanner && (
          <ErrorMessage>
            Disk size must be between {appMinDiskSize[appType]} GB and{' '}
            {appMaxDiskSize} GB
          </ErrorMessage>
        )}
      </FlexRow>
      <FlexRow
        style={{
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: '2rem',
        }}
      >
        {showDeleteDiskButton && (
          <DeletePersistentDiskButton
            onClick={onClickDeleteUnattachedPersistentDisk}
            style={{ flexShrink: 0 }}
          />
        )}
        {showDeleteAppButton && (
          <LinkButton
            style={{ ...styles.deleteLink, flexShrink: 0 }}
            aria-label='Delete Environment'
            onClick={onClickDeleteGkeApp}
          >
            Delete Environment
          </LinkButton>
        )}
        <CreateAppText />
        {isInteractiveUserApp(app?.appType) &&
        app?.status === AppStatus.RUNNING ? (
          <OpenGkeAppButton
            {...{ billingStatus, workspace, onClose }}
            userApp={app}
            style={openOrCreateButtonStyle}
          />
        ) : (
          <CreateGkeAppButton
            {...{ billingStatus, createAppRequest }}
            existingApp={app}
            workspaceNamespace={workspace.namespace}
            username={profile.username}
            style={openOrCreateButtonStyle}
            onDismiss={() => {
              onClose();
              setTimeout(() => sidebarActiveIconStore.next('apps'), 3000);
            }}
          />
        )}
      </FlexRow>
    </FlexColumn>
  );
};
