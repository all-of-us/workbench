import * as React from 'react';
import { useParams } from 'react-router';
import * as fp from 'lodash/fp';
import { Dropdown } from 'primereact/dropdown';

import {
  CdrVersionTiersResponse,
  Criteria,
  CriteriaType,
  Domain,
  Modifier,
  ModifierType,
  Operator,
} from 'generated/fetch';

import { ClrIcon } from 'app/components/icons';
import { DatePicker, NumberInput } from 'app/components/inputs';
import { TooltipTrigger } from 'app/components/popups';
import { Spinner } from 'app/components/spinners';
import { CalculateFooter } from 'app/pages/data/cohort/attributes-page';
import { encountersStore } from 'app/pages/data/cohort/search-state.service';
import { domainToTitle, mapParameter } from 'app/pages/data/cohort/utils';
import { cohortBuilderApi } from 'app/services/swagger-fetch-clients';
import {
  reactStyles,
  withCdrVersions,
  withCurrentCohortSearchContext,
  withCurrentWorkspace,
} from 'app/utils';
import { AnalyticsTracker } from 'app/utils/analytics';
import { findCdrVersion } from 'app/utils/cdr-versions';
import { currentCohortSearchContextStore } from 'app/utils/navigation';
import { MatchParams, serverConfigStore } from 'app/utils/stores';
import { WorkspaceData } from 'app/utils/workspace-data';
import moment from 'moment';

const { useEffect, useState } = React;

const styles = reactStyles({
  header: {
    color: '#262262',
    fontWeight: 600,
    fontSize: '16px',
    borderBottom: '1px solid #262262',
    paddingBottom: '0.75rem',
  },
  errors: {
    background: '#f5dbd9',
    color: '#565656',
    fontSize: '11px',
    border: '1px solid #ebafa6',
    borderRadius: '3px',
    marginTop: '0.375rem',
    padding: '3px 5px',
  },
  errorItem: {
    lineHeight: '16px',
  },
  label: {
    color: '#262262',
    fontWeight: 500,
  },
  modifier: {
    marginTop: '0.6rem',
  },
  select: {
    width: '18rem',
    height: '2.4rem',
    paddingLeft: '0.75rem',
    marginRight: '1.5rem',
  },
  date: {
    width: '9.75rem',
    display: 'inline-block',
  },
  count: {
    display: 'inline-flex',
    height: '0.9375rem',
    background: '#0079b8',
    color: '#ffffff',
    fontSize: '10px',
    padding: '0 0.25rem',
    borderRadius: '10px',
    verticalAlign: 'middle',
    alignItems: 'center',
  },
  info: {
    color: '#0077b7',
    marginLeft: '0.375rem',
  },
  footer: {
    position: 'absolute',
    bottom: '1.5rem',
  },
  row: {
    display: 'flex',
    flexWrap: 'wrap',
    marginRight: '-.75rem',
    marginLeft: '-.75rem',
  },
  col: {
    position: 'relative',
    minHeight: '1px',
    width: '100%',
    paddingLeft: '0.75rem',
    paddingRight: '0.75rem',
  },
  button: {
    color: '#ffffff',
    height: '2.25rem',
    margin: '0.375rem 0.75rem 0.375rem 0',
    borderRadius: '3px',
  },
  previewCount: {
    color: '#4a4a4a',
    fontWeight: 'bold',
  },
  error: {
    background: '#F7981C',
    color: '#ffffff',
    fontSize: '12px',
    fontWeight: 500,
    textAlign: 'left',
    border: '1px solid #ebafa6',
    borderRadius: '5px',
    marginTop: '0.375rem',
    padding: '8px',
  },
  addButton: {
    height: '3rem',
    borderRadius: '5px',
    fontWeight: 600,
    marginRight: '0.75rem',
  },
});

const validatorFuncs = {
  AGE_AT_EVENT: (value) => {
    if (value === '') {
      return 'Age At Event is required';
    }
    if (value < 0 || value > 120) {
      return 'Age At Event must be between 0 - 120';
    }
    if (!Number.isInteger(parseFloat(value))) {
      return 'Age At Event must be a whole number';
    }
    return null;
  },
  EVENT_DATE: (value) => {
    if (value === '') {
      return 'Event Date is required';
    }
    if (!moment(value, 'YYYY-MM-DD', true).isValid()) {
      return "Dates must be in format 'YYYY-MM-DD'";
    }
    return null;
  },
  NUM_OF_OCCURRENCES: (value) => {
    if (value === '') {
      return 'Number Of Occurrence Dates is required';
    }
    if (value < 1 || value > 99) {
      return 'Number Of Occurrence Dates must be between 1 - 99';
    }
    if (!Number.isInteger(parseFloat(value))) {
      return 'Number Of Occurrence Dates must be a whole number';
    }
    return null;
  },
};

const dateTooltip = `Dates are consistently shifted within a participant’s record
      by a time period of up to 364 days backwards to de-identify patient data.
      The date shift differs across participants.`;

const getDefaultFormState = () => {
  const defaultFormState = {
    name: ModifierType.AGEATEVENT,
    label: 'Age At Event',
    type: 'number',
    operator: undefined,
    values: [undefined, undefined],
    options: [
      {
        label: 'Any',
        value: undefined,
      },
      {
        label: 'Greater Than or Equal To',
        value: Operator.GREATERTHANOREQUALTO,
      },
      {
        label: 'Less Than or Equal To',
        value: Operator.LESSTHANOREQUALTO,
      },
      {
        label: 'Between',
        value: Operator.BETWEEN,
      },
    ],
  };
  // Object.assign prevents changes from being passed back to default state
  return [Object.assign({}, defaultFormState)];
};

interface Selection extends Criteria {
  parameterId: string;
}

interface Props {
  cdrVersionTiersResponse: CdrVersionTiersResponse;
  closeModifiers: (modifiers?: Array<Modifier>) => void;
  cohortContext: any;
  selections: Array<Selection>;
  workspace: WorkspaceData;
}

export const ModifierPage = fp.flow(
  withCdrVersions(),
  withCurrentWorkspace(),
  withCurrentCohortSearchContext()
)(
  ({
    cdrVersionTiersResponse,
    closeModifiers,
    cohortContext,
    selections,
    workspace,
  }: Props) => {
    const { ns, wsid } = useParams<MatchParams>();
    const [calculateError, setCalculateError] = useState(false);
    const [calculating, setCalculating] = useState(false);
    const [count, setCount] = useState(null);
    const [formErrors, setFormErrors] = useState([]);
    const [formState, setFormState] = useState(getDefaultFormState());
    const [formUntouched, setFormUntouched] = useState(false);
    const [initialFormState, setInitialFormState] = useState(true);
    const [loading, setLoading] = useState(true);
    const [visitCounts, setVisitCounts] = useState(undefined);

    const addEncounters = () => {
      return ![
        Domain.PHYSICALMEASUREMENT,
        Domain.SURVEY,
        Domain.VISIT,
      ].includes(cohortContext.domain);
    };

    const getExisting = () => {
      // This reseeds the form state with existing data if we're editing an existing item
      cohortContext.item.modifiers.forEach((existing) => {
        const index = formState.findIndex((mod) => existing.name === mod.name);
        if (index > -1) {
          const mod = formState[index];
          const values = existing.operands.filter((val) => !!val);
          formState[index] = {
            ...mod,
            operator: [ModifierType.CATI, ModifierType.ENCOUNTERS].includes(
              mod.name
            )
              ? +existing.operands[0]
              : existing.operator,
            values:
              mod.name === ModifierType.EVENTDATE
                ? values.map((val) => new Date(val + 'T08:00:00'))
                : values,
          };
        }
      });
      setFormState(formState);
      setLoading(false);
    };

    const initModifiersForm = async () => {
      if (cohortContext.domain !== Domain.SURVEY) {
        formState.push({
          name: ModifierType.NUMOFOCCURRENCES,
          label: 'Number Of Occurrence Dates',
          type: 'number',
          operator: undefined,
          values: [undefined, undefined],
          options: [
            {
              label: 'Any',
              value: undefined,
            },
            {
              label: 'N or More',
              value: Operator.GREATERTHANOREQUALTO,
            },
          ],
        });
      } else {
        const cdrVersion = findCdrVersion(
          workspace.cdrVersionId,
          cdrVersionTiersResponse
        );
        // Add CATI modifier for cdrs with hasSurveyConductData
        if (cdrVersion.hasSurveyConductData) {
          formState.push({
            name: ModifierType.CATI,
            label: 'CATI(Computer Assisted Telephone Interview)',
            type: null,
            operator: undefined,
            values: [undefined],
            options: [
              {
                label: 'Any',
                value: undefined,
              },
              {
                label: 'CATI(Computer Assisted Telephone Interview)',
                value: 42530794,
              },
              {
                label: 'Non-CATI(Non Computer Assisted Telephone Interview)',
                value: 42531021,
              },
            ],
          });
        }
      }
      if (serverConfigStore.get().config.enableEventDateModifier) {
        formState.push({
          name: ModifierType.EVENTDATE,
          label: 'Event Date',
          type: 'date',
          operator: undefined,
          values: [undefined, undefined],
          options: [
            {
              label: 'Any',
              value: undefined,
            },
            {
              label: 'Is On or Before',
              value: Operator.LESSTHANOREQUALTO,
            },
            {
              label: 'Is On or After',
              value: Operator.GREATERTHANOREQUALTO,
            },
            {
              label: 'Is Between',
              value: Operator.BETWEEN,
            },
          ],
        });
      }
      if (addEncounters()) {
        let encountersOptions = encountersStore.getValue();
        if (!encountersOptions) {
          // get options for visit modifier from api
          const res = await cohortBuilderApi().findCriteriaBy(
            ns,
            wsid,
            Domain.VISIT.toString(),
            CriteriaType.VISIT.toString()
          );
          encountersOptions = res.items;
          encountersStore.next(encountersOptions);
        }
        const initVisitCounts = {};
        const encounters = {
          name: ModifierType.ENCOUNTERS,
          label: 'During Visit Type',
          type: null,
          operator: undefined,
          values: [undefined],
          options: [
            {
              label: 'Any',
              value: undefined,
            },
          ],
        };
        encountersOptions.forEach((option) => {
          if (option.count > 0) {
            encounters.options.push({
              label: option.name,
              value: option.conceptId,
            });
            initVisitCounts[option.conceptId] = option.count;
          }
        });
        formState.push(encounters);
        setVisitCounts(initVisitCounts);
      }
      setFormState(formState);
      getExisting();
    };

    useEffect(() => {
      initModifiersForm();
    }, []);

    const validateValues = () => {
      let initialState = true;
      let untouched = false;
      const errors = formState.reduce((acc, item) => {
        if (![ModifierType.CATI, ModifierType.ENCOUNTERS].includes(item.name)) {
          item.values.forEach((val, v) => {
            if (val !== undefined) {
              initialState = false;
              const error = validatorFuncs[item.name](val);
              if (error) {
                acc.add(error);
              }
            } else if (item.operator !== undefined) {
              initialState = false;
              if (v === 0 || (v === 1 && item.operator === Operator.BETWEEN)) {
                untouched = true;
              }
            }
          });
        } else if (item.values[0] !== undefined) {
          initialState = false;
        }
        return acc;
      }, new Set());
      setFormErrors(Array.from(errors));
      setFormUntouched(untouched);
      setInitialFormState(initialState);
    };

    const selectChange = (sel: any, index: number) => {
      AnalyticsTracker.CohortBuilder.ModifierDropdown(formState[index].label);
      const { name } = formState[index];
      if ([ModifierType.CATI, ModifierType.ENCOUNTERS].includes(name)) {
        formState[index].values = [sel];
      } else if (!sel) {
        formState[index].values = [undefined, undefined];
      } else if (sel !== Operator.BETWEEN) {
        formState[index].values[1] = undefined;
      }
      formState[index].operator = sel;
      setCount(null);
      setFormState(formState);
      validateValues();
    };

    const inputChange = (index: number, field: string, value: any) => {
      formState[index].values[field] = value;
      setCount(null);
      setFormState(formState);
      validateValues();
    };

    const getModifiersFromForm = () => {
      return formState.reduce((acc, mod) => {
        const { name, operator, values } = mod;
        if (operator) {
          switch (name) {
            case ModifierType.CATI:
              acc.push({
                name,
                operator: Operator.IN,
                operands: [operator.toString()],
              });
              break;
            case ModifierType.ENCOUNTERS:
              acc.push({
                name,
                operator: Operator.IN,
                operands: [operator.toString()],
              });
              break;
            case ModifierType.EVENTDATE:
              const formatted = values.map((val) =>
                moment(val, 'YYYY-MM-DD', true).isValid()
                  ? moment(val).format('YYYY-MM-DD')
                  : undefined
              );
              acc.push({
                name,
                operator,
                operands: formatted.filter((val) => !!val),
              });
              break;
            default:
              acc.push({
                name,
                operator,
                operands: values.filter(
                  (val) => !['', null, undefined].includes(val)
                ),
              });
          }
        }
        return acc;
      }, []);
    };

    const updateMods = () => {
      AnalyticsTracker.CohortBuilder.ModifiersAction(
        `Apply modifiers - ${domainToTitle(cohortContext.domain)}`
      );
      cohortContext.item.modifiers = getModifiersFromForm();
      currentCohortSearchContextStore.next(cohortContext);
      closeModifiers(cohortContext.item.modifiers);
    };

    const calculate = async () => {
      const { domain, role } = cohortContext;
      const { id, namespace } = workspace;
      AnalyticsTracker.CohortBuilder.ModifiersAction(
        `Calculate - ${domainToTitle(domain)}`
      );
      try {
        setCalculateError(false);
        setCalculating(true);
        setCount(null);
        const request = {
          includes: [],
          excludes: [],
          [role]: [
            {
              items: [
                {
                  type: domain,
                  searchParameters: selections.map(mapParameter),
                  modifiers: getModifiersFromForm(),
                },
              ],
            },
          ],
          dataFilters: [],
        };
        await cohortBuilderApi()
          .countParticipants(namespace, id, request)
          .then((response) => {
            setCalculating(false);
            setCount(response);
          });
      } catch (error) {
        console.error(error);
        setCalculateError(true);
        setCalculating(false);
      }
    };

    const optionTemplate = (opt: any, name: any) => {
      if (name !== ModifierType.ENCOUNTERS || !opt.value) {
        return opt.label;
      }
      return (
        <div className='p-clearfix'>
          {opt.label}
          &nbsp;
          <span style={styles.count}>
            {visitCounts[opt.value].toLocaleString()}
          </span>
        </div>
      );
    };

    const renderInput = (index: number, field: string, type) => {
      const { values } = formState[index];
      switch (type) {
        case 'number':
          return (
            <NumberInput
              style={{ padding: '0 0.375rem', width: '4.5rem' }}
              value={values[field]}
              min={0}
              onChange={(v) => inputChange(index, field, v)}
            />
          );
        case 'date':
          return (
            <div style={styles.date}>
              <DatePicker
                value={values[field]}
                placeholder='YYYY-MM-DD'
                onChange={(e) => inputChange(index, field, e)}
                maxDate={new Date()}
              />
            </div>
          );
      }
    };

    return (
      <div id='modifiers-form'>
        <h3 style={{ ...styles.header, marginTop: 0 }}>
          Apply optional Modifiers
        </h3>
        <div style={{ marginTop: '1.5rem' }}>
          <div>
            The following modifiers are optional and apply to all selected
            criteria
          </div>
          {calculateError && (
            <div style={styles.error}>
              <ClrIcon
                style={{ margin: '0 0.75rem 0 0.375rem' }}
                className='is-solid'
                shape='exclamation-triangle'
                size='22'
              />
              Sorry, the request cannot be completed. Please try again or
              contact Support in the left hand navigation.
            </div>
          )}
          {formErrors.length > 0 && (
            <div style={styles.errors}>
              {formErrors.map((err, e) => (
                <div key={e} style={styles.errorItem}>
                  {err}
                </div>
              ))}
            </div>
          )}
          {loading ? (
            <div style={{ margin: '1.5rem 0 3rem', textAlign: 'center' }}>
              <Spinner />
            </div>
          ) : (
            formState.map((mod, i) => {
              const { label, name, options, operator } = mod;
              return (
                <div
                  data-test-id={name}
                  key={i}
                  style={{ marginTop: '1.125rem' }}
                >
                  <label style={styles.label}>{label}</label>
                  {name === ModifierType.EVENTDATE && (
                    <TooltipTrigger content={<div>{dateTooltip}</div>}>
                      <ClrIcon
                        style={styles.info}
                        className='is-solid'
                        shape='info-standard'
                      />
                    </TooltipTrigger>
                  )}
                  <div style={styles.modifier}>
                    <Dropdown
                      value={operator}
                      appendTo='self'
                      style={styles.select}
                      onChange={(e) => selectChange(e.value, i)}
                      options={options}
                      optionValue='value'
                      itemTemplate={(e) => optionTemplate(e, name)}
                    />
                    {operator &&
                      ![ModifierType.CATI, ModifierType.ENCOUNTERS].includes(
                        name
                      ) && (
                        <div style={{ paddingTop: '1.5rem' }}>
                          {renderInput(i, '0', mod.type)}
                          {operator === Operator.BETWEEN && (
                            <React.Fragment>
                              <span style={{ margin: '0 0.375rem' }}>and</span>
                              {renderInput(i, '1', mod.type)}
                            </React.Fragment>
                          )}
                        </div>
                      )}
                  </div>
                </div>
              );
            })
          )}
          <CalculateFooter
            addButtonText='APPLY MODIFIERS'
            addFn={() => updateMods()}
            backFn={() => closeModifiers()}
            calculateFn={() => calculate()}
            calculating={calculating}
            count={count}
            disableAdd={formErrors.length > 0 || formUntouched}
            disableCalculate={
              formErrors.length > 0 || formUntouched || initialFormState
            }
          />
        </div>
      </div>
    );
  }
);
