import * as React from 'react';
import * as fp from 'lodash';

import { Profile } from 'generated/fetch';

import { Button, LinkButton } from 'app/components/buttons';
import { AoU } from 'app/components/text-wrappers';
import { ToastBanner, ToastType } from 'app/components/toast-banner';
import { withCurrentWorkspace, withUserProfile } from 'app/utils';
import { plusDays } from 'app/utils/dates';
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

const InitialCreditsArticleLink = () => (
  <>
    "
    <LinkButton
      onClick={() => window.open(supportUrls.createBillingAccount, '_blank')}
    >
      Using <AoU /> Initial Credits
    </LinkButton>
    "
  </>
);

const BillingAccountArticleLink = () => (
  <>
    "
    <LinkButton
      onClick={() => window.open(supportUrls.createBillingAccount, '_blank')}
    >
      Paying for Your Research
    </LinkButton>
    "
  </>
);

export const InvalidBillingBanner = fp.flow(
  withCurrentWorkspace(),
  withUserProfile(),
  withNavigation
)(({ onClose, navigate, workspace, profileState }: Props) => {
  const { profile } = profileState;
  const isCreator =
    workspace && profile && profile.username === workspace.creator;
  const eligibleForExtension =
    workspace && !workspace.initialCredits.extensionEpochMillis;
  const isExpired =
    workspace && workspace.initialCredits.expirationEpochMillis < Date.now();
  const isExpiringSoon =
    workspace &&
    !isExpired &&
    plusDays(workspace.initialCredits.expirationEpochMillis, 5) < Date.now();
  let message;
  if (isCreator) {
    if (eligibleForExtension) {
      if (isExpired) {
        // Banner 3 in spec (changed to trigger modal from here instead of on Profile page)
        message = (
          <div>
            Your initial credits have expired. You can request an extension
            here. For more information, read the <InitialCreditsArticleLink />{' '}
            article on the User Support Hub.
          </div>
        );
      } else if (isExpiringSoon) {
        // Banner 1 in spec (changed to trigger modal from here instead of on Profile page)
        message = (
          <div>
            Your initial credits are expiring soon. You can request an extension
            here. For more information, read the <InitialCreditsArticleLink />{' '}
            article on the User Support Hub.
          </div>
        );
      }
    } else {
      if (isExpired) {
        // Banner 5 in spec (use this also for exhausted)
        message = (
          <div>
            Your initial credits have run out. To use the workspace, a valid
            billing account needs to be provided. To learn more about
            establishing a billing account, read the{' '}
            <BillingAccountArticleLink /> article on the User Support Hub.
          </div>
        );
      } else if (isExpiringSoon) {
        // Not accounted for in spec
      }
    }
  } else {
    if (eligibleForExtension) {
      if (isExpired) {
        // Banner 4 in spec
        message = (
          <div>
            This workspace creator’s initial credits have expired. This
            workspace was created by FIRST NAME LAST NAME. For more information,
            read the <InitialCreditsArticleLink /> article on the User Support
            Hub.
          </div>
        );
      } else if (isExpiringSoon) {
        // Banner 2 in spec
        message = (
          <div>
            This workspace creator’s initial credits are expiring soon. This
            workspace was created by FIRST NAME LAST NAME. For more information,
            read the <InitialCreditsArticleLink /> article on the User Support
            Hub.
          </div>
        );
      }
    } else {
      if (isExpired) {
        // Banner 6 in spec
        message = (
          <div>
            This workspace creator’s initial credits have run out. This
            workspace was created by FIRST NAME LAST NAME. To use the workspace,
            a valid billing account needs to be provided. To learn more about
            establishing a billing account, read the{' '}
            <BillingAccountArticleLink /> article on the User Support Hub.
          </div>
        );
      } else if (isExpiringSoon) {
        // Not accounted for in spec
      }
    }
  }

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
    <ToastBanner
      {...{ message, footer, onClose }}
      title='This workspace has run out of initial credits'
      toastType={ToastType.WARNING}
      zIndex={500}
    />
  );
});
