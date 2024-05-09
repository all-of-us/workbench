import '@testing-library/jest-dom';

import { AppType } from 'generated/fetch';

import { render, screen } from '@testing-library/react';
import { defaultSASCreateRequest } from 'app/components/apps-panel/utils';
import { DEFAULT_MACHINE_TYPE } from 'app/utils/machines';
import { appTypeToString } from 'app/utils/user-apps-utils';

import { DisabledCloudComputeProfile } from './disabled-cloud-compute-profile';

describe(DisabledCloudComputeProfile.name, () => {
  const machine = DEFAULT_MACHINE_TYPE;
  const { persistentDiskRequest } = defaultSASCreateRequest;

  // note: update these if we add more app types
  test.each([
    [AppType.CROMWELL, 'RStudio and SAS'],
    [AppType.RSTUDIO, 'Cromwell and SAS'],
    [AppType.SAS, 'Cromwell and RStudio'],
  ])(
    'should display the correct sharing text for %s',
    (appType: AppType, otherTypes: string) => {
      render(
        <DisabledCloudComputeProfile
          {...{ appType, machine, persistentDiskRequest }}
        />
      );
      expect(
        screen.getByText(
          `Your ${appTypeToString[appType]} environment will share CPU and RAM resources with any ` +
            otherTypes +
            ' environments you run in this workspace.',
          { exact: false }
        )
      ).toBeInTheDocument();
    }
  );
});
