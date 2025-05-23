import * as React from 'react';

import { Profile } from 'generated/fetch';

import { Button, LinkButton } from 'app/components/buttons';
import { ToastBanner, ToastType } from 'app/components/toast-banner';
import { useNavigation } from 'app/utils/navigation';
import { WorkspaceData } from 'app/utils/workspace-data';
import { supportUrls } from 'app/utils/zendesk';

interface BillingBannerProps {
  workspace?: WorkspaceData;
  profile?: Profile;
  onClose: () => void;
}

export const InvalidBillingBanner = ({
  workspace,
  profile,
  onClose,
}: BillingBannerProps) => {
  const [navigate] = useNavigation();

  const { creatorUser } = workspace;
  const isCreator = profile.username === workspace.creatorUser?.userName;

  // Helper function to navigate to edit workspace
  const goToEditWorkspace = () => {
    onClose();
    navigate(['workspaces', workspace.namespace, workspace.terraName, 'edit'], {
      queryParams: {
        highlightBilling: true,
      },
    });
  };

  // Whose credits message
  const whose = isCreator ? 'Your' : "This workspace creator's";
  const creatorInfo = isCreator
    ? ''
    : `This workspace was created by ${creatorUser?.givenName} ${creatorUser?.familyName}. `;

  // Create message content
  const message = (
    <>
      {whose} initial credits have run out. {creatorInfo}
      To use the workspace, a valid billing account needs to be added
      {isCreator && (
        <>
          {' '}
          <LinkButton onClick={goToEditWorkspace} style={{ display: 'inline' }}>
            on the Edit Workspace page
          </LinkButton>
        </>
      )}
      . To learn more about establishing a billing account, read "
      <LinkButton
        onClick={() => window.open(supportUrls.createBillingAccount, '_blank')}
        style={{ display: 'inline' }}
      >
        Paying for Your Research
      </LinkButton>
      " on the User Support Hub.
    </>
  );

  // Create footer with Edit Workspace button only for creators
  const footer = isCreator ? (
    <Button
      style={{ height: '38px', width: '70%', fontWeight: 400 }}
      onClick={goToEditWorkspace}
    >
      Edit Workspace
    </Button>
  ) : undefined;

  return (
    <ToastBanner
      message={message}
      title='This workspace is out of initial credits'
      footer={footer}
      onClose={onClose}
      toastType={ToastType.WARNING}
      zIndex={500}
    />
  );
};
