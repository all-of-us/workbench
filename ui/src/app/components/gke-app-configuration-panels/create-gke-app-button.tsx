import * as React from 'react';
import { useState } from 'react';

import {
  AppType,
  BillingStatus,
  CreateAppRequest,
  UserAppEnvironment,
} from 'generated/fetch';

import { cond } from '@terra-ui-packages/core-utils';
import {
  isAppActive,
  toAppType,
  UIAppType,
} from 'app/components/apps-panel/utils';
import { Button } from 'app/components/buttons';
import { TooltipTrigger } from 'app/components/popups';
import { SUPPORT_EMAIL } from 'app/components/support';
import { ApiErrorResponse, fetchWithErrorModal } from 'app/utils/errors';
import { NotificationStore } from 'app/utils/stores';
import { appTypeToString, createUserApp } from 'app/utils/user-apps-utils';

export interface CreateGKEAppButtonProps {
  createAppRequest: CreateAppRequest;
  existingApp: UserAppEnvironment | null | undefined;
  workspaceNamespace: string;
  onDismiss: () => void;
  username: string;
  billingStatus: BillingStatus;
}

export function CreateGkeAppButton({
  createAppRequest,
  existingApp,
  workspaceNamespace,
  onDismiss,
  username,
  billingStatus,
}: CreateGKEAppButtonProps) {
  const [creatingApp, setCreatingApp] = useState(false);
  const createEnabled =
    !creatingApp &&
    !isAppActive(existingApp) &&
    billingStatus === BillingStatus.ACTIVE;

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
    [!createEnabled, () => `A ${appTypeString} app exists or is being created`],
    [
      billingStatus !== BillingStatus.ACTIVE,
      () =>
        'You have either run out of initial credits or have an inactive billing account.',
    ],
    () => ''
  );

  const onCreate = () => {
    setCreatingApp(true);
    fetchWithErrorModal(
      () => createUserApp(workspaceNamespace, createAppRequest),
      {
        customErrorResponseFormatter: (error: ApiErrorResponse) =>
          (error?.originalResponse?.status === 409 && conflictError) ||
          (error?.originalResponse?.status === 401 &&
            createAppRequest.appType === AppType.SAS &&
            sasAuthError),
      }
    ).then(() => onDismiss());
  };
  console.log('YYYYYYYYYYYYYY: ', tooltip);

  return (
    <TooltipTrigger disabled={createEnabled} content={tooltip}>
      {/* tooltip trigger needs a div for some reason */}
      <div>
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
