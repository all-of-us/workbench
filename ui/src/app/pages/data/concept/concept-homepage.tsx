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
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import colors, {addOpacity, colorWithWhiteness} from 'app/styles/colors';
import {
  reactStyles,
  ReactWrapperBase,
  validateInputForMySQL,
  withCurrentCohortSearchContext,
  withCurrentConcept,
  withCurrentWorkspace
} from 'app/utils';
import {
  conceptSetUpdating,
  currentCohortSearchContextStore,
  currentConceptSetStore,
  currentConceptStore,
  NavStore,
} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {environment} from 'environments/environment';
import {Concept, Domain, DomainInfo, SurveyModule} from 'generated/fetch';
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

const DomainCard: React.FunctionComponent<{conceptDomainInfo: DomainInfo, browseInDomain: Function, updating: boolean}> =
  ({conceptDomainInfo, browseInDomain, updating}) => {
    const conceptCount = conceptDomainInfo.allConceptCount.toLocaleString();
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
  cohortContext: any;
  concept?: Array<Concept>;
}

interface State {
  // Array of domains and their metadata
  conceptDomainList: Array<DomainInfo>;
  // Array of surveys
  conceptSurveysList: Array<SurveyModule>;
  // Current string in search box
  currentInputString: string;
  // Last string that was searched
  currentSearchString: string;
  // True if the getDomainInfo call fails
  domainInfoError: boolean;
  // List of domains loading updated counts for domain cards
  domainsLoading: Array<Domain>;
  // List of error messages to display if the search input is invalid
  inputErrors: Array<string>;
  // If concept metadata is still being gathered for any domain
  loadingDomains: boolean;
  // Function to execute when canceling navigation due to unsaved changes
  onModalCancel: Function;
  // Function to execute when discarding unsaved changes
  onModalDiscardChanges: Function;
  // Show if a search error occurred
  showSearchError: boolean;
  // Show if trying to navigate away with unsaved changes
  showUnsavedModal: boolean;
  // True if the getSurveyInfo call fails
  surveyInfoError: boolean;
  // List of surveys loading updated counts for survey cards
  surveysLoading: Array<string>;
  // True if there are unsaved concepts
  unsavedChanges: boolean;
}

export const ConceptHomepage = fp.flow(withCurrentCohortSearchContext(), withCurrentConcept(), withCurrentWorkspace())(
  class extends React.Component<Props, State> {
    resolveUnsavedModal: Function;
    subscription: Subscription;
    constructor(props) {
      super(props);
      this.state = {
        conceptDomainList: [],
        conceptSurveysList: [],
        currentInputString: props.cohortContext ? props.cohortContext.searchTerms : '',
        currentSearchString: props.cohortContext ? props.cohortContext.searchTerms : '',
        domainInfoError: false,
        domainsLoading: [],
        inputErrors: [],
        loadingDomains: true,
        onModalCancel: undefined,
        onModalDiscardChanges: undefined,
        showSearchError: false,
        showUnsavedModal: false,
        surveyInfoError: false,
        surveysLoading: [],
        unsavedChanges: false,
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
      this.subscription.unsubscribe();
    }

    async loadDomainsAndSurveys() {
      const {cohortContext, workspace: {cdrVersionId}} = this.props;
      const getDomainInfo = cohortBuilderApi().findDomainInfos(+cdrVersionId)
        .then(conceptDomainInfo => this.setState({conceptDomainList: conceptDomainInfo.items}))
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
      if (cohortContext && cohortContext.searchTerms) {
        this.updateCardCounts();
      }
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

    clearSearch() {
      this.setState({
        currentInputString: '',
        currentSearchString: '',
        inputErrors: [],
        showSearchError: false,
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
            showSearchError: false,
            showUnsavedModal: false
          }),
          showUnsavedModal: true
        });
      } else {
        this.setState({
          inputErrors: [],
          showSearchError: false,
        });
      }
    }

    browseDomain(domain: Domain, surveyName?: string) {
      const {namespace, id} = this.props.workspace;
      currentCohortSearchContextStore.next({domain: domain, searchTerms: this.state.currentSearchString, surveyName});
      NavStore.navigate(['workspaces', namespace, id, 'data', 'concepts', domain]);
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

    render() {
      const {conceptDomainList, conceptSurveysList, currentInputString, currentSearchString, domainInfoError, domainsLoading, inputErrors,
        loadingDomains, onModalCancel, onModalDiscardChanges, surveyInfoError, showSearchError, showUnsavedModal, surveysLoading}
        = this.state;
      const conceptDomainCards = conceptDomainList.filter(domain => domain.domain !== Domain.PHYSICALMEASUREMENT);
      const physicalMeasurementsCard = conceptDomainList.find(domain => domain.domain === Domain.PHYSICALMEASUREMENT);
      return <React.Fragment>
        <FadeBox style={{margin: 'auto', paddingTop: '1rem', width: '95.7%'}}>
          <div style={{display: 'flex', alignItems: 'center'}}>
            <ClrIcon shape='search' style={{position: 'absolute', height: '1rem', width: '1rem',
              fill: colors.accent, left: 'calc(1rem + 3.5%)'}}/>
            <TextInput style={styles.searchBar} data-test-id='concept-search-input'
                       placeholder='Search concepts in domain'
                       value={currentInputString}
                       onChange={(e) => this.setState({currentInputString: e})}
                       onKeyPress={(e) => this.handleSearchKeyPress(e)}/>
            {currentSearchString !== '' && <Clickable onClick={() => this.clearSearch()} data-test-id='clear-search'>
                <ClrIcon shape='times-circle' style={styles.clearSearchIcon}/>
            </Clickable>}
          </div>
          {inputErrors.map((error, e) => <AlertDanger key={e} style={styles.inputAlert}>
            <span data-test-id='input-error-alert'>{error}</span>
          </AlertDanger>)}
          {showSearchError && <AlertDanger style={styles.inputAlert}>
              Minimum concept search length is three characters.
              <AlertClose style={{width: 'unset'}}
                          onClick={() => this.setState({showSearchError: false})}/>
          </AlertDanger>}
          {loadingDomains ? <div style={{position: 'relative', minHeight: '10rem'}}><SpinnerOverlay/></div> :
            <div>
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
                                                        browseInDomain={() => this.browseDomain(domain.domain)}
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
                                                     browseSurvey={() => this.browseDomain(Domain.SURVEY, survey.name)}
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
                                                    browsePhysicalMeasurements={() => this.browseDomain(Domain.PHYSICALMEASUREMENT)}
                                                    updating={domainsLoading.includes(Domain.PHYSICALMEASUREMENT)}/>
                  }
                </div>
              </React.Fragment>}
            </div>
          }
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
