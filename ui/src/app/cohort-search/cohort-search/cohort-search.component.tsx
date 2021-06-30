import {Growl} from 'primereact/growl';
import * as React from 'react';
import {Subscription} from 'rxjs/Subscription';

import {Demographics} from 'app/cohort-search/demographics/demographics.component';
import {searchRequestStore} from 'app/cohort-search/search-state.service';
import {Selection} from 'app/cohort-search/selection-list/selection-list.component';
import {generateId, typeToTitle} from 'app/cohort-search/utils';
import {Button, Clickable} from 'app/components/buttons';
import {FlexRowWrap} from 'app/components/flex';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {CriteriaSearch, growlCSS, LOCAL_STORAGE_KEY_COHORT_CONTEXT} from 'app/pages/data/criteria-search';
import colors, {addOpacity} from 'app/styles/colors';
import {reactStyles, withCurrentCohortSearchContext} from 'app/utils';
import {triggerEvent} from 'app/utils/analytics';
import {
  attributesSelectionStore,
  currentCohortCriteriaStore,
  currentCohortSearchContextStore,
  currentCohortStore,
  setSidebarActiveIconStore,

} from 'app/utils/navigation';
import {CriteriaType, Domain, TemporalMention, TemporalTime} from 'generated/fetch';
import {urlParamsStore} from '../../utils/url-params-store';

const styles = reactStyles({
  arrowIcon: {
    height: '21px',
    marginTop: '-0.2rem',
    width: '18px'
  },
  backArrow: {
    background: `${addOpacity(colors.accent, 0.15)}`,
    borderRadius: '50%',
    display: 'inline-block',
    height: '1.5rem',
    lineHeight: '1.6rem',
    textAlign: 'center',
    width: '1.5rem',
  },
  finishButton: {
    marginTop: '1.5rem',
    borderRadius: '5px',
    bottom: '1rem',
    position: 'absolute',
    right: '3rem',
  },
  growl: {
    position: 'absolute',
    right: '0',
    top: 0
  },
  searchContainer: {
    display: 'flex',
    flexWrap: 'wrap',
    height: '70vh',
    position: 'relative',
    width: '100%',
  },
  searchContent: {
    height: '100%',
    padding: '0 0.5rem',
    position: 'relative',
    width: '100%'
  },
  titleBar: {
    color: colors.primary,
    display: 'table',
    margin: '1rem 0 0.25rem',
    width: '65%',
    height: '1.5rem',
  },
  titleHeader: {
    display: 'inline-block',
    lineHeight: '1.5rem',
    margin: '0 0 0 0.75rem'
  }
});

const arrowIcon = '/assets/icons/arrow-left-regular.svg';

function initGroup(role: string, item: any) {
  return {
    id: generateId(role),
    items: [item],
    count: null,
    temporal: false,
    mention: TemporalMention.ANYMENTION,
    time: TemporalTime.DURINGSAMEENCOUNTERAS,
    timeValue: '',
    timeFrame: '',
    isRequesting: false,
    status: 'active'
  };
}


export function getItemFromSearchRequest(groupId: string, itemId: string, role: string) {
  const searchRequest = searchRequestStore.getValue();
  const groupIndex = searchRequest[role].findIndex(grp => grp.id === groupId);
  return groupIndex > -1 ? searchRequest[role][groupIndex].items.find(it => it.id === itemId) : null;
}

export function saveCriteria(selections?: Array<Selection>) {
  const {domain, groupId, item, role, type} = currentCohortSearchContextStore.getValue();
  if (domain === Domain.PERSON) {
    triggerEvent('Cohort Builder Search', 'Click', `Demo - ${typeToTitle(type)} - Finish`);
  }
  const searchRequest = searchRequestStore.getValue();
  item.searchParameters = selections || currentCohortCriteriaStore.getValue();
  if (groupId) {
    const groupIndex = searchRequest[role].findIndex(grp => grp.id === groupId);
    if (groupIndex > -1) {
      const itemIndex = searchRequest[role][groupIndex].items.findIndex(it => it.id === item.id);
      if (itemIndex > -1) {
        searchRequest[role][groupIndex].items[itemIndex] = item;
      } else {
        searchRequest[role][groupIndex].items.push(item);
      }
    }
  } else {
    searchRequest[role].push(initGroup(role, item));
  }
  searchRequestStore.next(searchRequest);
  currentCohortSearchContextStore.next(undefined);
  currentCohortCriteriaStore.next(undefined);
}

interface Props {
  cohortContext: any;
  selections?: Array<Selection>;
  setUnsavedChanges: (unsavedChanges: boolean) => void;
}

interface State {
  growlVisible: boolean;
  selectedIds: Array<string>;
  selections: Array<Selection>;
  showUnsavedModal: boolean;
  unsavedChanges: boolean;
}

export const CohortSearch = withCurrentCohortSearchContext()(class extends React.Component<Props, State> {
  growl: any;
  growlTimer: NodeJS.Timer;
  subscription: Subscription;
  constructor(props: Props) {
    super(props);
    this.state = {
      growlVisible: false,
      selectedIds: [],
      selections: [],
      showUnsavedModal: false,
      unsavedChanges: false,
    };
  }

  componentDidMount(): void {
    const {cohortContext: {domain, item, type}} = this.props;
    // JSON stringify and parse prevents changes to selections from being passed to the cohortContext
    const selections = JSON.parse(JSON.stringify(item.searchParameters));
    if (type === CriteriaType.DECEASED) {
      this.selectDeceased();
    } else if (domain === Domain.FITBIT) {
      this.selectFitbit();

    } else if (domain === Domain.WHOLEGENOMEVARIANT) {
      this.selectGenome();
    }
    currentCohortCriteriaStore.next(selections);
    this.subscription = currentCohortCriteriaStore.subscribe(newSelections => {
      if (!!newSelections) {
        this.setState({
          selectedIds: newSelections.map(s => s.parameterId),
          selections: newSelections
        }, () => this.setUnsavedChanges());
      }
    });
    // Check for changes when the search context changes, mainly to detect modifier changes
    this.subscription.add(currentCohortSearchContextStore.subscribe(() => this.setUnsavedChanges()));
  }

  componentWillUnmount() {
    this.subscription.unsubscribe();
    currentCohortCriteriaStore.next(undefined);
    localStorage.removeItem(LOCAL_STORAGE_KEY_COHORT_CONTEXT);
  }

  closeSearch() {
    currentCohortSearchContextStore.next(undefined);
    // Delay hiding attributes page until sidebar is closed
    setTimeout(() => attributesSelectionStore.next(undefined), 500);
  }

  setUnsavedChanges() {
    const {cohortContext: {groupId, item, role}} = this.props;
    const {selections} = this.state;
    let unsavedChanges = selections.length > 0;
    if (groupId) {
      const requestItem = getItemFromSearchRequest(groupId, item.id, role);
      if (requestItem) {
        // Use clone to compare to prevent passing changes back to actual selections
        const selectionsClone = JSON.parse(JSON.stringify(selections));
        const sortAndStringify = (params) => JSON.stringify(params.sort((a, b) => a.id - b.id));
        if (this.criteriaIdsLookedUp(requestItem.searchParameters)) {
          // If a lookup has been done, we delete the ids before comparing search parameters and selections
          selectionsClone.forEach(selection => delete selection.id);
        }
        unsavedChanges = sortAndStringify(requestItem.searchParameters) !== sortAndStringify(selectionsClone) ||
          JSON.stringify(item.modifiers) !== JSON.stringify(requestItem.modifiers);
      }
    }
    this.setState({unsavedChanges});
    this.props.setUnsavedChanges(unsavedChanges);
  }

  checkUnsavedChanges() {
    const {unsavedChanges} = this.state;
    if (unsavedChanges) {
      this.setState({showUnsavedModal: true});
    } else {
      this.closeSearch();
    }
  }

  // Checks if a lookup has been done to add the criteria ids to the selections
  criteriaIdsLookedUp(searchParameters: Array<Selection>) {
    const {selections} = this.state;
    return selections.length && searchParameters.length && selections.some(sel => !!sel.id) && searchParameters.some(sel => !sel.id);
  }

  addSelection = (param: any) => {
    const {cohortContext} = this.props;
    let {selectedIds, selections} = this.state;
    if (selectedIds.includes(param.parameterId)) {
      selections = selections.filter(p => p.parameterId !== param.parameterId);
    } else {
      selectedIds = [...selectedIds, param.parameterId];
    }
    selections = [...selections, param];
    currentCohortCriteriaStore.next(selections);
    const {wsid} = urlParamsStore.getValue();
    const cohort = currentCohortStore.getValue();
    cohortContext.item.searchParameters = selections;
    const localStorageContext = {
      workspaceId: wsid,
      cohortId: !!cohort ? cohort.id : null,
      cohortContext
    };
    localStorage.setItem(LOCAL_STORAGE_KEY_COHORT_CONTEXT, JSON.stringify(localStorageContext));
    this.setState({selections, selectedIds});
    this.growl.show({severity: 'success', detail: 'Criteria Added', closable: false, life: 2000});
    if (!!this.growlTimer) {
      clearTimeout(this.growlTimer);
    }
    // This is to set style display: 'none' on the growl so it doesn't block the nav icons in the sidebar
    this.growlTimer = global.setTimeout(() => this.setState({growlVisible: false}), 2500);
    this.setState({growlVisible: true});
  }

  selectDeceased() {
    const param = {
      id: null,
      parentId: null,
      parameterId: '',
      type: CriteriaType.DECEASED.toString(),
      name: 'Deceased',
      group: false,
      domainId: Domain.PERSON.toString(),
      hasAttributes: false,
      selectable: true,
      attributes: []
    } as Selection;
    saveCriteria([param]);
  }

  selectFitbit() {
    const param = {
      id: null,
      parentId: null,
      parameterId: '',
      type: CriteriaType.PPI.toString(),
      name: 'Has any Fitbit data',
      group: false,
      domainId: Domain.FITBIT.toString(),
      hasAttributes: false,
      selectable: true,
      attributes: []
    } as Selection;
    saveCriteria([param]);
  }

  selectGenome() {
    const param = {
      id: null,
      parentId: null,
      parameterId: '',
      type: CriteriaType.PPI.toString(),
      name: 'Whole Genome Variant',
      group: false,
      domainId: Domain.WHOLEGENOMEVARIANT.toString(),
      hasAttributes: false,
      selectable: true,
      attributes: []
    } as Selection;
    saveCriteria([param]);
  }

  render() {
    const {cohortContext, cohortContext: {domain, type}} = this.props;
    const {growlVisible, selectedIds, selections, showUnsavedModal} = this.state;
    return !!cohortContext && <FlexRowWrap style={styles.searchContainer}>
      <style>{growlCSS}</style>
      <Growl ref={(el) => this.growl = el} style={!growlVisible ? {...styles.growl, display: 'none'} : styles.growl}/>
      <div id='cohort-search-container' style={styles.searchContent}>
        {domain === Domain.PERSON && <div style={styles.titleBar}>
          <Clickable data-test-id='cohort-search-back-arrow'
                     style={styles.backArrow}
                     onClick={() => this.checkUnsavedChanges()}>
            <img src={arrowIcon} style={styles.arrowIcon} alt='Go back' />
          </Clickable>
          <h2 style={styles.titleHeader}>{typeToTitle(type)}</h2>
        </div>}
        <div style={
          (domain === Domain.PERSON && type !== CriteriaType.AGE)
            ? {marginBottom: '3.5rem'}
            : {height: 'calc(100% - 3.5rem)'}
        }>
          {domain === Domain.PERSON ? <div data-test-id='demographics' style={{flex: 1, overflow: 'auto'}}>
              <Demographics
                criteriaType={type}
                select={this.addSelection}
                selectedIds={selectedIds}
                selections={selections}/>
            </div>
            : <CriteriaSearch backFn={() => this.checkUnsavedChanges()}
                              cohortContext={cohortContext}/>}
        </div>
      </div>
      <Button type='primary'
              style={styles.finishButton}
              disabled={!!selectedIds && selectedIds.length === 0}
              onClick={() => setSidebarActiveIconStore.next('criteria')}>
        Finish & Review
      </Button>
      {showUnsavedModal && <Modal>
        <ModalTitle>Warning! </ModalTitle>
        <ModalBody data-test-id='cohort-search-unsaved-message'>
          Your cohort has not been saved. If you’d like to save your cohort criteria, please click CANCEL
          and save your changes in the right sidebar.
        </ModalBody>
        <ModalFooter>
          <Button type='link' onClick={() => this.setState({showUnsavedModal: false})}>Cancel</Button>
          <Button type='primary' onClick={() => this.closeSearch()}>Discard Changes</Button>
        </ModalFooter>
      </Modal>}
    </FlexRowWrap>;
  }
});
