import {Component} from '@angular/core';
import * as fp from 'lodash/fp';
import * as React from 'react';
import {Subscription} from 'rxjs/Subscription';

import {AlertClose, AlertDanger} from 'app/components/alert';
import {Button, Clickable} from 'app/components/buttons';
import {DomainCardBase} from 'app/components/card';
import {FadeBox} from 'app/components/containers';
import {ClrIcon} from 'app/components/icons';
import {TextInput} from 'app/components/inputs';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {Spinner, SpinnerOverlay} from 'app/components/spinners';
import {ConceptAddModal} from 'app/pages/data/concept/concept-add-modal';
import {ConceptSurveyAddModal} from 'app/pages/data/concept/concept-survey-add-modal';
import {CriteriaSearch} from 'app/pages/data/criteria-search';
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import colors, {addOpacity, colorWithWhiteness} from 'app/styles/colors';
import {
  reactStyles,
  ReactWrapperBase,
  validateInputForMySQL,
  withCurrentConcept,
  withCurrentWorkspace
} from 'app/utils';
import {
  conceptSetUpdating,
  currentCohortSearchContextStore,
  currentConceptSetStore,
  currentConceptStore,
  NavStore,
  queryParamsStore,
  setSidebarActiveIconStore
} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {WorkspacePermissions} from 'app/utils/workspace-permissions';
import {environment} from 'environments/environment';
import {Concept, ConceptSet, Domain, DomainCount, DomainInfo, SurveyModule, SurveyQuestions} from 'generated/fetch';
import {Key} from 'ts-key-enum';

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
    width: '1.5rem'
  },
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
  error: {
    background: colors.warning,
    color: colors.white,
    fontSize: '12px',
    fontWeight: 500,
    textAlign: 'left',
    border: '1px solid #ebafa6',
    borderRadius: '5px',
    marginTop: '0.25rem',
    padding: '8px',
  },
  inputAlert: {
    justifyContent: 'space-between',
    padding: '0.2rem',
    width: '64.3%',
  }
});

interface ConceptCacheItem {
  domain: Domain;
  items: Array<Concept | SurveyQuestions>;
}

const DomainCard: React.FunctionComponent<{conceptDomainInfo: DomainInfo,
  standardConceptsOnly: boolean, browseInDomain: Function, updating: boolean}> =
    ({conceptDomainInfo, standardConceptsOnly, browseInDomain, updating}) => {
      const conceptCount = standardConceptsOnly ?
          conceptDomainInfo.standardConceptCount.toLocaleString() : conceptDomainInfo.allConceptCount.toLocaleString();
      return <DomainCardBase style={{width: 'calc(25% - 1rem)'}} data-test-id='domain-box'>
        <Clickable style={styles.domainBoxHeader}
             onClick={browseInDomain}
             data-test-id='domain-box-name'>{conceptDomainInfo.name}</Clickable>
        <div style={styles.conceptText}>
          {updating ? <Spinner size={42}/> : <React.Fragment>
            <span style={{fontSize: 30}}>{conceptCount.toLocaleString()}</span> concepts in this domain. <p/>
          </React.Fragment>}
          <div><b>{conceptDomainInfo.participantCount.toLocaleString()}</b> participants in domain.</div>
        </div>
        <Clickable style={styles.domainBoxLink}
                   onClick={browseInDomain}>Select Concepts</Clickable>
      </DomainCardBase>;
    };

const SurveyCard: React.FunctionComponent<{survey: SurveyModule, browseSurvey: Function, updating: boolean}> =
    ({survey, browseSurvey, updating}) => {
      return <DomainCardBase style={{maxHeight: 'auto', width: 'calc(25% - 1rem)'}}>
        <Clickable style={styles.domainBoxHeader}
          onClick={browseSurvey}
          data-test-id='survey-box-name'>{survey.name}</Clickable>
        <div style={styles.conceptText}>
          {updating ? <Spinner size={42}/> : <React.Fragment>
            <span style={{fontSize: 30}}>{survey.questionCount.toLocaleString()}</span> survey questions with
          </React.Fragment>}
          <div><b>{survey.participantCount.toLocaleString()}</b> participants</div>
        </div>
        <div style={{...styles.conceptText, height: '3.5rem'}}>
          {survey.description}
        </div>
        <Clickable style={{...styles.domainBoxLink}} onClick={browseSurvey}>Select Concepts</Clickable>
      </DomainCardBase>;
    };

const PhysicalMeasurementsCard: React.FunctionComponent<{physicalMeasurement: DomainInfo,
  browsePhysicalMeasurements: Function, updating: boolean}> =
    ({physicalMeasurement, browsePhysicalMeasurements, updating}) => {
      return <DomainCardBase style={{maxHeight: 'auto', width: '11.5rem'}}>
        <Clickable style={styles.domainBoxHeader}
          onClick={browsePhysicalMeasurements}
          data-test-id='pm-box-name'>{physicalMeasurement.name}</Clickable>
        <div style={styles.conceptText}>
          {updating ? <Spinner size={42}/> : <React.Fragment>
            <span style={{fontSize: 30}}>{physicalMeasurement.allConceptCount.toLocaleString()}</span> physical measurements.
          </React.Fragment>}
          <div><b>{physicalMeasurement.participantCount.toLocaleString()}</b> participants in this domain</div>
        </div>
        <div style={{...styles.conceptText, height: 'auto'}}>
          {physicalMeasurement.description}
        </div>
        <Clickable style={styles.domainBoxLink} onClick={browsePhysicalMeasurements}>Select Concepts</Clickable>
      </DomainCardBase>;
    };

interface Props {
  setConceptSetUpdating: (conceptSetUpdating: boolean) => void;
  setShowUnsavedModal: (showUnsavedModal: () => Promise<boolean>) => void;
  setUnsavedConceptChanges: (unsavedConceptChanges: boolean) => void;
  workspace: WorkspaceData;
  concept?: Array<Concept>;
}

interface State {
  // Domain tab being viewed when all tabs are visible
  activeDomainTab: DomainCount;
  // Browse survey
  browsingSurvey: boolean;
  // Array of domains that have finished being searched for concepts with search string
  completedDomainSearches: Array<Domain>;
  // Array of concepts found in the search
  concepts: Array<any>;
  // If modal to add concepts to set is open
  conceptAddModalOpen: boolean;
  // Cache for storing selected concepts, their domain, and vocabulary
  conceptsCache: Array<ConceptCacheItem>;
  // Array of domains and the number of concepts found in the search for each
  conceptDomainCounts: Array<DomainCount>;
  // Array of domains and their metadata
  conceptDomainList: Array<DomainInfo>;
  // Array of surveys
  conceptSurveysList: Array<SurveyModule>;
  // True if the domainCounts call fails
  countsError: boolean;
  // Current string in search box
  currentInputString: string;
  // Last string that was searched
  currentSearchString: string;
  // List of domains where the search api call failed
  domainErrors: Domain[];
  // True if the getDomainInfo call fails
  domainInfoError: boolean;
  // List of domains loading updated counts for domain cards
  domainsLoading: Array<Domain>;
  // List of error messages to display if the search input is invalid
  inputErrors: Array<string>;
  // If concept metadata is still being gathered for any domain
  loadingDomains: boolean;
  // If we are still searching concepts and should show a spinner on the table
  countsLoading: boolean;
  // Function to execute when canceling navigation due to unsaved changes
  onModalCancel: Function;
  // Function to execute when discarding unsaved changes
  onModalDiscardChanges: Function;
  // If we are in 'search mode' and should show the table
  searching: boolean;
  // Map of domain to selected concepts in domain
  selectedConceptDomainMap: Map<String, any[]>;
  // Domain being viewed. Will be the domain that the add button uses.
  selectedDomain: Domain;
  // Name of the survey selected
  selectedSurvey: string;
  // Array of survey questions selected to be added to concept set
  selectedSurveyQuestions: Array<SurveyQuestions>;
  // Show if a search error occurred
  showSearchError: boolean;
  // Show if trying to navigate away with unsaved changes
  showUnsavedModal: boolean;
  // Only search on standard concepts
  standardConceptsOnly: boolean;
  // Open modal to add survey questions to concept set
  surveyAddModalOpen: boolean;
  // True if the getSurveyInfo call fails
  surveyInfoError: boolean;
  // List of surveys loading updated counts for survey cards
  surveysLoading: Array<string>;
  // True if there are unsaved concepts
  unsavedChanges: boolean;
  workspacePermissions: WorkspacePermissions;
}

export const ConceptHomepage = fp.flow(withCurrentWorkspace(), withCurrentConcept())(
  class extends React.Component<Props, State> {
    resolveUnsavedModal: Function;
    subscription: Subscription;
    constructor(props) {
      super(props);
      this.state = {
        activeDomainTab: {name: '', domain: undefined, conceptCount: 0},
        browsingSurvey: false,
        completedDomainSearches: [],
        conceptAddModalOpen: false,
        conceptDomainCounts: [],
        conceptDomainList: [],
        concepts: [],
        conceptsCache: [],
        conceptSurveysList: [],
        countsError: false,
        countsLoading: false,
        currentInputString: '',
        currentSearchString: '',
        domainErrors: [],
        domainInfoError: false,
        domainsLoading: [],
        inputErrors: [],
        loadingDomains: true,
        onModalCancel: undefined,
        onModalDiscardChanges: undefined,
        searching: false,
        selectedConceptDomainMap: new Map<string, Concept[]>(),
        selectedDomain: undefined,
        selectedSurvey: '',
        selectedSurveyQuestions: [],
        showSearchError: false,
        showUnsavedModal: false,
        standardConceptsOnly: false,
        surveyAddModalOpen: false,
        surveyInfoError: false,
        surveysLoading: [],
        unsavedChanges: false,
        workspacePermissions: new WorkspacePermissions(props.workspace),
      };
      this.showUnsavedModal = this.showUnsavedModal.bind(this);
    }

    componentDidMount() {
      this.loadDomainsAndSurveys();
      this.subscription = currentConceptStore.subscribe(currentConcepts => {
        if (![null, undefined].includes(currentConcepts)) {
          const currentConceptSet = currentConceptSetStore.getValue();
          const unsavedChanges = (!currentConceptSet && currentConcepts.length > 0)
            || (!!currentConceptSet && JSON.stringify(currentConceptSet.criteriums.sort()) !== JSON.stringify(currentConcepts.sort()));
          this.props.setUnsavedConceptChanges(unsavedChanges);
          this.setState({unsavedChanges});
        }
      });
      this.subscription.add(conceptSetUpdating.subscribe(updating => this.props.setConceptSetUpdating(updating)));
      this.props.setShowUnsavedModal(this.showUnsavedModal);
    }

    componentWillUnmount() {
      currentConceptStore.next(null);
      this.subscription.unsubscribe();
    }

    async loadDomainsAndSurveys() {
      const {cdrVersionId} = this.props.workspace;
      const getDomainInfo = cohortBuilderApi().findDomainInfos(+cdrVersionId)
        .then(conceptDomainInfo => {
          let conceptsCache: ConceptCacheItem[] = conceptDomainInfo.items.map((domain) => ({
            domain: domain.domain,
            items: []
          }));
          // Add ConceptCacheItem for Surveys tab
          conceptsCache.push({domain: Domain.SURVEY, items: []});
          let conceptDomainCounts: DomainCount[] = conceptDomainInfo.items.map((domain) => ({
            domain: domain.domain,
            name: domain.name,
            conceptCount: 0
          }));
          // Add DomainCount for Surveys tab
          conceptDomainCounts.push({domain: Domain.SURVEY, name: 'Surveys', conceptCount: 0});
          if (!environment.enableNewConceptTabs) {
            // Don't show Physical Measurements tile or tab if feature flag disabled
            conceptsCache = conceptsCache.filter(item => item.domain !== Domain.PHYSICALMEASUREMENT);
            conceptDomainCounts = conceptDomainCounts.filter(item => item.domain !== Domain.PHYSICALMEASUREMENT);
          }
          this.setState({
            conceptsCache: conceptsCache,
            conceptDomainList: conceptDomainInfo.items,
            conceptDomainCounts: conceptDomainCounts,
            activeDomainTab: conceptDomainCounts[0],
          });
        })
        .catch((e) => {
          this.setState({domainInfoError: true});
          console.error(e);
        });
      const getSurveyInfo = cohortBuilderApi().findSurveyModules(+cdrVersionId)
        .then(surveysInfo => this.setState({conceptSurveysList: surveysInfo.items}))
        .catch((e) => {
          this.setState({surveyInfoError: true});
          console.error(e);
        });
      await Promise.all([getDomainInfo, getSurveyInfo]);
      this.browseDomainFromQueryParams();
      this.setState({loadingDomains: false});
    }

    async updateCardCounts() {
      const {cdrVersionId} = this.props.workspace;
      const {conceptDomainList, conceptSurveysList, currentInputString} = this.state;
      this.setState({
        domainsLoading: conceptDomainList.map(domain => domain.domain),
        surveysLoading: conceptSurveysList.map(survey => survey.name),
      });
      const promises = [];
      conceptDomainList.forEach(conceptDomain => {
        promises.push(cohortBuilderApi().findDomainCount(+cdrVersionId, conceptDomain.domain.toString(), currentInputString)
          .then(domainCount => {
            conceptDomain.allConceptCount = domainCount.conceptCount;
            this.setState({domainsLoading: this.state.domainsLoading.filter(domain => domain !== conceptDomain.domain)});
          })
        );
      });
      conceptSurveysList.forEach(conceptSurvey => {
        promises.push(cohortBuilderApi().findSurveyCount(+cdrVersionId, conceptSurvey.name, currentInputString)
          .then(surveyCount => {
            conceptSurvey.questionCount = surveyCount.conceptCount;
            this.setState({surveysLoading: this.state.surveysLoading.filter(survey => survey !== conceptSurvey.name)});
          }));
      });
      await Promise.all(promises);
      this.setState({conceptDomainList, conceptSurveysList});
    }

    browseDomainFromQueryParams() {
      const queryParams = queryParamsStore.getValue();
      if (queryParams.survey) {
        this.browseSurvey(queryParams.survey);
      }
      if (queryParams.domain) {
        if (queryParams.domain === Domain.SURVEY) {
          this.browseSurvey('');
        } else {
          this.browseDomain(this.state.conceptDomainList.find(dc => dc.domain === queryParams.domain));
        }
      }
    }

    handleSearchKeyPress(e) {
      const {currentInputString} = this.state;
      // search on enter key if no forbidden characters are present
      if (e.key === Key.Enter) {
        if (currentInputString.trim().length < 3) {
          this.setState({inputErrors: [], showSearchError: true});
        } else {
          const inputErrors = validateInputForMySQL(currentInputString);
          this.setState({inputErrors, showSearchError: false});
          if (inputErrors.length === 0) {
            this.setState({currentSearchString: currentInputString}, () => this.updateCardCounts());
          }
        }
      }
    }

    selectConcepts(concepts: any[]) {
      const {activeDomainTab: {domain}, selectedConceptDomainMap} = this.state;
      if (domain === Domain.SURVEY) {
        selectedConceptDomainMap[domain] = concepts.filter(concept => !!concept.question);
      } else {
        selectedConceptDomainMap[domain] = concepts.filter(
          concept => concept.domainId.replace(' ', '')
            .toLowerCase() === Domain[domain].toLowerCase());
      }
      this.setState({selectedConceptDomainMap: selectedConceptDomainMap});
    }

    clearSearch() {
      const {conceptDomainCounts} = this.state;
      this.setState({
        activeDomainTab: conceptDomainCounts[0],
        currentInputString: '',
        currentSearchString: '',
        inputErrors: [],
        selectedDomain: undefined,
        selectedSurvey: '',
        showSearchError: false,
        searching: false // reset the search result table to show browse/domain cards instead
      });
      currentConceptStore.next(null);
      this.setState({loadingDomains: true}, () => this.loadDomainsAndSurveys());
    }

    back() {
      if (this.state.unsavedChanges) {
        this.setState({
          onModalCancel: () => this.setState({showUnsavedModal: false}),
          onModalDiscardChanges: () => this.setState({
            inputErrors: [],
            searching: false,
            selectedDomain: undefined,
            selectedSurvey: '',
            showSearchError: false,
            showUnsavedModal: false
          }),
          showUnsavedModal: true
        });
      } else {
        this.setState({
          inputErrors: [],
          selectedDomain: undefined,
          selectedSurvey: '',
          showSearchError: false,
          searching: false // reset the search result table to show browse/domain cards instead
        });
      }
    }

    browseDomain(domain: DomainInfo) {
      const {namespace, id} = this.props.workspace;
      currentCohortSearchContextStore.next({domain: domain.domain, type: 'PPI', standard: this.state.standardConceptsOnly});
      NavStore.navigate(['workspaces', namespace, id, 'data', 'concepts', domain.domain]);
    }

    browseSurvey(surveyName) {
      currentConceptStore.next([]);
      this.setState({
        activeDomainTab: {domain: Domain.SURVEY, name: 'Surveys', conceptCount: 0},
        searching: true,
        selectedDomain: Domain.SURVEY,
        selectedSurvey: surveyName
      });
    }

    get activeSelectedConceptCount(): number {
      const {activeDomainTab, selectedConceptDomainMap} = this.state;
      if (!this.props.concept) {
        if (!activeDomainTab || !activeDomainTab.domain || !selectedConceptDomainMap[activeDomainTab.domain]) {
          return 0;
        }
        return selectedConceptDomainMap[activeDomainTab.domain].length;
      } else {
        const selectedConcept = this.props.concept;
        if (!activeDomainTab && selectedConcept && selectedConcept.length === 0) {
          return 0;
        }
        return selectedConcept.length;
      }
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

    async showUnsavedModal() {
      this.setState({
        onModalCancel: () => this.getModalResponse(false),
        onModalDiscardChanges: () => this.getModalResponse(true),
        showUnsavedModal: true});
      return await new Promise<boolean>((resolve => this.resolveUnsavedModal = resolve));
    }

    getModalResponse(res: boolean) {
      this.setState({showUnsavedModal: false});
      this.resolveUnsavedModal(res);
    }

    errorMessage() {
      return <div style={styles.error}>
        <ClrIcon style={{margin: '0 0.5rem 0 0.25rem'}} className='is-solid' shape='exclamation-triangle' size='22'/>
        Sorry, the request cannot be completed. Please try refreshing the page or contact Support in the left hand navigation.
      </div>;
    }

    renderConcepts() {
      const {activeDomainTab, currentSearchString} = this.state;
      return <React.Fragment>
        <CriteriaSearch backFn={() => this.back()}
          cohortContext={{domain: activeDomainTab.domain, type: 'PPI', standard: this.state.standardConceptsOnly}}
          conceptSearchTerms={currentSearchString}
          source='concept' selectedSurvey={this.state.selectedSurvey}/>
        <Button style={{float: 'right', marginBottom: '2rem'}}
              disabled={this.activeSelectedConceptCount === 0 ||
              !this.state.workspacePermissions.canWrite}
              onClick={() => setSidebarActiveIconStore.next('concept')}>Finish & Review</Button>
      </React.Fragment>;
    }

    render() {
      const {activeDomainTab, browsingSurvey, conceptAddModalOpen, conceptDomainList, conceptSurveysList, currentInputString,
        currentSearchString, domainInfoError, domainsLoading, inputErrors, loadingDomains, onModalCancel, onModalDiscardChanges,
        surveyInfoError, standardConceptsOnly, showSearchError, searching, selectedSurvey, selectedConceptDomainMap,
        selectedSurveyQuestions, showUnsavedModal, surveyAddModalOpen, surveysLoading} = this.state;
      const conceptDomainCards = conceptDomainList.filter(domain => domain.domain !== Domain.PHYSICALMEASUREMENT);
      const physicalMeasurementsCard = conceptDomainList.find(domain => domain.domain === Domain.PHYSICALMEASUREMENT);
      return <React.Fragment>
        <FadeBox style={{margin: 'auto', paddingTop: '1rem', width: '95.7%'}}>
          {!searching && <div style={{display: 'flex', alignItems: 'center'}}>
            <ClrIcon shape='search' style={{position: 'absolute', height: '1rem', width: '1rem',
              fill: colors.accent, left: 'calc(1rem + 3.5%)'}}/>
            <TextInput style={styles.searchBar} data-test-id='concept-search-input'
                       placeholder='Search concepts in domain'
                       value={currentInputString}
                       onChange={(e) => this.setState({currentInputString: e})}
                       onKeyPress={(e) => this.handleSearchKeyPress(e)}/>
            {currentSearchString !== '' && <Clickable onClick={() => this.clearSearch()}
                                                      data-test-id='clear-search'>
                <ClrIcon shape='times-circle' style={styles.clearSearchIcon}/>
            </Clickable>}
          </div>}
          {inputErrors.map((error, e) => <AlertDanger key={e} style={styles.inputAlert}>
            <span data-test-id='input-error-alert'>{error}</span>
          </AlertDanger>)}
          {showSearchError && <AlertDanger style={styles.inputAlert}>
              Minimum concept search length is three characters.
              <AlertClose style={{width: 'unset'}}
                          onClick={() => this.setState({showSearchError: false})}/>
          </AlertDanger>}
          {!browsingSurvey && loadingDomains ? <div style={{position: 'relative', minHeight: '10rem'}}><SpinnerOverlay/></div> :
            searching ? this.renderConcepts() :  !browsingSurvey && <div>
              <div style={styles.sectionHeader}>
                Domains
              </div>
              <div style={styles.cardList}>
                {domainInfoError
                  ? this.errorMessage()
                  : conceptDomainCards.some(domain => domainsLoading.includes(domain.domain))
                    ? <Spinner size={42}/>
                    : conceptDomainCards.every(domain => domain.allConceptCount === 0)
                      ? 'No Domain Results. Please type in a new search term.'
                      : conceptDomainCards
                        .filter(domain => domain.allConceptCount !== 0)
                        .map((domain, i) => <DomainCard conceptDomainInfo={domain}
                                                        standardConceptsOnly={standardConceptsOnly}
                                                        browseInDomain={() => this.browseDomain(domain)}
                                                        key={i} data-test-id='domain-box'
                                                        updating={domainsLoading.includes(domain.domain)}/>)
                }
              </div>
              <div style={styles.sectionHeader}>
                Survey Questions
              </div>
              <div style={styles.cardList}>
                {surveyInfoError
                  ? this.errorMessage()
                  : conceptSurveysList.some(survey => surveysLoading.includes(survey.name))
                    ? <Spinner size={42}/>
                    : conceptSurveysList.every(survey => survey.questionCount === 0)
                      ? 'No Survey Question Results. Please type in a new search term.'
                      : conceptSurveysList
                        .filter(survey => survey.questionCount > 0)
                        .map((survey) => <SurveyCard survey={survey}
                                                     key={survey.orderNumber}
                                                     browseSurvey={() => this.browseSurvey(survey.name)}
                                                     updating={surveysLoading.includes(survey.name)}/>)
                }
              </div>
              {environment.enableNewConceptTabs && !!physicalMeasurementsCard && <React.Fragment>
                <div style={styles.sectionHeader}>
                  Program Physical Measurements
                </div>
                <div style={{...styles.cardList, marginBottom: '1rem'}}>
                  {domainInfoError
                    ? this.errorMessage()
                    : domainsLoading.includes(Domain.PHYSICALMEASUREMENT)
                      ? <Spinner size={42}/>
                      : physicalMeasurementsCard.allConceptCount === 0
                        ? 'No Program Physical Measurement Results. Please type in a new search term.'
                        : <PhysicalMeasurementsCard physicalMeasurement={physicalMeasurementsCard}
                                                    browsePhysicalMeasurements={() => this.browseDomain(physicalMeasurementsCard)}
                                                    updating={domainsLoading.includes(Domain.PHYSICALMEASUREMENT)}/>
                  }
                </div>
              </React.Fragment>}
            </div>
          }
          {conceptAddModalOpen &&
            <ConceptAddModal activeDomainTab={activeDomainTab}
                             selectedConcepts={selectedConceptDomainMap[activeDomainTab.domain]}
                             onSave={(conceptSet) => this.afterConceptsSaved(conceptSet)}
                             onClose={() => this.setState({conceptAddModalOpen: false})}/>}
          {surveyAddModalOpen &&
          <ConceptSurveyAddModal selectedSurvey={selectedSurveyQuestions}
                                 onClose={() => this.setState({surveyAddModalOpen: false})}
                                 onSave={() => this.setState({surveyAddModalOpen: false})}
                                 surveyName={selectedSurvey}/>}
        </FadeBox>
        {showUnsavedModal && <Modal>
          <ModalTitle>Warning! </ModalTitle>
          <ModalBody>
            Your concept set has not been saved. If you’d like to save your concepts, please click CANCEL
            and save your changes in the right sidebar.
          </ModalBody>
          <ModalFooter>
            <Button type='link' onClick={onModalCancel}>Cancel</Button>
            <Button type='primary' onClick={onModalDiscardChanges}>Discard Changes</Button>
          </ModalFooter>
        </Modal>}
      </React.Fragment>;
    }
  }
);

@Component({
  template: '<div #root></div>'
})
export class ConceptHomepageComponent extends ReactWrapperBase {
  conceptSetUpdating: boolean;
  showUnsavedModal: () => Promise<boolean>;
  unsavedConceptChanges: boolean;
  constructor() {
    super(ConceptHomepage, ['setConceptSetUpdating', 'setShowUnsavedModal', 'setUnsavedConceptChanges']);
    this.setConceptSetUpdating = this.setConceptSetUpdating.bind(this);
    this.setShowUnsavedModal = this.setShowUnsavedModal.bind(this);
    this.setUnsavedConceptChanges = this.setUnsavedConceptChanges.bind(this);
  }

  setShowUnsavedModal(showUnsavedModal: () => Promise<boolean>): void {
    this.showUnsavedModal = showUnsavedModal;
  }

  setConceptSetUpdating(csUpdating: boolean): void {
    this.conceptSetUpdating = csUpdating;
  }

  setUnsavedConceptChanges(unsavedConceptChanges: boolean): void {
    this.unsavedConceptChanges = unsavedConceptChanges;
  }

  canDeactivate(): Promise<boolean> | boolean {
    return !this.unsavedConceptChanges || this.conceptSetUpdating || this.showUnsavedModal();
  }
}

