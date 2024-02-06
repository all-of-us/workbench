import * as React from 'react';

import { AppType } from 'generated/fetch';

import { InfoMessage } from 'app/components/messages';

import {
  CommonGKEAppPanelProps,
  GKEAppConfigPanelMain,
} from './gke-app-config-panel-main';

const SupportNote = () => (
  <InfoMessage>
    <h4 style={{ marginTop: 0, fontSize: '1rem' }}>
      How to create RStudio artifacts:
    </h4>
    <p style={{ marginTop: '0.5rem' }}>
      You can create R and RMD files within RStudio’s menu bar. Saved files will
      appear in the analysis tab and be stored in the workspace bucket. Access
      your files in RStudio through the Output pane.
    </p>
  </InfoMessage>
);

export const RStudioPanel = (props: CommonGKEAppPanelProps) => (
  <GKEAppConfigPanelMain
    {...{
      ...props,
      SupportNote,
    }}
    appType={AppType.RSTUDIO}
  />
);
