import * as fp from 'lodash/fp';
import * as moment from 'moment';
import {RadioButton} from 'primereact/radiobutton';
import * as React from 'react';
import {validate, validators} from 'validate.js';

import {DatePicker, NumberInput, Select, ValidationError} from 'app/components/inputs';
import {filterStateStore, reviewPaginationStore, visitsFilterOptions} from 'app/services/review-state.service';
import {cohortReviewApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {reactStyles, summarizeErrors, withCurrentCohortReview, withCurrentWorkspace} from 'app/utils';
import {triggerEvent} from 'app/utils/analytics';
import {currentCohortReviewStore, currentWorkspaceStore, navigate, urlParamsStore} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {
  CohortReview,
  Filter,
  FilterColumns as Columns,
  Operator,
  PageFilterRequest,
  ParticipantCohortStatus,
  SortOrder
} from 'generated/fetch';

validators.dateFormat = (value: string) => {
  return moment(value, 'YYYY-MM-DD', true).isValid() ? null : 'must be in format \'YYYY-MM-DD\'';
};

const css = `
  .error-messages > div {
    margin: 0 0 0 25% !important;
    line-height: 1;
  }
  .detail-page .global-filters {
    width: 40%;
  }
  .detail-page.sidebar-open .global-filters {
    width: 50%;
  }
`;
const EVENT_CATEGORY = 'Review Individual';

const styles = reactStyles({
  backBtn: {
    padding: 0,
    border: 0,
    fontSize: '14px',
    color: colors.accent,
    background: 'transparent',
    cursor: 'pointer',
  },
  title: {
    marginTop: 0,
    fontSize: '20px',
    fontWeight: 600,
    color: colors.primary,
  },
  description: {
    margin: '0.5rem 0',
    color: colors.primary,
  },
  headerSection: {
    float: 'left',
    height: '100%',
    marginRight: '2%',
    border: '1px solid #cccccc',
    borderRadius: '5px',
    background: '#fafafa',
  },
  navBtn: {
    display: 'inline-block',
    fontSize: '12px',
    padding: '5px',
    borderRadius: '3px',
  },
  icon: {
    display: 'block',
    fontSize: '12px',
  },
  participantText: {
    fontSize: '14px',
    color: colors.primary,
  },
  filterHeader: {
    height: '38%',
    marginBottom: '0.3rem',
    borderBottom: '1px solid #216fb4',
  },
  filterTab: {
    height: '100%',
    margin: '0 4% 0 1%',
    padding: '5px 7px 0',
    fontSize: '12px',
    color: colors.accent,
    border: 0,
    borderBottom: 0,
    background: 'transparent',
    cursor: 'pointer',
  },
  filterBody: {
    paddingLeft: '0.5rem',
    fontSize: '12px',
  },
  filterDiv: {
    float: 'left',
    marginRight: '0.5rem'
  },
  resetBtn: {
    float: 'right',
    lineHeight: '16px',
    padding: '0.5px 3.5px',
    margin: '6px 5px 0 0',
    fontSize: '10px',
    color: colors.accent,
    border: '1px solid ' + colors.accent,
    borderRadius: '3px',
    background: 'transparent',
    cursor: 'pointer',
  },
  validation: {
    margin: '0 0 0 25%',
    lineHeight: 1,
  }
});
const otherStyles = {
  navigation: {
    ...styles.headerSection,
    width: '22%',
    minWidth: '8.5rem',
    padding: '1.15rem 0.4rem'
  },
  filters: {
    ...styles.headerSection,
    minWidth: '16rem'
  },
  radios: {
    ...styles.headerSection,
    fontSize: '12px',
    width: '24%',
    minWidth: '8.5rem',
    padding: '0.5rem 0 0.5rem 0.5rem',
  },
  navBtnActive: {
    ...styles.navBtn,
    color: colors.accent,
    border: '1px solid ' + colors.accent,
    cursor: 'pointer',
  },
  navBtnDisabled: {
    ...styles.navBtn,
    color: '#cccccc',
    border: '1px solid #cccccc',
    cursor: 'not-allowed',
  },
  tabActive: {
    ...styles.filterTab,
    borderBottom: '4px solid #216FB4',
    fontWeight: 600,
  },
  filterLabel: {
    ...styles.filterDiv,
    marginTop: '4px'
  },
  filterInput: {
    ...styles.filterDiv,
    width: '30%',
  },
  filterSelect: {
    ...styles.filterDiv,
    marginLeft: '1rem',
    width: '70%'
  },
  filterText: {
    ...styles.filterDiv,
    marginTop: '4px ',
  }
};
const FILTER_KEYS = {
  AGE: 'Age',
  DATE: 'Date',
  VISITS: 'Visits'
};
export interface DetailHeaderProps {
  cohortReview: CohortReview;
  participant: ParticipantCohortStatus;
  workspace: WorkspaceData;
}

export interface DetailHeaderState {
  isFirstParticipant: boolean;
  isLastParticipant: boolean;
  priorId: number;
  afterId: number;
  filterState: any;
  filterTab: string;
}

export const DetailHeader = fp.flow(withCurrentCohortReview(), withCurrentWorkspace())(
  class extends React.Component<DetailHeaderProps, DetailHeaderState> {
    constructor(props: DetailHeaderProps) {
      super(props);
      this.state = {
        isFirstParticipant: undefined,
        isLastParticipant: undefined,
        priorId: undefined,
        afterId: undefined,
        filterState: filterStateStore.getValue(),
        filterTab: FILTER_KEYS.DATE
      };
    }

    componentDidMount() {
      this.update();
    }

    componentDidUpdate(prevProps: any) {
      if (prevProps.participant !== this.props.participant) {
        this.update();
      }
    }

    update = () => {
      const {cohortReview: {participantCohortStatuses}, participant} = this.props;
      const pagination = reviewPaginationStore.getValue();
      const id = participant && participant.participantId;
      const index = participantCohortStatuses.findIndex(({participantId}) => participantId === id);

      // The participant is not on the current page... for now, just log it and ignore it
      // We get here by URL (when a direct link to a detail page is shared, for example)
      if (index < 0) {
        console.log('Participant not on page');
        // For now, disable next / prev entirely
        this.setState({
          isFirstParticipant: true,
          isLastParticipant: true,
        });
        return;
      }

      const totalPages = Math.ceil(pagination.queryResultSize / pagination.pageSize);

      this.setState({
        afterId: participantCohortStatuses[index + 1] && participantCohortStatuses[index + 1]['participantId'],
        isFirstParticipant: pagination.page === 0 && index === 0,
        isLastParticipant: (pagination.page + 1) === totalPages && (index + 1) === participantCohortStatuses.length,
        priorId: participantCohortStatuses[index - 1] && participantCohortStatuses[index - 1]['participantId']
      });
    }

    backToTable() {
      const {ns, wsid, cid} = urlParamsStore.getValue();
      navigate(['/workspaces', ns, wsid, 'data', 'cohorts', cid, 'review', 'participants']);
    }

    previous = () => {
      this.navigate(true);
    }

    next = () => {
      this.navigate(false);
    }

    navigate = (left: boolean) => {
      const {afterId, isFirstParticipant, isLastParticipant, priorId} = this.state;
      const id = left ? priorId : afterId;
      const hasNext = !(left ? isFirstParticipant : isLastParticipant);

      if (id !== undefined) {
        this.navigateById(id);
      } else if (hasNext) {
        const statusGetter = (statuses: ParticipantCohortStatus[]) => left
          ? statuses[statuses.length - 1]
          : statuses[0];

        const {page, pageSize} = reviewPaginationStore.getValue();
        const {ns, wsid, cid} = urlParamsStore.getValue();
        const {cdrVersionId} = currentWorkspaceStore.getValue();
        const request = {
          page: left ? page - 1 : page + 1,
          pageSize: pageSize,
          sortOrder: SortOrder.Asc,
          filters: {items: this.getRequestFilters()}
        } as PageFilterRequest;
        cohortReviewApi().getParticipantCohortStatuses(ns, wsid, cid, +cdrVersionId, request).then(response => {
          currentCohortReviewStore.next(response.cohortReview);
          const status = statusGetter(response.cohortReview.participantCohortStatuses);
          this.navigateById(status.participantId);
        });
      }
    }

    navigateById = (id: number): void => {
      const {ns, wsid, cid} = urlParamsStore.getValue();
      navigate(['/workspaces', ns, wsid, 'data', 'cohorts', cid, 'review', 'participants', id]);
    }

    getRequestFilters = () => {
      const filters = filterStateStore.getValue().participants;
      return Object.keys(filters).reduce((acc, _type) => {
        const values = filters[_type];
        if (_type === Columns[Columns.PARTICIPANTID] && values) {
          acc.push({
            property: Columns[_type],
            values: [values],
            operator: Operator.LIKE
          } as Filter);
        } else if (values.length && !values.includes('Select All')) {
          acc.push({
            property: Columns[_type],
            values: values,
            operator: Operator.IN
          } as Filter);
        }
        return acc;
      }, []);
    }

    vocabChange = (event: any) => {
      const {value} = event;
      triggerEvent(
        EVENT_CATEGORY,
        'Click',
        `View ${value.charAt(0).toUpperCase() + value.slice(1)} - Review Individual`
      );
      const {filterState} = this.state;
      filterState.vocab = value;
      filterStateStore.next(filterState);
      this.setState({filterState: filterState});
    }
    setFilterTab = (filterTab: string) => {
      triggerEvent(EVENT_CATEGORY, 'Click', `Filter - ${filterTab} - Review Individual`);
      this.setState({filterTab});
    }

    setFilter = (value: any, type: string) => {
      const {filterState} = this.state;
      filterState.global[type] = value;
      filterStateStore.next(filterState);
      this.setState({filterState: filterState});
    }

    clearFilters = () => {
      triggerEvent(EVENT_CATEGORY, 'Click', 'Filter - Reset - Review Individual');
      const {filterState} = this.state;
      filterState.global = {
        dateMin: null,
        dateMax: null,
        ageMin: '',
        ageMax: '',
        visits: null
      };
      filterStateStore.next(filterState);
      this.setState({filterState: filterState});
    }

    render() {
      const {cohortReview: {cohortName, description}, participant} = this.props;
      const {
        filterState: {global: {ageMin, ageMax, dateMin, dateMax, visits}},
        filterState,
        filterTab,
        isFirstParticipant,
        isLastParticipant
      } = this.state;
      const errors = validate({ageMin, ageMax, dateMin, dateMax}, {
        ageMin: {
          numericality: {
            onlyInteger: true,
            greaterThanOrEqualTo: 0,
            lessThanOrEqualTo: 120,
            message: 'must be a whole number 0 - 120'
          }
        },
        ageMax: {
          numericality: {
            onlyInteger: true,
            greaterThanOrEqualTo: 0,
            lessThanOrEqualTo: 120,
            message: 'must be a whole number 0 - 120'
          }
        },
        dateMin: {
          dateFormat: {}
        },
        dateMax: {
          dateFormat: {}
        }
      });
      return <div className='detail-header'>
        <style>{css}</style>
        <button
          style={styles.backBtn}
          type='button'
          title='Go Back to the review set table'
          onClick={() => this.backToTable()}>
          Back to review set
        </button>
        <h4 style={styles.title}>{cohortName}</h4>
        <div style={styles.description}>{description}</div>
        {errors && <div className='error-messages'>
          <ValidationError>
            {summarizeErrors(errors && (
              (filterTab === FILTER_KEYS.AGE && ((ageMin && errors.ageMin) || (ageMax && errors.ageMax))) ||
              (filterTab === FILTER_KEYS.DATE && ((dateMin && errors.dateMin) || (dateMax && errors.dateMax)))
            ))}
          </ValidationError>
        </div>}
        <div style={{height: '3.5rem'}}>
          <div style={{...otherStyles.navigation, textAlign: 'center'}}>
            <button
              style={{
                ...(isFirstParticipant ? otherStyles.navBtnDisabled : otherStyles.navBtnActive),
                float: 'left',
              }}
              type='button'
              title='Go To the Prior Participant'
              disabled={isFirstParticipant}
              onClick={() => this.previous()}>
              <i style={styles.icon} className='pi pi-angle-left' />
            </button>
            <span style={styles.participantText}>Participant {participant.participantId}</span>
            <button
              style={{
                ...(isLastParticipant ? otherStyles.navBtnDisabled : otherStyles.navBtnActive),
                float: 'right',
              }}
              type='button'
              title='Go To the Next Participant'
              disabled={isLastParticipant}
              onClick={() => this.next()}>
              <i style={styles.icon} className='pi pi-angle-right' />
            </button>
          </div>
          <div style={otherStyles.filters} className={'global-filters'}>
            <div style={styles.filterHeader}>
              <button
                style={filterTab === FILTER_KEYS.DATE ? otherStyles.tabActive : styles.filterTab}
                onClick={() => this.setFilterTab(FILTER_KEYS.DATE)}>
                Date Range
              </button>
              <button
                style={filterTab === FILTER_KEYS.AGE ? otherStyles.tabActive : styles.filterTab}
                onClick={() => this.setFilterTab(FILTER_KEYS.AGE)}>
                Age Range
              </button>
              <button
                style={filterTab === FILTER_KEYS.VISITS ? otherStyles.tabActive : styles.filterTab}
                onClick={() => this.setFilterTab(FILTER_KEYS.VISITS)}>
                Visits
              </button>
              <button
                style={styles.resetBtn}
                onClick={() => this.clearFilters()}>
                RESET FILTER
              </button>
            </div>
            <div style={styles.filterBody}>
              {filterTab === FILTER_KEYS.DATE && <div>
                <div style={otherStyles.filterLabel}>
                  Date Range:
                </div>
                <div style={otherStyles.filterInput}>
                  <DatePicker
                    value={dateMin}
                    onChange={v => this.setFilter(v, 'dateMin')}
                    maxDate={new Date()}
                  />
                </div>
                <div style={otherStyles.filterText}>
                  and
                </div>
                <div style={otherStyles.filterInput}>
                  <DatePicker
                    value={dateMax}
                    onChange={v => this.setFilter(v, 'dateMax')}
                    maxDate={new Date()}
                  />
                </div>
              </div>}
              {filterTab === FILTER_KEYS.AGE && <div>
                <div style={otherStyles.filterLabel}>
                  Age Range:
                </div>
                <div style={otherStyles.filterInput}>
                  <NumberInput
                    min='0'
                    max='120'
                    value={ageMin}
                    onChange={(e) => this.setFilter(e, 'ageMin')}
                  />
                </div>
                <div style={otherStyles.filterText}>
                  and
                </div>
                <div style={otherStyles.filterInput}>
                  <NumberInput
                    min='0'
                    max='120'
                    value={ageMax}
                    onChange={(e) => this.setFilter(e, 'ageMax')}
                  />
                </div>
              </div>}
              {filterTab === FILTER_KEYS.VISITS && <div>
                <div style={otherStyles.filterLabel}>
                  Visits:
                </div>
                <div style={otherStyles.filterSelect}>
                  <Select
                    options={visitsFilterOptions.getValue()}
                    value={visits}
                    onChange={(e) => this.setFilter(e, 'visits')}
                    theme={(theme) => ({
                      ...theme,
                      colors: {
                        ...theme.colors,
                        primary: '#216FB4',
                      },
                    })}
                  />
                </div>
              </div>}
            </div>
          </div>
          <div style={{...otherStyles.radios, marginRight: 0}}>
            <div style={{marginBottom: '0.5rem'}}>
              <RadioButton
                name='vocab'
                value='source'
                onChange={this.vocabChange}
                checked={filterState.vocab === 'source'} />
              <label className='p-radiobutton-label'>View Source Concepts</label>
            </div>
            <div>
              <RadioButton
                name='vocab'
                value='standard'
                onChange={this.vocabChange}
                checked={filterState.vocab === 'standard'} />
              <label className='p-radiobutton-label'>View Standard Concepts</label>
            </div>
          </div>
        </div>
      </div>;
    }
  }
);
