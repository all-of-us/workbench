import {Component, Input} from '@angular/core';
import {selectionsStore, wizardStore} from 'app/cohort-search/search-state.service';
import {ClrIcon} from 'app/components/icons';
import {TextInput} from 'app/components/inputs';
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import {WorkspaceData} from 'app/services/workspace-storage.service';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
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
  },
  selectIcon: {
    color: 'rgb(98, 164, 32)',
    cursor: 'pointer'
  },
  selectedIcon: {
    color: 'rgb(98, 164, 32)',
    opacity: 0.4,
    cursor: 'not-allowed'
  }
});

interface ListSearchProps {
  selections: Array<string>;
  workspace: WorkspaceData;
  wizard: any;
}

export const ListSearch = withCurrentWorkspace()(
  class extends React.Component<ListSearchProps, {data: any, selections: Array<string>}> {
    constructor(props: any) {
      super(props);
      this.state = {data: null, selections: selectionsStore.getValue()};
    }

    handleInput = (event) => {
      if (event.key === 'Enter') {
        const {wizard: {domain}, workspace: {cdrVersionId}} = this.props;
        cohortBuilderApi().findCriteriaByDomainAndSearchTerm(
          +cdrVersionId, domain, event.target.value, true
        ).then(resp => {
          const data = resp.items.length ? resp.items : null;
          this.setState({data});
        });
      }
    }

    selectItem = (row: any) => {
      const {wizard} = this.props;
      let {selections} = this.props;
      const paramId = this.getParamId(row);
      if (!selections.includes(paramId)) {
        wizard.item.searchParameters.push({paramId, ...row});
        selections = [paramId, ...selections];
        selectionsStore.next(selections);
        wizardStore.next(wizard);
        // this.setState({selections});
      }
    }

    isSelected = (row: any) => {
      const {selections} = this.props;
      const paramId = this.getParamId(row);
      return selections.includes(paramId);
    }

    getParamId(row: any) {
      return `param${row.conceptId ? (row.conceptId + row.code) : row.id}`;
    }

    render() {
      const {data} = this.state;
      return <div>
        <div style={styles.searchBar}>
          <ClrIcon shape='search'/>
          <TextInput style={styles.searchInput} onKeyPress={this.handleInput} />
        </div>
        {data && <table>
          <thead>
            <tr>
              <th> </th>
              <th>Name</th>
              <th>Vocab</th>
              <th>Count</th>
            </tr>
          </thead>
          <tbody>
            {data.map((row, r) => {
              return <tr key={r}>
                <td>
                  {this.isSelected(row) &&
                    <ClrIcon style={styles.selectedIcon} shape='check-circle' size='20'/>}
                  {!this.isSelected(row) &&
                    <ClrIcon style={styles.selectIcon}
                      shape='plus-circle' size='20'
                      onClick={() => this.selectItem(row)}
                    />}
                </td>
                <td>{row.name}</td>
                <td>{row.type}</td>
                <td>{row.count}</td>
              </tr>;
            })}
          </tbody>
        </table>}
      </div>;
    }
  }
);

@Component ({
  selector: 'crit-list-search',
  template: '<div #root></div>'
})
export class ListSearchComponent extends ReactWrapperBase {
  @Input('selections') selections: ListSearchProps['selections'];
  @Input('wizard') wizard: ListSearchProps['wizard'];
  constructor() {
    super(ListSearch, ['selections', 'wizard']);
  }
}
