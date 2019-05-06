import {Component, Input} from '@angular/core';
import {attributesStore, groupSelectionsStore, selectionsStore, wizardStore} from 'app/cohort-search/search-state.service';
import {ClrIcon} from 'app/components/icons';
import {TextInput} from 'app/components/inputs';
import {SpinnerOverlay} from 'app/components/spinners';
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import {WorkspaceData} from 'app/services/workspace-storage.service';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import * as React from 'react';
import {DomainType} from '../../../generated/fetch';

const styles = reactStyles({
  searchContainer: {
    position: 'absolute',
    width: '95%',
    padding: '0.4rem 0',
    background: '#ffffff',
    zIndex: 10,
  },
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
    marginLeft: '0.25rem',
    padding: '0',
    background: 'transparent',
    border: 0,
    outline: 'none',
  },
  attrIcon: {
    marginRight: '0.5rem',
    color: '#216FB4',
    cursor: 'pointer'
  },
  selectIcon: {
    margin: '2px 0.5rem 2px 2px',
    color: 'rgb(98, 164, 32)',
    cursor: 'pointer'
  },
  selectedIcon: {
    marginRight: '0.4rem',
    color: 'rgb(98, 164, 32)',
    opacity: 0.4,
    cursor: 'not-allowed'
  },
  treeIcon: {
    color: '#0086C1',
    cursor: 'pointer',
    fontSize: '1.15rem'
  },
  table: {
    width: '99%',
    margin: '3rem 0 1rem',
    fontSize: '12px',
    textAlign: 'left',
    border: '1px solid #c8c8c8',
    borderRadius: '3px',
    tableLayout: 'fixed',
  },
  columnHeader: {
    padding: '10px',
    background: '#f4f4f4',
    color: '#262262',
    border: 0,
    borderBottom: '1px solid #c8c8c8',
    fontWeight: 600,
    textAlign: 'left',
    verticalAlign: 'middle',
    lineHeight: '0.75rem'
  },
  columnBody: {
    background: '#ffffff',
    padding: '0.4rem',
    verticalAlign: 'top',
    textAlign: 'left',
    border: 0,
    borderBottom: '1px solid #c8c8c8',
    lineHeight: '0.8rem',
  },
  selectDiv: {
    width: '6%',
    float: 'left',
    lineHeight: '0.6rem',
  },
  nameDiv: {
    width: '94%',
    float: 'left',
    whiteSpace: 'nowrap',
    overflow: 'hidden',
    textOverflow: 'ellipsis'
  }
});

interface ListSearchProps {
  hierarchy: Function;
  selections: Array<string>;
  wizard: any;
  workspace: WorkspaceData;
}

export const ListSearch = withCurrentWorkspace()(
  class extends React.Component<ListSearchProps, {data: any, loading: boolean}> {
    constructor(props: any) {
      super(props);
      this.state = {data: null, loading: false};
    }

    handleInput = (event) => {
      if (event.key === 'Enter') {
        this.setState({data: null, loading: true});
        const {wizard: {domain}, workspace: {cdrVersionId}} = this.props;
        if (domain === DomainType.DRUG) {
          // temp call until new api call is ready
          cohortBuilderApi().getDrugBrandOrIngredientByValue(+cdrVersionId, event.target.value)
            .then(resp => {
              const data = resp.items.length ? resp.items : null;
              this.setState({data, loading: false});
            });
        } else {
          cohortBuilderApi().findCriteriaByDomainAndSearchTerm(
            +cdrVersionId, domain, event.target.value
          ).then(resp => {
            const data = resp.items.length ? resp.items : null;
            this.setState({data, loading: false});
          });
        }
      }
    }

    selectItem = (row: any) => {
      const {wizard} = this.props;
      let {selections} = this.props;
      const parameterId = this.getParamId(row);
      if (!selections.includes(parameterId)) {
        if (row.group) {
          const groups = [...groupSelectionsStore.getValue(), row.id];
          groupSelectionsStore.next(groups);
        }
        wizard.item.searchParameters.push({parameterId, ...row});
        selections = [parameterId, ...selections];
        selectionsStore.next(selections);
        wizardStore.next(wizard);
      }
    }

    launchAttributes = (row: any) => {
      attributesStore.next(row);
    }

    showHierarchy = (row: any) => {
      this.props.hierarchy(row);
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
      const {data, loading} = this.state;
      return <div style={{overflow: 'auto'}}>
        <div style={styles.searchContainer}>
          <div style={styles.searchBar}>
            <ClrIcon shape='search' size='18'/>
            <TextInput style={styles.searchInput} onKeyPress={this.handleInput} />
          </div>
        </div>
        {data && <table className='p-datatable' style={styles.table}>
          <thead className='p-datatable-thead'>
            <tr>
              <th style={styles.columnHeader}>Name</th>
              <th style={{...styles.columnHeader, width: '15%'}}>Vocab</th>
              <th style={{...styles.columnHeader, width: '15%'}}>Count</th>
              <th style={{...styles.columnHeader, padding: '0.2rem 0.5rem', width: '10%'}}>
                More Info
              </th>
            </tr>
          </thead>
          <tbody className='p-datatable-tbody'>
            {data.map((row, r) => {
              return <tr key={r}>
                <td style={styles.columnBody}>
                  {row.selectable && <div style={{...styles.selectDiv}}>
                    {row.hasAttributes &&
                      <ClrIcon style={styles.attrIcon}
                        shape='slider' dir='right' size='20'
                        onClick={() => this.launchAttributes(row)}/>
                    }
                    {!row.hasAttributes && this.isSelected(row) &&
                      <ClrIcon style={styles.selectedIcon} shape='check-circle' size='20'/>
                    }
                    {!row.hasAttributes && !this.isSelected(row) &&
                      <ClrIcon style={styles.selectIcon}
                        shape='plus-circle' size='16'
                        onClick={() => this.selectItem(row)}/>
                    }
                  </div>}
                  <div style={{...styles.nameDiv}}>{row.name}</div>
                </td>
                <td style={styles.columnBody}>{row.type}</td>
                <td style={styles.columnBody}>{row.count.toLocaleString()}</td>
                <td style={{...styles.columnBody, padding: '0.2rem'}}>
                  {row.hasHierarchy &&
                    <i className='pi pi-sitemap'
                      style={styles.treeIcon}
                      onClick={() => this.showHierarchy(row)}/>
                  }
                </td>
              </tr>;
            })}
          </tbody>
        </table>}
        {loading && <SpinnerOverlay />}
      </div>;
    }
  }
);

@Component ({
  selector: 'crit-list-search',
  template: '<div #root></div>'
})
export class ListSearchComponent extends ReactWrapperBase {
  @Input('hierarchy') hierarchy: ListSearchProps['hierarchy'];
  @Input('selections') selections: ListSearchProps['selections'];
  @Input('wizard') wizard: ListSearchProps['wizard'];
  constructor() {
    super(ListSearch, ['hierarchy', 'selections', 'wizard']);
  }
}
