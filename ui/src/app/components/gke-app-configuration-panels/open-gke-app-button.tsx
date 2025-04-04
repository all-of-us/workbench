import * as React from 'react';
import { CSSProperties } from 'react';

import { BillingStatus, UserAppEnvironment, Workspace } from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { TooltipTrigger } from 'app/components/popups';
import { useNavigation } from 'app/utils/navigation';
import { appTypeToString, openAppInIframe } from 'app/utils/user-apps-utils';

export interface OpenGkeAppButtonProps {
  userApp: UserAppEnvironment;
  billingStatus: BillingStatus;
  workspace: Workspace;
  onClose: () => void;
  style?: CSSProperties;
}
export function OpenGkeAppButton({
  userApp,
  billingStatus,
  workspace: { namespace, terraName },
  onClose,
  style,
}: OpenGkeAppButtonProps) {
  const [navigate] = useNavigation();

  const openEnabled = billingStatus === BillingStatus.ACTIVE;
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
