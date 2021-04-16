import * as fp from 'lodash/fp';
import * as React from 'react';
import {CSSProperties} from 'react';
import {Key} from 'ts-key-enum';

import {domainToTitle} from 'app/cohort-search/utils';
import {AlertDanger} from 'app/components/alert';
import {Clickable} from 'app/components/buttons';
import {FlexRow} from 'app/components/flex';
import {ClrIcon} from 'app/components/icons';
import {TextInput} from 'app/components/inputs';
import {TooltipTrigger} from 'app/components/popups';
import {Spinner, SpinnerOverlay} from 'app/components/spinners';
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, validateInputForMySQL, withCdrVersions, withCurrentConcept, withCurrentWorkspace} from 'app/utils';
import {triggerEvent} from 'app/utils/analytics';
import {findCdrVersion} from 'app/utils/cdr-versions';
import {
  attributesSelectionStore,
  currentCohortSearchContextStore,
  setSidebarActiveIconStore
} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {
  CdrVersion,
  CdrVersionTiersResponse,
  Criteria,
  CriteriaSubType,
  CriteriaType,
  Domain
} from 'generated/fetch';

const borderStyle = `1px solid ${colorWithWhiteness(colors.dark, 0.7)}`;
const styles = reactStyles({
  searchContainer: {
    width: '80%',
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
  disableSelectIcon: {
    opacity: 0.4,
    cursor: 'not-allowed'
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
    borderRadius: '3px 3px 0 0',
    borderBottom: 0,
    tableLayout: 'fixed'
  },
  tableBody: {
    borderRadius: '0 3px 3px 0',
    borderTop: 0
  },
  columnNameHeader: {
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
  columnBodyName: {
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
    verticalAlign: 'middle',
  },
  infoIcon: {
    color: colorWithWhiteness(colors.accent, 0.1),
    marginLeft: '0.25rem'
  },
  clearSearchIcon: {
    color: colors.accent,
    display: 'inline-block',
    float: 'right',
    marginTop: '0.25rem'
  },
  inputAlert: {
    justifyContent: 'space-between',
    padding: '0.2rem',
    width: '64.3%',
  }
});

const columnBodyStyle =  {
  ...styles.columnBodyName,
  width: '9%'
};

const columnHeaderStyle = {
  ...styles.columnNameHeader,
  width: '9%'
};

const columns = [
  {
    name: 'Name',
    tooltip: 'Name from vocabulary',
    style: {...styles.columnNameHeader, width: '31%', borderLeft: 0},
  },
  {
    name: 'Concept Id',
    tooltip: 'Unique ID for concept in OMOP',
    style: {...styles.columnNameHeader, width: '10%', paddingLeft: '0', paddingRight: '0.5rem'},
  },
  {
    name: 'Source/Standard',
    tooltip: 'Indicates if code is an OMOP standard or a source vocabulary code (ICD9/10, CPT, etc)',
    style: {...styles.columnNameHeader, width: '10%', paddingLeft: '0', paddingRight: '0.5rem'},
  },
  {
    name: 'Vocab',
    tooltip: 'Vocabulary for concept',
    style: {...columnHeaderStyle, paddingLeft: '0'},
  },
  {
    name: 'Code',
    tooltip: 'Code from vocabulary',
    style: columnHeaderStyle,
  },
  {
    name: 'Roll-up Count',
    tooltip: 'Number of distinct participants that have either the parent concept OR any of the parent’s child concepts',
    style: columnHeaderStyle,
  },
  {
    name: 'Item Count',
    tooltip: 'Number of distinct participants for this concept',
    style: columnHeaderStyle,
  },
  {
    name: 'View Hierarchy',
    tooltip: null,
    style: {...styles.columnNameHeader, textAlign: 'center', width: '12%'},
  },
];

const searchTrigger = 2;

interface Props {
  cdrVersionTiersResponse: CdrVersionTiersResponse;
  concept?: Array<Criteria>;
  hierarchy: Function;
  searchContext: any;
  searchTerms: string;
  select: Function;
  selectedIds: Array<string>;
  setAttributes: Function;
  workspace: WorkspaceData;
}

interface State {
  apiError: boolean;
  cdrVersion: CdrVersion;
  data: any;
  hoverId: string;
  ingredients: any;
  inputErrors: Array<string>;
  loading: boolean;
  searching: boolean;
  searchTerms: string;
  standardOnly: boolean;
  sourceMatch: any;
  standardData: any;
  totalCount: number;
}

const tableBodyOverlayStyle = `
  body .tablebody {
    overflow-y: overlay
  }
`;

export const ListSearch = fp.flow(withCdrVersions(), withCurrentWorkspace(), withCurrentConcept())(
  class extends React.Component<Props, State> {
    constructor(props: Props) {
      super(props);
      this.state = {
        apiError: false,
        cdrVersion: undefined,
        data: null,
        ingredients: {},
        inputErrors: [],
        hoverId: undefined,
        loading: false,
        searching: false,
        searchTerms: props.searchTerms,
        standardOnly: false,
        sourceMatch: undefined,
        standardData: null,
        totalCount: null
      };
    }
    componentDidMount(): void {
      const {cdrVersionTiersResponse, searchTerms, searchContext: {source}, workspace: {cdrVersionId}} = this.props;
      this.setState({cdrVersion: findCdrVersion(cdrVersionId, cdrVersionTiersResponse)});
      if (source === 'conceptSetDetails') {
        this.setState({data: this.props.concept});
      } else {
        const searchString = searchTerms || '';
        this.getResults(searchString);
      }
    }

    componentDidUpdate(prevProps: Readonly<Props>, prevState: Readonly<State>, snapshot?: any) {
      const {concept, searchContext: {source}} = this.props;
      const {searching} = this.state;
      if (source === 'conceptSetDetails' && prevProps.concept !== concept && !searching) {
        this.setState({data: concept});
      }
    }

    handleInput = (event: any) => {
      const {key, target: {value}} = event;
      if (key === Key.Enter) {
        if (value.trim().length < searchTrigger) {
          this.setState({inputErrors: ['Minimum criteria search length is two characters']});
        } else {
          const inputErrors = validateInputForMySQL(value);
          if (inputErrors.length > 0) {
            this.setState({inputErrors});
          } else {
            const {searchContext} = this.props;
            triggerEvent(`Cohort Builder Search - ${domainToTitle(searchContext.domain)}`, 'Search', value);
            if (searchContext.source === 'concept') {
              // Update search terms so they will persist if user returns to concept homepage
              currentCohortSearchContextStore.next({...searchContext, searchTerms: value});
            }
            this.getResults(value);
          }
        }
      }
    }

    getResults = async(value: string) => {
      let sourceMatch;
      try {
        this.setState({data: null, apiError: false, inputErrors: [], loading: true, searching: true, standardOnly: false});
        const {searchContext: {domain, source, selectedSurvey}, workspace: {cdrVersionId}} = this.props;
        const surveyName = selectedSurvey || 'All';
        const resp = await cohortBuilderApi().findCriteriaByDomainAndSearchTerm(+cdrVersionId, domain, value.trim(), surveyName);
        const data = source !== 'cohort' && this.isSurvey
          ? resp.items.filter(survey => survey.subtype === CriteriaSubType.QUESTION.toString())
          : resp.items;
        if (data.length && this.checkSource) {
          sourceMatch = data.find(item => item.code.toLowerCase() === value.trim().toLowerCase() && !item.isStandard);
          if (sourceMatch) {
            const stdResp = await cohortBuilderApi().findStandardCriteriaByDomainAndConceptId(+cdrVersionId, domain, sourceMatch.conceptId);
            this.setState({standardData: stdResp.items});
          }
        }
        this.setState({data, totalCount: resp.totalCount});
      } catch (err) {
        this.setState({apiError: true});
      } finally {
        this.setState({loading: false, sourceMatch});
      }
    }

    showStandardResults() {
      this.trackEvent('Standard Vocab Hyperlink');
      this.setState({standardOnly: true});
    }

    get checkSource() {
      return [Domain.CONDITION, Domain.PROCEDURE].includes(this.props.searchContext.domain);
    }

    selectIconDisabled() {
      const {searchContext: {source}, selectedIds} = this.props;
      return source !== 'cohort' && selectedIds && selectedIds.length >= 1000;
    }

    selectItem = (row: any) => {
      let param = {parameterId: this.getParamId(row), ...row, attributes: []};
      if (row.domainId === Domain.SURVEY) {
        param = {...param, surveyName: this.props.searchContext.selectedSurvey};
      }
      this.props.select(param);
    }

    setAttributes(node: any) {
      attributesSelectionStore.next(node);
      setSidebarActiveIconStore.next('criteria');
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
      return `param${row.conceptId ? (row.conceptId + row.code) : row.id}${row.isStandard}`;
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

    get textInputPlaceholder() {
      const {searchContext: {domain, source, selectedSurvey}} = this.props;
      switch (source) {
        case 'concept':
          return `Search ${!!selectedSurvey ? selectedSurvey : domainToTitle(domain)} by code or description`;
        case 'conceptSetDetails':
          return `Search ${this.isSurvey ? 'across all ' : ''}${domainToTitle(domain)} by code or description`;
        case 'cohort':
          return `Search ${domainToTitle(domain)} by code or description`;
      }
    }

    get isSurvey() {
      return this.props.searchContext.domain === Domain.SURVEY;
    }

    renderRow(row: any, child: boolean, elementId: string) {
      const {hoverId, ingredients} = this.state;
      const attributes = this.props.searchContext.source === 'cohort' && row.hasAttributes;
      const brand = row.type === CriteriaType.BRAND;
      const displayName = row.name + (brand ? ' (BRAND NAME)' : '');
      const selected = !attributes && !brand && this.props.selectedIds.includes(this.getParamId(row));
      const unselected = !attributes && !brand && !this.isSelected(row);
      const open = ingredients[row.id] && ingredients[row.id].open;
      const loadingIngredients = ingredients[row.id] && ingredients[row.id].loading;
      const columnStyle = child ?
        {...styles.columnBodyName, paddingLeft: '1.25rem'} : styles.columnBodyName;
      const selectIconStyle = this.selectIconDisabled() ? {...styles.selectIcon, ...styles.disableSelectIcon} : styles.selectIcon;
      return <tr style={{height: '1.75rem'}}>
        <td style={{...columnStyle, width: '31%', textAlign: 'left', borderLeft: 0, padding: '0 0.25rem'}}>
          {row.selectable && <div style={styles.selectDiv}>
            {attributes &&
              <ClrIcon style={styles.attrIcon} shape='slider' dir='right' size='20' onClick={() => this.setAttributes(row)}/>
            }
            {selected && <ClrIcon style={styles.selectedIcon} shape='check-circle' size='20'/>}
            {unselected && <ClrIcon style={selectIconStyle} shape='plus-circle' size='16'
                                    onClick={() => this.selectItem(row)}/>}
            {brand && !loadingIngredients &&
              <ClrIcon style={styles.brandIcon}
                shape={'angle ' + (open ? 'down' : 'right')} size='20'
                onClick={() => this.showIngredients(row)}/>
            }
            {loadingIngredients && <Spinner size={16}/>}
          </div>}
          <TooltipTrigger disabled={hoverId !== elementId} content={<div>{displayName}</div>}>
            <div data-test-id='name-column-value'
                 style={styles.nameDiv}
                 onMouseOver={(e) => this.onNameHover(e.target as HTMLDivElement, elementId)}
                 onMouseOut={() => this.setState({hoverId: undefined})}>
              {displayName}
            </div>
          </TooltipTrigger>
        </td>
        <td style={{...columnBodyStyle, width: '10%', paddingRight: '0.5rem'}}>{row.conceptId}</td>
        <td style={{...columnBodyStyle, width: '10%', paddingRight: '0.5rem'}}>{row.isStandard ? 'Standard' : 'Source'}</td>
        <td style={{...columnBodyStyle}}>{!brand && row.type}</td>
        <td style={{...columnBodyStyle, paddingLeft: '0.2rem', paddingRight: '0.5rem'}}>
          <TooltipTrigger disabled={hoverId !== elementId} content={<div>{row.code}</div>}>
            <div data-test-id='code-column-value'
                 style={styles.nameDiv}
                 onMouseOver={(e) => this.onNameHover(e.target as HTMLDivElement, elementId)}
                 onMouseOut={() => this.setState({hoverId: undefined})}>
              {row.code}
            </div>
          </TooltipTrigger></td>
        <td style={{...columnBodyStyle, paddingLeft: '0.2rem'}}>{row.parentCount > -1 && row.parentCount.toLocaleString()}</td>
        <td style={{...columnBodyStyle, paddingLeft: '0.2rem'}}>{row.childCount > -1 && row.childCount.toLocaleString()}</td>
        <td style={{...columnBodyStyle, textAlign: 'center', width: '12%'}}>
          {row.hasHierarchy && <i className='pi pi-sitemap' style={styles.treeIcon} onClick={() => this.showHierarchy(row)}/>}
        </td>
      </tr>;
    }

    renderColumnWithToolTip(columnLabel, toolTip) {
      return <FlexRow>
        <label>{columnLabel}</label>
        <TooltipTrigger side='top' content={<div>{toolTip}</div>}>
          <ClrIcon style={styles.infoIcon} className='is-solid' shape='info-standard'/>
        </TooltipTrigger>
      </FlexRow>;
    }

    render() {
      const {concept, searchContext: {domain, source}} = this.props;
      const {apiError, cdrVersion, data, ingredients, inputErrors, loading, searching, searchTerms, standardOnly, sourceMatch, standardData,
        totalCount} = this.state;
      const showStandardOption = !standardOnly && !!standardData && standardData.length > 0;
      const displayData = standardOnly ? standardData : data;
      return <div style={{overflow: 'auto'}}>
        {this.selectIconDisabled() && <div style={{color: colors.warning, fontWeight: 'bold', maxWidth: '1000px'}}>
          NOTE: Concept Set can have only 1000 concepts. Please delete some concepts before adding more.
        </div>}
        <div style={styles.searchContainer}>
          <div style={styles.searchBar}>
            <ClrIcon shape='search' size='18'/>
            <TextInput data-test-id='list-search-input'
                       style={styles.searchInput}
                       value={searchTerms}
                       placeholder={this.textInputPlaceholder}
                       onChange={(e) => this.setState({searchTerms: e})}
                       onKeyPress={this.handleInput}/>
            {source === 'conceptSetDetails' && searching && <Clickable style={styles.clearSearchIcon}
                onClick={() => this.setState({data: concept, searching: false, searchTerms: ''})}>
              <ClrIcon size={24} shape='times-circle'/>
            </Clickable>}
          </div>
          {inputErrors.map((error, e) => <AlertDanger key={e} style={styles.inputAlert}>
            <span data-test-id='input-error-alert'>{error}</span>
          </AlertDanger>)}
        </div>
        <div style={{display: 'table', height: '100%', width: '100%'}}>
          <div style={styles.helpText}>
            {domain === Domain.DRUG && <div>
              Your search may bring back brand names, generics and ingredients. Only ingredients may be added to your search criteria
            </div>}
            {!loading && <React.Fragment>
              {sourceMatch && !standardOnly && <div>
                There are {sourceMatch.count.toLocaleString()} participants with source code {sourceMatch.code}.
                {showStandardOption && <span> For more results, browse
                  &nbsp;<Clickable style={styles.vocabLink} onClick={() => this.showStandardResults()}>Standard Vocabulary</Clickable>.
                </span>}
              </div>}
              {standardOnly && <div>
                {!!displayData.length && <span>
                  There are {displayData[0].count.toLocaleString()} participants for the standard version of the code you searched
                </span>}
                {!displayData.length && <span>
                  There are no standard matches for source code {sourceMatch.code}
                </span>}
                &nbsp;<Clickable style={styles.vocabLink}
                                 onMouseDown={() => this.trackEvent('Source Vocab Hyperlink')}
                                 onClick={() => this.setState({standardOnly: false})}>
                  Return to source code
                </Clickable>.
              </div>}
              {!!totalCount && <div>
                There are {totalCount.toLocaleString()} results{!!cdrVersion && <span> in {cdrVersion.name}</span>}
              </div>}
            </React.Fragment>}
          </div>
        </div>
        {!loading && !!displayData && <div style={styles.listContainer}>
          {!!displayData.length && <React.Fragment>
            <table className='p-datatable' style={styles.table}>
              <thead className='p-datatable-thead'>
                <tr style={{height: '2rem'}}>
                  {columns.map((column, index) => <th key={index} style={column.style as CSSProperties}>
                    {column.tooltip !== null ? this.renderColumnWithToolTip(column.name, column.tooltip) : column.name}
                  </th>)}
                </tr>
              </thead>
            </table>
            <style>
              {tableBodyOverlayStyle}
            </style>
            <div style={{height: '15rem'}} className='tablebody'>
              <table data-test-id='list-search-results-table'
                     className='p-datatable'
                     style={{...styles.table, ...styles.tableBody}}>
                <tbody className='p-datatable-tbody'>
                {displayData.map((row, index) => {
                  const open = ingredients[row.id] && ingredients[row.id].open;
                  const err = ingredients[row.id] && ingredients[row.id].error;
                  return <React.Fragment key={index}>
                    {this.renderRow(row, false, index)}
                    {open && !err && ingredients[row.id].items.map((item, i) => {
                      return <React.Fragment key={i}>
                        {this.renderRow(item, true, `${index}.${i}`)}
                      </React.Fragment>;
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
              </table>
            </div>
          </React.Fragment>}
          {!standardOnly && !displayData.length && <div>No results found</div>}
        </div>}
        {loading && <SpinnerOverlay/>}
        {apiError && <div style={{...styles.error, ...(domain === Domain.DRUG ? {marginTop: '3.75rem'} : {})}}>
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
