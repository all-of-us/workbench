import {Component} from '@angular/core';
import * as React from 'react';

import {AlertClose, AlertDanger} from 'app/components/alert';
import {Clickable} from 'app/components/buttons';
import {WorkspaceCardBase} from 'app/components/card';
import {FadeBox} from 'app/components/containers';
import {ClrIcon} from 'app/components/icons';
import {CheckBox, TextInput} from 'app/components/inputs';
import {Spinner, SpinnerOverlay} from 'app/components/spinners';
import {conceptsApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {queryParamsStore} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {ConceptAddModal} from 'app/views/concept-add-modal/component';
import {ConceptNavigationBar} from 'app/views/concept-navigation-bar/component';
import {ConceptTable} from 'app/views/concept-table/component';
import {SlidingFabReact} from 'app/views/sliding-fab/component';
import * as Color from 'color';
import {
  Concept,
  Domain,
  DomainCount,
  DomainInfo,
  StandardConceptFilter,
  VocabularyCount
} from 'generated/fetch';

const styles = reactStyles({
  searchBar: {
    marginLeft: '1%', boxShadow: '0 4px 12px 0 rgba(0,0,0,0.15)',
    height: '3rem', width: '64.3%',
    backgroundColor: Color('#A3D3F2').alpha(0.2).toString(), fontSize: '16px',
    lineHeight: '19px', paddingLeft: '2rem'
  },
  domainBoxHeader: {
    color: colors.blue[0], fontSize: '18px', lineHeight: '22px'
  },
  domainBoxLink: {
    color: colors.blue[0], lineHeight: '18px', fontWeight: 600, letterSpacing: '0.05rem'
  },
  conceptText: {
    marginTop: '0.3rem', fontSize: '14px', fontWeight: 400, color: colors.gray[0],
    display: 'flex', flexDirection: 'column', marginBottom: '0.3rem'
  },
  domainHeaderLink: {
    justifyContent: 'center', padding: '0.1rem 1rem', color: colors.blue[0],
    lineHeight: '18px'
  },
  domainHeaderSelected: {
    height: '2.5px', width: '100%', backgroundColor: colors.blue[0], border: 'none'
  },
  conceptCounts: {
    backgroundColor: colors.white, height: '2rem', border: '1px solid #CCCCCC',
    marginTop: '-1px', paddingLeft: '0.5rem', display: 'flex',
    justifyContent: 'flex-start', lineHeight: '15px', fontWeight: 600, fontSize: '14px',
    color: colors.purple[0], alignItems: 'center'
  },
  selectedConceptsCount: {
    backgroundColor: colors.blue[0], color: colors.white, borderRadius: '20px',
    textAlign: 'center', height: '1.5em', padding: '0 5px'
  },
  clearSearchIcon: {
    fill: colors.blue[0], transform: 'translate(-1.5rem)', height: '1rem', width: '1rem'
  }
});

interface ConceptCacheItem {
  domain: Domain;
  vocabularyList: Array<VocabularyCount>;
  items: Array<Concept>;
}

const DomainCard: React.FunctionComponent<{conceptDomainInfo: DomainInfo,
  standardConceptsOnly: boolean, browseInDomain: Function}> =
    ({conceptDomainInfo, standardConceptsOnly, browseInDomain}) => {
      const conceptCount = standardConceptsOnly ?
          conceptDomainInfo.standardConceptCount : conceptDomainInfo.allConceptCount;
      return <WorkspaceCardBase style={{minWidth: '11rem'}} data-test-id='domain-box'>
        <div style={styles.domainBoxHeader}
             data-test-id='domain-box-name'>{conceptDomainInfo.name}</div>
        <div style={styles.conceptText}>
          <span style={{fontSize: '30px'}}>{conceptCount}</span> concepts in this domain. <p/>
          <b>{conceptDomainInfo.participantCount}</b> participants in domain.</div>
        <Clickable style={styles.domainBoxLink}
                   onClick={browseInDomain}>Browse Domain</Clickable>
      </WorkspaceCardBase>;
    };


export const ConceptHomepage = withCurrentWorkspace()(
  class extends React.Component<{workspace: WorkspaceData},
    { // Array of domains that have finished being searched for concepts with search string
      completedDomainSearches: Array<Domain>,
      // If modal to add concepts to set is open
      conceptAddModalOpen: boolean,
      // Array of domains and the number of concepts found in the search for each
      conceptDomainCounts: Array<DomainCount>,
      // Array of domains and their metadata
      conceptDomainList: Array<DomainInfo>,
      // Array of concepts found in the search
      concepts: Array<Concept>,
      // Cache for storing selected concepts, their domain, and vocabulary
      conceptsCache: Array<ConceptCacheItem>,
      conceptsSavedText: string,
      // Array of concepts that have been selected
      conceptsToAdd: Concept[],
      // Current string in search box
      currentSearchString: string,
      // If concept metadata is still being gathered for any domain
      loadingDomains: boolean,
      // If we are still searching concepts and should show a spinner on the table
      searchLoading: boolean,
      // If we are in 'search mode' and should show the table
      searching: boolean,
      // Map of domain to number of selected concepts in domain
      selectedConceptDomainMap: Map<String, number>,
      // Domain being viewed. Will be the domain that the add button uses.
      selectedDomain: DomainCount,
      // Show if a search error occurred
      showSearchError: boolean,
      // Only search on standard concepts
      standardConceptsOnly: boolean,
      // Array of vocabulary id and number of concepts in vocabulary
      vocabularies: Array<VocabularyCount>
    }> {

    private MAX_CONCEPT_FETCH = 100;
    constructor(props) {
      super(props);
      this.state = {
        completedDomainSearches: [],
        conceptAddModalOpen: false,
        conceptDomainCounts: [],
        conceptDomainList: [],
        concepts: [],
        conceptsCache: [],
        conceptsSavedText: '',
        conceptsToAdd: [],
        currentSearchString: '',
        loadingDomains: true,
        searchLoading: false,
        searching: false,
        selectedConceptDomainMap: new Map<string, number>(),
        selectedDomain: {
          name: '',
          domain: undefined,
          conceptCount: 0
        },
        showSearchError: false,
        standardConceptsOnly: true,
        vocabularies: []
      };
    }

    componentDidMount() {
      this.loadDomains();
    }

    async loadDomains() {
      const {namespace, id} = this.props.workspace;
      try {
        const conceptsCache: ConceptCacheItem[] = [];
        const conceptDomainCounts: DomainCount[] = [];
        const resp = await conceptsApi().getDomainInfo(namespace, id);
        this.setState({conceptDomainList: resp.items});
        resp.items.forEach((domain) => {
          conceptsCache.push({
            domain: domain.domain,
            items: [],
            vocabularyList: []
          });
          conceptDomainCounts.push({
            domain: domain.domain,
            name: domain.name,
            conceptCount: 0
          });
        });
        this.setState({
          conceptsCache: conceptsCache,
          conceptDomainCounts: conceptDomainCounts,
          selectedDomain: conceptDomainCounts[0],
        });
      } catch (e) {
        console.error(e);
      } finally {
        this.browseDomainFromQueryParams();
        this.setState({loadingDomains: false});
      }
    }

    browseDomainFromQueryParams() {
      const queryParams = queryParamsStore.getValue();
      if (queryParams.domain) {
        this.browseDomain(this.state.conceptDomainList
          .find(dc => dc.domain === queryParams.domain));
      }
    }

    triggerSearch(e) {
      // search on enter key
      if (e.keyCode === 13) {
        const searchTermLength = e.target.value.trim().length;
        if (searchTermLength < 3) {
          this.setState({showSearchError: true});
        } else {
          this.setState({currentSearchString: e.target.value}, this.searchConcepts);
        }
      }
    }

    selectDomain(domainCount: DomainCount) {
      this.setState({selectedDomain: domainCount},
        this.setConceptsAndVocabularies);
    }

    setConceptsAndVocabularies() {
      const cacheItem = this.state.conceptsCache
        .find(c => c.domain === this.state.selectedDomain.domain);
      this.setState({concepts: cacheItem.items});
      this.setState({vocabularies: cacheItem.vocabularyList});
    }

    async searchConcepts() {
      const {standardConceptsOnly, currentSearchString, conceptsCache,
        selectedDomain, completedDomainSearches} = this.state;
      const {namespace, id} = this.props.workspace;
      this.setState({concepts: [], searchLoading: true, searching: true, conceptsToAdd: [],
        selectedConceptDomainMap: new Map<string, number>()});
      const standardConceptFilter = standardConceptsOnly ?
        StandardConceptFilter.STANDARDCONCEPTS : StandardConceptFilter.ALLCONCEPTS;

      conceptsCache.forEach(async(cacheItem) => {
        const activeTabSearch = cacheItem.domain === selectedDomain.domain;
        const resp = await conceptsApi().searchConcepts(namespace, id, {
          query: currentSearchString,
          standardConceptFilter: standardConceptFilter,
          domain: cacheItem.domain,
          includeDomainCounts: activeTabSearch,
          includeVocabularyCounts: true,
          maxResults: this.MAX_CONCEPT_FETCH
        });
        completedDomainSearches.push(cacheItem.domain);
        cacheItem.items = resp.items;
        cacheItem.vocabularyList = resp.vocabularyCounts;
        this.setState({completedDomainSearches: completedDomainSearches});
        if (activeTabSearch) {
          this.setState({
            searchLoading: false,
            conceptDomainCounts: resp.domainCounts,
            selectedDomain: resp.domainCounts
              .find(domainCount => domainCount.domain === cacheItem.domain)});
          this.setConceptsAndVocabularies();
        }
      });
    }

    async getNextConceptSet(pageNumber) {
      const {standardConceptsOnly, currentSearchString, selectedDomain} = this.state;
      const {namespace, id} = this.props.workspace;
      const standardConceptFilter = standardConceptsOnly ?
          StandardConceptFilter.STANDARDCONCEPTS : StandardConceptFilter.ALLCONCEPTS;

      const concepts = this.state.concepts;
      const resp = await conceptsApi().searchConcepts(namespace, id, {
        query: currentSearchString,
        standardConceptFilter: standardConceptFilter,
        domain: selectedDomain.domain,
        includeDomainCounts: true,
        includeVocabularyCounts: true,
        maxResults: this.MAX_CONCEPT_FETCH,
        pageNumber: pageNumber ? pageNumber : 0
      });
      this.setState({concepts: concepts.concat(resp.items)});
    }

    selectConcepts(concepts: Concept[]) {
      const {selectedDomain, selectedConceptDomainMap} = this.state;
      selectedConceptDomainMap[selectedDomain.domain] = concepts.filter(concept => {
        return concept.domainId.toLowerCase() === selectedDomain.domain.toString().toLowerCase();
      }).length;
      this.setState({selectedConceptDomainMap: selectedConceptDomainMap, conceptsToAdd: concepts});
    }

    clearSearch() {
      this.setState({currentSearchString: ''});
      this.searchConcepts();
    }

    browseDomain(domain: DomainInfo) {
      const {conceptDomainCounts} = this.state;
      this.setState({currentSearchString: '',
        selectedDomain: conceptDomainCounts
          .find(domainCount => domainCount.domain === domain.domain)},
        this.searchConcepts);
    }

    domainLoading(domain) {
      return this.state.searchLoading || !this.state.completedDomainSearches
        .includes(domain.domain);
    }

    get noConceptsConstant() {
      return 'No concepts found for domain \'' + this.state.selectedDomain.name + '\' this search.';
    }

    get activeSelectedConceptCount(): number {
      const {selectedDomain, selectedConceptDomainMap} = this.state;
      if (!selectedDomain
        || !selectedDomain.domain
        || !selectedConceptDomainMap[selectedDomain.domain]) {
        return 0;
      }
      return selectedConceptDomainMap[selectedDomain.domain];
    }

    get addToSetText(): string {
      const count = this.activeSelectedConceptCount;
      return count === 0 ? 'Add to set' : 'Add (' + count + ') to set';
    }

    afterConceptsSaved() {
      const {selectedConceptDomainMap, selectedDomain, conceptsToAdd} = this.state;
      this.setConceptsSaveText();
      // Once concepts are saved clear the selection from concept homepage for active Domain
      selectedConceptDomainMap[selectedDomain.domain] = 0;
      const remainingConcepts = conceptsToAdd.filter((c) =>
        c.domainId.toLowerCase() !== selectedDomain.domain.toString().toLowerCase());
      this.setState({conceptAddModalOpen: false, conceptsToAdd: remainingConcepts});
    }

    setConceptsSaveText() {
      const {selectedConceptDomainMap, selectedDomain} = this.state;
      const conceptsCount = selectedConceptDomainMap[selectedDomain.domain];
      this.setState({conceptsSavedText: conceptsCount + ' ' + selectedDomain.name.toLowerCase() +
        (conceptsCount > 1 ? ' concepts have ' : ' concept has ') + 'been added to set.'});
      setTimeout(() => {
        this.setState({conceptsSavedText: ''});
      }, 5000);
    }

    render() {
      const {loadingDomains, conceptDomainList, standardConceptsOnly, showSearchError,
        searching, concepts, searchLoading, conceptDomainCounts, selectedDomain,
        conceptAddModalOpen, conceptsToAdd, selectedConceptDomainMap,
        currentSearchString, conceptsSavedText} = this.state;
      const {workspace} = this.props;
      return <FadeBox style={{margin: 'auto', marginTop: '1rem', width: '95.7%'}}>
        <ConceptNavigationBar showConcepts={true} ns={workspace.namespace} wsId={workspace.id}/>
        <div style={{marginBottom: '6%', marginTop: '1.5%'}}>
          <div style={{display: 'flex', alignItems: 'center'}}>
            <ClrIcon shape='search' style={{position: 'absolute', height: '1rem', width: '1rem',
              fill: '#216FB4', left: 'calc(1rem + 4.5%)'}}/>
            <TextInput style={styles.searchBar} data-test-id='concept-search-input'
                       placeholder='Search concepts in domain'
                       onKeyDown={e => {this.triggerSearch(e); }}/>
            {currentSearchString !== '' && <Clickable onClick={() => this.clearSearch()}
                                                      data-test-id='clear-search'>
                <ClrIcon shape='times-circle' style={styles.clearSearchIcon}/>
            </Clickable>}
            <CheckBox checked={standardConceptsOnly}
                      data-test-id='standardConceptsCheckBox'
                      style={{marginLeft: '0.5rem', height: '16px', width: '16px'}}
                      onChange={() => this.setState({
                        standardConceptsOnly: !standardConceptsOnly
                      })}/>
            <label style={{marginLeft: '0.2rem'}}>
              Standard concepts only
            </label>
          </div>
          {showSearchError &&
          <AlertDanger style={{width: '64.3%', marginLeft: '1%', justifyContent: 'space-between'}}>
              Minimum concept search length is three characters.
              <AlertClose style={{width: 'unset'}}
                          onClick={() => this.setState({showSearchError: false})}/>
          </AlertDanger>}
          <div style={{marginTop: '0.5rem'}}>{conceptsSavedText}</div>
        </div>

        {loadingDomains ? <SpinnerOverlay/> :
          searching ?
            <FadeBox>
              <div style={{display: 'flex', flexDirection: 'row', justifyContent: 'flex-start'}}>
                {conceptDomainCounts.map((domain) => {
                  return <div style={{display: 'flex', flexDirection: 'column'}}>
                    <Clickable style={styles.domainHeaderLink}
                               onClick={() => this.selectDomain(domain)}
                               disabled={this.domainLoading(domain)}
                               data-test-id={'domain-header-' + domain.name}>
                    <div style={{fontSize: '16px'}}>{domain.name}</div>
                    {this.domainLoading(domain) ?
                      <Spinner style={{height: '15px', width: '15px'}}/> :
                      <div style={{display: 'flex', flexDirection: 'row',
                        justifyContent: 'space-between'}}>
                        <div>{domain.conceptCount}</div>
                        {(selectedConceptDomainMap[domain.domain] > 0) &&
                        <div style={styles.selectedConceptsCount} data-test-id='selectedConcepts'>
                          {selectedConceptDomainMap[domain.domain]}
                        </div>}
                      </div>
                    }
                  </Clickable>
                  {domain === selectedDomain && <hr data-test-id='active-domain'
                                                    key={selectedDomain.domain}
                                                    style={styles.domainHeaderSelected}/>}
                  </div>;
                })}
              </div>
              <div style={styles.conceptCounts}>
                Showing top {concepts.length} of {selectedDomain.conceptCount} {selectedDomain.name}
              </div>
              <ConceptTable concepts={concepts}
                            loading={searchLoading}
                            onSelectConcepts={this.selectConcepts.bind(this)}
                            placeholderValue={this.noConceptsConstant}
                            searchTerm={this.state.currentSearchString}
                            selectedConcepts={conceptsToAdd}
                            reactKey={selectedDomain.name}
                            nextPage={(page) => this.getNextConceptSet(page)}/>
              <SlidingFabReact submitFunction={() => this.setState({conceptAddModalOpen: true})}
                               iconShape='plus'
                               expanded={this.addToSetText}
                               disable={this.activeSelectedConceptCount === 0}/>
            </FadeBox> :
            <div style={{display: 'flex', flexDirection: 'row', width: '94.3%', flexWrap: 'wrap'}}>
              {conceptDomainList.map((domain, i) => {
                return <DomainCard conceptDomainInfo={domain}
                                    standardConceptsOnly={standardConceptsOnly}
                                    browseInDomain={() => this.browseDomain(domain)}
                                    key={i} data-test-id='domain-box'/>;
              })}
          </div>
        }
        {conceptAddModalOpen &&
          <ConceptAddModal selectedDomain={selectedDomain}
                           selectedConcepts={conceptsToAdd}
                           onSave={() => this.afterConceptsSaved()}
                           onClose={() => this.setState({conceptAddModalOpen: false})}/>}
      </FadeBox>;
    }
  }
);

@Component({
  template: '<div #root></div>'
})
export class ConceptHomepageComponent extends ReactWrapperBase {
  constructor() {
    super(ConceptHomepage, []);
  }
}

