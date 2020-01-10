import {Component} from '@angular/core';
import * as React from 'react';

import {AlertClose, AlertDanger} from 'app/components/alert';
import {Clickable, SlidingFabReact} from 'app/components/buttons';
import {DomainCardBase} from 'app/components/card';
import {FadeBox} from 'app/components/containers';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {Header} from 'app/components/headers';
import {ClrIcon} from 'app/components/icons';
import {CheckBox, TextInput} from 'app/components/inputs';
import {Spinner, SpinnerOverlay} from 'app/components/spinners';
import {ConceptAddModal} from 'app/pages/data/concept/concept-add-modal';
import {ConceptSurveyAddModal} from 'app/pages/data/concept/concept-survey-add-modal';
import {ConceptTable} from 'app/pages/data/concept/concept-table';
import {conceptsApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {NavStore, queryParamsStore} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {WorkspacePermissions} from 'app/utils/workspace-permissions';
import {environment} from 'environments/environment';
import {
  Concept,
  ConceptSet,
  Domain,
  DomainCount,
  DomainInfo,
  StandardConceptFilter,
  SurveyModule,
  SurveyQuestions,
} from 'generated/fetch';
import {Key} from 'ts-key-enum';
import {SurveyDetails} from './survey-details';

const styles = reactStyles({
  searchBar: {
    boxShadow: '0 4px 12px 0 rgba(0,0,0,0.15)', height: '3rem', width: '64.3%', lineHeight: '19px', paddingLeft: '2rem',
    backgroundColor: colorWithWhiteness(colors.secondary, 0.85), fontSize: '16px'
  },
  domainBoxHeader: {
    color: colors.accent, fontSize: '18px', lineHeight: '22px'
  },
  domainBoxLink: {
    color: colors.accent, lineHeight: '18px', fontWeight: 400, letterSpacing: '0.05rem'
  },
  conceptText: {
    marginTop: '0.3rem', fontSize: '14px', fontWeight: 400, color: colors.primary,
    display: 'flex', flexDirection: 'column', marginBottom: '0.3rem'
  },
  domainHeaderLink: {
    justifyContent: 'center', padding: '0.1rem 1rem', color: colors.accent,
    lineHeight: '18px'
  },
  domainHeaderSelected: {
    height: '4px', width: '100%', backgroundColor: colors.accent, border: 'none'
  },
  conceptCounts: {
    backgroundColor: colors.white, height: '2rem', border: `1px solid ${colorWithWhiteness(colors.black, 0.8)}`, borderBottom: 0,
    borderTopLeftRadius: '3px', borderTopRightRadius: '3px', marginTop: '-1px', paddingLeft: '0.5rem', display: 'flex',
    justifyContent: 'flex-start', lineHeight: '15px', fontWeight: 600, fontSize: '14px',
    color: colors.primary, alignItems: 'center'
  },
  selectedConceptsCount: {
    backgroundColor: colors.accent, color: colors.white, borderRadius: '5px',
    padding: '0 5px', fontSize: 12
  },
  clearSearchIcon: {
    fill: colors.accent, transform: 'translate(-1.5rem)', height: '1rem', width: '1rem'
  },
  sectionHeader: {
    height: 24,
    color: colors.primary,
    fontFamily: 'Montserrat',
    fontSize: 20,
    fontWeight: 600,
    lineHeight: '24px',
    marginBottom: '1rem',
    marginTop: '2.5rem'
  },
  cardList: {
    display: 'flex',
    flexDirection: 'row',
    width: '94.3%',
    flexWrap: 'wrap'
  },
  backBtn: {
    border: 0,
    fontSize: '14px',
    color: colors.accent,
    background: 'transparent',
    cursor: 'pointer'
  },
});

interface ConceptCacheItem {
  domain: Domain;
  items: Array<Concept>;
}

const DomainCard: React.FunctionComponent<{conceptDomainInfo: DomainInfo,
  standardConceptsOnly: boolean, browseInDomain: Function}> =
    ({conceptDomainInfo, standardConceptsOnly, browseInDomain}) => {
      const conceptCount = standardConceptsOnly ?
          conceptDomainInfo.standardConceptCount.toLocaleString() : conceptDomainInfo.allConceptCount.toLocaleString();
      return <DomainCardBase style={{width: 'calc(25% - 1rem)'}} data-test-id='domain-box'>
        <Clickable style={styles.domainBoxHeader}
             onClick={browseInDomain}
             data-test-id='domain-box-name'>{conceptDomainInfo.name}</Clickable>
        <div style={styles.conceptText}>
          <span style={{fontSize: 30}}>{conceptCount.toLocaleString()}</span> concepts in this domain. <p/>
          <div><b>{conceptDomainInfo.participantCount.toLocaleString()}</b> participants in domain.</div>
        </div>
        <Clickable style={styles.domainBoxLink}
                   onClick={browseInDomain}>Browse Domain</Clickable>
      </DomainCardBase>;
    };

const SurveyCard: React.FunctionComponent<{survey: SurveyModule, browseSurvey: Function}> =
    ({survey, browseSurvey}) => {
      return <DomainCardBase style={{maxHeight: 'auto', width: 'calc(25% - 1rem)'}}>
        <Clickable style={styles.domainBoxHeader}
          onClick={browseSurvey}
          data-test-id='survey-box-name'>{survey.name}</Clickable>
        <div style={styles.conceptText}>
          <span style={{fontSize: 30}}>{survey.questionCount.toLocaleString()}</span> survey questions with
          <div><b>{survey.participantCount.toLocaleString()}</b> participants</div>
        </div>
        <div style={{...styles.conceptText, height: '3.5rem'}}>
          {survey.description}
        </div>
        <Clickable style={{...styles.domainBoxLink}} onClick={browseSurvey}>Browse Survey</Clickable>
      </DomainCardBase>;
    };

// Placeholder version of Physical Measurements card. TODO update styles when api call is ready
const PhysicalMeasurementsCard: React.FunctionComponent<{physicalMeasurement: DomainInfo, browsePhysicalMeasurements: Function}> =
    ({physicalMeasurement, browsePhysicalMeasurements}) => {
      return <DomainCardBase style={{maxHeight: 'auto', width: '11.5rem'}}>
        <Clickable style={styles.domainBoxHeader}
          onClick={browsePhysicalMeasurements}
          data-test-id='pm-box-name'>{physicalMeasurement.name}</Clickable>
        <div style={styles.conceptText}>
          <span style={{fontSize: 30}}>{physicalMeasurement.allConceptCount}</span> physical measurements.
          <div><b>{physicalMeasurement.participantCount}</b> participants in this domain</div>
        </div>
        <div style={{...styles.conceptText, height: 'auto'}}>
          {physicalMeasurement.description}
        </div>
        <Clickable style={styles.domainBoxLink} onClick={browsePhysicalMeasurements}>
          Browse Physical Measurements
        </Clickable>
      </DomainCardBase>;
    };

// Stub used to display placeholder tabs in concept search
const conceptCacheStub = [
  {
    domain: Domain.SURVEY,
    items: []
  }
];

interface Props {
  workspace: WorkspaceData;
}

interface State { // Browse survey
  browsingSurvey: boolean;
  // Array of domains that have finished being searched for concepts with search string
  completedDomainSearches: Array<Domain>;
  // Array of concepts found in the search
  concepts: Array<Concept>;
  // If modal to add concepts to set is open
  conceptAddModalOpen: boolean;
  // Cache for storing selected concepts, their domain, and vocabulary
  conceptsCache: Array<ConceptCacheItem>;
  // Array of domains and the number of concepts found in the search for each
  conceptDomainCounts: Array<DomainCount>;
  // Array of domains and their metadata
  conceptDomainList: Array<DomainInfo>;
  // Array of Physical Measurements
  conceptPhysicalMeasurementsList: Array<any>;
  conceptsSavedText: string;
  // Array of surveys
  conceptSurveysList: Array<SurveyModule>;
  // Current string in search box
  currentSearchString: string;
  // If concept metadata is still being gathered for any domain
  loadingDomains: boolean;
  // If we are still searching concepts and should show a spinner on the table
  searchLoading: boolean;
  // If we are in 'search mode' and should show the table
  searching: boolean;
  // Map of domain to selected concepts in domain
  selectedConceptDomainMap: Map<String, Concept[]>;
  // Domain being viewed. Will be the domain that the add button uses.
  selectedDomain: DomainCount;
  // Name of the survey selected
  selectedSurvey: string;
  // Array of survey questions selected to be added to concept set
  selectedSurveyQuestions: Array<SurveyQuestions>;
  // Show if a search error occurred
  showSearchError: boolean;
  // Only search on standard concepts
  standardConceptsOnly: boolean;
  // Open modal to add survey questions to concept set
  surveyAddModalOpen: boolean;
  workspacePermissions: WorkspacePermissions;
}

export const ConceptHomepage = withCurrentWorkspace()(
  class extends React.Component<Props, State> {

    private MAX_CONCEPT_FETCH = 1000;
    constructor(props) {
      super(props);
      this.state = {
        browsingSurvey: false,
        completedDomainSearches: [],
        conceptAddModalOpen: false,
        surveyAddModalOpen: false,
        conceptDomainCounts: [],
        conceptDomainList: [],
        conceptPhysicalMeasurementsList: [],
        concepts: [],
        conceptsCache: [],
        conceptsSavedText: '',
        conceptSurveysList: [],
        currentSearchString: '',
        loadingDomains: true,
        searchLoading: false,
        searching: false,
        selectedConceptDomainMap: new Map<string, Concept[]>(),
        selectedDomain: {
          name: '',
          domain: undefined,
          conceptCount: 0
        },
        showSearchError: false,
        standardConceptsOnly: true,
        workspacePermissions: new WorkspacePermissions(props.workspace),
        selectedSurvey: '',
        selectedSurveyQuestions: []
      };
    }

    componentDidMount() {
      this.loadDomainsAndSurveys();
    }

    async loadDomainsAndSurveys() {
      const {namespace, id} = this.props.workspace;
      try {
        const [conceptDomainInfo, surveysInfo] = await Promise.all([
          conceptsApi().getDomainInfo(namespace, id),
          conceptsApi().getSurveyInfo(namespace, id)]);
        let conceptsCache: ConceptCacheItem[] = conceptDomainInfo.items.map((domain) => ({
          domain: domain.domain,
          items: []
        }));
        let conceptDomainCounts: DomainCount[] = conceptDomainInfo.items.map((domain) => ({
          domain: domain.domain,
          name: domain.name,
          conceptCount: 0
        }));
        if (environment.enableNewConceptTabs) {
          conceptsCache = [...conceptsCache, ...conceptCacheStub];
          conceptDomainCounts = [...conceptDomainCounts];
        } else {
          conceptsCache = conceptsCache.filter(item => item.domain !== Domain.PHYSICALMEASUREMENT);
          conceptDomainCounts = conceptDomainCounts.filter(item => item.domain !== Domain.PHYSICALMEASUREMENT);
        }
        this.setState({
          conceptsCache: conceptsCache,
          conceptDomainList: conceptDomainInfo.items.filter(item => item.domain !== Domain.PHYSICALMEASUREMENT),
          conceptDomainCounts: conceptDomainCounts,
          conceptSurveysList: surveysInfo.items,
          selectedDomain: conceptDomainCounts[0],
          conceptPhysicalMeasurementsList: conceptDomainInfo.items.filter(item => item.domain === Domain.PHYSICALMEASUREMENT),
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
      if (queryParams.survey) {
        this.browseSurvey(queryParams.survey);
      }
      if (queryParams.domain) {
        this.browseDomain(this.state.conceptDomainList
          .find(dc => dc.domain === queryParams.domain));
      }
    }

    handleSearchStringChange(e) {
      this.setState({currentSearchString: e});
    }

    handleSearchKeyPress(e) {
      // search on enter key
      if (e.key === Key.Enter) {
        const searchTermLength = this.state.currentSearchString.trim().length;
        if (searchTermLength < 3) {
          this.setState({showSearchError: true});
        } else {
          this.setState({showSearchError: false});
          this.searchConcepts().then(/* ignore promise returned */);
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
    }

    async searchConcepts() {
      const {standardConceptsOnly, currentSearchString, conceptsCache, selectedDomain,
        selectedConceptDomainMap} = this.state;
      const {namespace, id} = this.props.workspace;
      this.setState({concepts: [], searchLoading: true, searching: true, completedDomainSearches: []});
      const standardConceptFilter = standardConceptsOnly ?
        StandardConceptFilter.STANDARDCONCEPTS : StandardConceptFilter.ALLCONCEPTS;
      selectedConceptDomainMap[Domain.SURVEY] = [];
      // TODO switch to empty array when we start actually searching surveys and PM
      const completedDomainSearches = [Domain.SURVEY];
      // TODO remove filter below when we start actually searching surveys and PM
      conceptsCache.forEach(async(cacheItem) => {
        selectedConceptDomainMap[cacheItem.domain] = [];
        const activeTabSearch = cacheItem.domain === selectedDomain.domain;
        const resp = await conceptsApi().searchConcepts(namespace, id, {
          query: currentSearchString,
          standardConceptFilter: standardConceptFilter,
          domain: cacheItem.domain,
          includeDomainCounts: activeTabSearch,
          maxResults: this.MAX_CONCEPT_FETCH
        });
        completedDomainSearches.push(cacheItem.domain);
        cacheItem.items = resp.items;
        this.setState({completedDomainSearches: completedDomainSearches});
        if (activeTabSearch) {
          const conceptDomainCounts = environment.enableNewConceptTabs ? [...resp.domainCounts]
            : resp.domainCounts.filter(item => item.domain !== Domain.PHYSICALMEASUREMENT);
          this.setState({
            searchLoading: false,
            conceptDomainCounts: conceptDomainCounts,
            selectedDomain: resp.domainCounts
              .find(domainCount => domainCount.domain === cacheItem.domain)});
          this.setConceptsAndVocabularies();
        }
      });
      this.setState({selectedConceptDomainMap: selectedConceptDomainMap});
    }

    selectConcepts(concepts: Concept[]) {
      const {selectedDomain, selectedConceptDomainMap} = this.state;
      selectedConceptDomainMap[selectedDomain.domain] = concepts.filter(concept => {
        return concept.domainId.toLowerCase() === selectedDomain.domain.toString().toLowerCase();
      });
      this.setState({selectedConceptDomainMap: selectedConceptDomainMap});
    }

    clearSearch() {
      this.setState({
        currentSearchString: '',
        showSearchError: false,
        searching: false // reset the search result table to show browse/domain cards instead
      });
    }

    browseDomain(domain: DomainInfo) {
      const {conceptDomainCounts} = this.state;
      this.setState({browsingSurvey: false, currentSearchString: '',
        selectedDomain: conceptDomainCounts
          .find(domainCount => domainCount.domain === domain.domain)},
        this.searchConcepts);
    }

    browseSurvey(surveyName) {
      this.setState({browsingSurvey: true,
        selectedSurvey: surveyName});
    }

    domainLoading(domain) {
      return this.state.searchLoading || !this.state.completedDomainSearches.includes(domain.domain);
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
      return selectedConceptDomainMap[selectedDomain.domain].length;
    }

    selectedQuestion(selectedQues) {
      this.setState({selectedSurveyQuestions: selectedQues});
    }
    get activeSelectedSurvey(): number {
      return this.state.selectedSurveyQuestions.length;
    }

    get addToSetText(): string {
      const count = this.activeSelectedConceptCount;
      return count === 0 ? 'Add to set' : 'Add (' + count + ') to set';
    }

    get addSurveyToSetText(): string {
      const count = this.state.selectedSurveyQuestions.length;
      return count === 0 ? 'Add to set' : 'Add (' + count + ') to set';
    }

    afterConceptsSaved(conceptSet: ConceptSet) {
      const {namespace, id} = this.props.workspace;
      NavStore.navigate(['workspaces', namespace, id, 'data',
        'concepts', 'sets', conceptSet.id, 'actions']);
    }

    renderConcepts() {
      const {concepts, searchLoading, conceptDomainCounts, selectedDomain, selectedConceptDomainMap} = this.state;

      return <React.Fragment>
        <button style={styles.backBtn} type='button' onClick={() => this.setState({searching: false})}>
          &lt; Back to Concept Set Homepage
        </button>
        <FadeBox>
          <FlexRow style={{justifyContent: 'flex-start'}}>
            {conceptDomainCounts.map((domain) => {
              return <FlexColumn key={domain.name}>
                <Clickable style={styles.domainHeaderLink}
                           onClick={() => this.selectDomain(domain)}
                           disabled={this.domainLoading(domain)}
                           data-test-id={'domain-header-' + domain.name}>
                  <div style={{fontSize: '16px'}}>{domain.name}</div>
                  {this.domainLoading(domain) ?
                      <Spinner style={{height: '15px', width: '15px'}}/> :
                      <FlexRow style={{justifyContent: 'space-between'}}>
                        <div>{domain.conceptCount.toLocaleString()}</div>
                        {(selectedConceptDomainMap && selectedConceptDomainMap[domain.domain].length > 0) &&
                        <div style={styles.selectedConceptsCount} data-test-id='selectedConcepts'>
                          {selectedConceptDomainMap[domain.domain].length}
                        </div>}
                      </FlexRow>
                  }
                </Clickable>
                {domain === selectedDomain && <hr data-test-id='active-domain'
                                                  key={selectedDomain.domain}
                                                  style={styles.domainHeaderSelected}/>}
              </FlexColumn>;
            })}
          </FlexRow>
          {!searchLoading && selectedDomain.conceptCount > 1000 && <div style={styles.conceptCounts}>
            Showing top {concepts.length} {selectedDomain.name}
          </div>}
          <ConceptTable concepts={concepts}
                        loading={searchLoading}
                        onSelectConcepts={this.selectConcepts.bind(this)}
                        placeholderValue={this.noConceptsConstant}
                        searchTerm={this.state.currentSearchString}
                        selectedConcepts={selectedConceptDomainMap[selectedDomain.domain]}
                        reactKey={selectedDomain.name}/>
          <SlidingFabReact submitFunction={() => this.setState({conceptAddModalOpen: true})}
                           iconShape='plus'
                           tooltip={!this.state.workspacePermissions.canWrite}
                           tooltipContent={<div>Requires Owner or Writer permission</div>}
                           expanded={this.addToSetText}
                           disable={this.activeSelectedConceptCount === 0 ||
                           !this.state.workspacePermissions.canWrite}/>
        </FadeBox>
      </React.Fragment>;
    }

    render() {
      const {loadingDomains, browsingSurvey, conceptDomainList, conceptPhysicalMeasurementsList, conceptSurveysList,
        standardConceptsOnly, showSearchError, searching, selectedDomain, conceptAddModalOpen,
        currentSearchString, conceptsSavedText, selectedSurvey, surveyAddModalOpen,
        selectedSurveyQuestions, selectedConceptDomainMap} =
          this.state;
      return <React.Fragment>
        <FadeBox style={{margin: 'auto', paddingTop: '1rem', width: '95.7%'}}>
          <Header style={{fontSize: '20px', marginTop: 0, fontWeight: 600}}>Search Concepts</Header>
          <div style={{margin: '1rem 0'}}>
            <div style={{display: 'flex', alignItems: 'center'}}>
              <ClrIcon shape='search' style={{position: 'absolute', height: '1rem', width: '1rem',
                fill: colors.accent, left: 'calc(1rem + 3.5%)'}}/>
              <TextInput style={styles.searchBar} data-test-id='concept-search-input'
                         placeholder='Search concepts in domain'
                         value={this.state.currentSearchString}
                         onChange={(e) => this.handleSearchStringChange(e)}
                         onKeyPress={(e) => this.handleSearchKeyPress(e)}/>
              {currentSearchString !== '' && <Clickable onClick={() => this.clearSearch()}
                                                        data-test-id='clear-search'>
                  <ClrIcon shape='times-circle' style={styles.clearSearchIcon}/>
              </Clickable>}
              <CheckBox checked={standardConceptsOnly}
                        label='Standard concepts only'
                        labelStyle={{marginLeft: '0.2rem'}}
                        data-test-id='standardConceptsCheckBox'
                        style={{marginLeft: '0.5rem', height: '16px', width: '16px'}}
                        onChange={(checked) => this.setState({
                          standardConceptsOnly: checked
                        })}/>
            </div>
            {showSearchError &&
            <AlertDanger style={{width: '64.3%', marginLeft: '1%',
              justifyContent: 'space-between'}}>
                Minimum concept search length is three characters.
                <AlertClose style={{width: 'unset'}}
                            onClick={() => this.setState({showSearchError: false})}/>
            </AlertDanger>}
            <div style={{marginTop: '0.5rem'}}>{conceptsSavedText}</div>
          </div>
          {browsingSurvey && <div><SurveyDetails surveyName={selectedSurvey}
                                               surveySelected={(selectedQuestion) =>
                                                   this.setState(
                                                     {selectedSurveyQuestions: selectedQuestion})}/>
            <SlidingFabReact submitFunction={() => this.setState({surveyAddModalOpen: true})}
                             iconShape='plus'
                             tooltip={!this.state.workspacePermissions.canWrite}
                             tooltipContent={<div>Requires Owner or Writer permission</div>}
                             expanded={this.addSurveyToSetText}
                             disable={selectedSurveyQuestions.length === 0}/>
          </div>}
          {!browsingSurvey && loadingDomains ? <div style={{position: 'relative', minHeight: '10rem'}}><SpinnerOverlay/></div> :
            searching ?
              this.renderConcepts() : !browsingSurvey &&
                <div>
                  <div style={styles.sectionHeader}>
                    Domains
                  </div>
                  <div style={styles.cardList}>
                  {conceptDomainList.map((domain, i) => {
                    return <DomainCard conceptDomainInfo={domain}
                                         standardConceptsOnly={standardConceptsOnly}
                                         browseInDomain={() => this.browseDomain(domain)}
                                         key={i} data-test-id='domain-box'/>;
                  })}
                  </div>
                  <div style={styles.sectionHeader}>
                    Survey Questions
                  </div>
                  <div style={styles.cardList}>
                    {conceptSurveysList.map((surveys) => {
                      return <SurveyCard survey={surveys} key={surveys.orderNumber}
                                         browseSurvey={() => {this.setState({
                                           browsingSurvey: true,
                                           selectedSurvey: surveys.name
                                         }); }}/>;
                    })}
                   </div>
                  {environment.enableNewConceptTabs && <React.Fragment>
                    <div style={styles.sectionHeader}>
                      Program Physical Measurements
                    </div>
                    <div style={styles.cardList}>
                      {conceptPhysicalMeasurementsList.map((physicalMeasurement, p) => {
                        return <PhysicalMeasurementsCard physicalMeasurement={physicalMeasurement} key={p}
                                           browsePhysicalMeasurements={() => this.browseDomain(physicalMeasurement)}/>;
                      })}
                    </div>
                  </React.Fragment>}
                </div>
          }
          {conceptAddModalOpen &&
            <ConceptAddModal selectedDomain={selectedDomain}
                             selectedConcepts={selectedConceptDomainMap[selectedDomain.domain]}
                             onSave={(conceptSet) => this.afterConceptsSaved(conceptSet)}
                             onClose={() => this.setState({conceptAddModalOpen: false})}/>}
          {surveyAddModalOpen &&
          <ConceptSurveyAddModal selectedSurvey={selectedSurveyQuestions}
                                 onClose={() => this.setState({surveyAddModalOpen: false})}
                                 onSave={() => this.setState({surveyAddModalOpen: false})}
                                 surveyName={selectedSurvey}/>}
        </FadeBox>
      </React.Fragment>;
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

