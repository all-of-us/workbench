import * as React from 'react';
import * as fp from 'lodash/fp';

import { Clickable } from 'app/components/buttons';
import { FadeBox } from 'app/components/containers';
import { FlexRow } from 'app/components/flex';
import { ResearchPurposeSection } from 'app/components/research-purpose-section';
import { EditComponentReact } from 'app/icons/edit';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles, withCurrentWorkspace } from 'app/utils';
import { useNavigation } from 'app/utils/navigation';
import { withNavigation } from 'app/utils/with-navigation-hoc';
import { WorkspaceData } from 'app/utils/workspace-data';
import { WorkspacePermissionsUtil } from 'app/utils/workspace-permissions';

const styles = reactStyles({
  editIcon: {
    marginTop: '0.15rem',
    height: 22,
    width: 22,
    fill: colors.light,
    backgroundColor: colors.accent,
    padding: '5px',
    borderRadius: '23px',
  },
  mainHeader: {
    fontSize: '18px',
    fontWeight: 600,
    color: colors.primary,
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center',
  },
  sectionContentContainer: {
    marginLeft: '1.5rem',
  },
  sectionHeader: {
    fontSize: '16px',
    fontWeight: 600,
    color: colors.primary,
    marginTop: '1.5rem',
  },
  sectionItemWithBackground: {
    padding: '10px',
    backgroundColor: colors.white,
    color: colors.primary,
    marginLeft: '0.75rem',
    borderRadius: '3px',
  },
  sectionSubHeader: {
    fontSize: '14px',
    fontWeight: 600,
    color: colors.primary,
    marginTop: '0.75rem',
  },
  sectionText: {
    fontSize: '14px',
    lineHeight: '24px',
    color: colors.primary,
    marginTop: '0.45rem',
  },
  reviewPurposeReminder: {
    marginTop: '0.45rem',
    borderStyle: 'solid',
    height: '3.75rem',
    color: colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
    borderColor: colors.warning,
    borderRadius: '0.6rem',
    borderWidth: '0.15rem',
    backgroundColor: colorWithWhiteness(colors.highlight, 0.7),
  },
});

export const ResearchPurpose = fp.flow(
  withCurrentWorkspace(),
  withNavigation
)(({ workspace }: { workspace: WorkspaceData }) => {
  const [navigate] = useNavigation();
  const isOwner = WorkspacePermissionsUtil.isOwner(workspace.accessLevel);

  return (
    <FadeBox>
      <FlexRow>
        <div style={styles.mainHeader}>Research Use Statement Questions</div>
        <Clickable
          disabled={!isOwner}
          style={{
            display: 'flex',
            alignItems: 'center',
            marginLeft: '.75rem',
          }}
          data-test-id='edit-workspace'
          onClick={() =>
            navigate([
              'workspaces',
              workspace.namespace,
              workspace.terraName,
              'edit',
            ])
          }
        >
          <EditComponentReact
            enableHoverEffect={true}
            disabled={!isOwner}
            style={styles.editIcon}
          />
        </Clickable>
      </FlexRow>
      <ResearchPurposeSection researchPurpose={workspace.researchPurpose} />
    </FadeBox>
  );
});
