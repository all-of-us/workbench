import { Profile } from 'generated/fetch';

import { Button, LinkButton } from 'app/components/buttons';
import { ToastBanner, ToastType } from 'app/components/toast-banner';
import { useNavigation } from 'app/utils/navigation';
import { WorkspaceData } from 'app/utils/workspace-data';

import { bannerConfigs, BannerScenario } from './banner-config';

interface CreditBannerProps {
  scenario: BannerScenario;
  expirationDate?: string;
  creatorName?: string;
  creditBalance?: string;
  workspace?: WorkspaceData;
  profile?: Profile;
  onClose: () => void;
}

export const CreditBanner = ({
  scenario,
  expirationDate,
  creatorName,
  creditBalance,
  workspace,
  profile,
  onClose,
}: CreditBannerProps) => {
  const [navigate] = useNavigation();

  const config = bannerConfigs[scenario];

  const isCreator = profile.username === workspace.creatorUser?.userName;

  const goToEditWorkspace = () => {
    onClose();
    navigate(['workspaces', workspace.namespace, workspace.terraName, 'edit'], {
      queryParams: {
        highlightBilling: true,
      },
    });
  };

  const renderButton = () => {
    if (config.button.action === 'editWorkspace' && isCreator) {
      return <Button onClick={goToEditWorkspace}>{config.button.label}</Button>;
    }
    if (config.button.action === 'openLink' && config.button.link) {
      return (
        <LinkButton onClick={() => window.open(config.button.link, '_blank')}>
          {config.button.label}
        </LinkButton>
      );
    }
    return null;
  };

  return (
    <ToastBanner
      title={config.title}
      message={config.body({ expirationDate, creatorName, creditBalance })}
      footer={renderButton()}
      onClose={onClose}
      toastType={ToastType.WARNING}
      zIndex={500}
    />
  );
};
