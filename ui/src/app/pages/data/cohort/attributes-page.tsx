import * as React from 'react';
import * as fp from 'lodash/fp';
import { Dropdown } from 'primereact/dropdown';

import {
  AttrName,
  CriteriaSubType,
  CriteriaType,
  Domain,
  Operator,
} from 'generated/fetch';

import { Button, StyledExternalLink } from 'app/components/buttons';
import { FlexRowWrap } from 'app/components/flex';
import { ClrIcon } from 'app/components/icons';
import { CheckBox, NumberInput } from 'app/components/inputs';
import { Spinner, SpinnerOverlay } from 'app/components/spinners';
import {
  PM_UNITS,
  PREDEFINED_ATTRIBUTES,
} from 'app/pages/data/cohort/constant';
import {
  ppiQuestions,
  ppiSurveys,
} from 'app/pages/data/cohort/search-state.service';
import { Selection } from 'app/pages/data/cohort/selection-list';
import {
  COPE_SURVEY_GROUP_NAME,
  MINUTE_SURVEY_GROUP_NAME,
} from 'app/pages/data/cohort/tree-node';
import {
  domainToTitle,
  mapParameter,
  sanitizeNumericalInput,
  stripHtml,
  subTypeToTitle,
} from 'app/pages/data/cohort/utils';
import { cohortBuilderApi } from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import {
  reactStyles,
  withCurrentCohortCriteria,
  withCurrentWorkspace,
} from 'app/utils';
import { AnalyticsTracker } from 'app/utils/analytics';
import {
  currentCohortCriteriaStore,
  currentWorkspaceStore,
} from 'app/utils/navigation';
import { WorkspaceData } from 'app/utils/workspace-data';

const styles = reactStyles({
  countPreview: {
    borderTop: `1px solid ${colorWithWhiteness(colors.black, 0.59)}`,
    marginTop: '1.5rem',
    paddingTop: '1.5rem',
  },
  row: {
    display: 'flex',
    flexWrap: 'wrap',
  },
  label: {
    color: colors.primary,
    fontWeight: 600,
    display: 'flex',
    lineHeight: '1.125rem',
  },
  orCircle: {
    backgroundColor: colorWithWhiteness(colors.primary, 0.75),
    borderRadius: '50%',
    color: colors.primary,
    width: '3.375rem',
    height: '3.375rem',
    margin: '0.75rem 0',
    lineHeight: '3.375rem',
    textAlign: 'center',
    fontSize: '12px',
    fontWeight: 600,
  },
  andCircle: {
    backgroundColor: colorWithWhiteness(colors.primary, 0.75),
    borderRadius: '50%',
    color: colors.primary,
    width: '3.375rem',
    height: '3.375rem',
    lineHeight: '3.375rem',
    textAlign: 'center',
    fontSize: '12px',
    fontWeight: 600,
    position: 'absolute',
    left: '42%',
  },
  andDivider: {
    height: '1.725rem',
    marginBottom: '1.725rem',
    width: '100%',
    borderBottom: `2px solid ${colorWithWhiteness(colors.primary, 0.75)}`,
  },
  container: {
    display: 'flex',
    marginLeft: 'auto',
    marginRight: 'auto',
  },
  dropdown: {
    width: '18rem',
    marginRight: '1.5rem',
  },
  categorical: {
    width: '100%',
    marginBottom: '0.375rem',
  },
  badge: {
    background: colors.primary,
    color: colors.white,
    fontSize: '10px',
    height: '0.9375rem',
    padding: '0 4px',
    marginLeft: '0.375rem',
    borderRadius: '10px',
    display: 'inline-flex',
    verticalAlign: 'middle',
    alignItems: 'center',
  },
  addButtonContainer: {
    bottom: '1.5rem',
    position: 'absolute',
    right: '1.5rem',
  },
  addButton: {
    height: '3rem',
    borderRadius: '5px',
    fontWeight: 600,
    marginRight: '0.75rem',
  },
  calculateButton: {
    height: '2.625rem',
    border: '1px solid',
    borderColor: colors.accent,
    borderRadius: '2px',
    fontWeight: 100,
  },
  spinner: {
    marginRight: '0.375rem',
    marginLeft: '-0.375rem',
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
    margin: '0.375rem 0',
  },
  errors: {
    background: colorWithWhiteness(colors.danger, 0.7),
    color: colorWithWhiteness(colors.dark, 0.1),
    fontSize: '11px',
    border: '1px solid #ebafa6',
    borderRadius: '3px',
    margin: '0.375rem 0',
    padding: '3px 5px',
  },
  errorItem: {
    lineHeight: '16px',
  },
  moreInfo: {
    color: colors.accent,
    cursor: 'pointer',
    fontWeight: 300,
    margin: '1.125rem 0',
    textDecoration: 'underline',
  },
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
  const {
    addButtonText,
    addFn,
    backFn,
    calculateFn,
    calculating,
    count,
    disableAdd,
    disableCalculate,
  } = props;
  return (
    <div
      style={{
        background: colorWithWhiteness(colors.primary, 0.87),
        bottom: 0,
        position: 'sticky',
      }}
    >
      <FlexRowWrap style={styles.countPreview}>
        <div style={styles.resultsContainer}>
          <Button
            id='attributes-calculate'
            data-test-id='attributes-calculate-btn'
            type='secondaryLight'
            disabled={disableCalculate}
            style={
              disableCalculate
                ? {
                    ...styles.calculateButton,
                    borderColor: colorWithWhiteness(colors.dark, 0.6),
                  }
                : styles.calculateButton
            }
            onClick={() => calculateFn()}
          >
            {calculating && <Spinner size={16} style={styles.spinner} />}{' '}
            Calculate
          </Button>
        </div>
        <div style={styles.resultsContainer}>
          <div style={{ fontWeight: 600 }}>
            Number of Participants:
            <span> {count === null ? '--' : count.toLocaleString()} </span>
          </div>
        </div>
      </FlexRowWrap>
      <FlexRowWrap
        style={{ flexDirection: 'row-reverse', marginTop: '0.75rem' }}
      >
        <Button
          type='primary'
          id='attributes-add-btn'
          data-test-id='attributes-add-btn'
          disabled={disableAdd}
          style={styles.addButton}
          onClick={() => addFn()}
        >
          {addButtonText}
        </Button>
        <Button
          type='link'
          style={{ color: colors.primary, marginRight: '1.125rem' }}
          onClick={() => backFn()}
        >
          BACK
        </Button>
      </FlexRowWrap>
    </div>
  );
};

const optionUtil = {
  ANY: { display: 'Any value', code: 'Any' },
  EQUAL: { display: '= ', code: '01' },
  GREATER_THAN_OR_EQUAL_TO: { display: '>= ', code: '02' },
  LESS_THAN_OR_EQUAL_TO: { display: '<= ', code: '03' },
  BETWEEN: { display: '', code: '04' },
};

interface AttributeForm {
  anyValue: boolean; // Include any values that exist (Measurements and COPE only)
  anyVersion: boolean; // Include any version that exist (COPE only)
  num: Array<any>; // Numerical attributes (Physical Measurements, Measurements or COPE)
  cat: Array<any>; // Categorical attributes (Measurements only)
}

export interface AttributesPageProps {
  back: Function;
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
  formErrors: Array<string>;
  formValid: boolean;
  isCOPESurvey: boolean;
  isCOPEOrMinuteSurvey: boolean;
  loading: boolean;
  options: any;
}
export const AttributesPage = fp.flow(
  withCurrentWorkspace(),
  withCurrentCohortCriteria()
)(
  class extends React.Component<AttributesPageProps, State> {
    constructor(props: AttributesPageProps) {
      super(props);
      this.state = {
        calculating: false,
        count: null,
        countError: false,
        form: { anyValue: false, anyVersion: false, num: [], cat: [] },
        formErrors: [],
        formValid: false,
        isCOPESurvey: false,
        isCOPEOrMinuteSurvey: false,
        loading: true,
        options: [
          { label: 'Equals', value: Operator.EQUAL },
          {
            label: 'Greater Than or Equal To',
            value: Operator.GREATERTHANOREQUALTO,
          },
          { label: 'Less Than or Equal To', value: Operator.LESSTHANOREQUALTO },
          { label: 'Between', value: Operator.BETWEEN },
        ],
      };
    }

    componentDidMount() {
      this.initAttributeForm();
    }

    componentDidUpdate(prevProps: Readonly<AttributesPageProps>): void {
      if (this.props.node !== prevProps.node) {
        // A different node has been selected, so we reset the form and load the new attributes
        this.setState(
          {
            form: { anyValue: false, anyVersion: false, num: [], cat: [] },
            formErrors: [],
            formValid: this.isPhysicalMeasurement,
            loading: true,
          },
          () => this.initAttributeForm()
        );
      }
    }

    initAttributeForm() {
      const {
        node: { subtype },
      } = this.props;
      const { form, options } = this.state;
      if (this.isSurvey) {
        this.getSurveyAttributes();
      } else if (this.isMeasurement || this.isObservation) {
        this.getAttributes();
      } else {
        if (!options.find((opt) => opt.value === AttrName.ANY.toString())) {
          options.unshift({
            label: optionUtil.ANY.display,
            value: AttrName[AttrName.ANY],
          });
        }
        form.num =
          subtype === CriteriaSubType[CriteriaSubType.BP]
            ? JSON.parse(JSON.stringify(PREDEFINED_ATTRIBUTES.BP_DETAIL))
            : [{ name: subtype, operator: 'ANY', operands: [] }];
        this.setState({
          count: this.nodeCount,
          form,
          formValid: true,
          loading: false,
          options,
        });
      }
    }

    async getSurveyAttributes() {
      const {
        node: { conceptId, parentId, path, subtype, value },
        workspace: { namespace, id },
      } = this.props;
      const { form, options } = this.state;
      const { cdrVersionId } = currentWorkspaceStore.getValue();
      const surveyId = path.split('.')[0];
      let surveyNode = ppiSurveys
        .getValue()
        [cdrVersionId]?.find((n) => n.id === +surveyId);
      if (!surveyNode) {
        await cohortBuilderApi()
          .findCriteriaBy(
            namespace,
            id,
            Domain.SURVEY.toString(),
            CriteriaType.PPI.toString(),
            false,
            0
          )
          .then(({ items }) => {
            const rootSurveys = ppiSurveys.getValue();
            rootSurveys[cdrVersionId] = items;
            ppiSurveys.next(rootSurveys);
            surveyNode = items.find((n) => n.id === +surveyId);
          });
      }
      if (
        !!surveyNode &&
        [COPE_SURVEY_GROUP_NAME, MINUTE_SURVEY_GROUP_NAME].includes(
          surveyNode.name
        )
      ) {
        const promises = [];
        if (
          subtype === CriteriaSubType.QUESTION ||
          (subtype === CriteriaSubType.ANSWER && !value)
        ) {
          promises.push(
            cohortBuilderApi().findSurveyVersionByQuestionConceptId(
              namespace,
              id,
              conceptId
            )
          );
        } else {
          promises.push(
            cohortBuilderApi().findSurveyVersionByQuestionConceptIdAndAnswerConceptId(
              namespace,
              id,
              ppiQuestions.getValue()[parentId].conceptId,
              +value
            )
          );
        }
        if (subtype === CriteriaSubType.ANSWER) {
          promises.push(
            cohortBuilderApi().findCriteriaAttributeByConceptId(
              namespace,
              id,
              conceptId
            )
          );
        }
        const [surveyVersions, numericalAttributes] = await Promise.all(
          promises
        );
        form.cat = surveyVersions.items.map((attr) => ({
          checked: false,
          conceptName: attr.displayName,
          estCount: attr.itemCount,
          valueAsConceptId: attr.surveyVersionConceptId,
        }));
        if (
          numericalAttributes &&
          !(subtype === CriteriaSubType.ANSWER.toString() && !!value)
        ) {
          numericalAttributes.items.forEach((attr) => {
            if (!form.num.length) {
              form.num.push({
                name: AttrName.NUM,
                operator: null,
                operands: [],
                conceptId: +value,
                [attr.conceptName]: parseFloat(attr.estCount),
              });
            } else {
              form.num[0][attr.conceptName] = parseFloat(attr.estCount);
            }
          });
        }
        this.setState({
          count: null,
          form,
          isCOPEOrMinuteSurvey: true,
          loading: false,
        });
      } else {
        if (
          !options.find((option) => option.value === AttrName.ANY.toString())
        ) {
          options.unshift({
            label: optionUtil.ANY.display,
            value: AttrName[AttrName.ANY],
          });
        }
        this.setState(
          { formValid: true, isCOPEOrMinuteSurvey: false, options },
          () => this.getAttributes()
        );
      }
    }

    getAttributes() {
      const {
        node: { conceptId },
        workspace: { id, namespace },
      } = this.props;
      const { form } = this.state;
      cohortBuilderApi()
        .findCriteriaAttributeByConceptId(namespace, id, conceptId)
        .then((resp) => {
          resp.items.forEach((attr) => {
            if (attr.type === AttrName[AttrName.NUM]) {
              // NUM attributes set the min and max range for the number inputs in the attributes form
              if (!form.num.length) {
                form.num.push({
                  name: AttrName.NUM,
                  operator: this.isSurvey ? 'ANY' : null,
                  operands: [],
                  conceptId: conceptId,
                  [attr.conceptName]: parseFloat(attr.estCount),
                });
              } else {
                form.num[0][attr.conceptName] = parseFloat(attr.estCount);
              }
            } else {
              // CAT attributes are displayed as checkboxes in the attributes form
              if (parseInt(attr.estCount, 10) > 0) {
                // Property 'checked' does not exist on type 'CriteriaAttribute'.
                // TODO RW-5572 confirm proper behavior and fix
                // eslint-disable-next-line @typescript-eslint/dot-notation
                attr['checked'] = false;
                form.cat.push(attr);
              }
            }
          });
          this.setState({ count: null, form, loading: false });
        });
    }

    toggleAnyValueCheckbox(checked: boolean) {
      const { form } = this.state;
      let {
        node: { count },
      } = this.props;
      form.anyValue = checked;
      if (checked) {
        form.num = form.num.map((attr) => ({
          ...attr,
          operator: this.isPhysicalMeasurement ? 'ANY' : null,
          operands: [],
        }));
        if (this.isMeasurement || this.isObservation) {
          form.cat = form.cat.map((attr) => ({ ...attr, checked: false }));
        }
      }
      if (!checked || count === -1) {
        count = null;
      }
      this.setState({ form, count }, () => this.validateForm());
    }

    toggleAnyVersionCheckbox(checked: boolean) {
      const { form } = this.state;
      form.anyVersion = checked;
      form.cat = form.cat.map((attr) => ({ ...attr, checked: false }));
      this.setState({ form }, () => this.validateForm());
    }

    selectChange(attributeIndex: number, value: string) {
      const { form } = this.state;
      form.num[attributeIndex].operator = value;
      if (this.isBloodPressure) {
        // for blood pressure, either both operators have to be 'ANY' OR neither can be 'ANY'
        const otherAttribute = attributeIndex === 0 ? 1 : 0;
        if (value === 'ANY') {
          form.num[attributeIndex].operands = [];
          form.num[otherAttribute].operands = [];
          form.num[otherAttribute].operator = 'ANY';
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
      const count = value === 'ANY' && !this.isSurvey ? this.nodeCount : null;
      this.setState({ form, count }, () => this.validateForm());
    }

    inputChange(input: string, attributeIndex: number, operandIndex: number) {
      const { form } = this.state;
      form.num[attributeIndex].operands[operandIndex] =
        sanitizeNumericalInput(input);
      this.setState({ form, count: null }, () => this.validateForm());
    }

    checkboxChange(checked: boolean, index: number) {
      const { form } = this.state;
      form.cat[index].checked = checked;
      this.setState({ form, count: null }, () => this.validateForm());
    }

    validateForm() {
      const { form, isCOPEOrMinuteSurvey } = this.state;
      if (
        (form.anyValue || (isCOPEOrMinuteSurvey && form.num.length === 0)) &&
        (!isCOPEOrMinuteSurvey || form.anyVersion)
      ) {
        this.setState({ formValid: true, formErrors: [] });
      } else {
        let formValid = true,
          operatorSelected = form.num.length !== 0;
        const formErrors = form.num.reduce((acc, attr) => {
          const { MIN, MAX, operator } = attr;
          const operands = attr.operands.map((op) => parseFloat(op));
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
          if (this.isPhysicalMeasurement && operands.some((op) => op < 0)) {
            formValid = false;
            acc.add('Form cannot accept negative values');
          }
          if (this.hasRange && operands.some((op) => op < MIN || op > MAX)) {
            formValid = false;
            acc.add(
              `Values must be between ${MIN.toLocaleString()} and ${MAX.toLocaleString()}`
            );
          }
          return acc;
        }, new Set());
        // The second condition sets formValid to false if this is a Measurements or COPE attribute with no operator selected from the
        // dropdown and no categorical checkboxes checked
        if ((this.isMeasurement || this.isObservation) && formValid) {
          formValid = operatorSelected || form.cat.some((attr) => attr.checked);
        }
        if (isCOPEOrMinuteSurvey && formValid) {
          formValid =
            (form.num.length === 0 || form.anyValue || operatorSelected) &&
            (form.anyVersion || form.cat.some((attr) => attr.checked));
        }
        this.setState({ formErrors: Array.from(formErrors), formValid });
      }
    }

    get nodeCount() {
      const {
        node: { count, parentId },
      } = this.props;
      if (this.isSurvey) {
        const parent = ppiQuestions.getValue()[parentId];
        return !!parent ? parent.count : null;
      } else {
        return count;
      }
    }

    get paramId() {
      const {
        node: { conceptId, id, value },
      } = this.props;
      const { form, isCOPEOrMinuteSurvey } = this.state;
      const code = form.anyValue
        ? 'Any'
        : form.num.reduce((acc, attr) => {
            if (attr.operator) {
              acc += optionUtil[attr.operator].code;
            }
            return acc;
          }, '');
      const paramConceptId =
        isCOPEOrMinuteSurvey && !!value ? value : conceptId;
      // make sure param ID is unique for different checkbox combinations
      const catValues = form.cat
        .filter((c) => c.checked)
        .map((c) => c.valueAsConceptId)
        .join('');
      return `param${(paramConceptId || id) + code + catValues}`;
    }

    get displayName() {
      const {
        node: { name },
      } = this.props;
      return stripHtml(name);
    }

    get paramWithAttributes() {
      const {
        node,
        node: { name, subtype, value },
      } = this.props;
      const { form, isCOPEOrMinuteSurvey } = this.state;
      let paramName;
      const attrs = [];
      if (!isCOPEOrMinuteSurvey && form.anyValue) {
        paramName = name + ` (${optionUtil.ANY.display})`;
      } else if (isCOPEOrMinuteSurvey && form.anyValue && form.anyVersion) {
        paramName = name + ' (Any version AND any value)';
      } else {
        form.num
          .filter((at) => at.operator)
          .forEach(({ operator, operands, conceptId }) => {
            const attr = { name: AttrName.NUM, operator, operands };
            if (subtype === CriteriaSubType.BP) {
              // Property 'conceptId' does not exist on type '{ name: AttrName; operator: any; operands: any; }'..
              // TODO RW-5572 confirm proper behavior and fix
              // eslint-disable-next-line @typescript-eslint/dot-notation
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
        if (form.cat.some((at) => at.checked)) {
          const catOperands = form.cat.reduce((checked, current) => {
            if (current.checked) {
              checked.push(current.valueAsConceptId.toString());
            }
            return checked;
          }, []);
          if (isCOPEOrMinuteSurvey && !form.anyVersion) {
            attrs.push({
              name: AttrName.SURVEYVERSIONCONCEPTID,
              operator: Operator.IN,
              operands: catOperands,
            });
          } else if (!isCOPEOrMinuteSurvey) {
            attrs.push({
              name: AttrName.CAT,
              operator: Operator.IN,
              operands: catOperands,
            });
          }
        }
        paramName = this.paramName;
      }
      if (
        isCOPEOrMinuteSurvey &&
        subtype === CriteriaSubType.ANSWER &&
        !!value
      ) {
        attrs.push({
          name: AttrName.CAT,
          operator: Operator.IN,
          operands: [value],
        });
      }
      if (
        subtype === CriteriaSubType.ANSWER &&
        (form.anyValue ||
          (form.num.length && form.num[0].operator === 'ANY')) &&
        value === ''
      ) {
        attrs.push({ name: AttrName.ANY });
      }
      return {
        ...node,
        parameterId: this.paramId,
        name: paramName,
        attributes: attrs,
      };
    }

    get paramName() {
      const { node } = this.props;
      const { form } = this.state;
      const selectionDisplay = [];
      let name = '';
      if (form.anyVersion) {
        selectionDisplay.push('Any version');
      }
      form.num
        .filter((at) => at.operator)
        .forEach((attr, i) => {
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
            name +=
              optionUtil[attr.operator].display +
              attr.operands
                .map((op) => parseFloat(op).toLocaleString())
                .join('-');
          }
        });
      if (name !== '') {
        selectionDisplay.push(name);
      }
      form.cat
        .filter((ca) => ca.checked)
        .forEach((attr) => selectionDisplay.push(attr.conceptName));
      const nodeName =
        node.domainId === Domain.SURVEY && !node.group
          ? `${ppiQuestions.getValue()[node.parentId].name} - ${node.name}`
          : node.name;
      return (
        nodeName +
        ' (' +
        selectionDisplay.join(', ') +
        (this.hasUnits && form.num[0].operator !== AttrName.ANY
          ? ' ' + PM_UNITS[node.subtype]
          : '') +
        ')'
      );
    }

    requestPreview() {
      const {
        workspace: { id, namespace },
      } = this.props;

      this.setState({ count: null, calculating: true, countError: false });
      const param = this.paramWithAttributes;
      const label = `Calculate - ${domainToTitle(param.domainId)}${
        this.isPhysicalMeasurement ? subTypeToTitle(param.subtype) : ''
      }`;
      AnalyticsTracker.CohortBuilder.AttributesAction(label);
      const request = {
        excludes: [],
        includes: [
          {
            items: [
              {
                type: param.domainId,
                searchParameters: [mapParameter(param)],
                modifiers: [],
              },
            ],
            temporal: false,
          },
        ],
        dataFilters: [],
      };
      cohortBuilderApi()
        .countParticipants(namespace, id, request)
        .then(
          (response) => {
            this.setState({ count: response, calculating: false });
          },
          () => {
            this.setState({ calculating: false, countError: true });
          }
        );
    }

    addParameterToSearchItem() {
      const { close } = this.props;
      let { criteria } = this.props;
      const param = this.paramWithAttributes;
      const label = `Add - ${domainToTitle(param.domainId)}${
        this.isPhysicalMeasurement ? subTypeToTitle(param.subtype) : ''
      }`;
      AnalyticsTracker.CohortBuilder.AttributesAction(label);
      criteria = criteria.filter(
        (crit) => crit.parameterId !== param.parameterId
      );
      currentCohortCriteriaStore.next([...criteria, param]);
      close();
    }

    get hasUnits() {
      const {
        node: { subtype },
      } = this.props;
      return (
        this.isPhysicalMeasurement && typeof PM_UNITS[subtype] !== 'undefined'
      );
    }

    get isMeasurement() {
      const {
        node: { domainId },
      } = this.props;
      return domainId === Domain.MEASUREMENT;
    }

    get isObservation() {
      const {
        node: { domainId },
      } = this.props;
      return domainId === Domain.OBSERVATION;
    }

    get isPhysicalMeasurement() {
      const {
        node: { domainId },
      } = this.props;
      return domainId === Domain.PHYSICALMEASUREMENT;
    }

    get isSurvey() {
      const {
        node: { domainId },
      } = this.props;
      return domainId === Domain.SURVEY;
    }

    get isBloodPressure() {
      const {
        node: { subtype },
      } = this.props;
      return subtype === CriteriaSubType.BP;
    }

    get hasRange() {
      return this.isMeasurement || this.isObservation || this.isSurvey;
    }

    get disableAddButton() {
      const { calculating, formValid } = this.state;
      return calculating || !formValid;
    }

    get disableCalculateButton() {
      const { calculating, count, form, formValid, isCOPEOrMinuteSurvey } =
        this.state;
      return (
        calculating ||
        !formValid ||
        (form.anyValue && count !== null) ||
        (isCOPEOrMinuteSurvey &&
          !form.anyVersion &&
          !form.cat.some((attr) => attr.checked)) ||
        (!this.isSurvey &&
          form.num.length &&
          form.num.every((attr) => attr.operator === 'ANY'))
      );
    }

    renderNumericalAttributes() {
      const {
        node: { count, subtype },
      } = this.props;
      const { form, isCOPEOrMinuteSurvey, options } = this.state;
      return (
        form.num.length > 0 && (
          <React.Fragment>
            {(this.isMeasurement || this.isObservation) && (
              <div style={styles.label}>Numeric Values</div>
            )}
            {isCOPEOrMinuteSurvey && (
              <div>
                <CheckBox onChange={(v) => this.toggleAnyValueCheckbox(v)} />{' '}
                Any value
                {count > -1 && (
                  <span style={styles.badge}> {count.toLocaleString()}</span>
                )}
              </div>
            )}
            {!(isCOPEOrMinuteSurvey && form.anyValue) &&
              form.num.map((attr, a) => (
                <div key={a}>
                  {this.isBloodPressure && (
                    <div style={styles.label}>{attr.name}</div>
                  )}
                  {isCOPEOrMinuteSurvey && (
                    <div style={styles.orCircle}>OR</div>
                  )}
                  <Dropdown
                    id={`numerical-dropdown-${a}`}
                    data-test-id={`numerical-dropdown-${a}`}
                    style={{ marginBottom: '0.75rem', width: '100%' }}
                    value={attr.operator}
                    options={options}
                    placeholder='Select Operator'
                    onChange={(e) => this.selectChange(a, e.value)}
                    appendTo='self'
                  />
                  <FlexRowWrap>
                    {![null, 'ANY'].includes(attr.operator) && (
                      <div style={{ width: '33%' }}>
                        <NumberInput
                          id={`numerical-input-${a}-0`}
                          data-test-id={`numerical-input-${a}-0`}
                          style={{
                            padding: '0 0.375rem',
                            ...(this.hasUnits ? { width: '70%' } : {}),
                          }}
                          value={attr.operands[0]}
                          min={attr.MIN}
                          max={attr.MAX}
                          onChange={(v) => this.inputChange(v, a, 0)}
                        />
                        {this.hasUnits && <span> {PM_UNITS[subtype]}</span>}
                      </div>
                    )}
                    {attr.operator === Operator.BETWEEN && (
                      <React.Fragment>
                        <div style={{ padding: '0.3rem 2.25rem 0 1.5rem' }}>
                          and
                        </div>
                        <div style={{ width: '33%' }}>
                          <NumberInput
                            id={`numerical-input-${a}-1`}
                            data-test-id={`numerical-input-${a}-1`}
                            style={{
                              padding: '0 0.375rem',
                              ...(this.hasUnits ? { width: '70%' } : {}),
                            }}
                            value={attr.operands[1]}
                            min={attr.MIN}
                            max={attr.MAX}
                            onChange={(v) => this.inputChange(v, a, 1)}
                          />
                          {this.hasUnits && <span> {PM_UNITS[subtype]}</span>}
                        </div>
                      </React.Fragment>
                    )}
                  </FlexRowWrap>
                  {this.hasRange && ![null, 'ANY'].includes(attr.operator) && (
                    <div style={{ paddingTop: '0.3rem' }}>
                      Range: {attr.MIN.toLocaleString()} -{' '}
                      {attr.MAX.toLocaleString()}
                    </div>
                  )}
                </div>
              ))}
          </React.Fragment>
        )
      );
    }

    renderCategoricalAttributes() {
      const {
        node: { count },
      } = this.props;
      const { form, isCOPEOrMinuteSurvey } = this.state;
      return (
        form.cat.length > 0 && (
          <React.Fragment>
            {isCOPEOrMinuteSurvey && (
              <div>
                <CheckBox onChange={(v) => this.toggleAnyVersionCheckbox(v)} />{' '}
                Any version
                {count > -1 && (
                  <span style={styles.badge}>{count.toLocaleString()}</span>
                )}
              </div>
            )}
            {!(isCOPEOrMinuteSurvey && form.anyVersion) && (
              <React.Fragment>
                {(form.num.length > 0 || this.isObservation) && (
                  <div style={styles.orCircle}>OR</div>
                )}
                {!isCOPEOrMinuteSurvey && (
                  <div style={styles.label}>Categorical Values</div>
                )}
                {form.cat.map((attr, a) => (
                  <div key={a} style={styles.categorical}>
                    <CheckBox
                      checked={attr.checked}
                      style={{ marginRight: '3px' }}
                      onChange={(v) => this.checkboxChange(v, a)}
                    />
                    {attr.conceptName}
                    <span style={styles.badge}>
                      {parseInt(attr.estCount, 10).toLocaleString()}
                    </span>
                  </div>
                ))}
              </React.Fragment>
            )}
          </React.Fragment>
        )
      );
    }

    render() {
      const {
        back,
        node: { domainId, name, parentId, subtype },
      } = this.props;
      const {
        calculating,
        count,
        countError,
        form,
        formErrors,
        isCOPEOrMinuteSurvey,
        loading,
      } = this.state;
      return loading ? (
        <SpinnerOverlay />
      ) : (
        <div id='attributes-form' style={{ marginTop: '0.75rem' }}>
          {isCOPEOrMinuteSurvey ? (
            <div>
              <h3
                style={{
                  fontWeight: 600,
                  margin: '0 0 0.75rem',
                  textTransform: 'capitalize',
                }}
              >
                COPE Survey (COVID-19) attribute
              </h3>
              <div style={{ lineHeight: '1.125rem', paddingRight: '2.25rem' }}>
                The COPE survey is longitudinal and will change over time. Use
                the following attributes to select data from one or more
                versions.
              </div>
              <div style={styles.moreInfo}>
                <StyledExternalLink
                  href='https://www.researchallofus.org/data-tools/survey-explorer/cope-survey/'
                  target='_blank'
                  rel='noopener noreferrer'
                >
                  More info
                </StyledExternalLink>
              </div>
            </div>
          ) : (
            <h3
              style={{
                fontWeight: 600,
                margin: '0 0 0.75rem',
                textTransform: 'capitalize',
              }}
            >
              {this.isPhysicalMeasurement
                ? name
                : domainId.toString().toLowerCase()}{' '}
              Detail
            </h3>
          )}
          {this.isSurvey && (
            <div style={{ ...styles.label, marginBottom: '0.75rem' }}>
              {subtype === CriteriaSubType.ANSWER
                ? `${ppiQuestions.getValue()[parentId].name} - ${name}`
                : name}
            </div>
          )}
          {countError && (
            <div style={styles.error}>
              <ClrIcon
                style={{ margin: '0 0.75rem 0 0.375rem' }}
                className='is-solid'
                shape='exclamation-triangle'
                size='22'
              />
              Sorry, the request cannot be completed.
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
          {isCOPEOrMinuteSurvey ? (
            <div>
              {this.renderCategoricalAttributes()}
              {form.num.length > 0 && form.cat.length > 0 && (
                <div style={{ position: 'relative' }}>
                  <div style={styles.andCircle}>AND</div>
                  <div style={styles.andDivider} />
                </div>
              )}
              {this.renderNumericalAttributes()}
            </div>
          ) : (
            <div>
              {(this.isMeasurement || this.isObservation) && (
                <div>
                  <div style={styles.label}>{this.displayName}</div>
                  <CheckBox
                    onChange={(v) => this.toggleAnyValueCheckbox(v)}
                  />{' '}
                  Any value {this.isMeasurement && <span> (lab exists)</span>}
                  {!form.anyValue && form.num.length > 0 && (
                    <div style={styles.orCircle}>OR</div>
                  )}
                </div>
              )}
              {!form.anyValue && (
                <div style={{ minHeight: '15rem' }}>
                  {this.renderNumericalAttributes()}
                  {this.renderCategoricalAttributes()}
                </div>
              )}
            </div>
          )}
          <CalculateFooter
            addButtonText='ADD THIS'
            addFn={() => this.addParameterToSearchItem()}
            backFn={() => back()}
            calculateFn={() => this.requestPreview()}
            calculating={calculating}
            count={count}
            disableAdd={this.disableAddButton}
            disableCalculate={this.disableCalculateButton}
          />
        </div>
      );
    }
  }
);
