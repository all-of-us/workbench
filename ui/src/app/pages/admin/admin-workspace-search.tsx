import { RouteRedirect } from 'app/components/app-router';
import {Button} from 'app/components/buttons';
import {FlexRow} from 'app/components/flex';
import {TextInput} from 'app/components/inputs';
import {WithSpinnerOverlayProps} from 'app/components/with-spinner-overlay';
import colors from 'app/styles/colors';
import * as React from 'react';
import {useEffect, useState} from 'react';

export const AdminWorkspaceSearch = (spinnerProps: WithSpinnerOverlayProps) => {
  const [workspaceNamespace, setWorkspaceNamespace] = useState();
  const [redirect, setRedirect] = useState(false);

  const pathToWorkspace = `/admin/workspaces/${workspaceNamespace}`

  useEffect(() => spinnerProps.hideSpinner(), []);

  return redirect
      ? <RouteRedirect path={pathToWorkspace}/>
      : <FlexRow style={{justifyContent: 'flex-start', alignItems: 'center', marginTop: '1rem'}}>
        <label style={{color: colors.primary, marginRight: '1rem'}}>Workspace namespace</label>
        <TextInput
            style={{
              width: '10rem',
              marginRight: '1rem'
            }}
            onChange={value => setWorkspaceNamespace(value)}
            onKeyDown={(event: KeyboardEvent) => {
              if (event.key === 'Enter') {
                setRedirect(true);
              }
            }}
        />
        <Button
            style={{height: '1.5rem'}}
            path={pathToWorkspace}
        >
          Load Workspace
        </Button>
      </FlexRow>;
};

