import * as React from 'react';
import { useState } from 'react';

import { Workspace, WorkspaceAccessLevel } from 'generated/fetch';

import { SnowmanButton } from 'app/components/buttons';
import { FlexColumn } from 'app/components/flex';
import { PopupTrigger } from 'app/components/popups';
import { WorkspaceActionsMenu } from 'app/pages/workspace/workspace-actions-menu';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import { AnalyticsTracker, triggerEvent } from 'app/utils/analytics';
import { EVENT_CATEGORY } from 'app/utils/constants';
import { useNavigation } from 'app/utils/navigation';

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

interface menuProps {
  tierAccessDisabled: boolean;
  workspace: Workspace;
  accessLevel: WorkspaceAccessLevel;
}
export const WorkspaceMenu = (props: menuProps) => {
  const {
    accessLevel,
    tierAccessDisabled,
    workspace,
    workspace: { accessTierShortName, adminLocked, namespace, id },
  } = props;
  const [navigate] = useNavigation();

  const [confirmDeleting, setConfirmDeleting] = useState<boolean>(false);
  const [showShareModal, setShowShareModal] = useState<boolean>(false);
  const [showResearchPurposeReviewModal, setShowResearchPurposeReviewModal] =
    useState<boolean>(false);

  return (
    <FlexColumn style={styles.workspaceMenuWrapper}>
      {!tierAccessDisabled && (
        <PopupTrigger
          side='bottom'
          closeOnClick
          content={
            <WorkspaceActionsMenu
              workspaceData={{ ...workspace, accessLevel }}
              onDuplicate={() => {
                // Using workspace.published here to identify Featured Workspaces. At some point, we will need a separate property for
                // this on the workspace object once users are able to publish their own workspaces
                workspace.published
                  ? AnalyticsTracker.Workspaces.DuplicateFeatured(
                      workspace.name
                    )
                  : AnalyticsTracker.Workspaces.OpenDuplicatePage('Card');
                navigate(['workspaces', namespace, id, 'duplicate']);
              }}
              onEdit={() => {
                AnalyticsTracker.Workspaces.OpenEditPage('Card');
                navigate(['workspaces', namespace, id, 'edit']);
              }}
              onDelete={() => {
                AnalyticsTracker.Workspaces.OpenDeleteModal('Card');
                triggerEvent(
                  EVENT_CATEGORY,
                  'delete',
                  'Card menu - click delete'
                );
                setConfirmDeleting(true);
              }}
              onShare={() => {
                AnalyticsTracker.Workspaces.OpenShareModal('Card');
                triggerEvent(
                  EVENT_CATEGORY,
                  'share',
                  'Card menu - click share'
                );
                setShowShareModal(true);
              }}
            />
          }
        >
          <SnowmanButton style={{ marginLeft: 0 }} />
        </PopupTrigger>
      )}
    </FlexColumn>
  );
};
