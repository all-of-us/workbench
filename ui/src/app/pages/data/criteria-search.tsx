import * as fp from 'lodash/fp';
import {Growl} from 'primereact/growl';
import * as React from 'react';
import {Subscription} from 'rxjs/Subscription';

import {ListSearch} from 'app/cohort-search/list-search/list-search.component';
import {Selection} from 'app/cohort-search/selection-list/selection-list.component';
import {CriteriaTree} from 'app/cohort-search/tree/tree.component';
import {domainToTitle, typeToTitle} from 'app/cohort-search/utils';
import {Clickable, StyledAnchorTag} from 'app/components/buttons';
import {FlexRowWrap} from 'app/components/flex';
import {SpinnerOverlay} from 'app/components/spinners';
import {AoU} from 'app/components/text-wrappers';
import colors, {addOpacity, colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, withCurrentWorkspace, withUrlParams} from 'app/utils';
import {
  attributesSelectionStore,
  currentCohortCriteriaStore,
  currentCohortStore,
  currentConceptStore,
  setSidebarActiveIconStore
} from 'app/utils/navigation';
import {environment} from 'environments/environment';
import {Criteria, Domain} from 'generated/fetch';

export const LOCAL_STORAGE_KEY_COHORT_CONTEXT = 'CURRENT_COHORT_CONTEXT';
export const LOCAL_STORAGE_KEY_CRITERIA_SELECTIONS = 'CURRENT_CRITERIA_SELECTIONS';

const styles = reactStyles({
  arrowIcon: {
    height: '21px',
    marginTop: '-0.2rem',
    width: '18px'
  },
  backArrow: {
    background: `${addOpacity(colors.accent, 0.15)}`,
    borderRadius: '50%',
    height: '1.5rem',
    lineHeight: '1.6rem',
    textAlign: 'center',
    width: '1.5rem',
  },
  detailExternalLinks: {
    width: '100%',
    lineHeight: '0.75rem',
    textAlign: 'right',
    verticalAlign: 'middle'
  },
  externalLinks: {
    flex: '0 0 calc(55% - 1.25rem)',
    maxWidth: 'calc(55% - 1.25rem)',
    lineHeight: '0.75rem',
    textAlign: 'right',
    verticalAlign: 'middle'
  },
  growl: {
    position: 'absolute',
    right: '0',
    top: 0
  },
  loadingSubTree: {
    height: '100%',
    minHeight: '15rem',
    pointerEvents: 'none',
    opacity: 0.3
  },
  titleBar: {
    alignItems: 'center',
    color: colors.primary,
    margin: '0 0.25rem',
    width: '80%',
    height: '2rem',
  },
  titleHeader: {
    flex: '0 0 calc(45% - 1rem)',
    maxWidth: 'calc(45% - 1rem)',
    lineHeight: '1rem',
    margin: '0 0 0 0.75rem'
  }
});
export const growlCSS = `
  .p-growl {
    position: sticky;
  }
  .p-growl.p-growl-topright {
    height: 1rem;
    width: 6.4rem;
    line-height: 0.7rem;
  }
  .p-growl .p-growl-item-container .p-growl-item .p-growl-image {
    font-size: 1rem !important;
    margin-top: 0.19rem
  }
  .p-growl-item-container:after {
    content:"";
    position: absolute;
    left: 97.5%;
    top: 0.1rem;
    width: 0px;
    height: 0px;
    border-top: 0.5rem solid transparent;
    border-left: 0.5rem solid ` + colorWithWhiteness(colors.success, 0.6) + `;
    border-bottom: 0.5rem solid transparent;
  }
  .p-growl-item-container {
    background-color: ` + colorWithWhiteness(colors.success, 0.6) + `!important;
  }
  .p-growl-item {
    padding: 0rem !important;
    background-color: ` + colorWithWhiteness(colors.success, 0.6) + `!important;
    margin-left: 0.3rem;
  }
  .p-growl-message {
    margin-left: 0.5em
  }
  .p-growl-details {
    margin-top: 0.1rem;
  }
 `;

const arrowIcon = '/assets/icons/arrow-left-regular.svg';

interface Props {
  backFn?: () => void;
  cohortContext: any;
  conceptSearchTerms?: string;
  urlParams: any;
}

interface State {
  backMode: string;
  autocompleteSelection: Criteria;
  growlVisible: boolean;
  groupSelections: Array<number>;
  hierarchyNode: Criteria;
  mode: string;
  selections: Array<Selection>;
  selectedCriteriaList: Array<any>;
  selectedIds: Array<string>;
  treeSearchTerms: string;
  loadingSubtree: boolean;
}

export const CriteriaSearch = fp.flow(withUrlParams(), withCurrentWorkspace())(class extends React.Component<Props, State>  {
  growl: any;
  growlTimer: NodeJS.Timer;
  subscription: Subscription;

  constructor(props: Props) {
    super(props);
    this.state = {
      autocompleteSelection: undefined,
      backMode: 'list',
      growlVisible: false,
      hierarchyNode: undefined,
      groupSelections: [],
      mode: 'list',
      selectedIds: [],
      selections: [],
      selectedCriteriaList: [],
      treeSearchTerms: props.cohortContext.source !== 'cohort' ? props.conceptSearchTerms : '',
      loadingSubtree: false
    };
  }

  componentDidMount(): void {
    const {cohortContext: {domain, standard, source, type}} = this.props;
    if (this.initTree) {
      this.setState({
        backMode: 'tree',
        hierarchyNode: {
          domainId: domain,
          type: type,
          isStandard: standard,
          id: 0,
        } as Criteria,
        mode: 'tree'
      });
    }
    const currentCriteriaStore = source === 'cohort' ? currentCohortCriteriaStore : currentConceptStore;
    this.subscription = currentCriteriaStore.subscribe(selectedCriteriaList => this.setState({selectedCriteriaList}));
    if (source !== 'cohort') {
      const existingCriteria = JSON.parse(localStorage.getItem(LOCAL_STORAGE_KEY_CRITERIA_SELECTIONS));
      if (!!existingCriteria && existingCriteria[0].domainId === domain) {
        currentCriteriaStore.next(existingCriteria);
      }
    }
  }

  componentWillUnmount() {
    this.subscription.unsubscribe();
  }

  get initTree() {
    const {cohortContext: {domain, source}} = this.props;
    return domain === Domain.VISIT
      || (source === 'cohort' && domain === Domain.PHYSICALMEASUREMENT)
      || (source === 'cohort' && domain === Domain.SURVEY);
  }

  get isConcept() {
    const {cohortContext: {source}} = this.props;
    return source === 'concept' || source === 'conceptSetDetails';
  }

  getGrowlStyle() {
    return !this.isConcept ? styles.growl : {...styles.growl, marginRight: '2.5rem', paddingTop: '2.75rem' };
  }

  searchContentStyle(mode: string) {
    let style = {
      display: 'none',
      flex: 1,
      minWidth: '14rem',
      overflowY: 'auto',
      overflowX: 'hidden',
      width: '100%',
      height: '100%',
    } as React.CSSProperties;
    if (this.state.mode === mode) {
      style = {...style, display: 'block', animation: 'fadeEffect 1s'};
    }
    return style;
  }

  showHierarchy = (criterion: Criteria) => {
    this.setState({
      autocompleteSelection: criterion,
      backMode: 'tree',
      hierarchyNode: {...criterion, id: 0},
      mode: 'tree',
      loadingSubtree: true,
      treeSearchTerms: criterion.name
    });
  }

  closeSidebar() {
    attributesSelectionStore.next(undefined);
    setSidebarActiveIconStore.next(null);
  }

  addSelection = (selectCriteria)  => {
    const {cohortContext, cohortContext: {source}, urlParams} = this.props;
    // In case of Criteria/Cohort, close existing attribute sidebar before selecting a new value
    if (!this.isConcept && !!attributesSelectionStore.getValue()) {
      this.closeSidebar();
    }
    let criteriaList = this.state.selectedCriteriaList;
    if (criteriaList && criteriaList.length > 0) {
      criteriaList.push(selectCriteria);
    } else {
      criteriaList = [selectCriteria];
    }
    // Save selections in local storage in case of error or page refresh
    if (source === 'cohort') {
      const {wsid} = urlParams;
      const cohort = currentCohortStore.getValue();
      cohortContext.item.searchParameters = criteriaList;
      const localStorageContext = {
        workspaceId: wsid,
        cohortId: !!cohort ? cohort.id : null,
        cohortContext
      };
      localStorage.setItem(LOCAL_STORAGE_KEY_COHORT_CONTEXT, JSON.stringify(localStorageContext));
    } else {
      localStorage.setItem(LOCAL_STORAGE_KEY_CRITERIA_SELECTIONS, JSON.stringify(criteriaList));
    }
    this.setState({selectedCriteriaList: criteriaList});
    this.isConcept ?  currentConceptStore.next(criteriaList) : currentCohortCriteriaStore.next(criteriaList);
    const growlMessage = this.isConcept ? 'Concept Added' : 'Criteria Added';
    this.growl.show({severity: 'success', detail: growlMessage, closable: false, life: 2000});
    if (!!this.growlTimer) {
      clearTimeout(this.growlTimer);
    }
    // This is to set style display: 'none' on the growl so it doesn't block the nav icons in the sidebar
    this.growlTimer = global.setTimeout(() => this.setState({growlVisible: false}), 2500);
    this.setState({growlVisible: true});
  }

  getListSearchSelectedIds() {
    const {cohortContext: {source}} = this.props;
    const {selectedCriteriaList} = this.state;
    return source === 'cohort' ?
      fp.map(selected => selected.parameterId , selectedCriteriaList) :
      fp.map(selected => ('param' + selected.conceptId + selected.code + selected.isStandard), selectedCriteriaList);
  }

  setScroll = (id: string) => {
    const nodeId = `node${id}`;
    const node = document.getElementById(nodeId);
    if (node) {
      node.scrollIntoView({behavior: 'smooth', block: 'start'});
    }
    this.setState({loadingSubtree: false});
  }

  back = () => {
    if (this.state.mode === 'tree') {
      this.setState({autocompleteSelection: undefined, backMode: 'list', hierarchyNode: undefined, mode: 'list'});
    } else {
      attributesSelectionStore.next(undefined);
      this.setState({mode: this.state.backMode});
    }
  }


  setTreeSearchTerms = (input: string) => {
    this.setState({treeSearchTerms: input});
  }

  setAutocompleteSelection = (selection: any) => {
    this.setState({loadingSubtree: true, autocompleteSelection: selection});
  }

  get showDataBrowserLink() {
    return [Domain.CONDITION, Domain.PROCEDURE, Domain.MEASUREMENT, Domain.DRUG].includes(this.props.cohortContext.domain);
  }

  get domainTitle() {
    const {cohortContext: {domain, type, selectedSurvey}} = this.props;
    if (!!selectedSurvey) {
      return selectedSurvey;
    } else {
      return domain === Domain.PERSON ? typeToTitle(type) : domainToTitle(domain);
    }
  }

  render() {
    const {backFn, cohortContext, cohortContext: {domain, selectedSurvey, source}, conceptSearchTerms} = this.props;
    const {autocompleteSelection, groupSelections, hierarchyNode, loadingSubtree,
      treeSearchTerms, growlVisible} = this.state;
    return <div id='criteria-search-container'>
      {loadingSubtree && <SpinnerOverlay/>}
      <Growl ref={(el) => this.growl = el} style={!growlVisible ? {...styles.growl, display: 'none'} : styles.growl}/>
      <FlexRowWrap style={{...styles.titleBar, marginTop: source === 'cohort' ? '1rem' : 0}}>
        {source !== 'conceptSetDetails' && <React.Fragment>
          <Clickable style={styles.backArrow} onClick={() => backFn()}>
            <img src={arrowIcon} style={styles.arrowIcon} alt='Go back' />
          </Clickable>
          <h2 style={styles.titleHeader}>{this.domainTitle}</h2>
        </React.Fragment>}
        <div style={source === 'conceptSetDetails' ? styles.detailExternalLinks : styles.externalLinks}>
          {domain === Domain.DRUG && <div>
            <StyledAnchorTag
                href='https://mor.nlm.nih.gov/RxNav/'
                target='_blank'
                rel='noopener noreferrer'>
              Explore
            </StyledAnchorTag>
            &nbsp;drugs by brand names outside of <AoU/>
          </div>}
          {domain === Domain.SURVEY && <div>
            Find more information about each survey in the&nbsp;
            <StyledAnchorTag
                href='https://www.researchallofus.org/survey-explorer/'
                target='_blank'
                rel='noopener noreferrer'>
              Survey Explorer
            </StyledAnchorTag>
          </div>}
          {this.showDataBrowserLink && <div>
            Explore Source information on the&nbsp;
            <StyledAnchorTag
                href={environment.publicUiUrl}
                target='_blank'
                rel='noopener noreferrer'>
              Data Browser
            </StyledAnchorTag>
          </div>}
        </div>
      </FlexRowWrap>
      <div style={loadingSubtree ? styles.loadingSubTree : {height: '100%', minHeight: '15rem'}}>
        <style>{growlCSS}</style>
        <Growl ref={(el) => this.growl = el}
               style={!growlVisible ? {...this.getGrowlStyle(), display: 'none'} : this.getGrowlStyle()}/>
        {hierarchyNode && <CriteriaTree
            source={source}
            selectedSurvey={selectedSurvey}
            autocompleteSelection={autocompleteSelection}
            back={this.back}
            groupSelections={groupSelections}
            node={hierarchyNode}
            scrollToMatch={this.setScroll}
            searchTerms={treeSearchTerms}
            select={this.addSelection}
            selectedIds={this.getListSearchSelectedIds()}
            selectOption={this.setAutocompleteSelection}
            setSearchTerms={this.setTreeSearchTerms}/>}
         {/*List View (using duplicated version of ListSearch) */}
        {!this.initTree && cohortContext.domain && <div style={this.searchContentStyle('list')}>
          <ListSearch hierarchy={this.showHierarchy}
                      searchContext={cohortContext}
                      searchTerms={conceptSearchTerms}
                      select={this.addSelection}
                      selectedIds={this.getListSearchSelectedIds()}/>
        </div>}
      </div>
     </div>;
  }
});
