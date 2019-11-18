import {Component, Input} from '@angular/core';
import {AttrName, CriteriaSubType, DomainType, Operator} from 'generated/fetch';
import * as React from 'react';

import {PM_UNITS, PREDEFINED_ATTRIBUTES} from 'app/cohort-search/constant';
import {ppiQuestions, selectionsStore, wizardStore} from 'app/cohort-search/search-state.service';
import {
  mapParameter,
  sanitizeNumericalInput,
  stripHtml,
  subTypeToTitle
} from 'app/cohort-search/utils';
import {Button} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import {CheckBox} from 'app/components/inputs';
import {Spinner, SpinnerOverlay} from 'app/components/spinners';
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {triggerEvent} from 'app/utils/analytics';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {Dropdown} from 'primereact/dropdown';

const styles = reactStyles({
  countPreview: {
    backgroundColor: '#E4F3FC',
    padding: '0.5rem',
    marginTop: '1rem',
    position: 'absolute',
    width: '93%',
    bottom: '1rem',
  },
  row: {
    display: 'flex',
    flexWrap: 'wrap'
  },
  label: {
    color: colors.primary,
    padding: '0.5rem',
    fontWeight: 500,
    display: 'flex',
  },
  orCircle: {
    backgroundColor: '#e2e2e9',
    borderRadius: '50%',
    width: '2.25rem',
    height: '2.25rem',
    margin: '0.75rem 3rem 0.25rem',
    lineHeight: '2.25rem',
    textAlign: 'center',
    fontSize: '0.45rem',
  },
  container: {
    display: 'flex',
    marginLeft: 'auto',
    marginRight: 'auto',
    padding: '0.2rem 0.5rem 0',
  },
  dropdown: {
    width: '12rem',
    marginRight: '1rem',
  },
  number: {
    borderRadius: '3px',
    border: '1px solid #a6a6a6',
    width: '3rem',
    height: '1.6rem',
    verticalAlign: 'middle',
  },
  categorical: {
    width: '25%',
    float: 'left',
    marginBottom: '0.25rem'
  },
  badge: {
    background: colors.primary,
    color: colors.white,
    fontSize: '10px',
    height: '0.625rem',
    padding: '0 4px',
    marginRight: '0.5rem',
    borderRadius: '10px',
    display: 'inline-flex',
    verticalAlign: 'middle',
    alignItems: 'center',
  },
  buttonContainer: {
    flex: '0 0 25%',
    maxWidth: '25%',
    padding: '0 0.5rem',
  },
  button: {
    height: '1.75rem',
    margin: '.25rem .5rem .25rem 0',
    borderRadius: '4px',
    fontWeight: 600
  },
  spinner: {
    marginRight: '0.25rem',
    marginLeft: '-0.25rem'
  },
  resultsContainer: {
    flex: '0 0 41.66667%',
    maxWidth: '41.66667%',
    padding: '0.3rem 0.5rem 0',
    color: colors.primary,
    fontSize: '14px',
    lineHeight: '20px'
  },
  error: {
    background: colors.warning,
    color: colors.white,
    fontSize: '12px',
    fontWeight: 500,
    textAlign: 'left',
    border: '1px solid #ebafa6',
    borderRadius: '5px',
    margin: '0.25rem 0.5rem',
    padding: '8px',
  },
  errors: {
    background: colorWithWhiteness(colors.danger, .7),
    color: colorWithWhiteness(colors.dark, .1),
    fontSize: '11px',
    border: '1px solid #ebafa6',
    borderRadius: '3px',
    margin: '0.25rem 0.5rem',
    padding: '3px 5px'
  },
  errorItem: {
    lineHeight: '16px',
  },
});

const optionUtil = {
  ANY: {display: 'Any', code: 'Any'},
  EQUAL: {display: '= ', code: '01'},
  GREATER_THAN_OR_EQUAL_TO: {display: '>= ', code: '02'},
  LESS_THAN_OR_EQUAL_TO: {display: '<= ', code: '03'},
  BETWEEN: {display: '', code: '04'},
};

interface AttributeForm {
  exists: boolean; // Check if attribute values exist (Measurements only)
  num: Array<any>; // Numerical attributes (Physical Measurements or Measurements)
  cat: Array<any>; // Categorical attributes (Measurements only)
}

interface Props {
  node: any;
  close: Function;
  workspace: WorkspaceData;
}

interface State {
  calculating: boolean;
  count: number;
  countError: boolean;
  form: AttributeForm;
  loading: boolean;
  options: any;
}
export const AttributesPage = withCurrentWorkspace() (
  class extends React.Component<Props, State> {
    constructor(props: Props) {
      super(props);
      this.state = {
        calculating: false,
        count: null,
        countError: false,
        form: {exists: false, num: [], cat: []},
        loading: true,
        options: [
          {label: 'Equals', value: Operator.EQUAL},
          {label: 'Greater than or Equal to', value: Operator.GREATERTHANOREQUALTO},
          {label: 'Less than or Equal to', value: Operator.LESSTHANOREQUALTO},
          {label: 'Between', value: Operator.BETWEEN},
        ],
      };
    }

    componentDidMount() {
      const {node: {subtype}} = this.props;
      const{form, options} = this.state;
      if (!this.isMeasurement) {
        options.unshift({label: 'Any', value: AttrName[AttrName.ANY]});
      }
      if (this.hasRange) {
        this.getAttributes();
      } else {
        form.num = subtype === CriteriaSubType[CriteriaSubType.BP]
          ? JSON.parse(JSON.stringify(PREDEFINED_ATTRIBUTES.BP_DETAIL))
          : [{name: subtype, operator: 'ANY', operands: []}];
        this.setState({form, options, count: this.nodeCount, loading: false});
      }
    }

    getAttributes() {
      const {node: {conceptId}} = this.props;
      const{form} = this.state;
      const {cdrVersionId} = currentWorkspaceStore.getValue();
      cohortBuilderApi().getCriteriaAttributeByConceptId(+cdrVersionId, conceptId).then(resp => {
        resp.items.forEach(attr => {
          if (attr.type === AttrName[AttrName.NUM]) {
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
            if (parseInt(attr.estCount, 10) > 0) {
              attr['checked'] = false;
              form.cat.push(attr);
            }
          }
        });
        this.setState({form, loading: false});
      });
    }

    toggleCheckbox(checked: boolean) {
      const {form} = this.state;
      let {node: {count}} = this.props;
      if (checked) {
        form.exists = true;
        form.num = form.num.map(attr =>
          ({...attr, operator: this.isPhysicalMeasurement ? 'ANY' : null, operands: []}));
        form.cat = form.cat.map(attr => ({...attr, checked: false}));
      } else {
        count = null;
        form.exists = false;
      }
      this.setState({form, count});
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
      const {form} = this.state;
      let formErrors = new Set(), formValid = true, operatorSelected = true;
      if (form.exists) {
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
      formValid = formValid ||
        (this.isMeasurement && !operatorSelected && form.cat.some(attr => attr.checked));
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
      const code = form.exists ? 'Any' : form.num.reduce((acc, attr) => {
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
      if (form.exists) {
        paramName = name + ' (Any)';
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
            name += 'Any';
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
      const nodeName = this.isSurvey ? ppiQuestions.getValue()[node.parentId].name : node.name;
      return nodeName + ' (' + selectionDisplay.join(', ') +
        (this.isPhysicalMeasurement && form.num[0].operator !== AttrName.ANY ? PM_UNITS[node.subtype] : '') + ')';
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
        }]
      };
      cohortBuilderApi().countParticipants(cdrVersionId, request).then(response => {
        this.setState({count: response, calculating: false});
      }, () => {
        this.setState({calculating: false, countError: true});
      });
    }

    addParameterToSearchItem() {
      const param = this.paramWithAttributes;
      // TODO remove condition to only track PM criteria for 'Phase 2' of CB Google Analytics
      if (this.isPhysicalMeasurement) {
        this.trackEvent(param.subtype, 'Add');
      }
      const wizard = wizardStore.getValue();
      let selections = selectionsStore.getValue();
      if (!selections.includes(param.parameterId)) {
        selections = [param.parameterId, ...selections];
        selectionsStore.next(selections);
      } else {
        wizard.item.searchParameters = wizard.item.searchParameters
          .filter(p => p.parameterId !== param.parameterId);
      }
      wizard.item.searchParameters.push(param);
      wizardStore.next(wizard);
      this.props.close();
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
      return typeof PM_UNITS[subtype] !== 'undefined';
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

    render() {
      const {node} = this.props;
      const {calculating, count, countError, form, loading, options} = this.state;
      const {formErrors, formValid} = this.validateForm();
      const disabled = calculating || form.exists || !formValid || form.num.every(attr => attr.operator === 'ANY');
      const showRange = this
      return (loading ?
        <SpinnerOverlay/> :
        <div style={{margin: '0.5rem 0 1.5rem'}}>
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
          {this.isMeasurement && <div>
            <div style={styles.label}>{this.displayName}</div>
            <CheckBox style={{marginLeft: '0.5rem'}}
              onChange={(v) => this.toggleCheckbox(v)}/> Any value (lab exists)
            {!form.exists && form.num.length > 0 && <div style={styles.orCircle}>OR</div>}
          </div>}
          {!form.exists && <React.Fragment>
            {form.num.map((attr, a) => <div key={a}>
              {this.isMeasurement && <div style={styles.label}>Numeric Values</div>}
              {this.isSurvey && <div style={styles.label}>{ppiQuestions.getValue()[node.parentId].name}</div>}
              {this.isBloodPressure && <div style={styles.label}>{attr.name}</div>}
              <div style={styles.container}>
                <div style={styles.dropdown}>
                  <Dropdown style={{width: '100%'}} value={attr.operator} options={options}
                    placeholder='Select Operator' onChange={(e) => this.selectChange(a, e.value)}/>
                </div>
                {![null, 'ANY'].includes(attr.operator) && <div>
                  <input style={styles.number} type='number' value={attr.operands[0] || ''}
                    min={attr.MIN} max={attr.MAX}
                    onChange={(e) => this.inputChange(e.target.value, a, 0)}/>
                  {this.hasUnits && <span> {PM_UNITS[node.subtype]}</span>}
                </div>}
                {attr.operator === Operator.BETWEEN && <React.Fragment>
                  <div style={{padding: '0.2rem 1.5rem 0 1rem'}}>and</div>
                  <div>
                    <input style={styles.number} type='number' value={attr.operands[1] || ''}
                      min={attr.MIN} max={attr.MAX}
                      onChange={(e) => this.inputChange(e.target.value, a, 1)}/>
                    {this.hasUnits && <span> {PM_UNITS[node.subtype]}</span>}
                  </div>
                </React.Fragment>}
                {this.hasRange && ![null, 'ANY'].includes(attr.operator) &&
                  <span style={{paddingTop: '0.2rem'}}>&nbsp;Range: {attr.MIN} - {attr.MAX}</span>
                }
              </div>
            </div>)}
            {form.cat.length > 0 && <React.Fragment>
              <div style={styles.orCircle}>OR</div>
              <div style={styles.label}>Categorical Values</div>
              <div style={{marginLeft: '0.5rem'}}>
                {form.cat.map((attr, a) => <div key={a} style={styles.categorical}>
                  <CheckBox checked={attr.checked} style={{marginRight: '3px'}}
                    onChange={(v) => this.checkboxChange(v, a)} />
                  {attr.conceptName}&nbsp;
                  <span style={styles.badge}> {parseInt(attr.estCount, 10).toLocaleString()}</span>
                </div>)}
              </div>
            </React.Fragment>}
          </React.Fragment>}
          <div style={styles.countPreview}>
            <div style={styles.row}>
              <div style={styles.buttonContainer}>
                <Button type='primary' disabled={disabled}
                  style={{
                    ...styles.button,
                    ...(disabled ? {opacity: 0.4} : {}),
                    background: colorWithWhiteness(colors.primary, .2)
                  }}
                  onClick={() => this.requestPreview()}>
                  {calculating && <Spinner size={16} style={styles.spinner}/>}
                  Calculate
                </Button>
              </div>
              <div style={styles.resultsContainer}>
                <div style={{fontWeight: 'bold'}}>Results</div>
                <div>
                  Number Participants:
                  <span> {count === null ? '--' : count.toLocaleString()} </span>
                </div>
              </div>
              {!calculating && formValid && <div style={styles.buttonContainer}>
                <Button type='link'
                  style={{...styles.button, color: colorWithWhiteness(colors.primary, .2)}}
                  onClick={() => this.addParameterToSearchItem()}> ADD THIS</Button>
              </div>}
            </div>
          </div>
        </div>
      );
    }
  }
);

@Component({
  selector: 'crit-attributes-page',
  template: '<div #root></div>'
})
export class AttributesPageComponent extends ReactWrapperBase {
  @Input('node') node: Props['node'];
  @Input('close') close: Props['close'];

  constructor() {
    super(AttributesPage, ['node', 'close']);
  }
}
