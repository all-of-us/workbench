import {Component} from '@angular/core';
import {WorkspaceData} from 'app/services/workspace-storage.service';
import {ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import * as React from 'react';

export const ListSearch = withCurrentWorkspace()(
  class extends React.Component<{workspace: WorkspaceData}> {
    constructor(props: any) {
      super(props);
      this.state = {};
    }

    render() {
      return <div>List search!!!</div>;
    }
  }
);

@Component ({
  selector: 'crit-list-search',
  template: '<div #root></div>'
})
export class ListSearchComponent extends ReactWrapperBase {
  constructor() {
    super(ListSearch, []);
  }
}
