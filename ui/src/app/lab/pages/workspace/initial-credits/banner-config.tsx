import React from 'react';

export const BannerScenario = {
  ExpiringSoon: 'expiringSoon',
  Expired: 'expired',
  LowBalance: 'lowBalance',
  Exhausted: 'exhausted',
} as const;

export type BannerScenario =
  (typeof BannerScenario)[keyof typeof BannerScenario];

interface BannerConfig {
  title: string;
  body: (params: {
    expirationDate?: string;
    creatorName?: string;
    creditBalance?: string;
  }) => React.ReactNode;
  button: {
    label: string;
    action: 'editWorkspace' | 'openLink';
    link?: string;
  };
}

const creditsAndBillingLink = (
  <a
    href='https://support.researchallofus.org/hc/en-us/sections/6000172946708-Paying-for-Your-Research'
    target='_blank'
    rel='noopener noreferrer'
  >
    Credits and Billing
  </a>
);

export const bannerConfigs: Record<BannerScenario, BannerConfig> = {
  [BannerScenario.ExpiringSoon]: {
    title: 'Workspace credits are expiring soon',
    body: ({ expirationDate }) => (
      <>
        This workspace creator’s initial credits expire on{' '}
        <b>{expirationDate}</b>. At expiration, a valid billing account must be
        added to continue analysis. Workspace creators can edit billing in the
        “About” tab of the workspace. To learn more about using a Google Cloud
        Platform (GCP) billing account to pay for your research, review the{' '}
        {creditsAndBillingLink} section on the User Support Hub.
      </>
    ),
    button: {
      label: 'Edit Workspace',
      action: 'editWorkspace',
    },
  },
  [BannerScenario.Expired]: {
    title: 'This workspace is out of initial credits',
    body: ({ creatorName }) => (
      <>
        This workspace creator’s initial credits have expired. This workspace
        was created by <b>{creatorName}</b>. To avoid interruption of analysis
        and possible loss of saved files, a valid Google Cloud Platform (GCP)
        billing account must be added. Creators can edit billing in the “About”
        tab of the workspace. To learn more about using a billing account to pay
        for your research, review the {creditsAndBillingLink} section on the
        User Support Hub.
      </>
    ),
    button: {
      label: 'Link to USH page',
      action: 'openLink',
      link: 'https://support.researchallofus.org/hc/en-us/categories/5942730401428-Credits-and-Billing',
    },
  },
  [BannerScenario.LowBalance]: {
    title: 'This workspace is running low on initial credits',
    body: ({ creditBalance }) => (
      <>
        This workspace creator’s remaining credit balance is{' '}
        <b>${creditBalance}</b>. When out of initial credits, you must set up a
        valid billing account to continue analysis. Workspace creators can edit
        billing in the “About” tab of the workspace. To learn more about using a
        Google Cloud Platform (GCP) billing account to pay for your research,
        review the {creditsAndBillingLink} section on the User Support Hub.
      </>
    ),
    button: {
      label: 'Edit Workspace',
      action: 'editWorkspace',
    },
  },
  [BannerScenario.Exhausted]: {
    title: 'This workspace is out of initial credits',
    body: ({ creatorName }) => (
      <>
        The initial credit balance for this creator’s workspace has run out.
        This workspace was created by <b>{creatorName}</b>. To avoid
        interruption of analysis and possible loss of saved files, a valid
        Google Cloud Platform billing account must be added. Creators can edit
        billing in the “About” tab of the workspace. To learn more about using a
        Google Cloud Platform (GCP) billing account to pay for your research,
        review the {creditsAndBillingLink} section on the User Support Hub.
      </>
    ),
    button: {
      label: 'Link to USH page',
      action: 'openLink',
      link: 'https://support.researchallofus.org/hc/en-us/categories/5942730401428-Credits-and-Billing',
    },
  },
};
