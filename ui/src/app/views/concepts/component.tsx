import {Component} from '@angular/core';
import * as fp from 'lodash/fp';
import * as React from 'react';

import {AlertClose, AlertDanger} from 'app/components/alert';
import {Clickable} from 'app/components/buttons';
import {WorkspaceCardBase} from 'app/components/card';
import {FadeBox} from 'app/components/containers';
import {ClrIcon} from 'app/components/icons';
import {CheckBox, TextInput} from 'app/components/inputs';
import {SpinnerOverlay} from 'app/components/spinners';
import {conceptsApi} from 'app/services/swagger-fetch-clients';
import {WorkspaceData} from 'app/services/workspace-storage.service';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {ConceptTable} from 'app/views/concept-table/component';
import {Concept, Domain, DomainCount, DomainInfo, StandardConceptFilter, VocabularyCount, } from 'generated/fetch';

const styles = reactStyles({
  searchBar: {
    marginLeft: '1%',
    boxShadow: '0 4px 12px 0 rgba(0,0,0,0.15)',
    height: '3rem',
    width: '64.3%',
    backgroundColor: '#A3D3F232',
    fontSize: '16px',
    lineHeight: '19px',
    paddingLeft: '2rem'
  },
  domainBoxHeader: {
    color: '#2691D0',
    fontSize: '18px',
    lineHeight: '22px'
  },
  domainBoxLink: {
    color: '#2691D0', lineHeight: '18px', fontWeight: 600,
    letterSpacing: '0.05rem'
  },
  conceptText: {
    marginTop: '0.3rem',
    fontSize: '14px',
    fontWeight: 400,
    color: '#4A4A4A',
    display: 'flex',
    flexDirection: 'column',
    marginBottom: '0.3rem'
  },
  domainHeaderLink: {
    fontSize: '12px', fontWeight: 300, display: 'flex', flexDirection: 'column',
    justifyContent: 'center', padding: '0.5rem', color: '#2691D0', lineHeight: '18px'
  }
});

interface ConceptCacheSet {
  domain: Domain;

  vocabularyList: Array<VocabularyCount>;

  items: Array<ConceptInfo>;
}
interface VocabularyCountSelected extends VocabularyCount {
  selected: boolean;
}
interface ConceptInfo extends Concept {
  selected: boolean;
}

const DomainBox: React.FunctionComponent<{conceptDomainInfo: DomainInfo,
  standardConceptsOnly: boolean}> =
    ({conceptDomainInfo, standardConceptsOnly}) => {
      const conceptCount = standardConceptsOnly ?
          conceptDomainInfo.standardConceptCount : conceptDomainInfo.allConceptCount;
      return <WorkspaceCardBase style={{minWidth: '11rem'}} data-test-id='domain-box'>
        <div style={styles.domainBoxHeader}>{conceptDomainInfo.name}</div>
        <div style={styles.conceptText}>
          <span style={{fontSize: '30px'}}>{conceptCount}</span> concepts in this domain. <p/>
          <b>{conceptDomainInfo.participantCount}</b> participants in domain.</div>
        <Clickable style={styles.domainBoxLink}>Browse Domain</Clickable>
      </WorkspaceCardBase>;
    };


export const ConceptWrapper = withCurrentWorkspace()(
  class extends React.Component<{workspace: WorkspaceData},
      {loadingDomains: boolean, currentSearchString: string, standardConceptsOnly: boolean,
        searching: boolean, searchLoading: boolean, showSearchError: boolean,
        selectedDomain: DomainCount, conceptDomainList: Array<DomainInfo>,
        conceptDomainCounts: Array<DomainCount>, concepts: Array<ConceptInfo>,
        conceptsCache: Array<ConceptCacheSet>, selectedConceptDomainMap: Map<String, number>,
        completedDomainSearches: Array<Domain>, conceptsToAdd: Concept[],
        vocabularies: Array<VocabularyCountSelected>}> {

    private MAX_CONCEPT_FETCH = 100;
    constructor(props) {
      super(props);
      this.state = {
        completedDomainSearches: [],
        conceptDomainCounts: [],
        conceptDomainList: [],
        concepts: [],
        conceptsCache: [],
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
        const conceptsCache: ConceptCacheSet[] = [];
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
          loadingDomains: false});
      } catch (e) {
        console.error(e);
      }
    }

    searchButton(e) {
      // search on enter key
      if (e.keyCode === 13) {
        const searchTermLength = e.target.value.trim().length;
        if (searchTermLength < 3) {
          this.setState({showSearchError: true});
        } else {
          this.setState({currentSearchString: e.target.value});
          this.searchConcepts();
        }
      }
    }

    selectDomain(domainCount: DomainCount) {
      if (!this.state.selectedConceptDomainMap[domainCount.domain]) {
        this.setState(fp.set(['selectedConceptDomainMap', domainCount.domain], 0));
      }
      this.setState({selectedDomain: domainCount},
        this.setConceptsAndVocabularies);
    }

    setConceptsAndVocabularies() {
      const cacheItem = this.state.conceptsCache
        .find(conceptDomain => conceptDomain.domain === this.state.selectedDomain.domain);
      this.setState({concepts: cacheItem.items});
      this.setState({
        conceptsToAdd: this.state.concepts.filter((c) => c.selected),
        vocabularies: cacheItem.vocabularyList.map((vocabulary) => {
          return {
            ...vocabulary,
            selected: true
          };
        })
      });
    }

    async searchConcepts() {
      const {standardConceptsOnly, currentSearchString, conceptsCache,
        selectedDomain, completedDomainSearches} = this.state;
      const {namespace, id} = this.props.workspace;
      this.setState({concepts: [], searchLoading: true, searching: true, conceptsToAdd: []});
      const standardConceptFilter = standardConceptsOnly ?
        StandardConceptFilter.STANDARDCONCEPTS : StandardConceptFilter.ALLCONCEPTS;

      conceptsCache.forEach(async(conceptDomain) => {
        const activeTabSearch = conceptDomain.domain === selectedDomain.domain;
        const resp = await conceptsApi().searchConcepts(namespace, id, {
          query: currentSearchString,
          standardConceptFilter: standardConceptFilter,
          domain: conceptDomain.domain,
          includeDomainCounts: activeTabSearch,
          includeVocabularyCounts: true,
          maxResults: this.MAX_CONCEPT_FETCH
        });
        completedDomainSearches.push(conceptDomain.domain);
        conceptDomain.items = this.convertToConceptInfo(resp.items);
        conceptDomain.vocabularyList = resp.vocabularyCounts;
        // console.log(this.state.conceptsCache);

        if (activeTabSearch) {
          this.setState({
            searchLoading: false,
            conceptDomainCounts: resp.domainCounts,
            selectedDomain: resp.domainCounts
              .find(domainCount => domainCount.domain === conceptDomain.domain)});
          this.setConceptsAndVocabularies();
        }

      });

    }

    filterList() {
      // TODO
    }

    selectConcept(concepts: ConceptInfo[]) {
      const {selectedDomain} = this.state;
      this.setState({conceptsToAdd: concepts});
      this.setState(fp.set(['selectedConceptDomain', selectedDomain.domain],
        concepts.filter(concept => {
        return concept.domainId.toLowerCase() === selectedDomain.domain.toString().toLowerCase();
      }).length));
    }

    convertToConceptInfo(concepts: Concept[]): ConceptInfo[] {
      const conceptInfos = concepts.map((concept) => {
        return {
          ...concept,
          selected: false
        };
      });
      this.filterConceptSelection(conceptInfos);
      return conceptInfos;
    }

    private filterConceptSelection(concepts: ConceptInfo[]) {
      const conceptSet = new Set(this.state.conceptsToAdd.map(c => c.conceptId));
      concepts.forEach((concept) => {
        concept.selected = conceptSet.has(concept.conceptId);
      });
    }

    clearSearch() {
      this.setState({currentSearchString: ''});
      this.searchConcepts();
    }

    browseDomain(domain: DomainInfo) {
      const {conceptDomainCounts} = this.state;
      this.setState({currentSearchString: '',
        selectedDomain: conceptDomainCounts
          .find(domainCount => domainCount.domain === domain.domain)});
      this.searchConcepts();
    }

    domainLoading(domain) {
      return this.state.searchLoading || !this.state.completedDomainSearches
        .includes(domain.domain);
    }

    get noConceptsConstant() {
      return 'No concepts found for domain \'' + this.state.selectedDomain.name + '\' this search.';
    }

    render() {
      const {loadingDomains, conceptDomainList, standardConceptsOnly, showSearchError,
        searching, concepts, searchLoading, conceptDomainCounts, selectedDomain} = this.state;
      return <React.Fragment>
        <div style={{marginBottom: '6%', marginTop: '1.5%'}}>
          <div style={{display: 'flex', alignItems: 'center'}}>
            <ClrIcon shape='search' style={{position: 'absolute', height: '1rem', width: '1rem',
              fill: '#216FB4', left: 'calc(1rem + 4.5%)'}}/>
            <TextInput style={styles.searchBar}
                       placeholder='Search concepts in domain'
                       onKeyDown={e => {this.searchButton(e); }}/>
            <CheckBox checked={standardConceptsOnly}
                      style={{marginLeft: '0.5rem', height: '16px', width: '16px'}}
                      onChange={() => this.setState({standardConceptsOnly: !standardConceptsOnly})}/>
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
        </div>

        {loadingDomains ? <SpinnerOverlay/> :
          searching ?
            <FadeBox>
              <div style={{display: 'flex', flexDirection: 'row', justifyContent: 'flex-start'}}>
                {conceptDomainCounts.map((domain) => {
                  return <Clickable style={domain === selectedDomain ?
                    {fontWeight: 600, ...styles.domainHeaderLink} : styles.domainHeaderLink}
                                    onClick={() => this.selectDomain(domain)}
                                    disabled={this.domainLoading(domain)}>
                    <div style={{fontSize: '16px'}}>{domain.name}</div>
                    <div>{domain.conceptCount}</div>
                    {domain === selectedDomain && <div>selected</div>}
                  </Clickable>;
                })}
              </div>

              <ConceptTable concepts={concepts}
                            loading={searchLoading}
                            getSelectedConcepts={() => this.selectConcept}
                            placeholderValue={this.noConceptsConstant}
                            setSelectedConcepts={() => {}}/>
            </FadeBox> :
            <div style={{display: 'flex', flexDirection: 'row', width: '94.3%', flexWrap: 'wrap'}}>
              {conceptDomainList.map((domain) => {
                return <DomainBox conceptDomainInfo={domain}
                                  standardConceptsOnly={standardConceptsOnly}/>;
              })}
          </div>
        }
      </React.Fragment>;
    }
  }
);

@Component({
  template: '<div #root></div>'
})
export class ConceptsComponent extends ReactWrapperBase {
  constructor() {
    super(ConceptWrapper, []);
  }
}

