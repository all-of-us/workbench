import * as fp from 'lodash/fp';
import * as React from 'react';
import {Key} from 'ts-key-enum';

import {domainToTitle} from 'app/cohort-search/utils';
import {Button, Clickable, StyledAnchorTag} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import {TextInput} from 'app/components/inputs';
import {TooltipTrigger} from 'app/components/popups';
import {Spinner, SpinnerOverlay} from 'app/components/spinners';
import {AoU} from 'app/components/text-wrappers';
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, withCdrVersions, withCurrentWorkspace} from 'app/utils';
import {triggerEvent} from 'app/utils/analytics';
import {attributesSelectionStore, setSidebarActiveIconStore} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {environment} from 'environments/environment';
import {CdrVersion, CdrVersionListResponse, CriteriaType, DomainType} from 'generated/fetch';

const borderStyle = `1px solid ${colorWithWhiteness(colors.dark, 0.7)}`;
const styles = reactStyles({
  searchContainer: {
    width: '65%',
    padding: '0.4rem 0',
    zIndex: 10,
  },
  searchBar: {
    height: '2.1rem',
    width: '100%',
    padding: '7px 14px',
    borderRadius: '5px',
    backgroundColor: colorWithWhiteness(colors.secondary, 0.8),
  },
  searchInput: {
    width: '90%',
    height: '1.5rem',
    marginLeft: '0.25rem',
    padding: '0',
    background: 'transparent',
    border: 0,
    outline: 'none',
  },
  drugsText: {
    marginTop: '0.25rem',
    lineHeight: '0.75rem'
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
    fontSize: '1.15rem',
  },
  listContainer: {
    width: '99%',
    margin: '0.5rem 0 1rem',
    fontSize: '12px',
    color: colors.primary,
  },
  vocabLink: {
    display: 'inline-block',
    color: colors.accent,
  },
  table: {
    width: '100%',
    border: borderStyle,
    borderRadius: '3px',
    tableLayout: 'fixed',
  },
  columnHeader: {
    padding: '0 0 0 0.25rem',
    background: colorWithWhiteness(colors.dark, 0.93),
    color: colors.primary,
    border: 0,
    borderBottom: borderStyle,
    fontWeight: 600,
    textAlign: 'left',
    verticalAlign: 'middle',
    lineHeight: '0.75rem'
  },
  columnBody: {
    background: colors.white,
    verticalAlign: 'middle',
    padding: 0,
    border: 0,
    borderBottom: borderStyle,
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
  },
  helpText: {
    color: colors.primary,
    display: 'table-cell',
    height: '100%',
    lineHeight: '0.75rem',
    verticalAlign: 'middle',
    width: '50%'
  }
});

interface Props {
  cdrVersionListResponse: CdrVersionListResponse;
  hierarchy: Function;
  searchContext: any;
  select: Function;
  selectedIds: Array<string>;
  setAttributes: Function;
  workspace: WorkspaceData;
}

interface State {
  cdrVersion: CdrVersion;
  data: any;
  error: boolean;
  hoverId: string;
  ingredients: any;
  loading: boolean;
  standardOnly: boolean;
  sourceMatch: any;
  standardData: any;
  totalCount: number;
}

export const ListSearchV2 = fp.flow(withCdrVersions(), withCurrentWorkspace())(
  class extends React.Component<Props, State> {
    constructor(props: any) {
      super(props);
      this.state = {
        cdrVersion: undefined,
        data: null,
        error: false,
        ingredients: {},
        hoverId: undefined,
        loading: false,
        standardOnly: false,
        sourceMatch: undefined,
        standardData: null,
        totalCount: null
      };
    }
    componentDidMount(): void {
      const {cdrVersionListResponse, workspace: {cdrVersionId}} = this.props;
      const cdrVersions = cdrVersionListResponse.items;
      this.setState({cdrVersion: cdrVersions.find(cdr => cdr.cdrVersionId === cdrVersionId)});
    }

    handleInput = (event: any) => {
      const {key, target: {value}} = event;
      if (key === Key.Enter && value !== '') {
        const {searchContext: {domain}} = this.props;
        triggerEvent(`Cohort Builder Search - ${domainToTitle(domain)}`, 'Search', value);
        this.getResults(value);
      }
    }

    getResults = async(value: string) => {
      let sourceMatch;
      try {
        this.setState({data: null, error: false, loading: true, standardOnly: false});
        const {searchContext: {domain}, workspace: {cdrVersionId}} = this.props;
        const resp = await cohortBuilderApi().findCriteriaByDomainAndSearchTerm(+cdrVersionId, domain, value.trim());
        const data = resp.items;
        if (data.length && this.checkSource) {
          sourceMatch = data.find(item => item.code.toLowerCase() === value.trim().toLowerCase() && !item.isStandard);
          if (sourceMatch) {
            const stdResp = await cohortBuilderApi().findStandardCriteriaByDomainAndConceptId(+cdrVersionId, domain, sourceMatch.conceptId);
            this.setState({standardData: stdResp.items});
          }
        }
        this.setState({data, totalCount: resp.totalCount});
      } catch (err) {
        this.setState({error: true});
      } finally {
        this.setState({loading: false, sourceMatch});
      }
    }

    showStandardResults() {
      this.trackEvent('Standard Vocab Hyperlink');
      this.setState({standardOnly: true});
    }

    get showDataBrowserLink() {
      return [DomainType.CONDITION, DomainType.PROCEDURE, DomainType.MEASUREMENT, DomainType.DRUG]
        .includes(this.props.searchContext.domain);
    }

    get checkSource() {
      return [DomainType.CONDITION, DomainType.PROCEDURE].includes(this.props.searchContext.domain);
    }

    selectItem = (row: any) => {
      const param = {parameterId: this.getParamId(row), ...row, attributes: []};
      this.props.select(param);
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
            .findDrugIngredientByConceptId(+cdrVersionId, row.conceptId)
            .then(resp => {
              ingredients[row.id] = {open: true, loading: false, error: false, items: resp.items};
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
      const {selectedIds} = this.props;
      const paramId = this.getParamId(row);
      return selectedIds.includes(paramId);
    }

    getParamId(row: any) {
      return `param${row.conceptId ? (row.conceptId + row.code) : row.id}`;
    }

    trackEvent = (label: string) => {
      const {searchContext: {domain}} = this.props;
      triggerEvent('Cohort Builder Search', 'Click', `${label} - ${domainToTitle(domain)} - Cohort Builder Search`);
    }

    onNameHover(el: HTMLDivElement, id: string) {
      if (el.offsetWidth < el.scrollWidth) {
        this.setState({hoverId: id});
      }
    }

    renderRow(row: any, child: boolean, elementId: string) {
      const {hoverId, ingredients} = this.state;
      const attributes = row.hasAttributes;
      const brand = row.type === CriteriaType.BRAND;
      const displayName = row.name + (brand ? ' (BRAND NAME)' : '');
      const selected = !attributes && !brand && this.props.selectedIds.includes(this.getParamId(row));
      const unselected = !attributes && !brand && !this.isSelected(row);
      const open = ingredients[row.id] && ingredients[row.id].open;
      const loadingIngredients = ingredients[row.id] && ingredients[row.id].loading;
      const columnStyle = child ?
        {...styles.columnBody, paddingLeft: '1.25rem'} : styles.columnBody;
      return <tr style={{height: '1.75rem'}}>
        <td style={{...columnStyle, textAlign: 'left', borderLeft: 0, padding: '0 0.25rem'}}>
          {row.selectable && <div style={{...styles.selectDiv}}>
            {attributes &&
              <ClrIcon style={styles.attrIcon} shape='slider' dir='right' size='20' onClick={() => attributesSelectionStore.next(row)}/>
            }
            {selected && <ClrIcon style={styles.selectedIcon} shape='check-circle' size='20'/>}
            {unselected && <ClrIcon style={styles.selectIcon} shape='plus-circle' size='16' onClick={() => this.selectItem(row)}/>}
            {brand && !loadingIngredients &&
              <ClrIcon style={styles.brandIcon}
                shape={'angle ' + (open ? 'down' : 'right')} size='20'
                onClick={() => this.showIngredients(row)}/>
            }
            {loadingIngredients && <Spinner size={16}/>}
          </div>}
          <TooltipTrigger disabled={hoverId !== elementId} content={<div>{displayName}</div>}>
            <div style={styles.nameDiv}
              onMouseOver={(e) => this.onNameHover(e.target as HTMLDivElement, elementId)}
              onMouseOut={() => this.setState({hoverId: undefined})}>
              {displayName}
            </div>
          </TooltipTrigger>
        </td>
        <td style={styles.columnBody}>{row.code}</td>
        <td style={styles.columnBody}>{!brand && row.type}</td>
        <td style={styles.columnBody}>{row.count > -1 && row.count.toLocaleString()}</td>
        <td style={{...styles.columnBody}}>
          {row.hasHierarchy && <i className='pi pi-sitemap' style={styles.treeIcon} onClick={() => this.showHierarchy(row)}/>}
        </td>
      </tr>;
    }

    render() {
      const {searchContext: {domain}, selectedIds} = this.props;
      const {cdrVersion, data, error, ingredients, loading, standardOnly, sourceMatch, standardData, totalCount} = this.state;
      const showStandardOption = !standardOnly && !!standardData && standardData.length > 0;
      const displayData = standardOnly ? standardData : data;
      return <div style={{overflow: 'auto'}}>
        <div style={styles.searchContainer}>
          <div style={styles.searchBar}>
            <ClrIcon shape='search' size='18'/>
            <TextInput style={styles.searchInput}
              placeholder={`Search ${domainToTitle(domain)} by code or description`}
              onKeyPress={this.handleInput} />
          </div>
        </div>
        <div style={{display: 'table', height: '100%', width: '100%'}}>
          <div style={styles.helpText}>
            {!!totalCount && <div>
              There are {totalCount.toLocaleString()} results{!!cdrVersion && <span> in {cdrVersion.name}</span>}.
            </div>}
            {sourceMatch && !standardOnly && <div>
              There are {sourceMatch.count.toLocaleString()} participants with source code {sourceMatch.code}.
              {showStandardOption && <span> For more results, browse
                &nbsp;<Clickable style={styles.vocabLink} onClick={() => this.showStandardResults()}>Standard Vocabulary</Clickable>.
              </span>}
            </div>}
            {standardOnly && <div>
              {!!displayData.length && <span>
                There are {displayData[0].count.toLocaleString()} participants for the standard version of the code you searched.
              </span>}
              {!displayData.length && <span>
                There are no standard matches for source code {sourceMatch.code}.
              </span>}
              &nbsp;<Clickable style={styles.vocabLink}
                               onMouseDown={() => this.trackEvent('Source Vocab Hyperlink')}
                               onClick={() => this.setState({standardOnly: false})}>
              Return to source code
            </Clickable>.
            </div>}
            {domain === DomainType.DRUG && <div style={styles.drugsText}>
              Your search may bring back brand names, generics and ingredients. Only ingredients may be added to your search criteria.
            </div>}
          </div>
          <div style={{...styles.helpText, textAlign: 'right'}}>
            {domain === DomainType.DRUG && <div>
              <StyledAnchorTag
                 href='https://mor.nlm.nih.gov/RxNav/'
                 target='_blank'
                 rel='noopener noreferrer'>
                Explore
              </StyledAnchorTag>
              &nbsp;drugs by brand names outside of <AoU/>.
            </div>}
            {this.showDataBrowserLink && <div>
              Explore Source information on the&nbsp;
              <StyledAnchorTag
                 href={environment.publicUiUrl}
                 target='_blank'
                 rel='noopener noreferrer'>
                Data Browser.
              </StyledAnchorTag>
            </div>}
          </div>
        </div>
        {!loading && !!displayData && <div style={styles.listContainer}>
          {!!displayData.length && <table className='p-datatable' style={styles.table}>
            <thead className='p-datatable-thead'>
              <tr style={{height: '2rem'}}>
                <th style={{...styles.columnHeader, borderLeft: 0}}>Name</th>
                <th style={{...styles.columnHeader, width: '20%'}}>Code</th>
                <th style={{...styles.columnHeader, width: '10%'}}>Vocab</th>
                <th style={{...styles.columnHeader, width: '8%'}}>Count</th>
                <th style={{...styles.columnHeader, textAlign: 'center', width: '12%'}}>View Hierarchy</th>
              </tr>
            </thead>
            <tbody className='p-datatable-tbody'>
              {displayData.map((row, r) => {
                const open = ingredients[row.id] && ingredients[row.id].open;
                const err = ingredients[row.id] && ingredients[row.id].error;
                return <React.Fragment key={r}>
                  {this.renderRow(row, false, r)}
                  {open && !err && ingredients[row.id].items.map((item, i) => {
                    return <React.Fragment key={i}>{this.renderRow(item, true, `${r}.${i}`)}</React.Fragment>;
                  })}
                  {open && err && <tr>
                    <td colSpan={5}>
                      <div style={{...styles.error, marginTop: 0}}>
                        <ClrIcon style={{margin: '0 0.5rem 0 0.25rem'}} className='is-solid' shape='exclamation-triangle' size='22'/>
                        Sorry, the request cannot be completed. Please try again or contact Support in the left hand navigation.
                      </div>
                    </td>
                  </tr>}
                </React.Fragment>;
              })}
            </tbody>
          </table>}
          {!standardOnly && !displayData.length && <div>No results found</div>}
          <Button type='primary'
                  style={{borderRadius: '5px', float: 'right', marginTop: '1rem'}}
                  disabled={!!selectedIds && selectedIds.length === 0}
                  onClick={() => setSidebarActiveIconStore.next('criteria')}>
            Finish & Review
          </Button>
        </div>}
        {loading && <SpinnerOverlay/>}
        {error && <div style={{...styles.error, ...(domain === DomainType.DRUG ? {marginTop: '3.75rem'} : {})}}>
          <ClrIcon style={{margin: '0 0.5rem 0 0.25rem'}} className='is-solid' shape='exclamation-triangle' size='22'/>
          Sorry, the request cannot be completed. Please try again or contact Support in the left hand navigation.
          {standardOnly && <Clickable style={styles.vocabLink} onClick={() => this.getResults(sourceMatch.code)}>
            &nbsp;Return to source code.
          </Clickable>}
        </div>}
      </div>;
    }
  }
);
