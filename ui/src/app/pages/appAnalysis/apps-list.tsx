import * as React from 'react';
import { useEffect } from 'react';
import { faPlusCircle } from '@fortawesome/pro-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { BillingStatus } from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { FadeBox } from 'app/components/containers';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { ListPageHeader } from 'app/components/headers';
import colors from 'app/styles/colors';
import { withCurrentWorkspace } from 'app/utils';
import { AnalyticsTracker } from 'app/utils/analytics';
import { WorkspacePermissionsUtil } from 'app/utils/workspace-permissions';

export const AppsList = withCurrentWorkspace()((props) => {
  const { workspace } = props;

  const canWrite = () => {
    return WorkspacePermissionsUtil.canWrite(workspace.accessLevel);
  };

  useEffect(() => {
    props.hideSpinner();
  }, []);

  return (
    <FadeBox style={{ margin: 'auto', marginTop: '1rem', width: '95.7%' }}>
      <FlexColumn>
        <FlexRow>
          <ListPageHeader style={{ paddingRight: '1.5rem' }}>
            Your Analysis
          </ListPageHeader>
          <Button
            style={{
              paddingLeft: '0.5rem',
              height: '2rem',
              backgroundColor: colors.secondary,
            }}
            onClick={() => {
              AnalyticsTracker.Notebooks.OpenCreateModal();
            }}
            disabled={
              workspace.billingStatus === BillingStatus.INACTIVE || !canWrite()
            }
          >
            <div style={{ paddingRight: '0.5rem' }}>Start</div>
            <FontAwesomeIcon icon={faPlusCircle}></FontAwesomeIcon>
          </Button>
        </FlexRow>
      </FlexColumn>
    </FadeBox>
  );
});
