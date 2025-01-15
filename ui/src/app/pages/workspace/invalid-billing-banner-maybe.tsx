import * as React from 'react';
import * as fp from 'lodash';

import { Profile, User } from 'generated/fetch';

import { Button, LinkButton } from 'app/components/buttons';
import { ExtendInitialCreditsModal } from 'app/components/extend-initial-credits-modal';
import { AoU } from 'app/components/text-wrappers';
import { ToastBanner, ToastType } from 'app/components/toast-banner';
import { workspacesApi } from 'app/services/swagger-fetch-clients';
import { withCurrentWorkspace, withUserProfile } from 'app/utils';
import { minusDays } from 'app/utils/dates';
import { currentWorkspaceStore, NavigationProps } from 'app/utils/navigation';
import { profileStore, serverConfigStore } from 'app/utils/stores';
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

const EditWorkspaceLink = ({
  navigate,
  onClose,
  workspace,
}: BillingUpdateOptionsProps) => (
  <LinkButton
    onClick={() => onEditWorkspace(navigate, onClose, workspace)}
    style={{ display: 'inline' }}
  >
    on the Edit Workspace page
  </LinkButton>
);

const RequestExtensionLink = ({
  onRequestExtension,
}: BillingUpdateOptionsProps) => (
  <LinkButton onClick={onRequestExtension} style={{ display: 'inline' }}>
    request an extension
  </LinkButton>
);

const whoseCredits = (isCreator: boolean) => {
  return isCreator ? 'Your' : 'This workspace creator’s';
};

const workspaceCreatorInformation = (isCreator: boolean, creatorUser: User) => {
  return isCreator
    ? ''
    : // This is not rendered when creatorUser is undefined.
      `This workspace was created by ${creatorUser?.givenName} ${creatorUser?.familyName}. `;
};

interface WhatHappenedProps {
  isExhausted: boolean;
  isExpired: boolean;
  isExpiringSoon: boolean;
  isEligibleForExtension: boolean;
  isCreator: boolean;
}
const WhatHappened = ({
  isExhausted,
  isExpired,
  isExpiringSoon,
  isEligibleForExtension,
  isCreator,
}: WhatHappenedProps) => {
  const whose = whoseCredits(isCreator);
  let whatIsHappening: string;
  if (isExhausted || (isExpired && !isEligibleForExtension)) {
    whatIsHappening = 'have run out.';
  } else if (isExpired && isEligibleForExtension) {
    whatIsHappening = 'have expired.';
  } else if (isExpiringSoon && isEligibleForExtension) {
    whatIsHappening = `are expiring soon, which may affect ${
      isCreator ? 'your' : 'the'
    } data and analyses${isCreator ? ' in your workspace' : ''}.`;
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
  isCreator: boolean;
  isExhausted: boolean;
  isExpired: boolean;
  isExpiringSoon: boolean;
  isEligibleForExtension: boolean;
  onRequestExtension: Function;
  navigate: Function;
  onClose: Function;
  workspace: WorkspaceData;
}

const WhatToDo = ({
  isCreator,
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
        To use the workspace, a valid billing account needs to be added
        {isCreator && (
          <>
            {' '}
            <EditWorkspaceLink
              {...{ onRequestExtension, navigate, onClose, workspace }}
            />
          </>
        )}
        . To learn more about establishing a billing account, read "
        <BillingAccountArticleLink />" on the User Support Hub.
      </>
    );
  } else {
    return (
      <>
        {isCreator &&
          isEligibleForExtension &&
          (isExpired || isExpiringSoon) && (
            <>
              You can{' '}
              <RequestExtensionLink
                {...{ onRequestExtension, navigate, onClose, workspace }}
              />{' '}
              or set up a valid billing account{' '}
              <EditWorkspaceLink
                {...{ onRequestExtension, navigate, onClose, workspace }}
              />
              {'. '}
            </>
          )}
        For more information, read "
        <InitialCreditsArticleLink />" on the User Support Hub.
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
    reload: Function;
  };
  onClose: Function;
}

export const InvalidBillingBannerMaybe = fp.flow(
  withCurrentWorkspace(),
  withUserProfile(),
  withNavigation
)(({ onClose, navigate, workspace, profileState }: Props) => {
  const profile = profileState.profile;
  const { creatorUser } = workspace || {};
  const [showExtensionModal, setShowExtensionModal] = React.useState(false);
  const isCreator = profile?.username === workspace?.creatorUser?.userName;
  const isEligibleForExtension =
    workspace?.initialCredits?.eligibleForExtension;
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

  const showBanner =
    !showExtensionModal &&
    creatorUser?.givenName &&
    creatorUser?.familyName &&
    ((!workspace.initialCredits.expirationBypassed &&
      ((isExpiringSoon && isEligibleForExtension) || isExpired)) ||
      isExhausted);

  const message = (
    <>
      <WhatHappened
        {...{
          isExhausted,
          isExpired,
          isExpiringSoon,
          isEligibleForExtension,
          isCreator,
        }}
      />{' '}
      {workspaceCreatorInformation(isCreator, creatorUser)}
      <WhatToDo
        {...{
          isCreator,
          isExhausted,
          isExpired,
          isExpiringSoon,
          isEligibleForExtension,
          navigate,
          onClose,
          workspace,
        }}
        onRequestExtension={() => setShowExtensionModal(true)}
      />
    </>
  );
  const footer = isCreator ? (
    <Button
      style={{ height: '38px', width: '70%', fontWeight: 400 }}
      onClick={() => onEditWorkspace(navigate, onClose, workspace)}
    >
      Edit Workspace
    </Button>
  ) : undefined;

  return (
    <>
      {showBanner && (
        <ToastBanner
          {...{ message, title, footer, onClose }}
          toastType={ToastType.WARNING}
          zIndex={500}
        />
      )}
      {showExtensionModal && (
        <ExtendInitialCreditsModal
          onClose={(updatedProfile: Profile) => {
            if (updatedProfile) {
              profileStore.get().updateCache(updatedProfile);
              workspacesApi()
                .getWorkspace(workspace.namespace, workspace.terraName)
                .then((updatedWorkspace) => {
                  currentWorkspaceStore.next({
                    ...updatedWorkspace.workspace,
                    accessLevel: updatedWorkspace.accessLevel,
                  });
                })
                .finally(() => setShowExtensionModal(false));
            } else {
              // In the case of cancel, just close the modal
              setShowExtensionModal(false);
            }
          }}
        />
      )}
    </>
  );
});
