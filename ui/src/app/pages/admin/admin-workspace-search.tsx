import * as React from 'react';
import { useEffect, useState } from 'react';

import { Button } from 'app/components/buttons';
import { FlexRow } from 'app/components/flex';
import { TextInput } from 'app/components/inputs';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import colors from 'app/styles/colors';
import { useNavigation } from 'app/utils/navigation';

export const AdminWorkspaceSearch = (spinnerProps: WithSpinnerOverlayProps) => {
  const [workspaceNamespace, setWorkspaceNamespace] = useState();
  const [navigate] = useNavigation();

  const navigateToWorkspace = () =>
    navigate(['admin/workspaces/' + workspaceNamespace]);

  useEffect(() => spinnerProps.hideSpinner(), []);

  return (
    <FlexRow
      style={{
        justifyContent: 'flex-start',
        alignItems: 'center',
        marginTop: '1.5rem',
      }}
    >
      <label style={{ color: colors.primary, marginRight: '1.5rem' }}>
        Workspace namespace
      </label>
      <TextInput
        style={{
          width: '15rem',
          marginRight: '1.5rem',
        }}
        onChange={(value) => setWorkspaceNamespace(value)}
        onKeyDown={(event: KeyboardEvent) => {
          if (event.key === 'Enter') {
            navigateToWorkspace();
          }
        }}
      />
      <Button
        style={{ height: '2.25rem' }}
        onClick={() => navigateToWorkspace()}
      >
        Load Workspace
      </Button>
    </FlexRow>
  );
};
