import {Component, Input} from '@angular/core';
import {attributesStore, groupSelectionsStore, selectionsStore, wizardStore} from 'app/cohort-search/search-state.service';
import {domainToTitle} from 'app/cohort-search/utils';
import {Clickable} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import {TextInput} from 'app/components/inputs';
import {Spinner, SpinnerOverlay} from 'app/components/spinners';
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {triggerEvent} from 'app/utils/analytics';
import {WorkspaceData} from 'app/utils/workspace-data';
import {CriteriaType, DomainType} from 'generated/fetch';
import * as React from 'react';

const styles = reactStyles({
  searchContainer: {
    position: 'absolute',
    width: '95%',
    padding: '0.4rem 0',
    background: colors.white,
    zIndex: 10,
  },
  searchBar: {
    height: '40px',
    width: '100%',
    padding: '7px 14px',
    borderRadius: '5px',
    backgroundColor: colorWithWhiteness(colors.secondary, 0.8),
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
  drugsText: {
    fontSize: '12px',
    marginTop: '0.25rem'
  },
  attrIcon: {
    marginRight: '0.5rem',
    color: colors.accent,
    cursor: 'pointer'
  },
  selectIcon: {
    margin: '2px 0.5rem 2px 2px',
    color: colorWithWhiteness(colors.success, -0.5),
    cursor: 'pointer'
  },
  selectedIcon: {
    marginRight: '0.4rem',
    color: colorWithWhiteness(colors.success, -0.5),
    opacity: 0.4,
    cursor: 'not-allowed'
  },
  disabledIcon: {
    marginRight: '0.4rem',
    color: colorWithWhiteness(colors.dark, 0.5),
    opacity: 0.4,
    cursor: 'not-allowed',
    pointerEvents: 'none'
  },
  brandIcon: {
    marginRight: '0.4rem',
    color: colorWithWhiteness(colors.dark, 0.5),
    cursor: 'pointer'
  },
  treeIcon: {
    color: colors.accent,
    cursor: 'pointer',
    fontSize: '1.15rem'
  },
  listContainer: {
    width: '99%',
    margin: '2.75rem 0 1rem',
    fontSize: '12px',
    color: colors.primary,
  },
  vocabLink: {
    display: 'inline-block',
    color: colors.accent,
  },
  table: {
    width: '100%',
    textAlign: 'left',
    border: `1px solid ${colorWithWhiteness(colors.dark, 0.7)}`,
    borderRadius: '3px',
    tableLayout: 'fixed',
  },
  columnHeader: {
    padding: '10px',
    background: colorWithWhiteness(colors.dark, 0.93),
    color: colors.primary,
    border: 0,
    borderBottom: `1px solid ${colorWithWhiteness(colors.dark, 0.7)}`,
    fontWeight: 600,
    textAlign: 'left',
    verticalAlign: 'middle',
    lineHeight: '0.75rem'
  },
  columnBody: {
    background: colors.white,
    padding: '0.4rem',
    verticalAlign: 'top',
    textAlign: 'left',
    border: 0,
    borderBottom: `1px solid ${colorWithWhiteness(colors.dark, 0.7)}`,
    color: colors.primary,
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
    background: colors.warning,
    color: colors.white,
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
        const {wizard: {domain}} = this.props;
        triggerEvent(`Cohort Builder Search - ${domainToTitle(domain)}`, 'Search', value);
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
        this.trackEvent('Standard Vocab Hyperlink');
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
      this.trackEvent('More Info');
      this.props.hierarchy(row);
    }

    showIngredients = (row: any) => {
      const {ingredients} = this.state;
      if (ingredients[row.id]) {
        if (ingredients[row.id].error) {
          ingredients[row.id] = undefined;
        } else {
          ingredients[row.id].open = !ingredients[row.id].open;
        }
        this.setState({ingredients});
      } else {
        try {
          ingredients[row.id] = {open: false, loading: true, error: false, items: []};
          this.setState({ingredients});
          const {workspace: {cdrVersionId}} = this.props;
          cohortBuilderApi()
            .getDrugIngredientByConceptId(+cdrVersionId, row.conceptId)
            .then(resp => {
              ingredients[row.id] = {
                open: true,
                loading: false,
                error: false,
                items: resp.items
              };
              this.setState({ingredients});
            });
        } catch (error) {
          console.error(error);
          ingredients[row.id] = {open: true, loading: false, error: true, items: []};
          this.setState({ingredients});
        }
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

    trackEvent = (label: string) => {
      const {wizard: {domain}} = this.props;
      triggerEvent(
        'Cohort Builder Search',
        'Click',
        `${label} - ${domainToTitle(domain)} - Cohort Builder Search`
      );
    }

    renderRow(row: any, child: boolean) {
      const {ingredients} = this.state;
      const attributes = row.hasAttributes;
      const brand = row.type === CriteriaType.BRAND;
      const selected = !attributes && !brand && this.isSelected(row);
      const unselected = !attributes && !brand && !this.isSelected(row);
      const open = ingredients[row.id] && ingredients[row.id].open;
      const loadingIngredients = ingredients[row.id] && ingredients[row.id].loading;
      const columnStyle = child ?
        {...styles.columnBody, paddingLeft: '1.25rem'} : styles.columnBody;
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
          <div style={styles.nameDiv}>{row.name}{brand && <span> (BRAND NAME)</span>}</div>
        </td>
        <td style={styles.columnBody}>{row.code}</td>
        <td style={styles.columnBody}>{!brand && row.type}</td>
        <td style={styles.columnBody}>{row.count > -1 && row.count.toLocaleString()}</td>
        <td style={{...styles.columnBody, padding: '0.2rem'}}>
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
      const listStyle = domain === DomainType.DRUG ? {...styles.listContainer, marginTop: '3.75rem'}
        : styles.listContainer;
      return <div style={{overflow: 'auto'}}>
        <div style={styles.searchContainer}>
          <div style={styles.searchBar}>
            <ClrIcon shape='search' size='18'/>
            <TextInput style={styles.searchInput}
              placeholder={`Search ${domainToTitle(domain)} by code or description`}
              onKeyPress={this.handleInput} />
          </div>
          {domain === DomainType.DRUG && <div style={styles.drugsText}>
            Your search may bring back brand name drugs. You must select ingredients to include them
             in your cohort.
          </div>}
        </div>
        {!loading && data && <div style={listStyle}>
          {results === 'all' && sourceMatch && <div style={{marginBottom: '0.75rem'}}>
            There are {sourceMatch.count.toLocaleString()} participants with source code
            &nbsp;{sourceMatch.code}. For more results, browse
            &nbsp;<Clickable style={styles.vocabLink}
              onClick={() => this.getStandardResults()}>
              Standard Vocabulary
            </Clickable>.
          </div>}
          {results === 'standard' && <div style={{marginBottom: '0.75rem'}}>
            {!!data.length && <span>
              There are {data[0].count.toLocaleString()} participants for the standard version of
               the code you searched.
            </span>}
            {!data.length && <span>
              There are no standard matches for source code {sourceMatch.code}.
            </span>}
            &nbsp;<Clickable style={styles.vocabLink}
              onMouseDown={() => this.trackEvent('Source Vocab Hyperlink')}
              onClick={() => this.getResults(sourceMatch.code)}>
              Return to source code
            </Clickable>.
          </div>}
          {!!data.length && <table className='p-datatable' style={styles.table}>
            <thead className='p-datatable-thead'>
              <tr>
                <th style={styles.columnHeader}>Name</th>
                <th style={{...styles.columnHeader, width: '20%'}}>Code</th>
                <th style={{...styles.columnHeader, width: '10%'}}>Vocab</th>
                <th style={{...styles.columnHeader, width: '10%'}}>Count</th>
                <th style={{...styles.columnHeader, padding: '0.2rem 0.5rem', width: '7%'}}>
                  More Info
                </th>
              </tr>
            </thead>
            <tbody className='p-datatable-tbody'>
              {data.map((row, r) => {
                const open = ingredients[row.id] && ingredients[row.id].open;
                const err = ingredients[row.id] && ingredients[row.id].error;
                return <React.Fragment key={r}>
                  {this.renderRow(row, false)}
                  {open && !err && ingredients[row.id].items.map((item, i) => {
                    return <React.Fragment key={i}>{this.renderRow(item, true)}</React.Fragment>;
                  })}
                  {open && err && <tr>
                    <td colSpan={5}>
                      <div style={{...styles.error, marginTop: 0}}>
                        <ClrIcon style={{margin: '0 0.5rem 0 0.25rem'}} className='is-solid'
                          shape='exclamation-triangle' size='22'/>
                        Sorry, the request cannot be completed. Please try again or contact Support in the left hand navigation.
                      </div>
                    </td>
                  </tr>}
                </React.Fragment>;
              })}
            </tbody>
          </table>}
          {results === 'all' && !data.length && <div>No results found</div>}
        </div>}
        {loading && <SpinnerOverlay/>}
        {error && <div style={{...styles.error, ...(domain === DomainType.DRUG ? {marginTop: '3.75rem'} : {})}}>
          <ClrIcon
            style={{margin: '0 0.5rem 0 0.25rem'}} className='is-solid'
            shape='exclamation-triangle' size='22'/>
          Sorry, the request cannot be completed. Please try again or contact Support in the left hand navigation.
          {results === 'standard' && <Clickable style={styles.vocabLink}
            onClick={() => this.getResults(sourceMatch.code)}>
            &nbsp;Return to source code.
          </Clickable>}
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
