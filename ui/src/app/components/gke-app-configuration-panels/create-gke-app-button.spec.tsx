import * as React from 'react';
import { shallow } from 'enzyme';

import { AppStatus } from 'generated/fetch';

import { defaultCromwellConfig } from 'app/components/apps-panel/utils';
import { Button } from 'app/components/buttons';
import {
  CreateGKEAppButton,
  CreateGKEAppButtonProps,
} from 'app/components/gke-app-configuration-panels/create-gke-app-button';
import { TooltipTrigger } from 'app/components/popups';

import { createListAppsCromwellResponse } from 'testing/stubs/apps-api-stub';
import { ALL_GKE_APP_STATUSES, minus } from 'testing/utils';

describe(CreateGKEAppButton.name, () => {
  const DEFAULT_PROPS: CreateGKEAppButtonProps = {
    createAppRequest: defaultCromwellConfig,
    existingApp: null,
    workspaceNamespace: 'aou-rw-test-1',
    onDismiss: () => {},
  };

  const component = async (
    propOverrides?: Partial<CreateGKEAppButtonProps>
  ) => {
    const allProps = { ...DEFAULT_PROPS, ...propOverrides };
    return shallow(<CreateGKEAppButton {...allProps} />);
  };

  const createEnabledStatuses = [AppStatus.DELETED, null, undefined];
  const createDisabledStatuses = minus(
    ALL_GKE_APP_STATUSES,
    createEnabledStatuses
  );

  describe('should allow creating a GKE app for certain app statuses', () => {
    test.each(createEnabledStatuses)('Status %s', async (appStatus) => {
      const wrapper = await component({
        createAppRequest: defaultCromwellConfig,
        existingApp: createListAppsCromwellResponse({ status: appStatus }),
      });
      expect(wrapper.find(TooltipTrigger).prop('disabled')).toBeTruthy();
      expect(wrapper.find(Button).prop('disabled')).toBeFalsy();
    });
  });

  describe('should not allow creating an RStudio app for certain app statuses', () => {
    test.each(createDisabledStatuses)('Status %s', async (appStatus) => {
      const wrapper = await component({
        createAppRequest: defaultCromwellConfig,
        existingApp: createListAppsCromwellResponse({ status: appStatus }),
      });
      expect(wrapper.find(TooltipTrigger).prop('disabled')).toBeFalsy();
      expect(wrapper.find(Button).prop('disabled')).toBeTruthy();
    });
  });
});
