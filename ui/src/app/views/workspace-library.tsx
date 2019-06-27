import {Component} from '@angular/core';
import * as React from 'react';

import {reactStyles, ReactWrapperBase, withUserProfile} from 'app/utils';

const styles = reactStyles({
  navPanel: {
    display: 'flex', flexDirection: 'column', backgroundColor: '#E7E6F0',
    minWidth: '300px', maxWidth: '300px', marginLeft: '-0.6rem', padding: '1rem'
  }
});

export const WorkspaceLibrary = withUserProfile()
(class extends React.Component<{}, {}> {
  constructor(props) {
    super(props);
  }

  render() {
    return <div style={{display: 'flex', flexDirection: 'row', height: '100%'}}>
      <div style={styles.navPanel}>
        nav
      </div>
      <div>
        Researcher Workbench Workspace Library
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
