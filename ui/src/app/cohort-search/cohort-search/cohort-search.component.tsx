import {Component, Input} from '@angular/core';
import * as React from 'react';

import {AttributesPage} from 'app/cohort-search/attributes-page/attributes-page.component';
import {Demographics} from 'app/cohort-search/demographics/demographics.component';
import {ListSearch} from 'app/cohort-search/list-search/list-search.component';
import {ModifierPage} from 'app/cohort-search/modifier-page/modifier-page.component';
import {searchRequestStore} from 'app/cohort-search/search-state.service';
import {SelectionList} from 'app/cohort-search/selection-list/selection-list.component';
import {CriteriaTree} from 'app/cohort-search/tree/tree.component';
import {domainToTitle, generateId, stripHtml, typeToTitle} from 'app/cohort-search/utils';
import {Button, Clickable} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import {SpinnerOverlay} from 'app/components/spinners';
import {AoU} from 'app/components/text-wrappers';
import colors, {addOpacity, colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, ReactWrapperBase} from 'app/utils';
import {triggerEvent} from 'app/utils/analytics';
import {environment} from 'environments/environment';
import {Criteria, CriteriaType, DomainType, TemporalMention, TemporalTime} from 'generated/fetch';

const styles = reactStyles({
  backArrow: {
    background: `${addOpacity(colors.accent, 0.15)}`,
    borderRadius: '50%',
    height: '1.5rem',
    lineHeight: '1.4rem',
    textAlign: 'center',
    width: '1.5rem',
  },
  footer: {
    marginTop: '0.5rem',
    padding: '0.45rem 0rem',
    display: 'flex',
    justifyContent: 'flex-end',
  },
  modalContent: {
    display: 'flex',
    flexWrap: 'wrap',
    height: '70vh',
    width: '100%',
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
  panelLeftActive: {
    animation: 'fadeEffect 1s',
    display: 'block',
  },
  tabButton: {
    borderRadius: 0,
    fontSize: '14px',
    height: '2.25rem',
    letterSpacing: 'normal',
    margin: '0 1rem',
    padding: '0 0.5rem 0.25rem',
  },
  tabButtonActive: {
    color: colors.accent,
    borderBottom: `7px solid ${colors.accent}`,
    fontWeight: 'bold',
    padding: '0 0.5rem',
  },
  titleBar: {
    marginBottom: '0.5rem',
    padding: '0rem 1rem',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'flex-end',
    height: '2.5rem',
    marginTop: '0.5rem',
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

interface Selection extends Criteria {
  parameterId: string;
}

interface Props {
  closeSearch: () => void;
  searchContext: any;
  setSearchContext: (context: any) => void;
}

interface State {
  attributesNode: Criteria;
  autocompleteSelection: Criteria;
  backMode: string;
  conceptType: string;
  count: number;
  disableFinish: boolean;
  groupSelections: Array<number>;
  hierarchyNode: Criteria;
  loadingSubtree: boolean;
  mode: string;
  selectedIds: Array<string>;
  selections: Array<Selection>;
  title: string;
  treeSearchTerms: string;
}

export class CohortSearch extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      attributesNode: undefined,
      autocompleteSelection: undefined,
      backMode: 'list',
      conceptType: null,
      count: 0,
      disableFinish: false,
      groupSelections: [],
      hierarchyNode: undefined,
      loadingSubtree: false,
      mode: 'list',
      selectedIds: [],
      selections: [],
      title: '',
      treeSearchTerms: '',
    };
  }

  componentDidMount(): void {
    const {searchContext: {domain, item, standard, type}} = this.props;
    const selections = item.searchParameters;
    const selectedIds = selections.map(s => s.parameterId);
    if (type === CriteriaType.DECEASED) {
      this.selectDeceased();
    } else {
      const title = domain === DomainType.PERSON ? typeToTitle(type) : domainToTitle(domain);
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
      this.setState({backMode, hierarchyNode, mode, selectedIds, selections, title});
    }
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
      this.setState({attributesNode: undefined, mode: this.state.backMode});
    }
  }

  finish = () => {
    const {searchContext: {domain, groupId, item, role, type}} = this.props;
    const {selections} = this.state;
    if (domain === DomainType.PERSON) {
      triggerEvent('Cohort Builder Search', 'Click', `Demo - ${typeToTitle(type)} - Finish`);
    }
    const searchRequest = searchRequestStore.getValue();
    item.searchParameters = selections;
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
    this.props.closeSearch();
  }

  get attributeTitle() {
    const {attributesNode: {domainId, name}} = this.state;
    return domainId === DomainType.PHYSICALMEASUREMENT.toString() ? stripHtml(name) : domainId + ' Detail';
  }

  get showModifiers() {
    const {searchContext: {domain}} = this.props;
    return domain !== DomainType.PHYSICALMEASUREMENT &&
      domain !== DomainType.PERSON;
  }

  get initTree() {
    const {searchContext: {domain}} = this.props;
    return domain === DomainType.PHYSICALMEASUREMENT
      || domain === DomainType.SURVEY
      || domain === DomainType.VISIT;
  }

  get showDataBrowserLink() {
    const {searchContext: {domain}} = this.props;
    const {mode} = this.state;
    return (domain === DomainType.CONDITION
      || domain === DomainType.PROCEDURE
      || domain === DomainType.MEASUREMENT
      || domain === DomainType.DRUG)
      && (mode === 'list' || mode === 'tree');
  }

  get leftColumnStyle() {
    const {searchContext: {domain, type}} = this.props;
    let width = '66.66667%';
    if (domain === DomainType.PERSON) {
      width = type === CriteriaType.AGE ? '100%' : '50%';
    }
    return {
      flex: `0 0 ${width}`,
      height: '100%',
      maxWidth: width,
      position: 'relative',
    } as React.CSSProperties;
  }

  get rightColumnStyle() {
    const width = this.props.searchContext.domain === DomainType.PERSON ? '50%' : '33.33333%';
    return {
      display: 'none',
      flex: `0 0 ${width}`,
      maxWidth: width,
      position: 'relative',
    } as React.CSSProperties;
  }

  panelLeftStyle(mode: string) {
    let style = {
      display: 'none',
      flex: 1,
      minWidth: '14rem',
      overflowY: 'auto',
      overflowX: 'hidden',
      width: '100%',
      height: '100%',
      padding: '0 0.4rem 0 1rem',
    } as React.CSSProperties;
    if (this.state.mode === mode) {
      style = {...style, display: 'block', animation: 'fadeEffect 1s'};
    }
    return style;
  }

  setMode = (newMode: any) => {
    const {searchContext: {domain}} = this.props;
    const {mode} = this.state;
    let {backMode} = this.state;
    if (newMode === 'modifiers') {
      triggerEvent(
        'Cohort Builder Search',
        'Click',
        `Modifiers - ${domainToTitle(domain)} - Cohort Builder Search`
      );
    }
    if (mode !== 'attributes') {
      backMode = mode;
    }
    this.setState({backMode, mode: newMode});
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

  modifiersFlag = (disabled: boolean) => {
    this.setState({disableFinish: disabled});
  }

  setTreeSearchTerms = (input: string) => {
    this.setState({treeSearchTerms: input});
  }

  setAutocompleteSelection = (selection: any) => {
    this.setState({loadingSubtree: true, autocompleteSelection: selection});
  }

  setAttributes = (criterion: Criteria) => {
    this.setState({attributesNode: criterion, backMode: this.state.mode, mode: 'attributes'});
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
    this.setState({groupSelections, selections, selectedIds});
  }

  removeSelection = (param: any) => {
    let {groupSelections, selectedIds, selections} = this.state;
    selectedIds = selectedIds.filter(id => id !== param.parameterId);
    selections = selections.filter(sel => sel.parameterId !== param.parameterId);
    if (param.group) {
      groupSelections = groupSelections.filter(id => id !== param.id);
    }
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
      domainId: DomainType.PERSON.toString(),
      hasAttributes: false,
      selectable: true,
      attributes: []
    } as Selection;
    // wrapping in a timeout here prevents 'ExpressionChangedAfterItHasBeenCheckedError' in the parent component
    // TODO remove timeout once cohort-search component is converted to React
    setTimeout(() => this.setState({selections: [param]}, () => this.finish()));
  }

  render() {
    const {closeSearch, searchContext, searchContext: {domain, type}, setSearchContext} = this.props;
    const {attributesNode, autocompleteSelection, conceptType, count, disableFinish, groupSelections, hierarchyNode, loadingSubtree, mode,
      selectedIds, selections, title, treeSearchTerms} = this.state;
    return !!searchContext ? <div style={styles.modalContent}>
      <div style={{height: '100%', width: '100%'}}>
        <div style={styles.titleBar}>
          <div style={{display: 'inline-flex', marginRight: '0.5rem'}}>
            <Clickable style={styles.backArrow} onClick={() => closeSearch()}>
              <img src={arrowIcon} style={{height: '21px', width: '18px'}} alt='Go back' />
            </Clickable>
            <h2 style={{color: colors.primary, lineHeight: '1.5rem', margin: '0 0 0 0.75rem'}}>
              {title}
            </h2>
          </div>
          <div style={{display: 'table', height: '100%'}}>
            <div style={{display: 'table-cell', height: '100%', verticalAlign: 'middle'}}>
              {domain === DomainType.DRUG && <div>
                <a href='https://mor.nlm.nih.gov/RxNav/' target='_blank' rel='noopener noreferrer'>
                  Explore
                </a>
                &nbsp;drugs by brand names outside of <AoU/>.
              </div>}
              {this.showDataBrowserLink && <div>
                Explore Source information on the&nbsp;
                <a href={environment.publicUiUrl} target='_blank' rel='noopener noreferrer'>Data Browser.</a>
              </div>}
            </div>
          </div>
          {mode === 'attributes' && <Button type='link' onClick={this.back}>
            <ClrIcon size='24' shape='close'/>
          </Button>}
        </div>
        <div style={
          (domain === DomainType.PERSON && type !== CriteriaType.AGE)
            ? {marginBottom: '3.5rem'}
            : {height: 'calc(100% - 3.5rem)'}
        }>
          {domain === DomainType.PERSON ? <div style={{flex: 1, overflow: 'auto'}}>
              <Demographics
                count={count}
                criteriaType={type}
                select={this.addSelection}
                selectedIds={selectedIds}
                selections={selections}/>
            </div>
            : <React.Fragment>
              {loadingSubtree && <SpinnerOverlay/>}
              <div style={loadingSubtree ? {height: '100%', pointerEvents: 'none', opacity: 0.3} : {height: '100%'}}>
                {/* Tree View */}
                <div style={this.panelLeftStyle('tree')}>
                  {hierarchyNode && <CriteriaTree
                      autocompleteSelection={autocompleteSelection}
                      back={this.back}
                      groupSelections={groupSelections}
                      node={hierarchyNode}
                      scrollToMatch={this.setScroll}
                      searchTerms={treeSearchTerms}
                      select={this.addSelection}
                      selectedIds={selectedIds}
                      selectOption={this.setAutocompleteSelection}
                      setAttributes={this.setAttributes}
                      setSearchTerms={this.setTreeSearchTerms}/>}
                </div>
                {/* List View */}
                <div style={this.panelLeftStyle('list')}>
                  <ListSearch hierarchy={this.showHierarchy}
                              searchContext={searchContext}
                              select={this.addSelection}
                              selectedIds={selectedIds}
                              setAttributes={this.setAttributes}/>
                </div>
                {/**
                 Modifiers Page - This will no longer be rendered, leaving here temporarily for reference
                 TODO move to sidebar in RW-4594
                 **/}
                <div style={this.panelLeftStyle('modifiers')}>
                  {this.showModifiers && <ModifierPage
                      disabled={this.modifiersFlag}
                      searchContext={searchContext}
                      selections={selections}
                      setSearchContext={setSearchContext}/>}
                </div>
                {/**
                 Attributes Page - This will no longer be rendered, leaving here temporarily for reference
                 TODO move to sidebar in RW-4595
                 **/}
                <div style={this.panelLeftStyle('attributes')}>
                  {!!attributesNode && <AttributesPage
                      close={this.back}
                      node={attributesNode}
                      select={this.addSelection}/>}
                </div>
              </div>
            </React.Fragment>}
          {type === CriteriaType.AGE && <div style={styles.footer}>
            <Button style={{height: '1.5rem', margin: '0.25rem 0.5rem'}}
                    type='link'
                    onClick={closeSearch}>
              Cancel
            </Button>
            <Button style={{height: '1.5rem', margin: '0.25rem 0.5rem'}}
                    type='primary'
                    onClick={this.finish}>
              Finish
            </Button>
          </div>}
        </div>
      </div>
      {/**
       Selection List - This will no longer be rendered, leaving here temporarily for reference
       TODO move to sidebar in RW-5113
     **/}
      {type !== CriteriaType.AGE && <div style={this.rightColumnStyle}>
        <div style={{height: '100%'}}>
          <SelectionList
              back={this.back}
              close={closeSearch}
              disableFinish={disableFinish}
              domain={domain}
              finish={this.finish}
              removeSelection={this.removeSelection}
              selections={selections}
              setView={this.setMode}
              view={mode}/>
        </div>
      </div>}
    </div> : '';
  }
}

@Component({
  selector: 'app-cohort-search',
  template: '<div #root></div>'
})
export class CohortSearchComponent extends ReactWrapperBase {
  @Input('closeSearch') closeSearch: Props['closeSearch'];
  @Input('searchContext') searchContext: Props['searchContext'];
  @Input('setSearchContext') setSearchContext: Props['setSearchContext'];
  constructor() {
    super(CohortSearch, ['closeSearch', 'searchContext', 'setSearchContext']);
  }
}
