import * as React from 'react';

import {
  AppStatus,
  AppType,
  CreateAppRequest,
  Disk,
  PersistentDiskRequest,
  UserAppEnvironment,
} from 'generated/fetch';

import { switchCase } from '@terra-ui-packages/core-utils';
import {
  canDeleteApp,
  defaultCromwellConfig,
  defaultRStudioConfig,
  defaultSASConfig, isAppActive,
  toUIAppType,
} from 'app/components/apps-panel/utils';
import { LinkButton } from 'app/components/buttons';
import { DeletePersistentDiskButton } from 'app/components/common-env-conf-panels/delete-persistent-disk-button';
import { EnvironmentInformedActionPanel } from 'app/components/common-env-conf-panels/environment-informed-action-panel';
import { styles } from 'app/components/common-env-conf-panels/styles';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { SidebarIconId } from 'app/components/help-sidebar-icons';
import { AnalysisConfig } from 'app/utils/analysis-config';
import {
  AutodeleteMinuteThresholds,
  ComputeType,
  DEFAULT_AUTODELETE_THRESHOLD_MINUTES,
  findMachineByName,
  Machine
} from 'app/utils/machines';
import { sidebarActiveIconStore } from 'app/utils/navigation';
import { ProfileStore } from 'app/utils/stores';
import {
  appTypeToString,
  isInteractiveUserApp,
  unattachedDiskExists,
} from 'app/utils/user-apps-utils';
import { WorkspaceData } from 'app/utils/workspace-data';

import { CreateGkeAppButton } from './create-gke-app-button';
import { DisabledCloudComputeProfile } from './disabled-cloud-compute-profile';
import { OpenGkeAppButton } from './open-gke-app-button';
import { Dropdown } from 'primereact/dropdown';
import {CheckBox} from "app/components/inputs";
import {getWholeDaysFromNow} from "app/utils/dates";
const defaultIntroText =
  'Your analysis environment consists of an application and compute resources. ' +
  'Your cloud environment is unique to this workspace and not shared with other users.';

export interface CreateGkeAppProps {
  appType: AppType;
  onClose: () => void;
  creatorFreeCreditsRemaining: number | null;
  workspace: WorkspaceData;
  profileState: ProfileStore;
  app: UserAppEnvironment | undefined;
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
  appType,
  onClose,
  creatorFreeCreditsRemaining,
  workspace,
  profileState,
  app,
  disk,
  onClickDeleteGkeApp,
  onClickDeleteUnattachedPersistentDisk,
  introText = defaultIntroText,
  CostNote = () => null,
  SupportNote = () => null,
  CreateAppText = () => null,
}: CreateGkeAppProps) => {
  const { profile } = profileState;
  const { billingStatus } = workspace;

  const onDismiss = () => {
    onClose();
    setTimeout(() => sidebarActiveIconStore.next('apps'), 3000);
  };

  const defaultConfig = switchCase(
    appType,
    [AppType.CROMWELL, () => defaultCromwellConfig],
    [AppType.RSTUDIO, () => defaultRStudioConfig],
    [AppType.SAS, () => defaultSASConfig]
  );

  const persistentDiskRequest: PersistentDiskRequest =
    disk ?? defaultConfig.persistentDiskRequest;
  const { kubernetesRuntimeConfig } = defaultConfig;
  const machine: Machine = findMachineByName(
    kubernetesRuntimeConfig.machineType
  );
  const analysisConfig: AnalysisConfig = {
    machine,
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

  const [createAppRequest, setCreateAppRequest] = React.useState<CreateAppRequest>({
    ...defaultConfig,
    persistentDiskRequest,
  });

  const [autodeleteDropdownChecked, setAutodeleteDropdownChecked] = React.useState(false);

  const autodeleteRemainingDays: number = (() => {
    if (app?.autodeleteEnabled && app.dateAccessed && app.autodeleteThreshold) {
      const dateAccessed = new Date(app.dateAccessed);
      const autodeleteDate = new Date(dateAccessed.getTime() + app.autodeleteThreshold * 60 * 1000);
      return getWholeDaysFromNow(autodeleteDate.getTime());
    }
    return null;
  })();

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
            analysisConfig,
          }}
          appType={toUIAppType[appType]}
          status={app?.status}
        />
        <CostNote />
      </div>
      <div style={{ ...styles.controlSection }}>
        <DisabledCloudComputeProfile
          {...{ appType, machine, persistentDiskRequest }}
        />
      </div>
      <SupportNote />
      <FlexRow
        style={{
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: '2rem',
        }}
      >
        <FlexRow>
          <CheckBox
            disabled={isAppActive(app)}
            onChange={(autodeleteEnabled) => {
              setCreateAppRequest(prevState => ({
                ...prevState,
                autodeleteEnabled
              }));
              setAutodeleteDropdownChecked(autodeleteEnabled)
            }}
            style={{ marginRight: '0.5rem', zoom: 1.5  }}
          />
          <FlexColumn>
          <label style={styles.label} htmlFor='gke-autodelete-label'>
            Automatically delete after idle for
          </label>
            <p style={{ marginTop: '0' }}>
           Your persistent disk will not be deleted.
            </p>
          </FlexColumn>
          <FlexColumn>
          <Dropdown
            id='gke-autodelete'
            appendTo='self'
            disabled={isAppActive(app) || !autodeleteDropdownChecked}
            options={Array.from(AutodeleteMinuteThresholds.entries()).map(
              (entry) => ({
                label: entry[1],
                value: entry[0],
              })
            )}
            value={
              createAppRequest.autodeleteThreshold ||
              DEFAULT_AUTODELETE_THRESHOLD_MINUTES
            }
            onChange={(e) => {
              setCreateAppRequest(prevState => ({
                ...prevState,
                autodeleteThreshold: e.value
              }));
            }}
            style={{ marginLeft: '1rem' }}
          />
            {autodeleteRemainingDays && (
              <p style={{ marginTop: '0', marginLeft: '1rem' }}>{`The app will be deleted in ${autodeleteRemainingDays} days.`}</p>
            )}
          </FlexColumn>
        </FlexRow>
      </FlexRow>
      <FlexRow
        style={{
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: '2rem',
        }}
      >
        {unattachedDiskExists(app, disk) && (
          <DeletePersistentDiskButton
            onClick={onClickDeleteUnattachedPersistentDisk}
            style={{ flexShrink: 0 }}
          />
        )}
        {canDeleteApp(app) && (
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
          />
        ) : (
          <CreateGkeAppButton
            {...{ billingStatus, createAppRequest, onDismiss }}
            existingApp={app}
            workspaceNamespace={workspace.namespace}
            username={profile.username}
          />
        )}
      </FlexRow>
    </FlexColumn>
  );
};
