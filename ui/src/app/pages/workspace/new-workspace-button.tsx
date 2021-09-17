import {CardButton} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import colors from 'app/styles/colors';
import {reactStyles} from 'app/utils';
import {AnalyticsTracker} from 'app/utils/analytics';
import * as React from 'react';

const styles = reactStyles({
  addCard: {
    margin: '0 1rem 1rem 0', fontWeight: 600, color: colors.accent,
  }
});

export const NewWorkspaceButton = () => {
  return <CardButton
    path='/workspaces/build'
    onClick={() => AnalyticsTracker.Workspaces.OpenCreatePage()}
    style={styles.addCard}
  >
    Create a <br/> New Workspace <br/>
    <ClrIcon shape='plus-circle' style={{height: '32px', width: '32px'}}/>
  </CardButton>;
};
