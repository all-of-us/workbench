import * as fp from 'lodash/fp';
import * as moment from 'moment';
import {Dropdown} from 'primereact/dropdown';
import * as React from 'react';

import {CalculateFooter} from 'app/cohort-search/attributes-page/attributes-page.component';
import {encountersStore} from 'app/cohort-search/search-state.service';
import {domainToTitle, mapParameter} from 'app/cohort-search/utils';
import {ClrIcon} from 'app/components/icons';
import {DatePicker, NumberInput} from 'app/components/inputs';
import {TooltipTrigger} from 'app/components/popups';
import {Spinner} from 'app/components/spinners';
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import {reactStyles, withCurrentCohortSearchContext, withCurrentWorkspace} from 'app/utils';
import {triggerEvent} from 'app/utils/analytics';
import {currentCohortSearchContextStore} from 'app/utils/navigation';
import {serverConfigStore} from 'app/utils/stores';
import {WorkspaceData} from 'app/utils/workspace-data';
import {Criteria, CriteriaType, Domain, Modifier, ModifierType, Operator} from 'generated/fetch';


const styles = reactStyles({
  header: {
    color: '#262262',
    fontWeight: 600,
    fontSize: '16px',
    borderBottom: '1px solid #262262',
    paddingBottom: '0.5rem'
  },
  errors: {
    background: '#f5dbd9',
    color: '#565656',
    fontSize: '11px',
    border: '1px solid #ebafa6',
    borderRadius: '3px',
    marginTop: '0.25rem',
    padding: '3px 5px'
  },
  errorItem: {
    lineHeight: '16px',
  },
  label: {
    color: '#262262',
    fontWeight: 500,
  },
  modifier: {
    marginTop: '0.4rem'
  },
  select: {
    width: '12rem',
    height: '1.6rem',
    paddingLeft: '0.5rem',
    marginRight: '1rem',
  },
  date: {
    width: '6.5rem',
    display: 'inline-block',
  },
  count: {
    display: 'inline-flex',
    height: '0.625rem',
    background: '#0079b8',
    color: '#ffffff',
    fontSize: '10px',
    padding: '0 0.166667rem',
    borderRadius: '10px',
    verticalAlign: 'middle',
    alignItems: 'center',
  },
  info: {
    color: '#0077b7',
    marginLeft: '0.25rem',
  },
  footer: {
    position: 'absolute',
    bottom: '1rem'
  },
  row: {
    display: 'flex',
    flexWrap: 'wrap',
    marginRight: '-.5rem',
    marginLeft: '-.5rem'
  },
  col: {
    position: 'relative',
    minHeight: '1px',
    width: '100%',
    paddingLeft: '0.5rem',
    paddingRight: '0.5rem',
  },
  button: {
    color: '#ffffff',
    height: '1.5rem',
    margin: '0.25rem 0.5rem 0.25rem 0',
    borderRadius: '3px',
  },
  previewCount: {
    color: '#4a4a4a',
    fontWeight: 'bold'
  },
  error: {
    background: '#F7981C',
    color: '#ffffff',
    fontSize: '12px',
    fontWeight: 500,
    textAlign: 'left',
    border: '1px solid #ebafa6',
    borderRadius: '5px',
    marginTop: '0.25rem',
    padding: '8px',
  },
  addButton: {
    height: '2rem',
    borderRadius: '5px',
    fontWeight: 600,
    marginRight: '0.5rem'
  }
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
      return 'Dates must be in format \'YYYY-MM-DD\'';
    }
    return null;
  },
  NUM_OF_OCCURRENCES: (value) => {
    if (value === '') {
      return 'Has Occurrences is required';
    }
    if (value < 1 || value > 99) {
      return 'Has Occurrences must be between 1 - 99';
    }
    if (!Number.isInteger(parseFloat(value))) {
      return 'Has Occurrences must be a whole number';
    }
    return null;
  }
};
const SURVEY_MODIFIERS = [ModifierType.AGEATEVENT];

interface Selection extends Criteria {
  parameterId: string;
}

interface Props {
  selections: Array<Selection>;
  closeModifiers: (modifiers?: Array<Modifier>) => void;
  workspace: WorkspaceData;
  cohortContext: any;
}

interface State {
  calculateError: boolean;
  calculating: boolean;
  count: number;
  formErrors: Array<string>;
  formState: any;
  formUntouched: boolean;
  initialFormState: boolean;
  loading: boolean;
  visitCounts: any;
}

export const ModifierPage = fp.flow(withCurrentWorkspace(), withCurrentCohortSearchContext())(
  class extends React.Component<Props, State> {
    constructor(props: Props) {
      super(props);
      this.state = {
        formState: [{
          name: ModifierType.AGEATEVENT,
          label: 'Age At Event',
          type: 'number',
          operator: undefined,
          values: [undefined, undefined],
          options: [{
            label: 'Any',
            value: undefined,
          }, {
            label: 'Greater Than or Equal To',
            value: Operator.GREATERTHANOREQUALTO,
          }, {
            label: 'Less Than or Equal To',
            value: Operator.LESSTHANOREQUALTO,
          }, {
            label: 'Between',
            value: Operator.BETWEEN,
          }]
        }],
        formErrors: [],
        formUntouched: false,
        initialFormState: true,
        calculating: false,
        count: null,
        visitCounts: undefined,
        calculateError: false,
        loading: true,
      };
    }

    async componentDidMount() {
      const {cohortContext: {domain}, workspace: {id, namespace}} = this.props;
      const {formState} = this.state;
      if (domain !== Domain.SURVEY) {
        formState.push({
          name: ModifierType.NUMOFOCCURRENCES,
          label: 'Has Occurrences',
          type: 'number',
          operator: undefined,
          values: [undefined, undefined],
          options: [{
            label: 'Any',
            value: undefined,
          }, {
            label: 'N or More',
            value: Operator.GREATERTHANOREQUALTO,
          }]
        });
      }
      if (serverConfigStore.get().config.enableEventDateModifier) {
        formState.push({
          name: ModifierType.EVENTDATE,
          label: 'Event Date',
          type: 'date',
          operator: undefined,
          values: [undefined, undefined],
          options: [{
            label: 'Any',
            value: undefined,
          }, {
            label: 'Is On or Before',
            value: Operator.LESSTHANOREQUALTO,
          }, {
            label: 'Is On or After',
            value: Operator.GREATERTHANOREQUALTO,
          }, {
            label: 'Is Between',
            value: Operator.BETWEEN,
          }]
        });
      }
      if (this.addEncounters) {
        let encountersOptions = encountersStore.getValue();
        if (!encountersOptions) {
          // get options for visit modifier from api
          const res = await cohortBuilderApi().findCriteriaBy(
            namespace, id, Domain[Domain.VISIT], CriteriaType[CriteriaType.VISIT]);
          encountersOptions = res.items;
          encountersStore.next(encountersOptions);
        }
        const visitCounts = {};
        const encounters = {
          name: ModifierType.ENCOUNTERS,
          label: 'During Visit Type',
          type: null,
          operator: undefined,
          values: [undefined],
          options: [{
            label: 'Any',
            value: undefined,
          }]
        };
        encountersOptions.forEach(option => {
          if (option.count > 0) {
            encounters.options.push({
              label: option.name,
              value: option.conceptId.toString()
            });
            visitCounts[option.conceptId] = option.count;
          }
        });
        formState.push(encounters);
        this.setState({visitCounts});
      }
      this.setState({formState});
      this.getExisting();
    }

    get formState() {
      const {cohortContext: {domain}} = this.props;
      const {formState} = this.state;
      return domain === Domain.SURVEY ?
        formState.filter(form => SURVEY_MODIFIERS.indexOf(form.name) > -1) : formState;
    }

    getExisting() {
      const {cohortContext} = this.props;
      const {formState} = this.state;
      // This reseeds the form state with existing data if we're editing an existing item
      cohortContext.item.modifiers.forEach(existing => {
        const index = formState.findIndex(mod => existing.name === mod.name);
        if (index > -1) {
          const mod = formState[index];
          const values = existing.operands.filter(val => !!val);
          switch (mod.name) {
            case ModifierType.ENCOUNTERS:
              formState[index] = {...mod, operator: existing.operands[0], values};
              break;
            case ModifierType.EVENTDATE:
              formState[index] = {
                ...mod,
                operator: existing.operator,
                values: values.map(val => new Date(val + 'T08:00:00'))
              };
              break;
            default:
              formState[index] = {...mod, operator: existing.operator, values};
          }
        }
      });
      this.setState({formState, loading: false});
    }

    selectChange = (sel: any, index: number) => {
      const {formState} = this.state;
      this.trackEvent(formState[index].label);
      const {name} = formState[index];
      if (name === ModifierType.ENCOUNTERS) {
        formState[index].values = [sel];
      } else if (!sel) {
        formState[index].values = [undefined, undefined];
      } else if (sel !== Operator.BETWEEN) {
        formState[index].values[1] = undefined;
      }
      formState[index].operator = sel;
      this.setState({count: null, formState}, () => this.validateValues());
    }

    inputChange = (index: string, field: string, value: any) => {
      const {formState} = this.state;
      formState[index].values[field] = value;
      this.setState({count: null, formState}, () => this.validateValues());
    }

    getModifiersFromForm() {
      return this.state.formState.reduce((acc, mod) => {
        const {name, operator, values} = mod;
        if (operator) {
          switch (name) {
            case ModifierType.ENCOUNTERS:
              acc.push({name, operator: Operator.IN, operands: [operator.toString()]});
              break;
            case ModifierType.EVENTDATE:
              const formatted = values.map(val => moment(val, 'YYYY-MM-DD', true).isValid()
                ? moment(val).format('YYYY-MM-DD') : undefined);
              acc.push({name, operator, operands: formatted.filter(val => !!val)});
              break;
            default:
              acc.push({name, operator, operands: values.filter(val => !['', null, undefined].includes(val))});
          }
        }
        return acc;
      }, []);
    }

    updateMods() {
      const {cohortContext} = this.props;
      cohortContext.item.modifiers = this.getModifiersFromForm();
      currentCohortSearchContextStore.next(cohortContext);
      this.props.closeModifiers(cohortContext.item.modifiers);
    }

    get addEncounters() {
      const {cohortContext: {domain}} = this.props;
      return ![Domain.PHYSICALMEASUREMENT, Domain.SURVEY, Domain.VISIT].includes(domain);
    }

    calculate = async() => {
      const {selections, cohortContext: {domain, role}, workspace: {id , namespace}} = this.props;
      this.trackEvent('Calculate');
      try {
        this.setState({calculating: true, count: null, calculateError: false});
        const request = {
          includes: [],
          excludes: [],
          [role]: [{
            items: [{
              type: domain,
              searchParameters: selections.map(mapParameter),
              modifiers: this.getModifiersFromForm()
            }]
          }],
          dataFilters: []
        };
        await cohortBuilderApi().countParticipants(namespace, id, request)
          .then(response => this.setState({count: response, calculating: false}));
      } catch (error) {
        console.error(error);
        this.setState({calculating: false, calculateError: true});
      }
    }

    trackEvent = (label: string) => {
      const {cohortContext: {domain}} = this.props;
      triggerEvent(
        'Cohort Builder Search',
        'Click',
        `Modifiers - ${label} - ${domainToTitle(domain)} - Cohort Builder Search`
      );
    }

    validateValues() {
      const {formState} = this.state;
      let initialState = true;
      let untouched = false;
      const errors = formState.reduce((acc, item) => {
        if (item.name !== ModifierType.ENCOUNTERS) {
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
      this.setState({formErrors: Array.from(errors), formUntouched: untouched, initialFormState: initialState});
    }

    optionTemplate = (opt: any, name: any) => {
      if (name !== ModifierType.ENCOUNTERS || !opt.value) {
        return opt.label;
      }
      const {visitCounts} = this.state;
      return <div className='p-clearfix'>
        {opt.label}
        &nbsp;<span style={styles.count}>{visitCounts[opt.value].toLocaleString()}</span>
      </div>;
    }

    renderInput(index: string, field: string, type) {
      const {values} = this.state.formState[index];
      switch (type) {
        case 'number':
          return <NumberInput style={{padding: '0 0.25rem', width: '3rem'}} value={values[field]}
                   min={0} onChange={v => this.inputChange(index, field, v)}/>;
        case 'date':
          return <div style={styles.date}>
            <DatePicker
              value={values[field]}
              placeholder='YYYY-MM-DD'
              onChange={e => this.inputChange(index, field, e)}
              maxDate={new Date()}
            />
          </div>;
      }
    }

    render() {
      const {calculateError, calculating, count, formErrors, formState, formUntouched, initialFormState, loading} = this.state;
      const tooltip = `Dates are consistently shifted within a participant’s record
      by a time period of up to 364 days backwards to de-identify patient data.
      The date shift differs across participants.`;
      const disableCalculate = formErrors.length > 0 || formUntouched || initialFormState;
      return <div id='modifiers-form'>
        <h3 style={{...styles.header, marginTop: 0}}>Apply optional Modifiers</h3>
        <div style={{marginTop: '1rem'}}>
          <div>
            The following modifiers are optional and apply to all selected criteria
          </div>
          {calculateError && <div style={styles.error}>
            <ClrIcon style={{margin: '0 0.5rem 0 0.25rem'}} className='is-solid'
                     shape='exclamation-triangle' size='22'/>
            Sorry, the request cannot be completed. Please try again or contact Support in the left
            hand navigation.
          </div>}
          {formErrors.length > 0 && <div style={styles.errors}>
            {formErrors.map((err, e) => <div key={e} style={styles.errorItem}>
              {err}
            </div>)}
          </div>}
          {loading
            ? <div style={{margin: '1rem 0 2rem', textAlign: 'center'}}>
              <Spinner/>
            </div>
            : formState.map((mod, i) => {
              const {label, name, options, operator} = mod;
              return <div data-test-id={name} key={i} style={{marginTop: '0.75rem'}}>
                <label style={styles.label}>{label}</label>
                {name === ModifierType.EVENTDATE &&
                <TooltipTrigger content={<div>{tooltip}</div>}>
                  <ClrIcon style={styles.info} className='is-solid' shape='info-standard'/>
                </TooltipTrigger>
                }
                <div style={styles.modifier}>
                  <Dropdown value={operator}
                            style={styles.select}
                            onChange={(e) => this.selectChange(e.value, i)}
                            options={options}
                            optionValue='value'
                            itemTemplate={(e) => this.optionTemplate(e, name)}/>
                  {operator && name !== ModifierType.ENCOUNTERS && <div style={{paddingTop: '1rem'}}>
                    {this.renderInput(i, '0', mod.type)}
                    {operator === Operator.BETWEEN && <React.Fragment>
                      <span style={{margin: '0 0.25rem'}}>and</span>
                      {this.renderInput(i, '1', mod.type)}
                    </React.Fragment>}
                  </div>}
                </div>
              </div>;
            })
          }
          <CalculateFooter addButtonText='APPLY MODIFIERS'
                           addFn={() => this.updateMods()}
                           backFn={() => this.props.closeModifiers()}
                           calculateFn={() => this.calculate()}
                           calculating={calculating}
                           count={count}
                           disableAdd={formErrors.length > 0 || formUntouched}
                           disableCalculate={disableCalculate}/>
        </div>
      </div>;
    }
  }
);
