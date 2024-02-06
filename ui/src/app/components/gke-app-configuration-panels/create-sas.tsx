import * as React from 'react';

import { AppType } from 'generated/fetch';

import {
  CommonGKEAppPanelProps,
  GKEAppConfigPanelMain,
} from './gke-app-config-panel-main';

export const SASPanel = (props: CommonGKEAppPanelProps) => (
  <GKEAppConfigPanelMain {...props} appType={AppType.SAS} />
);
