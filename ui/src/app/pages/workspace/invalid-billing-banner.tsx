import * as React from 'react';
import * as fp from 'lodash';

import { Profile } from 'generated/fetch';

import { Button, LinkButton } from 'app/components/buttons';
import { ExtendInitialCreditsModal } from 'app/components/extend-initial-credits-modal';
import { AoU } from 'app/components/text-wrappers';
import { ToastBanner, ToastType } from 'app/components/toast-banner';
import { withCurrentWorkspace, withUserProfile } from 'app/utils';
import { plusDays } from 'app/utils/dates';
import { NavigationProps } from 'app/utils/navigation';
import { serverConfigStore } from 'app/utils/stores';
import { withNavigation } from 'app/utils/with-navigation-hoc';
import { WorkspaceData } from 'app/utils/workspace-data';
import { supportUrls } from 'app/utils/zendesk';

interface Props extends NavigationProps {
  workspace: WorkspaceData;
  profileState: {
    profile: Profile;
  };
  onClose: Function;
}

const InitialCreditsArticleLink = () => (
  <LinkButton
    onClick={() => window.open(supportUrls.createBillingAccount, '_blank')}
  >
    Using <AoU /> Initial Credits
  </LinkButton>
);

const BillingAccountArticleLink = () => (
  <LinkButton
    onClick={() => window.open(supportUrls.createBillingAccount, '_blank')}
  >
    Paying for Your Research
  </LinkButton>
);

interface ExtensionRequestLinkProps {
  onClick: Function;
}

const ExtensionRequestLink = ({ onClick }: ExtensionRequestLinkProps) => (
  <>
    You can request an extension{' '}
    <LinkButton {...{ onClick }} style={{ display: 'inline' }}>
      here
    </LinkButton>
    .
  </>
);

const whoseCredits = (isCreator: boolean) => {
  return isCreator ? 'Your' : 'This workspace creator’s';
};

const workspaceCreatorInformation = (isCreator: boolean) => {
  return isCreator ? '' : 'This workspace was created by First Name Last Name.';
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
  return (
    <>
      {whose} initial credits {whatIsHappening}
    </>
  );
};

const whatToDo = (
  isExhausted: boolean,
  isExpired: boolean,
  isExpiringSoon: boolean,
  isEligibleForExtension: boolean,
  onClick: Function
) => {
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
          <ExtensionRequestLink {...{ onClick }} />
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
    plusDays(
      workspace.initialCredits.expirationEpochMillis,
      -serverConfigStore.get().config.initialCreditsExpirationWarningDays
    ) < Date.now();
  const isExhausted = workspace?.initialCredits.exhausted;
  const title = titleText(
    isExhausted,
    isExpired,
    isExpiringSoon,
    isEligibleForExtension
  );

  const workspaceCreatorInformationIfApplicable =
    workspaceCreatorInformation(isCreator);
  const whatHappenedMessage = whatHappened(
    isExhausted,
    isExpired,
    isExpiringSoon,
    isEligibleForExtension,
    isCreator
  );
  const whatToDoMessage = whatToDo(
    isExhausted,
    isExpired,
    isExpiringSoon,
    isEligibleForExtension,
    () => setShowExtensionModal(true)
  );

  const message = (
    <>
      {whatHappenedMessage} {workspaceCreatorInformationIfApplicable}
      {whatToDoMessage}
    </>
  );
  const footer = isCreator && (
    <Button
      style={{ height: '38px', width: '70%', fontWeight: 400 }}
      onClick={() => {
        onClose();
        navigate(
          ['workspaces', workspace.namespace, workspace.terraName, 'edit'],
          {
            queryParams: {
              highlightBilling: true,
            },
          }
        );
      }}
    >
      Edit Workspace
    </Button>
  );

  return (
    <>
      <ToastBanner
        {...{ message, title, footer, onClose }}
        toastType={ToastType.WARNING}
        zIndex={500}
      />
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
