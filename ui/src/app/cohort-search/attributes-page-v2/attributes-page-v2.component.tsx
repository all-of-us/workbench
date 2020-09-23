import * as fp from 'lodash/fp';
import {Dropdown} from 'primereact/dropdown';
import * as React from 'react';

import {PM_UNITS, PREDEFINED_ATTRIBUTES} from 'app/cohort-search/constant';
import {ppiQuestions, ppiSurveys} from 'app/cohort-search/search-state.service';
import {Selection} from 'app/cohort-search/selection-list/selection-list.component';
import {COPE_SURVEY_GROUP_NAME} from 'app/cohort-search/tree-node/tree-node.component';
import {
  mapParameter,
  sanitizeNumericalInput,
  stripHtml,
  subTypeToTitle
} from 'app/cohort-search/utils';
import {Button} from 'app/components/buttons';
import {FlexRowWrap} from 'app/components/flex';
import {ClrIcon} from 'app/components/icons';
import {CheckBox, NumberInput} from 'app/components/inputs';
import {Spinner, SpinnerOverlay} from 'app/components/spinners';
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import colors, {addOpacity, colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, withCurrentCohortCriteria, withCurrentWorkspace} from 'app/utils';
import {triggerEvent} from 'app/utils/analytics';
import {currentCohortCriteriaStore, currentWorkspaceStore} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {AttrName, CriteriaSubType, DomainType, Operator} from 'generated/fetch';

const styles = reactStyles({
  countPreview: {
    borderTop: `1px solid ${colorWithWhiteness(colors.black, 0.59)}`,
    marginTop: '1rem',
    paddingTop: '1rem',
  },
  row: {
    display: 'flex',
    flexWrap: 'wrap'
  },
  label: {
    color: colors.primary,
    fontWeight: 600,
    display: 'flex',
    lineHeight: '0.75rem',
  },
  orCircle: {
    backgroundColor: colorWithWhiteness(colors.primary, 0.75),
    borderRadius: '50%',
    color: colors.primary,
    width: '2.25rem',
    height: '2.25rem',
    margin: '0.5rem 0',
    lineHeight: '2.25rem',
    textAlign: 'center',
    fontSize: '12px',
    fontWeight: 600
  },
  andCircle: {
    backgroundColor: colorWithWhiteness(colors.primary, 0.75),
    borderRadius: '50%',
    color: colors.primary,
    width: '2.25rem',
    height: '2.25rem',
    lineHeight: '2.25rem',
    textAlign: 'center',
    fontSize: '12px',
    fontWeight: 600,
    position: 'absolute',
    left: '42%'
  },
  andDivider: {
    height: '1.15rem',
    marginBottom: '1.15rem',
    width: '100%',
    borderBottom: `2px solid ${colorWithWhiteness(colors.primary, 0.75)}`
  },
  container: {
    display: 'flex',
    marginLeft: 'auto',
    marginRight: 'auto',
  },
  dropdown: {
    width: '12rem',
    marginRight: '1rem',
  },
  categorical: {
    width: '100%',
    marginBottom: '0.25rem'
  },
  badge: {
    background: colors.primary,
    color: colors.white,
    fontSize: '10px',
    height: '0.625rem',
    padding: '0 4px',
    marginLeft: '0.25rem',
    borderRadius: '10px',
    display: 'inline-flex',
    verticalAlign: 'middle',
    alignItems: 'center',
  },
  addButtonContainer: {
    bottom: '1rem',
    position: 'absolute',
    right: '1rem'
  },
  addButton: {
    height: '2rem',
    borderRadius: '5px',
    fontWeight: 600,
    marginRight: '0.5rem'
  },
  calculateButton: {
    height: '1.75rem',
    border: `1px solid`,
    borderColor: colors.accent,
    borderRadius: '2px',
    fontWeight: 100
  },
  spinner: {
    marginRight: '0.25rem',
    marginLeft: '-0.25rem'
  },
  resultsContainer: {
    flex: '0 0 50%',
    maxWidth: '50%',
    color: colors.primary,
  },
  error: {
    background: colors.warning,
    color: colors.white,
    fontSize: '12px',
    fontWeight: 500,
    textAlign: 'left',
    border: '1px solid #ebafa6',
    borderRadius: '5px',
    margin: '0.25rem 0',
  },
  errors: {
    background: colorWithWhiteness(colors.danger, .7),
    color: colorWithWhiteness(colors.dark, .1),
    fontSize: '11px',
    border: '1px solid #ebafa6',
    borderRadius: '3px',
    margin: '0.25rem 0',
    padding: '3px 5px'
  },
  errorItem: {
    lineHeight: '16px',
  },
  moreInfo: {
    color: colors.accent,
    cursor: 'pointer',
    fontWeight: 300,
    margin: '0.75rem 0',
    textDecoration: 'underline'
  }
});

interface CalculateFooterProps {
  addButtonText: string;
  addFn: () => void;
  backFn: () => void;
  calculateFn: () => void;
  calculating: boolean;
  count: number;
  disableAdd: boolean;
  disableCalculate: boolean;
}

export const CalculateFooter = (props: CalculateFooterProps) => {
  const {addButtonText, addFn, backFn, calculateFn, calculating, count, disableAdd, disableCalculate} = props;
  return <div style={{background: colorWithWhiteness(colors.primary, .87), bottom: 0, position: 'sticky'}}>
    <FlexRowWrap style={styles.countPreview}>
      <div style={styles.resultsContainer}>
        <Button id='attributes-calculate'
          type='secondaryLight'
          disabled={disableCalculate}
          style={disableCalculate
            ? {...styles.calculateButton, borderColor: colorWithWhiteness(colors.dark, 0.6)}
            : styles.calculateButton}
          onClick={() => calculateFn()}>
          {calculating && <Spinner size={16} style={styles.spinner}/>} Calculate
        </Button>
      </div>
      <div style={styles.resultsContainer}>
        <div style={{fontWeight: 600}}>Number of Participants:
          <span> {count === null ? '--' : count.toLocaleString()} </span>
        </div>
      </div>
    </FlexRowWrap>
    <FlexRowWrap style={{flexDirection: 'row-reverse', marginTop: '0.5rem'}}>
      <Button type='primary'
              disabled={disableAdd}
              style={styles.addButton}
              onClick={() => addFn()}>
        {addButtonText}
      </Button>
      <Button type='link'
              style={{color: colors.primary, marginRight: '0.75rem'}}
              onClick={() => backFn()}>
        BACK
      </Button>
    </FlexRowWrap>
  </div>;
};

const optionUtil = {
  ANY: {display: 'Any value', code: 'Any'},
  EQUAL: {display: '= ', code: '01'},
  GREATER_THAN_OR_EQUAL_TO: {display: '>= ', code: '02'},
  LESS_THAN_OR_EQUAL_TO: {display: '<= ', code: '03'},
  BETWEEN: {display: '', code: '04'},
};

interface AttributeForm {
  anyValue: boolean; // Include any values that exist (Measurements and COPE only)
  anyVersion: boolean; // Include any version that exist (COPE only)
  num: Array<any>; // Numerical attributes (Physical Measurements, Measurements or COPE)
  cat: Array<any>; // Categorical attributes (Measurements only)
}

interface Props {
  close: Function;
  criteria: Array<Selection>;
  node: any;
  workspace: WorkspaceData;
}

interface State {
  calculating: boolean;
  count: number;
  countError: boolean;
  form: AttributeForm;
  isCOPESurvey: boolean;
  loading: boolean;
  options: any;
}
export const AttributesPageV2 = fp.flow(withCurrentWorkspace(), withCurrentCohortCriteria()) (
  class extends React.Component<Props, State> {
    constructor(props: Props) {
      super(props);
      this.state = {
        calculating: false,
        count: null,
        countError: false,
        form: {anyValue: false, anyVersion: false, num: [], cat: []},
        isCOPESurvey: false,
        loading: true,
        options: [
          {label: 'Equals', value: Operator.EQUAL},
          {label: 'Greater Than or Equal To', value: Operator.GREATERTHANOREQUALTO},
          {label: 'Less Than or Equal To', value: Operator.LESSTHANOREQUALTO},
          {label: 'Between', value: Operator.BETWEEN},
        ],
      };
    }

    componentDidMount() {
      this.initAttributeForm();
    }

    componentDidUpdate(prevProps: Readonly<Props>): void {
      if (this.props.node !== prevProps.node) {
        // A different node has been selected, so we reset the form and load the new attributes
        this.setState({
          form: {anyValue: false, anyVersion: false, num: [], cat: []},
          loading: true
        }, () => this.initAttributeForm());
      }
    }

    initAttributeForm() {
      const {node: {subtype}} = this.props;
      const {form, options} = this.state;
      if (this.isSurvey) {
        this.getSurveyAttributes();
      } else if (this.isMeasurement) {
        this.getAttributes();
      } else {
        options.unshift({label: optionUtil.ANY.display, value: AttrName[AttrName.ANY]});
        form.num = subtype === CriteriaSubType[CriteriaSubType.BP]
          ? JSON.parse(JSON.stringify(PREDEFINED_ATTRIBUTES.BP_DETAIL))
          : [{name: subtype, operator: 'ANY', operands: []}];
        this.setState({form, count: this.nodeCount, loading: false, options});
      }
    }

    async getSurveyAttributes() {
      const {node: {conceptId, parentId, path, subtype, value}} = this.props;
      const {form, options} = this.state;
      const {cdrVersionId} = currentWorkspaceStore.getValue();
      const surveyId = path.split('.')[0];
      const surveyNode = !!ppiSurveys.getValue()[cdrVersionId] && ppiSurveys.getValue()[cdrVersionId].find(n => n.id === +surveyId);
      if (!!surveyNode && surveyNode.name === COPE_SURVEY_GROUP_NAME) {
        const promises = [];
        if (subtype === CriteriaSubType.QUESTION || (subtype === CriteriaSubType.ANSWER && !value)) {
          promises.push(cohortBuilderApi().findSurveyVersionByQuestionConceptId(+cdrVersionId, surveyNode.conceptId, conceptId));
        } else {
          promises.push(
            cohortBuilderApi().findSurveyVersionByQuestionConceptIdAndAnswerConceptId(
              +cdrVersionId,
              surveyNode.conceptId,
              ppiQuestions.getValue()[parentId].conceptId,
              +value
            )
          );
        }
        if (subtype === CriteriaSubType.ANSWER) {
          promises.push(cohortBuilderApi().findCriteriaAttributeByConceptId(+cdrVersionId, conceptId));
        }
        const [surveyVersions, numericalAttributes] = await Promise.all(promises);
        form.cat = surveyVersions.items.map(attr => ({
          checked: false,
          conceptName: attr.version,
          estCount: attr.itemCount,
          valueAsConceptId: attr.surveyId
        }));
        if (!form.cat.length) {
          // Mock survey versions for testing COPE attributes layout
          // TODO remove before merging
          form.cat = [
            {
              checked: false,
              conceptName: 'May 2020',
              estCount: 4622,
              valueAsConceptId: 100,
            },
            {
              checked: false,
              conceptName: 'June 2020',
              estCount: 2080,
              valueAsConceptId: 101,
            },
            {
              checked: false,
              conceptName: 'July 2020',
              estCount: 1621,
              valueAsConceptId: 102
            }
          ];
        }
        if (numericalAttributes) {
          numericalAttributes.items.forEach(attr => {
            if (!form.num.length) {
              form.num.push({
                name: AttrName.NUM,
                operator: null,
                operands: [],
                conceptId: +value,
                [attr.conceptName]: parseInt(attr.estCount, 10)
              });
            } else {
              form.num[0][attr.conceptName] = parseInt(attr.estCount, 10);
            }
          });
        }
        this.setState({count: null, form, isCOPESurvey: true, loading: false});
      } else {
        options.unshift({label: optionUtil.ANY.display, value: AttrName[AttrName.ANY]});
        this.setState({options}, () => this.getAttributes());
      }
    }

    getAttributes() {
      const {node: {conceptId}} = this.props;
      const{form} = this.state;
      const {cdrVersionId} = currentWorkspaceStore.getValue();
      cohortBuilderApi().findCriteriaAttributeByConceptId(+cdrVersionId, conceptId).then(resp => {
        resp.items.forEach(attr => {
          if (attr.type === AttrName[AttrName.NUM]) {
            // NUM attributes set the min and max range for the number inputs in the attributes form
            if (!form.num.length) {
              form.num.push({
                name: AttrName.NUM,
                operator: this.isSurvey ? 'ANY' : null,
                operands: [],
                conceptId: conceptId,
                [attr.conceptName]: parseInt(attr.estCount, 10)
              });
            } else {
              form.num[0][attr.conceptName] = parseInt(attr.estCount, 10);
            }
          } else {
            // CAT attributes are displayed as checkboxes in the attributes form
            if (parseInt(attr.estCount, 10) > 0) {
              attr['checked'] = false;
              form.cat.push(attr);
            }
          }
        });
        const count = this.isSurvey ? this.nodeCount : null;
        this.setState({count, form, loading: false});
      });
    }

    toggleAnyValueCheckbox(checked: boolean) {
      const {form} = this.state;
      let {node: {count}} = this.props;
      if (checked) {
        form.anyValue = true;
        form.num = form.num.map(attr =>
          ({...attr, operator: this.isPhysicalMeasurement ? 'ANY' : null, operands: []}));
        form.cat = form.cat.map(attr => ({...attr, checked: false}));
      } else {
        count = null;
        form.anyValue = false;
      }
      this.setState({form, count});
    }

    toggleAnyVersionCheckbox(checked: boolean) {
      const {form} = this.state;
      form.anyVersion = checked;
      form.cat = form.cat.map(attr => ({...attr, checked}));
      this.setState({form});
    }

    selectChange(attributeIndex: number, value: string) {
      const {form} = this.state;
      form.num[attributeIndex].operator = value;
      if (this.isBloodPressure) {
        // for blood pressure, either both operators have to be 'ANY' OR neither can be 'ANY'
        const otherAttribute = attributeIndex === 0 ? 1 : 0;
        if (value === 'ANY') {
          form.num[otherAttribute].operator = 'ANY';
          form.num[otherAttribute].operands = form.num[attributeIndex].operands = [];
        } else if (form.num[otherAttribute].operator === 'ANY') {
          form.num[otherAttribute].operator = value;
        }
      } else if (value === 'ANY') {
        form.num[attributeIndex].operands = [];
      }
      if (value !== Operator[Operator.BETWEEN]) {
        // delete second operand if it exists
        form.num[attributeIndex].operands.splice(1);
      }
      const count = value === 'ANY' ? this.nodeCount : null;
      this.setState({form, count});
    }

    inputChange(input: string, attributeIndex: number, operandIndex: number) {
      const {form} = this.state;
      form.num[attributeIndex].operands[operandIndex] = sanitizeNumericalInput(input);
      this.setState({form, count: null});
    }

    checkboxChange(checked: boolean, index: number) {
      const {form} = this.state;
      form.cat[index].checked = checked;
      this.setState({form});
    }

    validateForm() {
      const {form, isCOPESurvey} = this.state;
      let formErrors = new Set(), formValid = true, operatorSelected = form.num.length !== 0;
      if (form.anyValue) {
        return {formValid, formErrors};
      }
      formErrors = form.num.reduce((acc, attr) => {
        const {MIN, MAX, operator} = attr;
        const operands = attr.operands.map(op => parseInt(op, 10));
        switch (operator) {
          case null:
            operatorSelected = false;
            return acc;
          case 'ANY':
            return acc;
          case Operator.BETWEEN:
            if (operands.length < 2) {
              formValid = false;
            }
            break;
          default:
            if (operands.length === 0) {
              formValid = false;
            }
        }
        if (operands.includes(NaN)) {
          formValid = false;
          acc.add('Form can only accept valid numbers');
        }
        if (this.isPhysicalMeasurement && operands.some(op => op < 0)) {
          formValid = false;
          acc.add('Form cannot accept negative values');
        }
        if (this.hasRange && operands.some(op => op < MIN || op > MAX)) {
          formValid = false;
          acc.add(`Values must be between ${MIN.toLocaleString()} and ${MAX.toLocaleString()}`);
        }
        return acc;
      }, formErrors);
      // The second condition sets formValid to false if this is a Measurements or COPE attribute with no operator selected from the
      // dropdown and no categorical checkboxes checked
      formValid = formValid && !((this.isMeasurement || isCOPESurvey) && !operatorSelected && !form.cat.some(attr => attr.checked));
      return {formErrors, formValid};
    }

    get nodeCount() {
      const {node: {count, parentId}} = this.props;
      if (this.isSurvey) {
        const parent = ppiQuestions.getValue()[parentId];
        return !!parent ? parent.count : null;
      } else {
        return count;
      }
    }

    get paramId() {
      const {node: {conceptId, id}} = this.props;
      const {form} = this.state;
      const code = form.anyValue ? 'Any' : form.num.reduce((acc, attr) => {
        if (attr.operator) {
          acc += optionUtil[attr.operator].code;
        }
        return acc;
      }, '');
      return `param${(conceptId || id) + code}`;
    }

    get displayName() {
      const {node: {name}} = this.props;
      return stripHtml(name);
    }

    get paramWithAttributes() {
      const {node, node: {name, subtype}} = this.props;
      const {form} = this.state;
      let paramName;
      const attrs = [];
      if (form.anyValue) {
        paramName = name + ` (${optionUtil.ANY.display})`;
      } else {
        form.num.filter(at => at.operator).forEach(({operator, operands, conceptId}) => {
          const attr = {name: AttrName.NUM, operator, operands};
          if (subtype === CriteriaSubType.BP) {
            attr['conceptId'] = conceptId;
          }
          if (attr.operator === 'ANY' && subtype === CriteriaSubType.BP) {
            attr.name = AttrName.ANY;
            attr.operands = [];
            delete attr.operator;
            attrs.push(attr);
          } else if (attr.operator !== 'ANY') {
            attrs.push(attr);
          }
        });
        if (form.cat.some(at => at.checked)) {
          const catOperands = form.cat.reduce((checked, current) => {
            if (current.checked) {
              checked.push(current.valueAsConceptId.toString());
            }
            return checked;
          }, []);
          attrs.push({name: AttrName.CAT, operator: Operator.IN, operands: catOperands});
        }
        paramName = this.paramName;
      }
      return {...node, parameterId: this.paramId, name: paramName, attributes: attrs};
    }

    get paramName() {
      const {node} = this.props;
      const {form} = this.state;
      const selectionDisplay = [];
      let name = '';
      form.num.filter(at => at.operator).forEach((attr, i) => {
        if (attr.operator === 'ANY') {
          if (i === 0) {
            name += optionUtil.ANY.display;
          }
        } else {
          if (i > 0) {
            name += ' / ';
          }
          if (node.subtype === CriteriaSubType.BP) {
            name += attr.name + ' ';
          }
          name += optionUtil[attr.operator].display + attr.operands.map(op => parseInt(op, 10).toLocaleString()).join('-');
        }
      });
      if (name !== '') {
        selectionDisplay.push(name);
      }
      form.cat.filter(ca => ca.checked).forEach(attr => selectionDisplay.push(attr.conceptName));
      const nodeName = node.subtype === CriteriaSubType.ANSWER ? ppiQuestions.getValue()[node.parentId].name : node.name;
      return nodeName + ' (' + selectionDisplay.join(', ') +
        (this.hasUnits && form.num[0].operator !== AttrName.ANY ? ' ' + PM_UNITS[node.subtype] : '') + ')';
    }

    requestPreview() {
      this.setState({count: null, calculating: true, countError: false});
      const param = this.paramWithAttributes;
      // TODO remove condition to only track PM criteria for 'Phase 2' of CB Google Analytics
      if (this.isPhysicalMeasurement) {
        this.trackEvent(param.subtype, 'Calculate');
      }
      const cdrVersionId = +(currentWorkspaceStore.getValue().cdrVersionId);
      const request = {
        excludes: [],
        includes: [{
          items: [{
            type: param.domainId,
            searchParameters: [mapParameter(param)],
            modifiers: []
          }],
          temporal: false
        }],
        dataFilters: []
      };
      cohortBuilderApi().countParticipants(cdrVersionId, request).then(response => {
        this.setState({count: response, calculating: false});
      }, () => {
        this.setState({calculating: false, countError: true});
      });
    }

    addParameterToSearchItem() {
      const {close} = this.props;
      let {criteria} = this.props;
      const param = this.paramWithAttributes;
      // TODO remove condition to only track PM criteria for 'Phase 2' of CB Google Analytics
      if (this.isPhysicalMeasurement) {
        this.trackEvent(param.subtype, 'Add');
      }
      criteria = criteria.filter(crit => crit.parameterId !== param.parameterId);
      currentCohortCriteriaStore.next([...criteria, param]);
      close();
    }

    trackEvent(subtype: string, eventType: string) {
      triggerEvent(
        'Cohort Builder Search',
        'Click',
        `Physical Measurements - ${subTypeToTitle(subtype)} - ${eventType}`
      );
    }

    get hasUnits() {
      const {node: {subtype}} = this.props;
      return this.isPhysicalMeasurement && typeof PM_UNITS[subtype] !== 'undefined';
    }

    get isMeasurement() {
      const {node: {domainId}} = this.props;
      return domainId === DomainType.MEASUREMENT;
    }

    get isPhysicalMeasurement() {
      const {node: {domainId}} = this.props;
      return domainId === DomainType.PHYSICALMEASUREMENT;
    }

    get isSurvey() {
      const {node: {domainId}} = this.props;
      return domainId === DomainType.SURVEY;
    }

    get isBloodPressure() {
      const {node: {subtype}} = this.props;
      return subtype === CriteriaSubType.BP;
    }

    get hasRange() {
      return this.isMeasurement || this.isSurvey;
    }

    renderNumericalAttributes() {
      const {node: {count, subtype}} = this.props;
      const {form, isCOPESurvey, options} = this.state;
      return form.num.length > 0 && <React.Fragment>
        {this.isMeasurement && <div style={styles.label}>Numeric Values</div>}
        {isCOPESurvey && <div>
          <CheckBox onChange={(v) => this.toggleAnyValueCheckbox(v)}/> Any value
          {count > -1 && <span style={styles.badge}> {count.toLocaleString()}</span>}
        </div>}
        {!(isCOPESurvey && form.anyValue) && form.num.map((attr, a) => <div key={a}>
          {this.isBloodPressure && <div style={styles.label}>{attr.name}</div>}
          {isCOPESurvey && <div style={styles.orCircle}>OR</div>}
          <Dropdown style={{marginBottom: '0.5rem', width: '100%'}}
                    value={attr.operator}
                    options={options}
                    placeholder='Select Operator'
                    onChange={(e) => this.selectChange(a, e.value)}/>
          <FlexRowWrap>
            {![null, 'ANY'].includes(attr.operator) && <div style={{width: '33%'}}>
              <NumberInput style={{padding: '0 0.25rem', ...(this.hasUnits ? {width: '70%'} : {})}}
                           value={attr.operands[0] || ''}
                           min={attr.MIN} max={attr.MAX}
                           onChange={(v) => this.inputChange(v, a, 0)}/>
              {this.hasUnits && <span> {PM_UNITS[subtype]}</span>}
            </div>}
            {attr.operator === Operator.BETWEEN && <React.Fragment>
              <div style={{padding: '0.2rem 1.5rem 0 1rem'}}>and</div>
              <div style={{width: '33%'}}>
                <NumberInput style={{padding: '0 0.25rem', ...(this.hasUnits ? {width: '70%'} : {})}}
                             value={attr.operands[1] || ''}
                             min={attr.MIN} max={attr.MAX}
                             onChange={(v) => this.inputChange(v, a, 1)}/>
                {this.hasUnits && <span> {PM_UNITS[subtype]}</span>}
              </div>
            </React.Fragment>}
          </FlexRowWrap>
          {this.hasRange && ![null, 'ANY'].includes(attr.operator) && <div style={{paddingTop: '0.2rem'}}>
            Range: {attr.MIN.toLocaleString()} - {attr.MAX.toLocaleString()}
          </div>}
        </div>)}
      </React.Fragment>;
    }

    renderCategoricalAttributes() {
      const {node: {count}} = this.props;
      const {form, isCOPESurvey} = this.state;
      return form.cat.length > 0 && <React.Fragment>
        {isCOPESurvey && <div>
          <CheckBox onChange={(v) => this.toggleAnyVersionCheckbox(v)}/> Any version
          {count > -1 && <span style={styles.badge}>{count.toLocaleString()}</span>}
        </div>}
        {!(isCOPESurvey && form.anyVersion) && <React.Fragment>
          <div style={styles.orCircle}>OR</div>
          {!isCOPESurvey && <div style={styles.label}>Categorical Values</div>}
          {form.cat.map((attr, a) => <div key={a} style={styles.categorical}>
            <CheckBox checked={attr.checked} style={{marginRight: '3px'}}
                      onChange={(v) => this.checkboxChange(v, a)}/>
            {attr.conceptName}
            <span style={styles.badge}>{parseInt(attr.estCount, 10).toLocaleString()}</span>
          </div>)}
        </React.Fragment>}
      </React.Fragment>;
    }

    render() {
      const {close, node: {domainId, name, parentId, subtype}} = this.props;
      const {calculating, count, countError, form, isCOPESurvey, loading, options} = this.state;
      const {formErrors, formValid} = this.validateForm();
      const disableAdd = calculating || !formValid;
      const disableCalculate = disableAdd || form.anyValue || (form.num.length && form.num.every(attr => attr.operator === 'ANY'));
      return (loading ?
        <SpinnerOverlay/> :
        <div id='attributes-form' style={{marginTop: '0.5rem'}}>
          {isCOPESurvey ? <div>
            <h3 style={{fontWeight: 600, margin: '0 0 0.5rem', textTransform: 'capitalize'}}>
              COPE Survey (COVID-19) attribute
            </h3>
            <div style={{lineHeight: '0.75rem', paddingRight: '1.5rem'}}>
              The COPE survey is longitudinal and will change over time. Use the following attributes to select data from one or more
               versions.
            </div>
            <div style={styles.moreInfo}>
              More info
            </div>
          </div> : <h3 style={{fontWeight: 600, margin: '0 0 0.5rem', textTransform: 'capitalize'}}>
            {this.isPhysicalMeasurement ? name : domainId.toString().toLowerCase()} Detail
          </h3>}
          {this.isSurvey && <div style={{...styles.label, marginBottom: '0.5rem'}}>
            {subtype === CriteriaSubType.ANSWER
              ? `${ppiQuestions.getValue()[parentId].name} - ${name}`
              : name
            }
          </div>}
          {countError && <div style={styles.error}>
            <ClrIcon style={{margin: '0 0.5rem 0 0.25rem'}} className='is-solid'
              shape='exclamation-triangle' size='22'/>
            Sorry, the request cannot be completed.
          </div>}
          {!!formErrors.size && <div style={styles.errors}>
            {Array.from(formErrors).map((err, e) => <div key={e} style={styles.errorItem}>
              {err}
            </div>)}
          </div>}
          {isCOPESurvey
            ? <div>
              {this.renderCategoricalAttributes()}
              {form.num.length > 0 && form.cat.length > 0 && <div style={{position: 'relative'}}>
                <div style={styles.andCircle}>AND</div>
                <div style={styles.andDivider}/>
              </div>}
              {this.renderNumericalAttributes()}
            </div>
            : <div>
              {this.isMeasurement && <div>
                <div style={styles.label}>{this.displayName}</div>
                <CheckBox onChange={(v) => this.toggleAnyValueCheckbox(v)}/> Any value (lab exists)
                {!form.anyValue && form.num.length > 0 && <div style={styles.orCircle}>OR</div>}
              </div>}
              {!form.anyValue && <div style={{minHeight: '10rem'}}>
                {this.renderNumericalAttributes()}
                {this.renderCategoricalAttributes()}
              </div>}
            </div>
          }
          <CalculateFooter addButtonText='ADD THIS'
                           addFn={() => this.addParameterToSearchItem()}
                           backFn={() => close()}
                           calculateFn={() => this.requestPreview()}
                           calculating={calculating}
                           count={count}
                           disableAdd={disableAdd}
                           disableCalculate={disableCalculate}/>
        </div>
      );
    }
  }
);
