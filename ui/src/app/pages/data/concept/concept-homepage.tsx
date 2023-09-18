import * as React from 'react';
import * as fp from 'lodash/fp';

import {
  Concept,
  Domain,
  DomainCard as ConceptDomainCard,
  SurveyModule,
} from 'generated/fetch';

import { AlertClose, AlertDanger } from 'app/components/alert';
import { Clickable } from 'app/components/buttons';
import { DomainCardBase } from 'app/components/card';
import { FadeBox } from 'app/components/containers';
import { ClrIcon } from 'app/components/icons';
import { TextInput } from 'app/components/inputs';
import { TooltipTrigger } from 'app/components/popups';
import { Spinner, SpinnerOverlay } from 'app/components/spinners';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { cohortBuilderApi } from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import {
  reactStyles,
  validateInputForMySQL,
  withCurrentCohortSearchContext,
  withCurrentConcept,
  withCurrentWorkspace,
} from 'app/utils';
import {
  currentCohortSearchContextStore,
  currentConceptStore,
  NavigationProps,
} from 'app/utils/navigation';
import { withNavigation } from 'app/utils/with-navigation-hoc';
import { WorkspaceData } from 'app/utils/workspace-data';

const styles = reactStyles({
  searchBar: {
    boxShadow: '0 4px 12px 0 rgba(0,0,0,0.15)',
    height: '4.5rem',
    width: '64.3%',
    lineHeight: '19px',
    paddingLeft: '3rem',
    backgroundColor: colorWithWhiteness(colors.secondary, 0.85),
    fontSize: '16px',
  },
  domainBoxHeader: {
    color: colors.accent,
    fontSize: '18px',
    lineHeight: '22px',
  },
  domainBoxLink: {
    color: colors.accent,
    lineHeight: '18px',
    fontWeight: 400,
    letterSpacing: '0.075rem',
  },
  conceptText: {
    marginTop: '0.45rem',
    fontSize: '14px',
    fontWeight: 400,
    color: colors.primary,
    display: 'flex',
    flexDirection: 'column',
    marginBottom: '0.45rem',
  },
  clearSearchIcon: {
    fill: colors.accent,
    height: '1.5rem',
    width: '1.5rem',
    marginLeft: '-2.25rem',
  },
  sectionHeader: {
    height: 24,
    color: colors.primary,
    fontFamily: 'Montserrat',
    fontSize: 20,
    fontWeight: 600,
    lineHeight: '24px',
    marginBottom: '1.5rem',
    marginTop: '3.75rem',
  },
  cardList: {
    display: 'flex',
    flexDirection: 'row',
    width: '94.3%',
    flexWrap: 'wrap',
  },
  error: {
    background: colors.warning,
    color: colors.white,
    fontSize: '12px',
    fontWeight: 500,
    textAlign: 'left',
    border: '1px solid #ebafa6',
    borderRadius: '5px',
    marginTop: '0.375rem',
    padding: '8px',
  },
  inputAlert: {
    justifyContent: 'space-between',
    padding: '0.3rem',
    width: '64.3%',
  },
  infoIcon: {
    color: colorWithWhiteness(colors.accent, 0.1),
    marginLeft: '0.375rem',
  },
});

const searchTrigger = 2;
const searchTooltip = (
  <div style={{ marginLeft: '0.75rem' }}>
    The following special operators can be used to augment search terms:
    <ul style={{ listStylePosition: 'outside' }}>
      <li>
        (*) is the wildcard operator. This operator can be used with a prefix or
        suffix. For example: ceph* (starts with) or *statin (ends with - NOTE:
        when searching for ends with it will only match with end of concept
        name)
      </li>
      <li>
        (-) indicates that this word must <b>not</b> be present. For example:
        lung -cancer
      </li>
      <li>
        (") a phrase that is enclosed within double quote (") characters matches
        only rows that contain the phrase literally, as it was typed. For
        example: "lung cancer"
      </li>
      <li>
        These operators can be combined to produce more complex search
        operations. For example: brain tum* -neoplasm
      </li>
    </ul>
  </div>
);

interface DomainProps {
  conceptDomainCard: ConceptDomainCard;
  browseInDomain: Function;
}
const DomainCard = ({ conceptDomainCard, browseInDomain }: DomainProps) => {
  const conceptCount = conceptDomainCard.conceptCount;
  const { name, participantCount } = conceptDomainCard;
  return (
    <DomainCardBase
      style={{ width: 'calc(25% - 1.5rem)' }}
      data-test-id='domain-box'
    >
      <Clickable
        style={styles.domainBoxHeader}
        onClick={browseInDomain}
        data-test-id='domain-box-name'
      >
        {name}
      </Clickable>
      <div style={styles.conceptText}>
        <span style={{ fontSize: 30 }}>{conceptCount.toLocaleString()}</span>{' '}
        concepts in this domain.
        <div>
          <b>{participantCount.toLocaleString()}</b> participants in domain.
        </div>
      </div>
      <Clickable style={styles.domainBoxLink} onClick={browseInDomain}>
        Select Concepts
      </Clickable>
    </DomainCardBase>
  );
};

interface SurveyProps {
  survey: SurveyModule;
  browseSurvey: Function;
}
const SurveyCard = ({ survey, browseSurvey }: SurveyProps) => {
  return (
    <DomainCardBase style={{ maxHeight: 'auto', width: 'calc(25% - 1.5rem)' }}>
      <Clickable
        style={styles.domainBoxHeader}
        onClick={browseSurvey}
        data-test-id='survey-box-name'
      >
        {survey.name}
      </Clickable>
      <div style={styles.conceptText}>
        <span style={{ fontSize: 30 }}>
          {survey.questionCount.toLocaleString()}
        </span>{' '}
        survey questions with
        <div>
          <b>{survey.participantCount.toLocaleString()}</b> participants
        </div>
      </div>
      <div style={{ ...styles.conceptText, height: '5.25rem' }}>
        {survey.description}
      </div>
      <Clickable style={{ ...styles.domainBoxLink }} onClick={browseSurvey}>
        Select Concepts
      </Clickable>
    </DomainCardBase>
  );
};

interface PhysicalMeasurementProps {
  physicalMeasurementCard: ConceptDomainCard;
  browsePhysicalMeasurements: Function;
}
const PhysicalMeasurementsCard = ({
  physicalMeasurementCard,
  browsePhysicalMeasurements,
}: PhysicalMeasurementProps) => {
  const conceptCount = physicalMeasurementCard.conceptCount;
  const { description, name, participantCount } = physicalMeasurementCard;
  return (
    <DomainCardBase style={{ maxHeight: 'auto', width: '17.25rem' }}>
      <Clickable
        style={styles.domainBoxHeader}
        onClick={browsePhysicalMeasurements}
        data-test-id='pm-box-name'
      >
        {name}
      </Clickable>
      <div style={styles.conceptText}>
        <span style={{ fontSize: 30 }}>{conceptCount.toLocaleString()}</span>{' '}
        physical measurements.
        <div>
          <b>{participantCount.toLocaleString()}</b> participants in this domain
        </div>
      </div>
      <div style={{ ...styles.conceptText, height: 'auto' }}>{description}</div>
      <Clickable
        style={styles.domainBoxLink}
        onClick={browsePhysicalMeasurements}
      >
        Select Concepts
      </Clickable>
    </DomainCardBase>
  );
};

interface Props extends WithSpinnerOverlayProps, NavigationProps {
  workspace: WorkspaceData;
  cohortContext: any;
  concept?: Array<Concept>;
}

interface State {
  // Array of domains and their metadata
  conceptDomainCards: Array<ConceptDomainCard>;
  // Array of surveys
  conceptSurveysList: Array<SurveyModule>;
  // Current string in search box
  currentInputString: string;
  // Last string that was searched
  currentSearchString: string;
  // True if the getDomainInfo call fails
  domainInfoError: boolean;
  // True if the getSurveyInfo call fails
  surveyInfoError: boolean;
  // List of error messages to display if the search input is invalid
  inputErrors: Array<string>;
  // Show if a search error occurred
  showSearchError: boolean;
  // If concept metadata is still being gathered for EHR domains, pm, and surveys
  loadingConceptCounts: boolean;
  // True of findConceptCounts fail
  conceptCountInfoError: boolean;
}

export const ConceptHomepage = fp.flow(
  withCurrentCohortSearchContext(),
  withCurrentConcept(),
  withCurrentWorkspace(),
  withNavigation
)(
  class extends React.Component<Props, State> {
    constructor(props) {
      super(props);
      this.state = {
        conceptDomainCards: [],
        conceptSurveysList: [],
        currentInputString: '',
        currentSearchString: '',
        domainInfoError: false,
        showSearchError: false,
        surveyInfoError: false,
        inputErrors: [],
        loadingConceptCounts: true,
        conceptCountInfoError: false,
      };
    }

    componentDidMount() {
      this.props.hideSpinner();
      this.loadDomainsAndSurveys();
    }

    async loadDomainsAndSurveys() {
      const {
        workspace: { id, namespace },
      } = this.props;
      this.setState({
        domainInfoError: false,
        surveyInfoError: false,
        conceptCountInfoError: false,
      });
      const getDomainCards = cohortBuilderApi()
        .findDomainCards(namespace, id)
        .then((conceptDomainCards) =>
          this.setState({ conceptDomainCards: conceptDomainCards.items })
        )
        .catch((e) => {
          this.setState({ domainInfoError: true });
          console.error(e);
        });
      const getSurveyInfo = cohortBuilderApi()
        .findSurveyModules(namespace, id)
        .then((surveysInfo) =>
          this.setState({ conceptSurveysList: surveysInfo.items })
        )
        .catch((e) => {
          this.setState({ surveyInfoError: true });
          console.error(e);
        });
      await Promise.all([getDomainCards, getSurveyInfo]);
      this.setState({ loadingConceptCounts: false });
    }

    async updateCardCounts() {
      const { id, namespace } = this.props.workspace;
      const { conceptDomainCards, conceptSurveysList, currentInputString } =
        this.state;
      this.setState({
        loadingConceptCounts: true,
        domainInfoError: false,
        surveyInfoError: false,
        conceptCountInfoError: false,
      });

      cohortBuilderApi()
        .findConceptCounts(namespace, id, currentInputString)
        .then((cardList) => {
          conceptDomainCards.forEach((conceptDomainCard) => {
            const cardCount = cardList.items.find(
              (card) => card.domain === conceptDomainCard.domain
            );
            conceptDomainCard.conceptCount = cardCount ? cardCount.count : 0;
          });
          conceptSurveysList.forEach((conceptSurvey) => {
            const cardCount = cardList.items.find(
              (card) => card.name === conceptSurvey.name
            );
            if (cardCount) {
              conceptSurvey.questionCount = cardCount.count;
            } else {
              conceptSurvey.questionCount = 0;
            }
          });
          this.setState({ loadingConceptCounts: false });
        })
        .catch((e) => {
          this.setState({ domainInfoError: true, surveyInfoError: true });
          console.error(e);
        });
      this.setState({ conceptDomainCards, conceptSurveysList });
    }

    handleSearchKeyPress(e) {
      const { currentInputString } = this.state;
      // search on enter key if no forbidden characters are present
      if (e.key === 'Enter') {
        if (currentInputString.trim().length < searchTrigger) {
          this.setState({ inputErrors: [], showSearchError: true });
        } else {
          const inputErrors = validateInputForMySQL(
            currentInputString,
            searchTrigger
          );
          this.setState({ inputErrors, showSearchError: false });
          if (inputErrors.length === 0) {
            this.setState({ currentSearchString: currentInputString }, () => {
              this.updateCardCounts();
            });
          }
        }
      }
    }

    clearSearch() {
      currentConceptStore.next(null);
      currentCohortSearchContextStore.next(undefined);
      this.setState(
        {
          currentInputString: '',
          currentSearchString: '',
          inputErrors: [],
          showSearchError: false,
          loadingConceptCounts: true,
        },
        () => this.loadDomainsAndSurveys()
      );
    }

    browseDomain(domain: Domain, surveyName?: string) {
      const { namespace, id } = this.props.workspace;
      currentCohortSearchContextStore.next({
        domain: domain,
        searchTerms: this.state.currentSearchString,
        surveyName,
      });
      const url = `workspaces/${namespace}/${id}/data/concepts/${domain}`;

      this.props.navigateByUrl(
        url,
        surveyName ? { queryParams: { survey: surveyName } } : {}
      );
    }

    errorMessage() {
      return (
        <div style={styles.error}>
          <ClrIcon
            style={{ margin: '0 0.75rem 0 0.375rem' }}
            className='is-solid'
            shape='exclamation-triangle'
            size='22'
          />
          Sorry, the request cannot be completed. Please try refreshing the page
          or contact Support in the left hand navigation.
        </div>
      );
    }

    render() {
      const {
        conceptDomainCards,
        conceptSurveysList,
        currentInputString,
        currentSearchString,
        domainInfoError,
        inputErrors,
        surveyInfoError,
        showSearchError,
        loadingConceptCounts,
        conceptCountInfoError,
      } = this.state;
      const domainCards = conceptDomainCards.filter(
        (domain) => domain.domain !== Domain.PHYSICAL_MEASUREMENTCSS
      );
      const physicalMeasurementsCard = conceptDomainCards.find(
        (domain) => domain.domain === Domain.PHYSICAL_MEASUREMENTCSS
      );
      return (
        <React.Fragment>
          <FadeBox
            style={{ margin: 'auto', paddingTop: '1.5rem', width: '95.7%' }}
          >
            <div style={{ display: 'flex', alignItems: 'center' }}>
              <ClrIcon
                shape='search'
                style={{
                  position: 'absolute',
                  height: '1.5rem',
                  width: '1.5rem',
                  fill: colors.accent,
                  left: 'calc(1.5rem + 3.5%)',
                }}
              />
              <TextInput
                style={styles.searchBar}
                data-test-id='concept-search-input'
                placeholder='Search concepts in domain'
                value={currentInputString}
                onChange={(e) => this.setState({ currentInputString: e })}
                onKeyPress={(e) => this.handleSearchKeyPress(e)}
              />
              {currentSearchString !== '' && (
                <Clickable
                  onClick={() => this.clearSearch()}
                  data-test-id='clear-search'
                >
                  <ClrIcon
                    shape='times-circle'
                    style={styles.clearSearchIcon}
                  />
                </Clickable>
              )}
              <TooltipTrigger side='top' content={searchTooltip}>
                <ClrIcon
                  style={styles.infoIcon}
                  className='is-solid'
                  shape='info-standard'
                />
              </TooltipTrigger>
            </div>
            {inputErrors.map((error, e) => (
              <AlertDanger key={e} style={styles.inputAlert}>
                <span data-test-id='input-error-alert'>{error}</span>
              </AlertDanger>
            ))}
            {showSearchError && (
              <AlertDanger style={styles.inputAlert}>
                {`Minimum concept search length is ${searchTrigger} characters.`}
                <AlertClose
                  style={{ width: 'unset' }}
                  onClick={() => this.setState({ showSearchError: false })}
                />
              </AlertDanger>
            )}
            {currentInputString === '' && loadingConceptCounts ? (
              <div style={{ position: 'relative', minHeight: '15rem' }}>
                <SpinnerOverlay />
              </div>
            ) : (
              <div>
                <div style={styles.sectionHeader}>Domains</div>
                <div style={styles.cardList}>
                  {conceptCountInfoError || domainInfoError ? (
                    this.errorMessage()
                  ) : (
                    <React.Fragment>
                      {loadingConceptCounts ? (
                        <Spinner size={42} />
                      ) : domainCards.every(
                          (domain) => domain.conceptCount === 0
                        ) ? (
                        'No Domain Results. Please type in a new search term.'
                      ) : (
                        domainCards
                          .filter((domain) => domain.conceptCount !== 0)
                          .map((domain, i) => (
                            <DomainCard
                              conceptDomainCard={domain}
                              browseInDomain={() =>
                                this.browseDomain(domain.domain)
                              }
                              key={i}
                              data-test-id='domain-box'
                            />
                          ))
                      )}
                    </React.Fragment>
                  )}
                </div>
                <div style={styles.sectionHeader}>Survey Questions</div>
                <div style={styles.cardList}>
                  {conceptCountInfoError || surveyInfoError ? (
                    this.errorMessage()
                  ) : loadingConceptCounts ? (
                    <Spinner size={42} />
                  ) : conceptSurveysList.every(
                      (survey) => survey.questionCount === 0
                    ) ? (
                    'No Survey Question Results. Please type in a new search term.'
                  ) : (
                    conceptSurveysList
                      .filter((survey) => survey.questionCount > 0)
                      .map((survey) => (
                        <SurveyCard
                          survey={survey}
                          key={survey.orderNumber}
                          browseSurvey={() =>
                            this.browseDomain(Domain.SURVEY, survey.name)
                          }
                        />
                      ))
                  )}
                </div>
                {!!physicalMeasurementsCard && (
                  <React.Fragment>
                    <div style={styles.sectionHeader}>
                      Program Physical Measurements
                    </div>
                    <div style={{ ...styles.cardList, marginBottom: '1.5rem' }}>
                      {conceptCountInfoError || domainInfoError ? (
                        this.errorMessage()
                      ) : loadingConceptCounts ? (
                        <Spinner size={42} />
                      ) : physicalMeasurementsCard.conceptCount === 0 ? (
                        'No Program Physical Measurement Results. Please type in a new search term.'
                      ) : (
                        <PhysicalMeasurementsCard
                          physicalMeasurementCard={physicalMeasurementsCard}
                          browsePhysicalMeasurements={() =>
                            this.browseDomain(Domain.PHYSICAL_MEASUREMENTCSS)
                          }
                        />
                      )}
                    </div>
                  </React.Fragment>
                )}
              </div>
            )}
          </FadeBox>
        </React.Fragment>
      );
    }
  }
);
