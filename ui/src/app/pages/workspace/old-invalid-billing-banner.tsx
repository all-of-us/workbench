import * as React from 'react';
import * as fp from 'lodash';

import { Profile } from 'generated/fetch';

import { Button, LinkButton } from 'app/components/buttons';
import { ToastBanner, ToastType } from 'app/components/toast-banner';
import { withCurrentWorkspace, withUserProfile } from 'app/utils';
import { NavigationProps } from 'app/utils/navigation';
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

export const OldInvalidBillingBanner = fp.flow(
  withCurrentWorkspace(),
  withUserProfile(),
  withNavigation
)(({ onClose, navigate, workspace }: Props) => {
  const message = (
    <div>
      The initial credits for the creator of this workspace have run out. Please
      provide a valid billing account.
      <LinkButton
        onClick={() => window.open(supportUrls.createBillingAccount, '_blank')}
      >
        Learn how to link a billing account.
      </LinkButton>
    </div>
  );
  const footer = (
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
    <ToastBanner
      {...{ message, footer, onClose }}
      title='This workspace has run out of initial credits'
      toastType={ToastType.WARNING}
      zIndex={500}
    />
  );
});
