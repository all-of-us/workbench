import {ListSearchV2} from 'app/cohort-search/list-search-v2/list-search-v2.component';
import {Selection} from 'app/cohort-search/selection-list/selection-list.component';
import {CriteriaTree} from 'app/cohort-search/tree/tree.component';
import {SpinnerOverlay} from 'app/components/spinners';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles} from 'app/utils';
import {
  attributesSelectionStore,
  currentCohortCriteriaStore,
  currentConceptStore
} from 'app/utils/navigation';
import {Criteria, Domain} from 'generated/fetch';
import * as fp from 'lodash/fp';
import {Growl} from 'primereact/growl';
import * as React from 'react';
import {Subscription} from 'rxjs/Subscription';

const styles = reactStyles({
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
  }
});
const css = `
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

interface Props {
  cohortContext: any;
  conceptSearchTerms?: string;
  selectedSurvey?: string;
  source: string;
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
export class CriteriaSearch extends React.Component<Props, State>  {
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
      treeSearchTerms: props.source === 'concept' ? props.conceptSearchTerms : '',
      loadingSubtree: false
    };
  }

  componentDidMount(): void {
    const {cohortContext: {domain, standard, type}, source} = this.props;
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
    this.setState({backMode, hierarchyNode, mode});
    this.subscription = currentCohortCriteriaStore.subscribe(currentCohortCriteria => {
      if (source === 'criteria') {
        this.setState({selectedCriteriaList: currentCohortCriteria});
      }
    });
    this.subscription.add(currentConceptStore.subscribe(currentConcepts => {
      if (source === 'concept') {
        this.setState({selectedCriteriaList: currentConcepts});
      }
    }));
  }

  componentWillUnmount() {
    this.subscription.unsubscribe();
  }

  get initTree() {
    const {cohortContext: {domain}, source} = this.props;
    return (domain === Domain.PHYSICALMEASUREMENT && source === 'criteria')
      || domain === Domain.SURVEY
        || domain === Domain.VISIT;
  }

  get isConcept() {
    return this.props.source === 'concept';
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

  addSelection = (selectCriteria)  => {
    let criteriaList = this.state.selectedCriteriaList;
    if (criteriaList && criteriaList.length > 0) {
      criteriaList.push(selectCriteria);
    } else {
      criteriaList =  [selectCriteria];
    }
    this.setState({selectedCriteriaList: criteriaList});
    this.isConcept ?  currentConceptStore.next(criteriaList) : currentCohortCriteriaStore.next(criteriaList);
    const growlMessage = this.isConcept ? 'Concept Added' : 'Criteria Added';
    this.growl.show({severity: 'success', detail: growlMessage, closable: false, life: 2000});
    if (!!this.growlTimer) {
      clearTimeout(this.growlTimer);
    }
    // This is to set style display: 'none' on the growl so it doesn't block the nav icons in the sidebar
    this.growlTimer = setTimeout(() => this.setState({growlVisible: false}), 2500);
    this.setState({growlVisible: true});
  }

  getListSearchSelectedIds() {
    const {selectedCriteriaList} = this.state;
    const vale = fp.map(selected => ('param' + selected.conceptId + selected.code), selectedCriteriaList);
    return vale;
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


  setTreeSearchTerms = (input: string) => {
    this.setState({treeSearchTerms: input});
  }

  setAutocompleteSelection = (selection: any) => {
    this.setState({loadingSubtree: true, autocompleteSelection: selection});
  }

  render() {
    const {cohortContext, conceptSearchTerms, selectedSurvey, source} = this.props;
    const {autocompleteSelection, groupSelections, hierarchyNode, loadingSubtree,
      selectedIds, treeSearchTerms, growlVisible} = this.state;
    return <div>
      {loadingSubtree && <SpinnerOverlay/>}
      <div style={loadingSubtree ? styles.loadingSubTree : {height: '100%', minHeight: '15rem'}}>
        <style>{css}</style>
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
            selectedIds={selectedIds}
            selectOption={this.setAutocompleteSelection}
            setSearchTerms={this.setTreeSearchTerms}/>}
        {/* List View (using duplicated version of ListSearch) */}
        <div style={this.searchContentStyle('list')}>
          <ListSearchV2 source={source}
                        hierarchy={this.showHierarchy}
                        searchContext={cohortContext}
                        searchTerms={conceptSearchTerms}
                        select={this.addSelection}
                        selectedIds={this.getListSearchSelectedIds()}/>
        </div>
      </div>
     </div>;
  }
}
