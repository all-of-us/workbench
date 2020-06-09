import {Component, Input} from '@angular/core';
import * as React from 'react';

import {AttributesPage} from 'app/cohort-search/attributes-page/attributes-page.component';
import {Demographics} from 'app/cohort-search/demographics/demographics.component';
import {ListSearch} from 'app/cohort-search/list-search/list-search.component';
import {ListModifierPage} from 'app/cohort-search/modifier-page/modifier-page.component';
import {searchRequestStore} from 'app/cohort-search/search-state.service';
import {SelectionList} from 'app/cohort-search/selection-list/selection-list.component';
import {CriteriaTree} from 'app/cohort-search/tree/tree.component';
import {domainToTitle, generateId, stripHtml, typeToTitle} from 'app/cohort-search/utils';
import {ClrIcon} from 'app/components/icons';
import {SpinnerOverlay} from 'app/components/spinners';
import {ReactWrapperBase} from 'app/utils';
import {triggerEvent} from 'app/utils/analytics';
import {environment} from 'environments/environment';
import {Criteria, CriteriaType, DomainType, SearchParameter, TemporalMention, TemporalTime} from 'generated/fetch';

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
  open: boolean;
  selectedIds: Array<string>;
  selections: Array<Selection>;
  title: string;
  treeSearchTerms: string;
}

export class CBModal extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      attributesNode: undefined,
      autocompleteSelection: undefined,
      backMode: undefined,
      conceptType: null,
      count: 0,
      disableFinish: false,
      groupSelections: [],
      hierarchyNode: undefined,
      loadingSubtree: false,
      mode: 'list',
      open: false,
      selectedIds: [],
      selections: [],
      title: '',
      treeSearchTerms: '',
    };
  }

  componentDidMount(): void {
    const {searchContext: {domain, item, standard, type}} = this.props;
    // reset to default each time the modal is opened
    if (!this.state.open) {
      const selections = item.searchParameters;
      const selectedIds = selections.map(s => s.parameterId);
      if (type === CriteriaType.DECEASED) {
        this.selectDeceased();
      } else {
        const title = domain === DomainType.PERSON ? typeToTitle(type) : domainToTitle(domain);
        let backMode, hierarchyNode, mode;
        if (this.initTree) {
          hierarchyNode = {
            domainId: domain,
            type: type,
            isStandard: standard,
            id: 0,
          };
          backMode = 'tree';
          mode = 'tree';
        } else {
          backMode = 'list';
          mode = 'list';
        }
        this.setState({backMode, hierarchyNode, mode, open: true, selectedIds, selections, title});
      }
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
      const group = initGroup(role, item);
      searchRequest[role].push(group);
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
      domain !== DomainType.PERSON &&
      domain !== DomainType.SURVEY;
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

  get treeClass() {
    const {searchContext: {domain, type}} = this.props;
    if (domain === DomainType.PERSON) {
      return type === CriteriaType.AGE ? 'col-md-12' : 'col-md-6';
    }
    return 'col-md-8';
  }

  get sidebarClass() {
    return this.props.searchContext.domain === DomainType.PERSON ? 'col-md-6' : 'col-md-4';
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
      parameterId: '',
      type: CriteriaType.DECEASED.toString(),
      name: 'Deceased',
      group: false,
      domain: DomainType.PERSON.toString(),
      hasHierarchy: false,
      ancestorData: false,
      standard: true,
      attributes: []
    } as SearchParameter;
    this.addSelection(param);
    this.finish();
  }

  render() {
    const {closeSearch, searchContext, searchContext: {domain, type}, setSearchContext} = this.props;
    const {attributesNode, autocompleteSelection, conceptType, count, disableFinish, groupSelections, hierarchyNode, loadingSubtree, mode,
      selectedIds, selections, title, treeSearchTerms} = this.state;
    let modalClass = 'crit-modal-content';
    if (domain === DomainType.PERSON) {
      modalClass += ' demographics';
    }
    if (type === CriteriaType.AGE) {
      modalClass += ' age';
    }
    let treeClass = 'panel';
    if (loadingSubtree) {
      treeClass += ' disableTree';
    }
    if (['tree', 'list', 'modifiers', 'attributes'].includes(mode)) {
      treeClass += 'show';
    }
    return !!searchContext ? <div className='crit-modal-container'>
      <div className={modalClass}>
        <div className='container title-margin'>
          <div className='row'>
            <div className={'col padding-zero' + this.treeClass}>
              <div className='title-bar'>
                <div className='btn-group btn-link'>
                  {!attributesNode ? <button
                    className={'btn tab' + (mode === 'list' || mode === 'tree') ? 'active' : ''}
                    disabled={selections.length !== 0 && conceptType === 'standard'}
                    onClick={() => this.setState({mode: this.state.backMode})}>
                    {title}
                  </button>
                  : <button
                    className={'btn tab' + (mode === 'attributes') ? 'active' : ''}
                    onClick={() => this.setState({mode: 'attributes'})}>
                    {this.attributeTitle}
                  </button>}
                  {this.showModifiers && <React.Fragment>
                    <div className='vbar'></div>
                    <button type='button'
                      className={'btn tab' + (mode === 'modifiers') ? 'active' : ''}
                      disabled={selections.length === 0}
                      onClick={() => this.setMode('modifiers')}>
                      Modifiers
                    </button>
                  </React.Fragment>}
                </div>
                <div className='link-container'>
                  <div className='inner-link-container'>
                    {domain === DomainType.DRUG && <div>
                      <a href='https://mor.nlm.nih.gov/RxNav/' target='_blank' rel='noopener noreferrer'>
                        Explore
                      </a>
                      drugs by brand names outside of <i>All of Us</i>.
                    </div>}
                    {this.showDataBrowserLink && <div>
                      Explore Source information on the
                      <a href={environment.publicUiUrl} target='_blank' rel='noopener noreferrer'>Data Browser.</a>
                    </div>}
                  </div>
                </div>
                {mode === 'attributes' && <button onClick={this.back} className='btn btn-link btn-icon'>
                  <ClrIcon size='24' shape='close'/>
                </button>}
              </div>
              <div className='content left'>
                {domain === DomainType.PERSON ? <Demographics
                  count={count}
                  criteriaType={type}
                  select={this.addSelection}
                  selectedIds={selectedIds}
                  selections={selections}/>
                : <React.Fragment>
                  {loadingSubtree && <SpinnerOverlay/>}
                  <div id='tree' className={treeClass}>
                    {/* Tree View */}
                    <div className={'panel-left' + mode === 'tree' ? 'show' : ''}>
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
                    <div className={'panel-left' + mode === 'list' ? 'show' : ''}>
                      <ListSearch hierarchy={this.showHierarchy}
                        searchContext={searchContext}
                        select={this.addSelection}
                        selectedIds={selectedIds}
                        setAttributes={this.setAttributes}/>
                    </div>
                    {/* Modifiers Page */}
                    <div className={'panel-left' + mode === 'list' ? 'modifiers' : ''}>
                      {this.showModifiers && <ListModifierPage
                        disabled={this.modifiersFlag}
                        searchContext={searchContext}
                        selections={selections}
                        setSearchContext={setSearchContext}/>}
                    </div>
                       {/* Attributes Page */}
                    <div className={'panel-left' + mode === 'list' ? 'attributes' : ''}>
                      {!!attributesNode && <AttributesPage
                        close={this.back}
                        node={attributesNode}
                        select={this.addSelection}/>}
                    </div>
                  </div>
                </React.Fragment>}
                {type === CriteriaType.AGE && <div className='footer'>
                  <button
                    onClick={closeSearch}
                    className='btn btn-link'>
                    Cancel
                  </button>
                  <button
                    onClick={this.finish}
                    className='btn btn-primary'>
                    Finish
                  </button>
                </div>}
              </div>
            </div>
            {type !== CriteriaType.AGE && <div className={'col padding-zero' + this.sidebarClass}>
              <div className='content right'>
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
          </div>
        </div>
      </div>
    </div> : '';
  }
}

@Component({
  selector: 'app-list-modal',
  template: '<div #root></div>'
})
export class ModalComponent extends ReactWrapperBase {
  @Input('closeSearch') closeSearch: Props['closeSearch'];
  @Input('searchContext') searchContext: Props['searchContext'];
  @Input('setSearchContext') setSearchContext: Props['setSearchContext'];
  constructor() {
    super(CBModal, ['closeSearch', 'searchContext', 'setSearchContext']);
  }
}
