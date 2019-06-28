import {Component} from '@angular/core';
import * as React from 'react';

import {Header} from 'app/components/headers';
import {ClrIcon} from 'app/components/icons';
import {TextInput} from 'app/components/inputs';
import {reactStyles, ReactWrapperBase, withUserProfile} from 'app/utils';

const styles = reactStyles({
  navPanel: {
    display: 'flex', flexDirection: 'column', backgroundColor: '#E7E6F0',
    minWidth: '300px', maxWidth: '300px', marginLeft: '-0.6rem', padding: '1rem'
  },
  searchBar: {
    height: '2rem', fontSize: '16px', lineHeight: '19px', paddingLeft: '2rem',
    marginTop: '0.5rem', borderStyle: 'none'
  },
});

export const WorkspaceLibrary = withUserProfile()
(class extends React.Component<{}, {}> {
  constructor(props) {
    super(props);
  }

  triggerSearch(e) {

  }

  render() {
    return <div style={{display: 'flex', flexDirection: 'row', height: '100%'}}>
      <div style={styles.navPanel}>
        <div style={{display: 'flex', flexDirection: 'row'}}>
          <ClrIcon shape='search' style={{position: 'absolute', height: '1rem', width: '1rem',
            fill: '#216FB4', left: 'calc(1.4rem)', top: '5.5%'}}/>
          <TextInput style={styles.searchBar}
                     placeholder='Search'
                     onKeyDown={e => {this.triggerSearch(e); }}/>
        </div>
      </div>
      <div style={{padding: '1rem'}}>
        <Header>RESEARCHER WORKBENCH WORKSPACE LIBRARY</Header>
      </div>
    </div>;
  }
});

@Component({
  template: '<div #root style="height: 100%"></div>'
})
export class WorkspaceLibraryComponent extends ReactWrapperBase {
  constructor() {
    super(WorkspaceLibrary, []);
  }
}
