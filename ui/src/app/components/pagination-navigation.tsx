import * as React from 'react';
import { useEffect, useState } from 'react';

import { Workspace } from 'generated/fetch';

import { Clickable, CloseButton } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { SidebarIconId } from 'app/components/help-sidebar-icons';
import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import { runtimeStore, userAppsStore, useStore } from 'app/utils/stores';
import { BILLING_ACCOUNT_DISABLED_TOOLTIP } from 'app/utils/strings';
import { maybeStartPollingForUserApps } from 'app/utils/user-apps-utils';
import { isValidBilling } from 'app/utils/workspace-utils';

import { AppBanner } from './apps-panel/app-banner';
import { ExpandedApp } from './apps-panel/expanded-app';
import {
  findApp,
  getAppsByDisplayGroup,
  openConfigPanelForUIApp,
  UIAppType,
} from './apps-panel/utils';
import { ArrowLeft, ArrowRight } from './icons';
import { TooltipTrigger } from './popups';

export const PaginationNavigation = (props: {
  currentIndex: number;
  setCurrentIndex: (index: number) => void;
  numElements: number;
  singularName?: string;
}) => {
  const { currentIndex, setCurrentIndex, numElements, singularName } = props;

  return (
    <FlexRow
      style={{
        marginTop: '0.5rem',
        alignItems: 'center',
        justifyContent: 'space-between',
      }}
    >
      <FlexRow style={{ alignItems: 'center', gap: '0.5rem' }}>
        <ArrowLeft
          size={16}
          title={`Previous ${singularName || 'element'}`}
          style={{
            cursor: currentIndex > 0 ? 'pointer' : 'not-allowed',
            color: currentIndex > 0 ? colors.accent : colors.disabled,
          }}
          onClick={() => currentIndex > 0 && setCurrentIndex(currentIndex - 1)}
        />
        <span style={{ fontSize: '12px', color: colors.primary }}>
          {currentIndex + 1} of {numElements}
        </span>
        <ArrowRight
          size={16}
          title={`Next ${singularName || 'element'}`}
          style={{
            cursor: currentIndex < numElements - 1 ? 'pointer' : 'not-allowed',
            color:
              currentIndex < numElements - 1 ? colors.accent : colors.disabled,
          }}
          onClick={() =>
            currentIndex < numElements - 1 && setCurrentIndex(currentIndex + 1)
          }
        />
      </FlexRow>
    </FlexRow>
  );
};
