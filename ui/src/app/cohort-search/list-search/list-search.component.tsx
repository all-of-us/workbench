import {Component} from '@angular/core';
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import {WorkspaceData} from 'app/services/workspace-storage.service';
import {ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {DomainType} from 'generated/fetch';
import * as React from 'react';

export const ListSearch = withCurrentWorkspace()(
  class extends React.Component<{workspace: WorkspaceData}> {
    constructor(props: any) {
      super(props);
      this.state = {};
    }

    componentDidMount(): void {
      const {cdrVersionId} = this.props.workspace;
      cohortBuilderApi().findCriteriaByDomainAndSearchTerm(
        +cdrVersionId, DomainType[DomainType.MEASUREMENT], 'amino', true
      ).then(resp => console.log(resp));
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
