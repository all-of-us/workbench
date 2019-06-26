import {Component} from '@angular/core';
import * as React from 'react';

import {ReactWrapperBase, withUserProfile} from 'app/utils';

export const WorkspaceLibrary = withUserProfile()
(class extends React.Component<{}, {}> {
  constructor(props) {
    super(props);
  }
});


@Component({
  template: '<div #root></div>'
})
export class WorkspaceLibraryComponent extends ReactWrapperBase {
  constructor() {
    super(WorkspaceLibrary, []);
  }
}
