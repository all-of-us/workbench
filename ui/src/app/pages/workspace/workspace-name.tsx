import * as React from 'react';
import { useState } from 'react';

import { Workspace } from 'generated/fetch';

import { StyledRouterLink } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { ControlledTierBadge } from 'app/components/icons';
import { TooltipTrigger } from 'app/components/popups';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import {
  AccessTierShortNames,
  displayNameForTier,
} from 'app/utils/access-tiers';
import { AnalyticsTracker, triggerEvent } from 'app/utils/analytics';
import { EVENT_CATEGORY } from 'app/utils/constants';
import { serverConfigStore, useStore } from 'app/utils/stores';
const styles = reactStyles({
  workspaceCard: {
    justifyContent: 'flex-end',
    height: '100%',
    // Set relative positioning so the spinner overlay is centered in the card.
    position: 'relative',
  },
  workspaceMenuWrapper: {
    paddingTop: '.75rem',
    borderRight: '1px solid',
    borderColor: colorWithWhiteness(colors.dark, 0.6),
    flex: '0 0 1.5rem',
    justifyContent: 'flex-start',
    alignItems: 'center',
  },
  workspaceName: {
    color: colors.accent,
    marginBottom: '0.75rem',
    fontSize: 18,
    wordBreak: 'break-all',
  },
  workspaceNameDisabled: {
    color: colors.disabled,
    marginBottom: '0.75rem',
    fontSize: 18,
    wordBreak: 'break-all',
    pointerEvents: 'none',
    cursor: 'not-allowed',
  },
  permissionBox: {
    color: colors.white,
    height: '1.5rem',
    width: '4.5rem',
    fontSize: 10,
    textAlign: 'center',
    borderRadius: '0.3rem',
    padding: 0,
  },
  lockWorkspace: {
    color: colors.warning,
    marginBottom: '0.15rem',
    width: '21px',
    height: '21px',
    viewBox: '0 0 25 27',
  },
});
interface WorkspaceNameprops {
  workspace: Workspace;
  tierAccessDisabled: boolean;
}
export const WorkspaceName = (props: WorkspaceNameprops) => {
  const {
    tierAccessDisabled,
    workspace,
    workspace: { accessTierShortName, adminLocked, namespace, id },
  } = props;

  const [showResearchPurposeReviewModal, setShowResearchPurposeReviewModal] =
    useState<boolean>(false);
  const { config } = useStore(serverConfigStore);
  function requiresReviewPrompt() {
    // return (
    //   config.enableResearchReviewPrompt &&
    //   props.workspace.researchPurpose.needsReviewPrompt
    // );
    return false;
  }

  function trackWorkspaceNavigation() {
    const {
      workspace: { name, published },
    } = props;
    published
      ? AnalyticsTracker.Workspaces.NavigateToFeatured(name)
      : triggerEvent(EVENT_CATEGORY, 'navigate', 'Click on workspace name');
  }

  function onClick(e) {
    if (requiresReviewPrompt()) {
      setShowResearchPurposeReviewModal(true);
      e.preventDefault();
    }
  }

  return (
    <FlexRow style={{ alignItems: 'flex-start' }}>
      <StyledRouterLink
        style={
          tierAccessDisabled
            ? styles.workspaceNameDisabled
            : styles.workspaceName
        }
        onClick={(e) => onClick(e)}
        analyticsFn={() => trackWorkspaceNavigation()}
        data-test-id={'workspace-card-link'}
        propagateDataTestId
        path={`/workspaces/${namespace}/${id}/data`}
      >
        <TooltipTrigger
          content={
            tierAccessDisabled && (
              <div>
                This workspace is a {displayNameForTier(accessTierShortName)}{' '}
                workspace. You do not have access. Please complete the data
                access requirements to gain access.
              </div>
            )
          }
        >
          <div
            style={
              tierAccessDisabled
                ? styles.workspaceNameDisabled
                : styles.workspaceName
            }
            data-test-id='workspace-card-name'
          >
            {workspace.name}
          </div>
        </TooltipTrigger>
      </StyledRouterLink>
      {adminLocked && (
        <FlexColumn
          data-test-id='workspace-lock'
          style={{ justifyContent: 'flex-end', paddingLeft: '0.5rem' }}
        >
          <TooltipTrigger content='Workspace compliance action is required'>
            {/* <FontAwesomeIcon icon={faLockAlt} style={styles.lockWorkspace} />*/}
            <div>Lock Icon needs to be here</div>
          </TooltipTrigger>
        </FlexColumn>
      )}
      {accessTierShortName === AccessTierShortNames.Controlled && (
        <ControlledTierBadge />
      )}
    </FlexRow>
  );
};
