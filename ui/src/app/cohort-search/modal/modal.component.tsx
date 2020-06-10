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
import {Button} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import {SpinnerOverlay} from 'app/components/spinners';
import {reactStyles, ReactWrapperBase} from 'app/utils';
import {triggerEvent} from 'app/utils/analytics';
import {environment} from 'environments/environment';
import {Criteria, CriteriaType, DomainType, SearchParameter, TemporalMention, TemporalTime} from 'generated/fetch';

const styles = reactStyles({
  footer: {
    marginTop: '0.5rem',
    padding: '0.45rem 0rem',
    display: 'flex',
    justifyContent: 'flex-end',
  },
  modalContainer: {
    position: 'absolute',
    top: '50%',
    left: '50%',
    overflowY: 'auto',
    transform: 'translate(-50%, -50%)',
    height: '90vh',
    width: '90vw',
    backgroundColor: 'white',
    borderRadius: '4px',
    display: 'flex',
    flexFlow: 'column nowrap',
    justifyContent: 'space-between',
  },
  modalContent: {
    display: 'flex',
    flexWrap: 'wrap',
    height: '100%',
    width: '100%',
  },
  modalOverlay: {
    position: 'fixed',
    top: 0,
    left: 0,
    width: '100%',
    height: '100%',
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    opacity: 0,
    visibility: 'hidden',
    transform: 'scale(1.1)',
    transition: 'visibility 0.25s linear, opacity 0.25s 0s, transform 0.25s',
    zIndex: 102,
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
  separator: {
    alignSelf: 'center',
    display: 'inline-block',
    margin: '0 0.75rem 0.5rem',
    boxSizing: 'border-box',
    height: '26px',
    width: '1px',
    border: '1px solid #979797',
  },
  tabButton: {
    borderRadius: 0,
    letterSpacing: 'normal',
    margin: '0 1rem'
  },
  tabButtonActive: {
    color: '#216FB4',
    borderBottom: '7px solid #216FB4',
    fontSize: '14px',
    fontWeight: 'bold',
  },
  titleBar: {
    boxShadow: '0 0.12rem 0.125rem 0 #216FB4',
    marginBottom: '0.5rem',
    padding: '0rem 1rem',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'flex-end',
    height: '59px',
    marginTop: '0.5rem',
  }
});
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

  get leftColumnStyle() {
    const {searchContext: {domain, type}} = this.props;
    let width = '66.66667%';
    if (domain === DomainType.PERSON) {
      width = type === CriteriaType.AGE ? '100%' : '50%';
    }
    return {
      flex: `0 0 ${width}`,
      maxWidth: width,
      position: 'relative',
    } as React.CSSProperties;
  }

  get rightColumnStyle() {
    const width = this.props.searchContext.domain === DomainType.PERSON ? '50%' : '33.33333%';
    return {
      flex: `0 0 ${width}`,
      height: '100%',
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
      open, selectedIds, selections, title, treeSearchTerms} = this.state;
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
    return !!searchContext ? <div style={{
      ...styles.modalOverlay,
      ...(open ? {opacity: 1, visibility: 'visible', transform: 'scale(1.0'} : {})
    }}>
      <div style={{...styles.modalContainer, ...(domain === DomainType.PERSON ? {width: '50vw', height: 'auto'} : {})}}>
        <div style={styles.modalContent}>
          <div style={this.leftColumnStyle}>
            <div style={styles.titleBar}>
              <div style={{display: 'inline-flex', marginRight: '0.5rem'}}>
                {!attributesNode ? <Button
                  style={{...styles.tabButton, ...((mode === 'list' || mode === 'tree') ? styles.tabButtonActive : {})}}
                  type='link'
                  disabled={selections.length !== 0 && conceptType === 'standard'}
                  onClick={() => this.setState({mode: this.state.backMode})}>
                  {title}
                </Button>
                : <Button
                  style={{...styles.tabButton, ...(mode === 'attributes' ? styles.tabButtonActive : {})}}
                  type='link'
                  onClick={() => this.setState({mode: 'attributes'})}>
                  {this.attributeTitle}
                </Button>}
                {this.showModifiers && <React.Fragment>
                  <div style={styles.separator}/>
                  <Button
                    style={{...styles.tabButton, ...(mode === 'modifiers' ? styles.tabButtonActive : {})}}
                    type='link'
                    disabled={selections.length === 0}
                    onClick={() => this.setMode('modifiers')}>
                    Modifiers
                  </Button>
                </React.Fragment>}
              </div>
              <div style={{display: 'table', height: '100%'}}>
                <div style={{display: 'table', height: '100%', verticalAlign: 'middle'}}>
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
              {mode === 'attributes' && <Button type='link' onClick={this.back}>
                <ClrIcon size='24' shape='close'/>
              </Button>}
            </div>
            <div style={(domain === DomainType.PERSON && type !== CriteriaType.AGE) ? {marginBottom: '3.5rem'} : {}}>
              {domain === DomainType.PERSON ? <Demographics
                count={count}
                criteriaType={type}
                select={this.addSelection}
                selectedIds={selectedIds}
                selections={selections}/>
              : <React.Fragment>
                {loadingSubtree && <SpinnerOverlay/>}
                <div id='tree' style={loadingSubtree ? {pointerEvents: 'none', opacity: 0.3} : {}}>
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
                  {/* Modifiers Page */}
                  <div style={this.panelLeftStyle('modifiers')}>
                    {this.showModifiers && <ListModifierPage
                      disabled={this.modifiersFlag}
                      searchContext={searchContext}
                      selections={selections}
                      setSearchContext={setSearchContext}/>}
                  </div>
                     {/* Attributes Page */}
                  <div style={this.panelLeftStyle('attributes')}>
                    {!!attributesNode && <AttributesPage
                      close={this.back}
                      node={attributesNode}
                      select={this.addSelection}/>}
                  </div>
                </div>
              </React.Fragment>}
              {type === CriteriaType.AGE && <div style={styles.footer}>
                <Button type='link' onClick={closeSearch}>
                  Cancel
                </Button>
                <Button type='primary' onClick={this.finish}>
                  Finish
                </Button>
              </div>}
            </div>
          </div>
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
