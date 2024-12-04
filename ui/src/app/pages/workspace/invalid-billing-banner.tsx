import * as React from 'react';
import * as fp from 'lodash';

import { Profile } from 'generated/fetch';

import { Button, LinkButton } from 'app/components/buttons';
import { ExtendInitialCreditsModal } from 'app/components/extend-initial-credits-modal';
import { AoU } from 'app/components/text-wrappers';
import { ToastBanner, ToastType } from 'app/components/toast-banner';
import { withCurrentWorkspace, withUserProfile } from 'app/utils';
import { minusDays } from 'app/utils/dates';
import { NavigationProps } from 'app/utils/navigation';
import { serverConfigStore } from 'app/utils/stores';
import { withNavigation } from 'app/utils/with-navigation-hoc';
import { WorkspaceData } from 'app/utils/workspace-data';
import { supportUrls } from 'app/utils/zendesk';

const InitialCreditsArticleLink = () => (
  <LinkButton
    style={{ display: 'inline' }}
    onClick={() => window.open(supportUrls.initialCredits, '_blank')}
  >
    Using <AoU /> Initial Credits
  </LinkButton>
);

const BillingAccountArticleLink = () => (
  <LinkButton
    onClick={() => window.open(supportUrls.createBillingAccount, '_blank')}
    style={{ display: 'inline' }}
  >
    Paying for Your Research
  </LinkButton>
);

interface BillingUpdateOptionsProps {
  onRequestExtension: Function;
  navigate: Function;
  onClose: Function;
  workspace: WorkspaceData;
}

const onEditWorkspace = (
  navigate: Function,
  onClose: Function,
  workspace: WorkspaceData
) => {
  onClose();
  navigate(['workspaces', workspace.namespace, workspace.terraName, 'edit'], {
    queryParams: {
      highlightBilling: true,
    },
  });
};

const BillingUpdateOptions = ({
  onRequestExtension,
  navigate,
  onClose,
  workspace,
}: BillingUpdateOptionsProps) => (
  <>
    You can setup your billing account by visiting the{' '}
    <LinkButton
      onClick={() => onEditWorkspace(navigate, onClose, workspace)}
      style={{ display: 'inline' }}
    >
      Edit Workspace
    </LinkButton>{' '}
    page. If necessary, you can request an extension to your initial credit
    expiration date{' '}
    <LinkButton onClick={onRequestExtension} style={{ display: 'inline' }}>
      here
    </LinkButton>
    .
  </>
);

const whoseCredits = (isCreator: boolean) => {
  return isCreator ? 'Your' : 'This workspace creator’s';
};

const workspaceCreatorInformation = (
  isCreator: boolean,
  creatorUsername: string
) => {
  return isCreator ? '' : `This workspace was created by ${creatorUsername}. `;
};

const whatHappened = (
  isExhausted: boolean,
  isExpired: boolean,
  isExpiringSoon: boolean,
  isEligibleForExtension: boolean,
  isCreator: boolean
) => {
  const whose = whoseCredits(isCreator);
  let whatIsHappening: string;
  if (isExhausted || (isExpired && !isEligibleForExtension)) {
    whatIsHappening = 'have run out.';
  } else if (isExpired && isEligibleForExtension) {
    whatIsHappening = 'have expired.';
  } else if (isExpiringSoon && isEligibleForExtension) {
    whatIsHappening = 'are expiring soon.';
  }
  // The case when isExpiringSoon && !isEligibleForExtension
  // never happens because the banner is not shown in that case.
  return (
    <>
      {whose} initial credits {whatIsHappening}
    </>
  );
};

interface WhatToDoProps {
  isExhausted: boolean;
  isExpired: boolean;
  isExpiringSoon: boolean;
  isEligibleForExtension: boolean;
  onRequestExtension: Function;
  navigate: Function;
  onClose: Function;
  workspace: WorkspaceData;
}
const whatToDo = ({
  isExhausted,
  isExpired,
  isExpiringSoon,
  isEligibleForExtension,
  onRequestExtension,
  navigate,
  onClose,
  workspace,
}: WhatToDoProps) => {
  if (isExhausted || (isExpired && !isEligibleForExtension)) {
    return (
      <>
        To use the workspace, a valid billing account needs to be provided. To
        learn more about establishing a billing account, read the{' '}
        <BillingAccountArticleLink /> article on the User Support Hub.
      </>
    );
  } else {
    return (
      <>
        {isEligibleForExtension && (isExpired || isExpiringSoon) && (
          <BillingUpdateOptions
            {...{ onRequestExtension, navigate, onClose, workspace }}
          />
        )}{' '}
        For more information, read the <InitialCreditsArticleLink /> article on
        the User Support Hub.
      </>
    );
  }
};

const titleText = (
  isExhausted: boolean,
  isExpired: boolean,
  isExpiringSoon: boolean,
  isEligibleForExtension: boolean
) => {
  if (isExhausted || (isExpired && !isEligibleForExtension)) {
    return 'This workspace is out of initial credits';
  } else if (isExpired && isEligibleForExtension) {
    return 'Workspace credits have expired';
  } else if (isExpiringSoon && isEligibleForExtension) {
    return 'Workspace credits are expiring soon';
  }
};

interface Props extends NavigationProps {
  workspace: WorkspaceData;
  profileState: {
    profile: Profile;
  };
  onClose: Function;
}

export const InvalidBillingBanner = fp.flow(
  withCurrentWorkspace(),
  withUserProfile(),
  withNavigation
)(({ onClose, navigate, workspace, profileState }: Props) => {
  const [profile, setProfile] = React.useState<Profile | undefined>(
    profileState?.profile
  );
  const [showExtensionModal, setShowExtensionModal] = React.useState(false);
  const isCreator = profile?.username === workspace?.creator;
  const isEligibleForExtension = profile?.eligibleForInitialCreditsExtension;
  const isExpired =
    workspace?.initialCredits.expirationEpochMillis < Date.now();
  const isExpiringSoon =
    workspace &&
    !isExpired &&
    minusDays(
      workspace.initialCredits.expirationEpochMillis,
      serverConfigStore.get().config.initialCreditsExpirationWarningDays
    ) < Date.now();
  const isExhausted = workspace?.initialCredits.exhausted;
  const title = titleText(
    isExhausted,
    isExpired,
    isExpiringSoon,
    isEligibleForExtension
  );

  const workspaceCreatorInformationIfApplicable = workspaceCreatorInformation(
    isCreator,
    workspace?.creator
  );
  const whatHappenedMessage = whatHappened(
    isExhausted,
    isExpired,
    isExpiringSoon,
    isEligibleForExtension,
    isCreator
  );
  const whatToDoMessage = whatToDo({
    ...{
      isExhausted,
      isExpired,
      isExpiringSoon,
      isEligibleForExtension,
      navigate,
      onClose,
      workspace,
    },
    onRequestExtension: () => setShowExtensionModal(true),
  });

  const message = (
    <>
      {whatHappenedMessage} {workspaceCreatorInformationIfApplicable}
      {whatToDoMessage}
    </>
  );
  const footer = isCreator && (
    <Button
      style={{ height: '38px', width: '70%', fontWeight: 400 }}
      onClick={() => onEditWorkspace(navigate, onClose, workspace)}
    >
      Edit Workspace
    </Button>
  );

  return (
    <>
      {((isExpiringSoon && isEligibleForExtension) ||
        isExpired ||
        isExhausted) && (
        <ToastBanner
          {...{ message, title, footer, onClose }}
          toastType={ToastType.WARNING}
          zIndex={500}
        />
      )}
      {showExtensionModal && (
        <ExtendInitialCreditsModal
          onClose={(updatedProfile: Profile) => {
            setShowExtensionModal(false);
            setProfile(updatedProfile);
          }}
        />
      )}
    </>
  );
});
