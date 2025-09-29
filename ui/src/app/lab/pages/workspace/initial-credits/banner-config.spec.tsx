import '@testing-library/jest-dom';

import { render, screen } from '@testing-library/react';

import { bannerConfigs, BannerScenario } from './banner-config';

describe('bannerConfigs', () => {
  it('ExpiringSoon config returns correct title and button', () => {
    const config = bannerConfigs[BannerScenario.ExpiringSoon];
    expect(config.title).toBe('Workspace credits are expiring soon');
    expect(config.button.label).toBe('Edit Workspace');
    expect(config.button.action).toBe('editWorkspace');
    render(config.body({ expirationDate: '2025-01-01' }));
    expect(screen.getByText(/2025-01-01/)).toBeInTheDocument();
    expect(screen.getByText(/expire on/i)).toBeInTheDocument();
  });

  it('Expired config returns correct title and button', () => {
    const config = bannerConfigs[BannerScenario.Expired];
    expect(config.title).toBe('This workspace is out of initial credits');
    expect(config.button.label).toBe('Link to USH page');
    expect(config.button.action).toBe('openLink');
    expect(config.button.link).toMatch(/credits-and-billing/i);
    render(config.body({ creatorName: 'Jane Doe' }));
    expect(screen.getByText(/Jane Doe/)).toBeInTheDocument();
    expect(screen.getByText(/expired/i)).toBeInTheDocument();
  });

  it('LowBalance config returns correct title and button', () => {
    const config = bannerConfigs[BannerScenario.LowBalance];
    expect(config.title).toBe(
      'This workspace is running low on initial credits'
    );
    expect(config.button.label).toBe('Edit Workspace');
    expect(config.button.action).toBe('editWorkspace');
    render(config.body({ creditBalance: '12.34' }));
    expect(screen.getByText(/\$12.34/)).toBeInTheDocument();
    expect(screen.getByText(/remaining credit balance/i)).toBeInTheDocument();
  });

  it('Exhausted config returns correct title and button', () => {
    const config = bannerConfigs[BannerScenario.Exhausted];
    expect(config.title).toBe('This workspace is out of initial credits');
    expect(config.button.label).toBe('Link to USH page');
    expect(config.button.action).toBe('openLink');
    expect(config.button.link).toMatch(/credits-and-billing/i);
    render(config.body({ creatorName: 'Jane Doe' }));
    expect(screen.getByText(/Jane Doe/)).toBeInTheDocument();
    expect(screen.getByText(/run out/i)).toBeInTheDocument();
  });
});
