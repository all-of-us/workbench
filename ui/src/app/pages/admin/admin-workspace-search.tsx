import {Button} from 'app/components/buttons';
import {FlexRow} from 'app/components/flex';
import {TextInput} from 'app/components/inputs';
import {WithSpinnerOverlayProps} from 'app/components/with-spinner-overlay';
import colors from 'app/styles/colors';
import {UrlParamsProps, withUrlParams} from 'app/utils';
import {navigate} from 'app/utils/navigation';
import * as React from 'react';
import {useEffect, useState} from 'react';

interface Props extends UrlParamsProps, WithSpinnerOverlayProps {}

const navigateToWorkspace = (workspaceNamespace) => {
  navigate(['admin', 'workspaces', workspaceNamespace]);
};

const AdminWorkspaceSearchImpl = (props: Props) => {
  const [workspaceNamespace, setWorkspaceNamespace] = useState();
  useEffect(() => props.hideSpinner(), []);

  return <FlexRow style={{justifyContent: 'flex-start', alignItems: 'center', marginTop: '1rem'}}>
    <label style={{color: colors.primary, marginRight: '1rem'}}>Workspace namespace</label>
    <TextInput
        style={{
          width: '10rem',
          marginRight: '1rem'
        }}
        onChange={value => setWorkspaceNamespace(value)}
        onKeyDown={(event: KeyboardEvent) => {
          if (event.key === 'Enter') {
            navigateToWorkspace(workspaceNamespace);
          }
        }}
    />
    <Button
        style={{height: '1.5rem'}}
        onClick={() => navigateToWorkspace(workspaceNamespace)}
    >
      Load Workspace
    </Button>
  </FlexRow>;
};

export const AdminWorkspaceSearch = withUrlParams()(AdminWorkspaceSearchImpl);

