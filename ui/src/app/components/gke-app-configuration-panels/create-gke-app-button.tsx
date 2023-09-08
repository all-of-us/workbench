import * as React from 'react';
import { useState } from 'react';

import { CreateAppRequest, UserAppEnvironment } from 'generated/fetch';

import { canCreateApp } from 'app/components/apps-panel/utils';
import { Button } from 'app/components/buttons';
import { TooltipTrigger } from 'app/components/popups';
import { ApiErrorResponse, fetchWithErrorModal } from 'app/utils/errors';
import { appTypeToString, createUserApp } from 'app/utils/user-apps-utils';

export interface CreateGKEAppButtonProps {
  createAppRequest: CreateAppRequest;
  existingApp: UserAppEnvironment | null | undefined;
  workspaceNamespace: string;
  onDismiss: () => void;
}

export function CreateGkeAppButton({
  createAppRequest,
  existingApp,
  workspaceNamespace,
  onDismiss,
}: CreateGKEAppButtonProps) {
  const [creatingApp, setCreatingApp] = useState(false);
  const createEnabled = !creatingApp && canCreateApp(existingApp);

  const appTypeString = appTypeToString[createAppRequest.appType];

  const onCreate = () => {
    setCreatingApp(true);
    fetchWithErrorModal(
      () => createUserApp(workspaceNamespace, createAppRequest),
      {
        customErrorResponseFormatter: (error: ApiErrorResponse) =>
          error?.originalResponse?.status === 409 && {
            title: `Error Creating ${appTypeString} Environment`,
            message: `Please wait a few minutes and try to create your ${appTypeString} Environment again.`,
            onDismiss,
          },
      }
    ).then(() => onDismiss());
  };

  return (
    <TooltipTrigger
      disabled={createEnabled}
      content={`A ${appTypeString} app exists or is being created`}
    >
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
