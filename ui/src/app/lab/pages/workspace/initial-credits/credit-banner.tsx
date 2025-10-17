import * as React from 'react';

import { Profile } from 'generated/fetch';

import { Button, LinkButton } from 'app/components/buttons';
import { MultiToastBanner } from 'app/components/multi-toast-banner';
import { ToastType } from 'app/components/toast-banner';
import { useNavigation } from 'app/utils/navigation';
import { WorkspaceData } from 'app/utils/workspace-data';

import { bannerConfigs, BannerScenario } from './banner-config';

interface CreditBannerProps {
  banners: Array<{
    scenario: BannerScenario;
    expirationDate?: string;
    creatorName?: string;
    creditBalance?: string;
    workspace?: WorkspaceData;
    profile?: Profile;
    onClose: () => void;
  }>;
}

export const CreditBanner = ({ banners }: CreditBannerProps) => {
  const [navigate] = useNavigation();
  const alertMessages = banners.map((banner) => {
    const {
      scenario,
      expirationDate,
      creatorName,
      creditBalance,
      workspace,
      profile,
      onClose,
    } = banner;
    const config = bannerConfigs[scenario];

    // Helper to generate a unique id for each message
    const id = `${scenario}_${expirationDate}_${creatorName}`;

    // Determine if the current user is the creator
    const isCreator = profile?.username === workspace?.creatorUser?.userName;

    // Button rendering logic
    const renderButton = () => {
      if (config.button?.action === 'editWorkspace' && isCreator) {
        return (
          <Button
            onClick={() => {
              onClose();
              navigate(
                [
                  'workspaces',
                  workspace.namespace,
                  workspace.terraName,
                  'edit',
                ],
                {
                  queryParams: { highlightBilling: true },
                }
              );
            }}
          >
            {config.button.label}
          </Button>
        );
      }
      if (config.button?.action === 'openLink' && config.button.link) {
        return (
          <LinkButton onClick={() => window.open(config.button.link, '_blank')}>
            {config.button.label}
          </LinkButton>
        );
      }
      return null;
    };

    return {
      id,
      title: config.title,
      message: config.body({ expirationDate, creatorName, creditBalance }),
      footer: renderButton(),
      onClose,
      toastType: ToastType.WARNING,
    };
  });

  return (
    <MultiToastBanner
      messages={alertMessages}
      onDismiss={(messageId) => {
        const banner = banners.find(
          (b) =>
            `${b.scenario}_${b.expirationDate}_${b.creatorName}` === messageId
        );
        banner?.onClose();
      }}
      zIndex={1000}
    />
  );
};
