import * as React from 'react';
import { useEffect } from 'react';

import { FadeBox } from 'app/components/containers';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { ListPageHeader } from 'app/components/headers';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import colors from 'app/styles/colors';
import { reactStyles, withCurrentWorkspace } from 'app/utils';
import { WorkspaceData } from 'app/utils/workspace-data';

import { AppSelector } from './app-selector';

const styles = reactStyles({
  fadeBox: {
    margin: 'auto',
    marginTop: '1.5rem',
    width: '95.7%',
  },
  appsLabel: {
    color: colors.primary,
    fontWeight: 600,
    fontSize: '14px',
    lineHeight: '24px',
    paddingBottom: '0.75rem',
  },
});

interface AppFilesListProps extends WithSpinnerOverlayProps {
  workspaceData: WorkspaceData;
}
export const AppFilesList = withCurrentWorkspace()(
  (props: AppFilesListProps) => {
    const { workspaceData } = props;

    useEffect(() => {
      props.hideSpinner();
    }, []);

    return (
      <FadeBox style={styles.fadeBox}>
        <FlexColumn>
          <FlexRow>
            <ListPageHeader style={{ paddingRight: '2.25rem' }}>
              Your Analyses
            </ListPageHeader>
            <AppSelector {...{ workspaceData }} />
          </FlexRow>
        </FlexColumn>
      </FadeBox>
    );
  }
);
