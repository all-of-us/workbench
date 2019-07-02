import {Component, Input} from '@angular/core';
import {attributesStore, groupSelectionsStore, selectionsStore, wizardStore} from 'app/cohort-search/search-state.service';
import {domainToTitle} from 'app/cohort-search/utils';
import {Button} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import {TextInput} from 'app/components/inputs';
import {Spinner, SpinnerOverlay} from 'app/components/spinners';
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {WorkspaceData} from 'app/utils/workspace-data';
import {CriteriaType, DomainType} from 'generated/fetch';
import * as React from 'react';

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
  brandIcon: {
    marginRight: '0.4rem',
    color: '#9a9a9a',
    cursor: 'pointer'
  },
  treeIcon: {
    color: '#0086C1',
    cursor: 'pointer',
    fontSize: '1.15rem'
  },
  listContainer: {
    width: '99%',
    margin: '2.75rem 0 1rem',
    fontSize: '12px',
    color: '#262262',
  },
  table: {
    width: '100%',
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
    color: '#262262',
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
  },
  error: {
    width: '99%',
    marginTop: '2.75rem',
    padding: '0.25rem',
    background: '#f7981c',
    color: '#ffffff',
    fontSize: '12px',
    borderRadius: '5px',
  }
});

interface Props {
  hierarchy: Function;
  selections: Array<string>;
  wizard: any;
  workspace: WorkspaceData;
}

interface State {
  data: any;
  error: boolean;
  loading: boolean;
  results: string;
  sourceMatch: any;
  ingredients: any;
}

export const ListSearch = withCurrentWorkspace()(
  class extends React.Component<Props, State> {
    constructor(props: any) {
      super(props);
      this.state = {
        data: null,
        error: false,
        loading: false,
        results: 'all',
        sourceMatch: undefined,
        ingredients: {}
      };
    }

    handleInput = (event: any) => {
      const {key, target: {value}} = event;
      if (key === 'Enter') {
        this.getResults(value);
      }
    }

    getResults = async(value: string) => {
      let sourceMatch;
      try {
        this.setState({data: null, error: false, loading: true, results: 'all'});
        const {wizard: {domain}, workspace: {cdrVersionId}} = this.props;
        const resp = await cohortBuilderApi().findCriteriaByDomainAndSearchTerm(
          +cdrVersionId, domain, value
        );
        const data = resp.items;
        if (data.length && this.checkSource) {
          sourceMatch = data.find(
            item => item.code.toLowerCase() === value.toLowerCase() && !item.isStandard
          );
        }
        this.setState({data});
      } catch (err) {
        this.setState({error: true});
      } finally {
        this.setState({loading: false, sourceMatch});
      }
    }

    getStandardResults = async() => {
      try {
        const {wizard: {domain}, workspace: {cdrVersionId}} = this.props;
        const {sourceMatch} = this.state;
        this.setState({data: null, error: false, loading: true, results: 'standard'});
        const resp = await cohortBuilderApi().getStandardCriteriaByDomainAndConceptId(
          +cdrVersionId, domain, sourceMatch.conceptId
        );
        this.setState({data: resp.items});
      } catch (err) {
        this.setState({error: true});
      } finally {
        this.setState({loading: false});
      }
    }

    get checkSource() {
      return [DomainType.CONDITION, DomainType.PROCEDURE].includes(this.props.wizard.domain);
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
        wizard.item.searchParameters.push({parameterId, ...row, attributes: []});
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

    showIngredients = (row: any) => {
      const {ingredients} = this.state;
      if (ingredients[row.id]) {
        ingredients[row.id].open = !ingredients[row.id].open;
        this.setState({ingredients});
      } else {
        ingredients[row.id] = {open: false, loading: true, items: []};
        this.setState({ingredients});
        const {workspace: {cdrVersionId}} = this.props;
        cohortBuilderApi()
          .getDrugIngredientByConceptId(+cdrVersionId, row.conceptId)
          .then(resp => {
            ingredients[row.id] = {
              open: true,
              loading: false,
              items: resp.items
            };
            this.setState({ingredients});
          });
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

    renderRow(row: any, child: boolean) {
      const {ingredients} = this.state;
      const attributes = row.hasAttributes;
      const brand = row.type === CriteriaType.BRAND;
      const selected = !attributes && !brand && this.isSelected(row);
      const unselected = !attributes && !brand && !this.isSelected(row);
      const open = ingredients[row.id] && ingredients[row.id].open;
      const loadingIngredients = ingredients[row.id] && ingredients[row.id].loading;
      const columnStyle = child ? {...styles.columnBody, paddingLeft: '1rem'} : styles.columnBody;
      return <tr>
          <td style={columnStyle}>
            {row.selectable && <div style={{...styles.selectDiv}}>
              {attributes &&
              <ClrIcon style={styles.attrIcon}
                       shape='slider' dir='right' size='20'
                       onClick={() => this.launchAttributes(row)}/>
              }
              {selected &&
              <ClrIcon style={styles.selectedIcon} shape='check-circle' size='20'/>
              }
              {unselected &&
              <ClrIcon style={styles.selectIcon}
                       shape='plus-circle' size='16'
                       onClick={() => this.selectItem(row)}/>
              }
              {brand && !loadingIngredients &&
              <ClrIcon style={styles.brandIcon}
                       shape={'angle ' + (open ? 'down' : 'right')} size='20'
                       onClick={() => this.showIngredients(row)}/>
              }
              {loadingIngredients && <Spinner size={16}/>}
            </div>}
            <div style={{...styles.nameDiv}}>
              <span style={{fontWeight: 'bold'}}>{row.code}</span> {row.name}
            </div>
          </td>
          <td style={columnStyle}>{row.type}</td>
          <td style={columnStyle}>
            {row.count > -1 && row.count.toLocaleString()}
          </td>
          <td style={{...columnStyle, padding: '0.2rem'}}>
            {row.hasHierarchy &&
            <i className='pi pi-sitemap'
               style={styles.treeIcon}
               onClick={() => this.showHierarchy(row)}/>
            }
          </td>
        </tr>;
    }

    render() {
      const {wizard: {domain}} = this.props;
      const {data, error, ingredients, loading, results, sourceMatch} = this.state;
      return <div style={{overflow: 'auto'}}>
        <div style={styles.searchContainer}>
          <div style={styles.searchBar}>
            <ClrIcon shape='search' size='18'/>
            <TextInput style={styles.searchInput}
              placeholder={`Search ${domainToTitle(domain)} by code or description`}
              onKeyPress={this.handleInput} />
          </div>
        </div>
        {!loading && data && <div style={styles.listContainer}>
          {results === 'all' && sourceMatch && <div style={{marginBottom: '0.75rem'}}>
            There are {sourceMatch.count.toLocaleString()} participants with source code
            &nbsp;{sourceMatch.code}. For more results, browse
            &nbsp;<Button type='link'
              style={{display: 'inline-block'}}
              onClick={() => this.getStandardResults()}>
              Standard Vocabulary
            </Button>.
          </div>}
          {results === 'standard' && <div style={{marginBottom: '0.75rem'}}>
            {!!data.length && <span>
              There are {data[0].count.toLocaleString()} participants for the standard version of
               the code you searched.
            </span>}
            {!data.length && <span>
              There are no standard matches for source code {sourceMatch.code}.
            </span>}
            &nbsp;<Button type='link'
              style={{display: 'inline-block'}}
              onClick={() => this.getResults(sourceMatch.code)}>
              Return to source code
            </Button>.
          </div>}
          {!!data.length && <table className='p-datatable' style={styles.table}>
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
                const open = ingredients[row.id] && ingredients[row.id].open;
                return <React.Fragment key={r}>
                  {this.renderRow(row, false)}
                  {open && ingredients[row.id].items.map((item, i) => {
                    return <React.Fragment key={i}>{this.renderRow(item, true)}</React.Fragment>;
                  })}
                </React.Fragment>;
              })}
            </tbody>
          </table>}
          {results === 'all' && !data.length && <div>No results found</div>}
        </div>}
        {loading && <SpinnerOverlay/>}
        {error && <div style={styles.error}>
          <ClrIcon style={{margin: '0 0.5rem 0 0.25rem'}} className='is-solid'
            shape='exclamation-triangle' size='22'/>
          Sorry, the request cannot be completed.
          {results === 'standard' && <Button type='link'
            style={{display: 'inline-block'}}
            onClick={() => this.getResults(sourceMatch.code)}>
            &nbsp;Return to source code.
            </Button>}
        </div>}
      </div>;
    }
  }
);

@Component ({
  selector: 'crit-list-search',
  template: '<div #root></div>'
})
export class ListSearchComponent extends ReactWrapperBase {
  @Input('hierarchy') hierarchy: Props['hierarchy'];
  @Input('selections') selections: Props['selections'];
  @Input('wizard') wizard: Props['wizard'];
  constructor() {
    super(ListSearch, ['hierarchy', 'selections', 'wizard']);
  }
}
