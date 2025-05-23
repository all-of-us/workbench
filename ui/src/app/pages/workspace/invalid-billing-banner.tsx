import * as React from 'react';
import * as fp from 'lodash';

import { Profile, User } from 'generated/fetch';

import { Button, LinkButton } from 'app/components/buttons';
import { ToastBanner, ToastType } from 'app/components/toast-banner';
import { withCurrentWorkspace, withUserProfile } from 'app/utils';
import { NavigationProps } from 'app/utils/navigation';
import { withNavigation } from 'app/utils/with-navigation-hoc';
import { WorkspaceData } from 'app/utils/workspace-data';
import { supportUrls } from 'app/utils/zendesk';

const BillingAccountArticleLink = () => (
  <LinkButton
    onClick={() => window.open(supportUrls.createBillingAccount, '_blank')}
    style={{ display: 'inline' }}
  >
    Paying for Your Research
  </LinkButton>
);

interface BillingUpdateOptionsProps {
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
  isCreator: boolean;
}
const WhatHappened = ({ isCreator }: WhatHappenedProps) => {
  const whose = whoseCredits(isCreator);
  return <>{whose} initial credits have run out.</>;
};

interface WhatToDoProps {
  isCreator: boolean;
  navigate: Function;
  onClose: Function;
  workspace: WorkspaceData;
}

const WhatToDo = ({
  isCreator,
  navigate,
  onClose,
  workspace,
}: WhatToDoProps) => {
  return (
    <>
      To use the workspace, a valid billing account needs to be added
      {isCreator && (
        <>
          {' '}
          <EditWorkspaceLink {...{ navigate, onClose, workspace }} />
        </>
      )}
      . To learn more about establishing a billing account, read "
      <BillingAccountArticleLink />" on the User Support Hub.
    </>
  );
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
  const isCreator = profile?.username === workspace?.creatorUser?.userName;
  const isExpired =
    workspace?.initialCredits.expirationEpochMillis < Date.now();
  const isExhausted = workspace?.initialCredits.exhausted;
  const title = 'This workspace is out of initial credits';

  const showBanner =
    creatorUser?.givenName &&
    creatorUser?.familyName &&
    (isExhausted ||
      (!workspace.initialCredits.expirationBypassed && isExpired));

  const message = (
    <>
      <WhatHappened
        {...{
          isCreator,
        }}
      />{' '}
      {workspaceCreatorInformation(isCreator, creatorUser)}
      <WhatToDo
        {...{
          isCreator,
          navigate,
          onClose,
          workspace,
        }}
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
    </>
  );
});
