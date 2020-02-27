import {Component} from '@angular/core';
import {Button} from 'app/components/buttons';
import {FlexRow} from 'app/components/flex';
import {TextInput} from 'app/components/inputs';
import colors from 'app/styles/colors';
import {ReactWrapperBase, UrlParamsProps, withUrlParams} from 'app/utils';
import {navigate} from 'app/utils/navigation';
import * as React from 'react';

interface State {
  workspaceNamespace: string;
}

class AdminWorkspaceSearchImpl extends React.Component<UrlParamsProps, State> {
  constructor(props) {
    super(props);

    this.state = {
      workspaceNamespace: '',
    };
  }

  navigateToWorkspace() {
    navigate(['/admin/workspaces/' + this.state.workspaceNamespace]);
  }

  render() {
    return <FlexRow style={{justifyContent: 'flex-start', alignItems: 'center', marginTop: '1rem'}}>
        <label style={{color: colors.primary, marginRight: '1rem'}}>Workspace namespace</label>
        <TextInput
            style={{
              width: '10rem',
              marginRight: '1rem'
            }}
            onChange={value => this.setState({workspaceNamespace: value})}
            onKeyDown={(event: KeyboardEvent) => {
              if (event.key === 'Enter') {
                this.navigateToWorkspace();
              }
            }}
        />
        <Button
            style={{height: '1.5rem'}}
            onClick={() => this.navigateToWorkspace()}
        >
          Load Workspace
        </Button>
      </FlexRow>;
  }
}

export const AdminWorkspaceSearch = withUrlParams()(AdminWorkspaceSearchImpl);

@Component({
  template: '<div #root></div>'
})
export class AdminWorkspaceSearchComponent extends ReactWrapperBase {
  constructor() {
    super(AdminWorkspaceSearch, []);
  }
}

