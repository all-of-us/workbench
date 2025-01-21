import * as React from 'react';
import { CSSProperties, useState } from 'react';

import {
  AppType,
  CreateAppRequest,
  Disk,
  UserAppEnvironment,
  Workspace,
} from 'generated/fetch';

import { cond } from '@terra-ui-packages/core-utils';
import {
  appMaxDiskSize,
  appMinDiskSize,
  isAppActive,
} from 'app/components/apps-panel/utils';
import { Button } from 'app/components/buttons';
import { TooltipTrigger } from 'app/components/popups';
import { SUPPORT_EMAIL } from 'app/components/support';
import { disksApi } from 'app/services/swagger-fetch-clients';
import { ApiErrorResponse, fetchWithErrorModal } from 'app/utils/errors';
import { NotificationStore } from 'app/utils/stores';
import {
  appTypeToString,
  createUserApp,
  isDiskSizeValid,
  unattachedDiskExists,
} from 'app/utils/user-apps-utils';
import { isValidBilling } from 'app/utils/workspace-utils';

export interface CreateGKEAppButtonProps {
  createAppRequest: CreateAppRequest;
  existingApp: UserAppEnvironment | null | undefined;
  workspace: Workspace;
  onDismiss: () => void;
  username: string;
  existingDisk?: Disk;
  style?: CSSProperties;
}

export function CreateGkeAppButton({
  createAppRequest,
  existingApp,
  workspace,
  onDismiss,
  username,
  existingDisk,
  style,
}: CreateGKEAppButtonProps) {
  const [creatingApp, setCreatingApp] = useState(false);

  const wouldDecreaseDiskSize =
    !!existingDisk &&
    createAppRequest.persistentDiskRequest.size < existingDisk.size;

  const active = isAppActive(existingApp);
  const validBilling = isValidBilling(workspace);
  const diskSizeValid = isDiskSizeValid(createAppRequest);

  const createEnabled =
    !creatingApp &&
    !active &&
    validBilling &&
    diskSizeValid &&
    !wouldDecreaseDiskSize;

  const appTypeString = appTypeToString[createAppRequest.appType];

  const conflictError: NotificationStore = {
    title: `Error Creating ${appTypeString} Environment`,
    message: `Please wait a few minutes and try to create your ${appTypeString} Environment again.`,
    onDismiss,
  };

  const sasAuthError: NotificationStore = {
    title: 'Error Creating SAS Environment',
    message:
      `The user ${username} is not authorized to create SAS apps. ` +
      `If you believe this to be in error, please contact the support team at ${SUPPORT_EMAIL}.`,
    onDismiss,
  };

  const tooltip = cond(
    [
      !isDiskSizeValid(createAppRequest),
      () =>
        `Disk cannot be more than ${appMaxDiskSize} GB or less than ${
          appMinDiskSize[createAppRequest.appType]
        } GB`,
    ],
    [
      wouldDecreaseDiskSize,
      () =>
        `The specified disk size (${createAppRequest.persistentDiskRequest.size} GB) is smaller than ` +
        `the existing unattached persistent disk size of ${existingDisk.size} GB. ` +
        'Preventing creation because this would cause data loss.',
    ],
    [
      !isValidBilling(workspace),
      () =>
        'You have either run out of initial credits or have an inactive billing account.',
    ],
    [!createEnabled, () => `A ${appTypeString} app exists or is being created`],
    () => ''
  );

  const onCreate = () => {
    setCreatingApp(true);
    fetchWithErrorModal(
      async () => {
        // if we need to update the disk before creation, wait for that to finish.
        // TODO: can we remove this after RW-11634 ?
        if (
          unattachedDiskExists(existingApp, existingDisk) &&
          createAppRequest.persistentDiskRequest.size > existingDisk.size
        ) {
          await disksApi().updateDisk(
            workspace.namespace,
            existingDisk.name,
            createAppRequest.persistentDiskRequest.size
          );
        }
        return createUserApp(workspace.namespace, createAppRequest);
      },
      {
        customErrorResponseFormatter: (error: ApiErrorResponse) =>
          (error?.originalResponse?.status === 409 && conflictError) ||
          (error?.originalResponse?.status === 401 &&
            createAppRequest.appType === AppType.SAS &&
            sasAuthError),
      }
    ).then(() => onDismiss());
  };

  return (
    <TooltipTrigger disabled={createEnabled} content={tooltip}>
      {/* tooltip trigger needs a div for some reason */}
      <div {...{ style }}>
        <Button
          id={`${appTypeString}-cloud-environment-create-button`}
          aria-label={`${appTypeString} cloud environment create button`}
          onClick={onCreate}
          disabled={!createEnabled}
        >
          Start
        </Button>
      </div>
    </TooltipTrigger>
  );
}
