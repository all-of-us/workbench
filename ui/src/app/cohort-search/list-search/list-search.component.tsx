import {Component, Input} from '@angular/core';
import {ClrIcon} from 'app/components/icons';
import {TextInput} from 'app/components/inputs';
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import {WorkspaceData} from 'app/services/workspace-storage.service';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {DomainType} from 'generated/fetch';
import {Column} from 'primereact/column';
import {DataTable} from 'primereact/datatable';
import * as React from 'react';

const styles = reactStyles({
  searchBar: {
    height: '40px',
    width: '100%',
    padding: '7px 14px',
    borderRadius: '5px',
    backgroundColor: '#DFF0FA',
  },
  searchInput: {
    width: '85%',
    height: '1rem',
    marginLeft: '0.5rem',
    padding: '0 0.5rem',
    background: 'transparent',
    border: 0,
  }
});

const columns = [{
  field: 'name',
  header: 'Name'
}, {
  field: 'type',
  header: 'Vocab'
}, {
  field: 'count',
  header: 'Count'
}];

interface ListSearchProps {
  workspace: WorkspaceData;
  wizard: any;
}

export const ListSearch = withCurrentWorkspace()(
  class extends React.Component<ListSearchProps, {data: any}> {
    constructor(props: any) {
      super(props);
      this.state = {data: null};
    }

    componentDidMount(): void {
      // const {wizard: {domain}, workspace: {cdrVersionId}} = this.props;
      // cohortBuilderApi().findCriteriaByDomainAndSearchTerm(
      //   +cdrVersionId, domain, 'amino', true
      // ).then(resp => console.log(resp));
    }

    handleInput = (event) => {
      if (event.key === 'Enter') {
        const {wizard: {domain}, workspace: {cdrVersionId}} = this.props;
        console.log(domain);
        cohortBuilderApi().findCriteriaByDomainAndSearchTerm(
          +cdrVersionId, domain, event.target.value, true
        ).then(resp => {
          console.log(resp);
          const data = resp.items.length ? resp.items : null;
          this.setState({data});
        });
      }
    }

    render() {
      const {data} = this.state;
      const cols = columns.map((col) => {
        return <Column
          key={col.field}
          field={col.field}
          header={col.header}/>;
      });
      return <div>
        <div style={styles.searchBar}>
          <ClrIcon shape='search'/>
          <TextInput style={styles.searchInput} onKeyPress={this.handleInput} />
        </div>
        {data && <DataTable value={data}>
          {cols}
        </DataTable>}
      </div>;
    }
  }
);

@Component ({
  selector: 'crit-list-search',
  template: '<div #root></div>'
})
export class ListSearchComponent extends ReactWrapperBase {
  @Input('wizard') wizard: ListSearchProps['wizard'];
  constructor() {
    super(ListSearch, ['wizard']);
  }
}
