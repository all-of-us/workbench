import * as React from 'react';

import { AppType } from 'generated/fetch';

import { InfoMessage } from 'app/components/messages';

import {
  CreateGKEAppPanel,
  CreateGKEAppPanelProps,
} from './gke-app-configuration-panels/create-gke-app-panel';

const IntroText = () => (
  <div>
    Your analysis environment consists of an application and compute resources.
    Your cloud environment is unique to this workspace and not shared with other
    users.
  </div>
);

const PostCompute = () => (
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

export const RStudioConfigurationPanel = (props: CreateGKEAppPanelProps) => (
  <CreateGKEAppPanel
    {...{
      ...props,
      IntroText,
      PostCompute,
    }}
    appType={AppType.RSTUDIO}
  />
);
