import * as React from 'react';
import {Subscription} from 'rxjs/Subscription';

import {Demographics} from 'app/cohort-search/demographics/demographics.component';
import {searchRequestStore} from 'app/cohort-search/search-state.service';
import {Selection} from 'app/cohort-search/selection-list/selection-list.component';
import {generateId, typeToTitle} from 'app/cohort-search/utils';
import {Button, Clickable} from 'app/components/buttons';
import {FlexRowWrap} from 'app/components/flex';
import {CriteriaSearch} from 'app/pages/data/criteria-search';
import colors, {addOpacity} from 'app/styles/colors';
import {reactStyles, withCurrentCohortSearchContext} from 'app/utils';
import {triggerEvent} from 'app/utils/analytics';
import {
  attributesSelectionStore,
  currentCohortCriteriaStore,
  currentCohortSearchContextStore,
  setSidebarActiveIconStore,
} from 'app/utils/navigation';
import {Criteria, CriteriaType, Domain, TemporalMention, TemporalTime} from 'generated/fetch';

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
  footer: {
    marginTop: '0.5rem',
    padding: '0.45rem 0rem',
    display: 'flex',
    justifyContent: 'flex-end',
  },
  footerButton: {
    height: '1.5rem',
    margin: '0.25rem 0.5rem'
  },
  panelLeft: {
    display: 'none',
    flex: 1,
    minWidth: '14rem',
    overflowY: 'auto',
    overflowX: 'hidden',
    width: '100%',
    height: '100%',
    padding: '0 0.4rem 0 1rem',
  },
  searchContainer: {
    display: 'flex',
    flexWrap: 'wrap',
    height: '70vh',
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
}

interface State {
  autocompleteSelection: Criteria;
  backMode: string;
  count: number;
  disableFinish: boolean;
  groupSelections: Array<number>;
  hierarchyNode: Criteria;
  loadingSubtree: boolean;
  mode: string;
  selectedIds: Array<string>;
  selections: Array<Selection>;
  treeSearchTerms: string;
}

export const CohortSearch = withCurrentCohortSearchContext()(class extends React.Component<Props, State> {
  subscription: Subscription;
  constructor(props: Props) {
    super(props);
    this.state = {
      autocompleteSelection: undefined,
      backMode: 'list',
      count: 0,
      disableFinish: false,
      groupSelections: [],
      hierarchyNode: undefined,
      loadingSubtree: false,
      mode: 'list',
      selectedIds: [],
      selections: [],
      treeSearchTerms: '',
    };
  }

  componentWillUnmount() {
    this.subscription.unsubscribe();
    currentCohortCriteriaStore.next(undefined);
  }

  componentDidMount(): void {
    const {cohortContext: {domain, item, standard, type}} = this.props;
    // JSON stringify and parse prevents changes to selections from being passed to the cohortContext
    const selections = JSON.parse(JSON.stringify(item.searchParameters));
    const selectedIds = selections.map(s => s.parameterId);
    if (type === CriteriaType.DECEASED) {
      this.selectDeceased();
    } else if (domain === Domain.FITBIT) {
      this.selectFitbit();
    } else {
      let {backMode, mode} = this.state;
      let hierarchyNode;
      if (this.initTree) {
        hierarchyNode = {
          domainId: domain,
          type: type,
          isStandard: standard,
          id: 0,
        };
        backMode = 'tree';
        mode = 'tree';
      }
      this.setState({backMode, hierarchyNode, mode, selectedIds, selections});
    }
    currentCohortCriteriaStore.next(selections);
    this.subscription = currentCohortCriteriaStore.subscribe(newSelections => {
      if (!!newSelections) {
        this.setState({
          groupSelections: newSelections.filter(s => s.group).map(s => s.id),
          selectedIds: newSelections.map(s => s.parameterId),
          selections: newSelections
        });
      }
    });
  }

  setScroll = (id: string) => {
    const nodeId = `node${id}`;
    const node = document.getElementById(nodeId);
    if (node) {
      setTimeout(() => node.scrollIntoView({behavior: 'smooth', block: 'center'}), 200);
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

  closeSearch() {
    currentCohortSearchContextStore.next(undefined);
    currentCohortCriteriaStore.next(undefined);
    // Delay hiding attributes page until sidebar is closed
    setTimeout(() => attributesSelectionStore.next(undefined), 500);
  }

  get initTree() {
    const {cohortContext: {domain}} = this.props;
    return domain === Domain.PHYSICALMEASUREMENT
      || domain === Domain.SURVEY
      || domain === Domain.VISIT;
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

  modifiersFlag = (disabled: boolean) => {
    this.setState({disableFinish: disabled});
  }

  setTreeSearchTerms = (input: string) => {
    this.setState({treeSearchTerms: input});
  }

  setAutocompleteSelection = (selection: any) => {
    this.setState({loadingSubtree: true, autocompleteSelection: selection});
  }

  addSelection = (param: any) => {
    let {groupSelections, selectedIds, selections} = this.state;
    if (selectedIds.includes(param.parameterId)) {
      selections = selections.filter(p => p.parameterId !== param.parameterId);
    } else {
      selectedIds = [...selectedIds, param.parameterId];
      if (param.group) {
        groupSelections = [...groupSelections, param.id];
      }
    }
    selections = [...selections, param];
    currentCohortCriteriaStore.next(selections);
    this.setState({groupSelections, selections, selectedIds});
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

  render() {
    const {cohortContext, cohortContext: {domain, type}} = this.props;
    const {count, selectedIds, selections} = this.state;
    return !!cohortContext && <FlexRowWrap style={styles.searchContainer}>
      <div id='cohort-search-container' style={styles.searchContent}>
        {domain === Domain.PERSON && <div style={styles.titleBar}>
          <Clickable style={styles.backArrow} onClick={() => this.closeSearch()}>
            <img src={arrowIcon} style={styles.arrowIcon} alt='Go back' />
          </Clickable>
          <h2 style={styles.titleHeader}>{typeToTitle(type)}</h2>
        </div>}
        <div style={
          (domain === Domain.PERSON && type !== CriteriaType.AGE)
            ? {marginBottom: '3.5rem'}
            : {height: 'calc(100% - 3.5rem)'}
        }>
          {domain === Domain.PERSON ? <div style={{flex: 1, overflow: 'auto'}}>
              <Demographics
                count={count}
                criteriaType={type}
                select={this.addSelection}
                selectedIds={selectedIds}
                selections={selections}/>
            </div>
            : <CriteriaSearch backFn={() => this.closeSearch()}
                              cohortContext={cohortContext}
                              source={'criteria'}/>}
        </div>
      </div>
      <Button type='primary'
              style={styles.finishButton}
              disabled={!!selectedIds && selectedIds.length === 0}
              onClick={() => setSidebarActiveIconStore.next('criteria')}>
        Finish & Review
      </Button>
    </FlexRowWrap>;
  }
});
