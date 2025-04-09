import * as React from 'react';
import { CSSProperties } from 'react';

import { UserAppEnvironment, Workspace } from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { TooltipTrigger } from 'app/components/popups';
import { useNavigation } from 'app/utils/navigation';
import { appTypeToString, openAppInIframe } from 'app/utils/user-apps-utils';
import { isValidBilling } from 'app/utils/workspace-utils';

export interface OpenGkeAppButtonProps {
  userApp: UserAppEnvironment;
  workspace: Workspace;
  onClose: () => void;
  style?: CSSProperties;
}
export function OpenGkeAppButton({
  userApp,
  workspace,
  onClose,
  style,
}: OpenGkeAppButtonProps) {
  const { namespace, terraName } = workspace;
  const [navigate] = useNavigation();

  const openEnabled = isValidBilling(workspace);
  const appTypeString = appTypeToString[userApp.appType];

  const tooltip =
    !openEnabled &&
    'You have either run out of initial credits or have an inactive billing account.';

  return (
    <TooltipTrigger disabled={openEnabled} content={tooltip}>
      {/* tooltip trigger needs a div for some reason */}
      <div {...{ style }}>
        <Button
          id={`${appTypeString}-cloud-environment-open-button`}
          aria-label={`${appTypeString} cloud environment open button`}
          onClick={() => {
            openAppInIframe(namespace, terraName, userApp, navigate);
            onClose();
          }}
          disabled={!openEnabled}
        >
          Open {appTypeString}
        </Button>
      </div>
    </TooltipTrigger>
  );
}
