import * as React from 'react';

import { Disk, Profile, Runtime, RuntimeStatus } from 'generated/fetch';

import { cond, switchCase } from '@terra-ui-packages/core-utils';
import { disksApi } from 'app/services/swagger-fetch-clients';
import { Machine } from 'app/utils/machines';
import {
  AnalysisConfig,
  PanelContent,
  RuntimeStatusRequest,
  UpdateMessaging,
} from 'app/utils/runtime-utils';
import { WorkspaceData } from 'app/utils/workspace-data';

import { UIAppType } from './apps-panel/utils';
import { ConfirmDelete } from './common-env-conf-panels/confirm-delete';
import { ConfirmDeleteEnvironmentWithPD } from './common-env-conf-panels/confirm-delete-environment-with-pd';
import { ConfirmDeleteUnattachedPD } from './common-env-conf-panels/confirm-delete-unattached-pd';
import { DisabledPanel } from './common-env-conf-panels/disabled-panel';
import { ConfirmUpdatePanel } from './runtime-configuration-panel/confirm-update-panel';
import { CreatePanel } from './runtime-configuration-panel/create-panel';
import { CustomizePanel } from './runtime-configuration-panel/customize-panel';
import { OfferDeleteDiskWithUpdate } from './runtime-configuration-panel/offer-delete-disk-with-update';
import { SparkConsolePanel } from './runtime-configuration-panel/spark-console-panel';

interface Props {
  allowDataproc: boolean;
  analysisConfig: AnalysisConfig;
  attachedPdExists: boolean;
  creatorFreeCreditsRemaining: number;
  currentRuntime: Runtime;
  dataprocExists: boolean;
  disableControls: boolean;
  disableDetachableReason: string;
  environmentChanged: boolean;
  existingAnalysisConfig: AnalysisConfig;
  gcePersistentDisk: Disk;
  getErrorMessageContent: () => JSX.Element[];
  getWarningMessageContent: () => JSX.Element[];
  onClose?: () => void;
  panelContent: PanelContent;
  profile: Profile;
  renderCreateButton: () => JSX.Element;
  renderNextUpdateButton: () => JSX.Element;
  renderNextWithDiskDeleteButton: () => JSX.Element;
  renderTryAgainButton: () => JSX.Element;
  renderUpdateButton: () => JSX.Element;
  requestAnalysisConfig: (config: AnalysisConfig) => void;
  runtimeExists: boolean;
  setAnalysisConfig: (config: AnalysisConfig) => void;
  setPanelContent: (pc: PanelContent) => void;
  setRuntimeStatusRequest: (rs: RuntimeStatusRequest) => Promise<void>;
  status: RuntimeStatus;
  unattachedDiskNeedsRecreate: boolean;
  unattachedPdExists: boolean;
  updateMessaging: UpdateMessaging;
  usingDataproc: boolean;
  validMainMachineTypes: Machine[];
  workspaceData: WorkspaceData;
}
export const RuntimeConfigurationPanelRenderer = ({
  allowDataproc,
  analysisConfig,
  attachedPdExists,
  creatorFreeCreditsRemaining,
  currentRuntime,
  dataprocExists,
  disableControls,
  disableDetachableReason,
  environmentChanged,
  existingAnalysisConfig,
  gcePersistentDisk,
  getErrorMessageContent,
  getWarningMessageContent,
  onClose,
  panelContent,
  profile,
  renderCreateButton,
  renderNextUpdateButton,
  renderNextWithDiskDeleteButton,
  renderTryAgainButton,
  renderUpdateButton,
  requestAnalysisConfig,
  runtimeExists,
  setAnalysisConfig,
  setPanelContent,
  setRuntimeStatusRequest,
  status,
  unattachedDiskNeedsRecreate,
  unattachedPdExists,
  updateMessaging,
  usingDataproc,
  validMainMachineTypes,
  workspaceData,
}: Props) => {
  return (
    <div id='runtime-panel'>
      {cond<React.ReactNode>(
        [
          [PanelContent.Create, PanelContent.Customize].includes(panelContent),
          () => (
            <div style={{ marginBottom: '1.5rem' }}>
              Your analysis environment consists of an application and compute
              resources. Your cloud environment is unique to this workspace and
              not shared with other users.
            </div>
          ),
        ],
        () => null
      )}
      {switchCase(
        panelContent,
        [
          PanelContent.Create,
          () => (
            <CreatePanel
              {...{
                profile,
                setPanelContent,
                analysisConfig,
                creatorFreeCreditsRemaining,
                status,
                setRuntimeStatusRequest,
                renderCreateButton,
              }}
              workspace={workspaceData}
            />
          ),
        ],
        [
          PanelContent.DeleteRuntime,
          () => {
            if (attachedPdExists) {
              return (
                <ConfirmDeleteEnvironmentWithPD
                  onConfirm={async (deletePDSelected) => {
                    const runtimeStatusReq = cond(
                      [
                        !deletePDSelected,
                        () => RuntimeStatusRequest.DeleteRuntime,
                      ],
                      [
                        deletePDSelected && !usingDataproc,
                        () => RuntimeStatusRequest.DeleteRuntimeAndPD,
                      ],
                      [
                        // TODO: this configuration is not supported.  Remove?
                        deletePDSelected && usingDataproc,
                        () => RuntimeStatusRequest.DeletePD,
                      ]
                    );
                    await setRuntimeStatusRequest(runtimeStatusReq);
                    onClose();
                  }}
                  onCancel={() => setPanelContent(PanelContent.Customize)}
                  appType={UIAppType.JUPYTER}
                  usingDataproc={usingDataproc}
                  disk={gcePersistentDisk}
                />
              );
            } else {
              return (
                <ConfirmDelete
                  onConfirm={async () => {
                    await setRuntimeStatusRequest(
                      RuntimeStatusRequest.DeleteRuntime
                    );
                    onClose();
                  }}
                  onCancel={() => setPanelContent(PanelContent.Customize)}
                />
              );
            }
          },
        ],
        [
          PanelContent.DeleteUnattachedPd,
          () => (
            <ConfirmDeleteUnattachedPD
              appType={UIAppType.JUPYTER}
              onConfirm={async () => {
                await disksApi().deleteDisk(
                  workspaceData.namespace,
                  gcePersistentDisk.name
                );
                onClose();
              }}
              onCancel={() => setPanelContent(PanelContent.Customize)}
            />
          ),
        ],
        [
          PanelContent.DeleteUnattachedPdAndCreate,
          () => (
            <ConfirmDeleteUnattachedPD
              appType={UIAppType.JUPYTER}
              showCreateMessaging
              onConfirm={async () => {
                await disksApi().deleteDisk(
                  workspaceData.namespace,
                  gcePersistentDisk.name
                );
                requestAnalysisConfig(analysisConfig);
                onClose();
              }}
              onCancel={() => setPanelContent(PanelContent.Customize)}
            />
          ),
        ],
        [
          PanelContent.Customize,
          () => (
            <CustomizePanel
              {...{
                allowDataproc,
                analysisConfig,
                creatorFreeCreditsRemaining,
                currentRuntime,
                dataprocExists,
                disableControls,
                disableDetachableReason,
                environmentChanged,
                existingAnalysisConfig,
                gcePersistentDisk,
                getErrorMessageContent,
                getWarningMessageContent,
                profile,
                renderCreateButton,
                renderNextUpdateButton,
                renderNextWithDiskDeleteButton,
                renderTryAgainButton,
                runtimeExists,
                setAnalysisConfig,
                setPanelContent,
                setRuntimeStatusRequest,
                status,
                unattachedDiskNeedsRecreate,
                unattachedPdExists,
                updateMessaging,
                validMainMachineTypes,
                workspaceData,
              }}
            />
          ),
        ],
        [
          PanelContent.ConfirmUpdate,
          () => (
            <ConfirmUpdatePanel
              existingAnalysisConfig={existingAnalysisConfig}
              newAnalysisConfig={analysisConfig}
              onCancel={() => {
                setPanelContent(PanelContent.Customize);
              }}
              updateButton={renderUpdateButton()}
            />
          ),
        ],
        [
          PanelContent.ConfirmUpdateWithDiskDelete,
          () => (
            <OfferDeleteDiskWithUpdate
              onNext={(deleteDetachedDisk: boolean) => {
                if (deleteDetachedDisk) {
                  setAnalysisConfig({
                    ...analysisConfig,
                    detachedDisk: null,
                  });
                }
                setPanelContent(PanelContent.ConfirmUpdate);
              }}
              onCancel={() => setPanelContent(PanelContent.Customize)}
              disk={gcePersistentDisk}
            />
          ),
        ],
        [PanelContent.Disabled, () => <DisabledPanel />],
        [
          PanelContent.SparkConsole,
          () => <SparkConsolePanel {...workspaceData} />,
        ]
      )}
    </div>
  );
};
