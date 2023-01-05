import * as React from 'react';

import { CardButton } from 'app/components/buttons';
import { ClrIcon } from 'app/components/icons';
import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import { AnalyticsTracker } from 'app/utils/analytics';
import { useNavigation } from 'app/utils/navigation';

const styles = reactStyles({
  addCard: {
    margin: '0 1.5rem 1.5rem 0',
    fontWeight: 600,
    color: colors.accent,
  },
});

export const NewWorkspaceButton = () => {
  const [navigate] = useNavigation();

  return (
    <CardButton
      onClick={() => {
        AnalyticsTracker.Workspaces.OpenCreatePage();
        navigate(['workspaces', 'build']);
      }}
      style={styles.addCard}
    >
      Create a <br /> New Workspace
      <ClrIcon shape='plus-circle' style={{ height: '32px', width: '32px' }} />
    </CardButton>
  );
};
