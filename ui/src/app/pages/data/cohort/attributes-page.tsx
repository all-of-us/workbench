import * as React from 'react';
import { useEffect, useState } from 'react';
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

const defaultOptions = [
  { label: 'Equals', value: Operator.EQUAL.toString() },
  {
    label: 'Greater Than or Equal To',
    value: Operator.GREATER_THAN_OR_EQUAL_TO.toString(),
  },
  {
    label: 'Less Than or Equal To',
    value: Operator.LESS_THAN_OR_EQUAL_TO.toString(),
  },
  { label: 'Between', value: Operator.BETWEEN.toString() },
];

const optionUtil = {
  ANY: { display: 'Any value', code: 'Any' },
  EQUAL: { display: '= ', code: '01' },
  GREATER_THAN_OR_EQUAL_TO: { display: '>= ', code: '02' },
  LESS_THAN_OR_EQUAL_TO: { display: '<= ', code: '03' },
  BETWEEN: { display: '', code: '04' },
};

export interface AttributesPageProps {
  back: Function;
  close: Function;
  criteria: Array<Selection>;
  node: any;
  workspace: WorkspaceData;
}

export const AttributesPage = fp.flow(
  withCurrentWorkspace(),
  withCurrentCohortCriteria()
)(({ back, close, criteria, node, workspace }: AttributesPageProps) => {
  const [calculating, setCalculating] = useState(false);
  const [attributeCount, setAttributeCount] = useState(null);
  const [countError, setCountError] = useState(false);
  const [anyValue, setAnyValue] = useState(false);
  const [anyVersion, setAnyVersion] = useState(false);
  const [numAttributes, setNumAttributes] = useState([]);
  const [catAttributes, setCatAttributes] = useState([]);
  const [formUpdated, setFormUpdated] = useState(0);
  const [formErrors, setFormErrors] = useState([]);
  const [formValid, setFormValid] = useState(false);
  const [isCOPEOrMinuteSurvey, setIsCOPEOrMinuteSurvey] = useState(false);
  const [loading, setLoading] = useState(true);
  const [options, setOptions] = useState(
    JSON.parse(JSON.stringify(defaultOptions))
  );

  const isSurvey = () => {
    return node.domainId === Domain.SURVEY;
  };

  const getAttributes = () => {
    const { conceptId } = node;
    const { id, namespace } = workspace;
    cohortBuilderApi()
      .findCriteriaAttributeByConceptId(namespace, id, conceptId)
      .then((resp) => {
        const newAttributes = { numAttributes: [], catAttributes: [] };
        resp.items.forEach((attr) => {
          if (attr.type === AttrName[AttrName.NUM]) {
            // NUM attributes set the min and max range for the number inputs in the attributes form
            if (!newAttributes.numAttributes.length) {
              newAttributes.numAttributes.push({
                name: AttrName.NUM,
                operator: isSurvey() ? 'ANY' : null,
                operands: [],
                conceptId: conceptId,
                [attr.conceptName]: parseFloat(attr.estCount),
              });
            } else {
              newAttributes.numAttributes[0][attr.conceptName] = parseFloat(
                attr.estCount
              );
            }
          } else {
            // CAT attributes are displayed as checkboxes in the attributes form
            if (parseInt(attr.estCount, 10) > 0) {
              // Property 'checked' does not exist on type 'CriteriaAttribute'.
              // TODO RW-5572 confirm proper behavior and fix
              // eslint-disable-next-line @typescript-eslint/dot-notation
              attr['checked'] = false;
              newAttributes.catAttributes.push(attr);
            }
          }
        });
        setAttributeCount(null);
        setCatAttributes(newAttributes.catAttributes);
        setNumAttributes(newAttributes.numAttributes);
        setLoading(false);
      });
  };

  const getSurveyAttributes = async () => {
    const { conceptId, parentId, path, subtype, value } = node;
    const { namespace, id } = workspace;
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
      const [surveyVersions, numericalAttributes] = await Promise.all(promises);
      const updatedCatAttributes = surveyVersions.items.map((attr) => ({
        checked: false,
        conceptName: attr.displayName,
        estCount: attr.itemCount,
        valueAsConceptId: attr.surveyVersionConceptId,
      }));
      setCatAttributes(updatedCatAttributes);
      if (
        numericalAttributes &&
        !(subtype === CriteriaSubType.ANSWER.toString() && !!value)
      ) {
        numericalAttributes.items.forEach((attr) => {
          if (!numAttributes.length) {
            numAttributes.push({
              name: AttrName.NUM,
              operator: null,
              operands: [],
              conceptId: +value,
              [attr.conceptName]: parseFloat(attr.estCount),
            });
          } else {
            numAttributes[0][attr.conceptName] = parseFloat(attr.estCount);
          }
        });
        setNumAttributes(numAttributes);
      }
      setAttributeCount(null);
      setIsCOPEOrMinuteSurvey(true);
      setLoading(false);
    } else {
      if (!options.find((option) => option.value === AttrName.ANY.toString())) {
        options.unshift({
          label: optionUtil.ANY.display,
          value: AttrName[AttrName.ANY],
        });
      }
      setFormValid(true);
      setIsCOPEOrMinuteSurvey(false);
      setOptions(options);
      getAttributes();
    }
  };

  const isPhysicalMeasurement = () => {
    return node.domainId === Domain.PHYSICAL_MEASUREMENT;
  };

  const hasUnits = () => {
    return (
      isPhysicalMeasurement && typeof PM_UNITS[node.subtype] !== 'undefined'
    );
  };

  const isMeasurement = () => {
    return node.domainId === Domain.MEASUREMENT;
  };

  const isObservation = () => {
    return node.domainId === Domain.OBSERVATION;
  };

  const isBloodPressure = () => {
    return node.subtype === CriteriaSubType.BP;
  };

  const hasRange = () => {
    return isMeasurement() || isObservation() || isSurvey();
  };

  const disableAddButton = () => {
    return calculating || !formValid;
  };

  const disableCalculateButton = () => {
    return (
      calculating ||
      !formValid ||
      (anyValue && attributeCount !== null) ||
      (isCOPEOrMinuteSurvey &&
        !anyVersion &&
        !catAttributes.some((attr) => attr.checked)) ||
      (!isSurvey() &&
        numAttributes.length &&
        numAttributes.every((attr) => attr.operator === 'ANY'))
    );
  };

  const nodeCount = () => {
    const { count, parentId } = node;
    if (isSurvey()) {
      const parent = ppiQuestions.getValue()[parentId];
      return !!parent ? parent.count : null;
    } else {
      return count;
    }
  };

  const initAttributeForm = () => {
    setLoading(true);
    setFormErrors([]);
    const { subtype } = node;
    if (isSurvey()) {
      getSurveyAttributes();
    } else if (isMeasurement() || isObservation()) {
      getAttributes();
    } else {
      if (!options.find((opt) => opt.value === AttrName.ANY.toString())) {
        options.unshift({
          label: optionUtil.ANY.display,
          value: AttrName[AttrName.ANY],
        });
      }
      const updatedNumAttributes =
        subtype === CriteriaSubType[CriteriaSubType.BP]
          ? JSON.parse(JSON.stringify(PREDEFINED_ATTRIBUTES.BP_DETAIL))
          : [{ name: subtype, operator: 'ANY', operands: [] }];
      setOptions(options);
      setAttributeCount(nodeCount());
      setNumAttributes(updatedNumAttributes);
      setFormValid(true);
      setLoading(false);
    }
  };

  const validateForm = () => {
    if (
      (anyValue || (isCOPEOrMinuteSurvey && numAttributes.length === 0)) &&
      (!isCOPEOrMinuteSurvey || anyVersion)
    ) {
      setFormErrors([]);
      setFormValid(true);
    } else {
      let updatedFormValid = true,
        operatorSelected = numAttributes.length !== 0;
      const updatedFormErrors = numAttributes.reduce((acc, attr) => {
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
              updatedFormValid = false;
            }
            break;
          default:
            if (operands.length === 0) {
              updatedFormValid = false;
            }
        }
        if (operands.includes(NaN)) {
          updatedFormValid = false;
          acc.add('Form can only accept valid numbers');
        }
        if (isPhysicalMeasurement() && operands.some((op) => op < 0)) {
          updatedFormValid = false;
          acc.add('Form cannot accept negative values');
        }
        if (hasRange() && operands.some((op) => op < MIN || op > MAX)) {
          updatedFormValid = false;
          acc.add(
            `Values must be between ${MIN.toLocaleString()} and ${MAX.toLocaleString()}`
          );
        }
        return acc;
      }, new Set());
      // The second condition sets formValid to false if this is a Measurements or COPE attribute with no operator selected from the
      // dropdown and no categorical checkboxes checked
      if ((isMeasurement() || isObservation()) && updatedFormValid) {
        updatedFormValid =
          operatorSelected || catAttributes.some((attr) => attr.checked);
      }
      if (isCOPEOrMinuteSurvey && updatedFormValid) {
        updatedFormValid =
          (numAttributes.length === 0 || anyValue || operatorSelected) &&
          (anyVersion || catAttributes.some((attr) => attr.checked));
      }
      setFormErrors(Array.from(updatedFormErrors));
      setFormValid(updatedFormValid);
    }
  };

  useEffect(() => {
    setOptions(defaultOptions);
    setFormUpdated(0);
    setAnyValue(false);
    setAnyVersion(false);
    setCatAttributes([]);
    setNumAttributes([]);
    initAttributeForm();
  }, [node.id]);

  useEffect(() => {
    if (formUpdated > 0) {
      validateForm();
    }
  }, [formUpdated]);

  const toggleAnyValueCheckbox = (checked: boolean) => {
    let { count } = node;
    setAnyValue(checked);
    if (checked) {
      setNumAttributes((prevNumAttributes) =>
        prevNumAttributes.map((attr) => ({
          ...attr,
          operator: isPhysicalMeasurement() ? 'ANY' : null,
          operands: [],
        }))
      );
      if (isMeasurement() || isObservation()) {
        setCatAttributes((prevCatAttributes) =>
          prevCatAttributes.map((attr) => ({ ...attr, checked: false }))
        );
      }
    }
    if (!checked || count === -1) {
      count = null;
    }
    setAttributeCount(count);
    setFormUpdated((prevFormUpdated) => prevFormUpdated + 1);
  };

  const toggleAnyVersionCheckbox = (checked: boolean) => {
    setAnyVersion(checked);
    setCatAttributes((prevCatAttributes) =>
      prevCatAttributes.map((attr) => ({ ...attr, checked: false }))
    );
    setFormUpdated((prevFormUpdated) => prevFormUpdated + 1);
  };

  const selectChange = (attributeIndex: number, value: string) => {
    const updatedNumAttributes = JSON.parse(JSON.stringify(numAttributes));
    updatedNumAttributes[attributeIndex].operator = value;
    if (isBloodPressure()) {
      // for blood pressure, either both operators have to be 'ANY' OR neither can be 'ANY'
      const otherAttribute = attributeIndex === 0 ? 1 : 0;
      if (value === 'ANY') {
        updatedNumAttributes[attributeIndex].operands = [];
        updatedNumAttributes[otherAttribute].operands = [];
        updatedNumAttributes[otherAttribute].operator = 'ANY';
      } else if (updatedNumAttributes[otherAttribute].operator === 'ANY') {
        updatedNumAttributes[otherAttribute].operator = value;
      }
    } else if (value === 'ANY') {
      updatedNumAttributes[attributeIndex].operands = [];
    }
    if (value !== Operator[Operator.BETWEEN]) {
      // delete second operand if it exists
      updatedNumAttributes[attributeIndex].operands.splice(1);
    }
    setAttributeCount(value === 'ANY' && !isSurvey() ? nodeCount() : null);
    setNumAttributes(updatedNumAttributes);
    setFormUpdated((prevFormUpdated) => prevFormUpdated + 1);
  };

  const inputChange = (
    input: string,
    attributeIndex: number,
    operandIndex: number
  ) => {
    setNumAttributes((prevNumAttributes) => {
      prevNumAttributes[attributeIndex].operands[operandIndex] =
        sanitizeNumericalInput(input);
      return prevNumAttributes;
    });
    setAttributeCount(null);
    setFormUpdated((prevFormUpdated) => prevFormUpdated + 1);
  };

  const checkboxChange = (checked: boolean, index: number) => {
    setCatAttributes((prevCatAttributes) => {
      prevCatAttributes[index].checked = checked;
      return prevCatAttributes;
    });
    setAttributeCount(null);
    setFormUpdated((prevFormUpdated) => prevFormUpdated + 1);
  };

  const paramId = () => {
    const { conceptId, id, value } = node;
    const code = anyValue
      ? 'Any'
      : numAttributes.reduce((acc, attr) => {
          if (attr.operator) {
            acc += optionUtil[attr.operator].code;
          }
          return acc;
        }, '');
    const paramConceptId = isCOPEOrMinuteSurvey && !!value ? value : conceptId;
    // make sure param ID is unique for different checkbox combinations
    const catValues = catAttributes
      .filter((c) => c.checked)
      .map((c) => c.valueAsConceptId)
      .join('');
    return `param${(paramConceptId || id) + code + catValues}`;
  };

  const displayName = () => {
    return stripHtml(node.name);
  };

  const getParamName = () => {
    const selectionDisplay = [];
    let name = '';
    if (anyVersion) {
      selectionDisplay.push('Any version');
    }
    numAttributes
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
    catAttributes
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
      (hasUnits() && numAttributes[0].operator !== AttrName.ANY
        ? ' ' + PM_UNITS[node.subtype]
        : '') +
      ')'
    );
  };

  const paramWithAttributes = () => {
    const { name, subtype, value } = node;
    let paramName;
    const attrs = [];
    if (!isCOPEOrMinuteSurvey && anyValue) {
      paramName = name + ` (${optionUtil.ANY.display})`;
    } else if (isCOPEOrMinuteSurvey && anyValue && anyVersion) {
      paramName = name + ' (Any version AND any value)';
    } else {
      numAttributes
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
      if (catAttributes.some((at) => at.checked)) {
        const catOperands = catAttributes.reduce((checked, current) => {
          if (current.checked) {
            checked.push(current.valueAsConceptId.toString());
          }
          return checked;
        }, []);
        if (isCOPEOrMinuteSurvey && !anyVersion) {
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
      paramName = getParamName();
    }
    if (isCOPEOrMinuteSurvey && subtype === CriteriaSubType.ANSWER && !!value) {
      attrs.push({
        name: AttrName.CAT,
        operator: Operator.IN,
        operands: [value],
      });
    }
    if (
      subtype === CriteriaSubType.ANSWER &&
      (anyValue ||
        (numAttributes.length && numAttributes[0].operator === 'ANY')) &&
      value === ''
    ) {
      attrs.push({ name: AttrName.ANY });
    }
    return {
      ...node,
      parameterId: paramId(),
      name: paramName,
      attributes: attrs,
    };
  };

  const requestPreview = () => {
    const { id, namespace } = workspace;
    setCalculating(true);
    setAttributeCount(null);
    setCountError(false);
    const param = paramWithAttributes();
    const label = `Calculate - ${domainToTitle(param.domainId)}${
      isPhysicalMeasurement() ? subTypeToTitle(param.subtype) : ''
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
          setCalculating(false);
          setAttributeCount(response);
        },
        () => {
          setCalculating(false);
          setCountError(true);
        }
      );
  };

  const addParameterToSearchItem = () => {
    const param = paramWithAttributes();
    const label = `Add - ${domainToTitle(param.domainId)}${
      isPhysicalMeasurement() ? subTypeToTitle(param.subtype) : ''
    }`;
    AnalyticsTracker.CohortBuilder.AttributesAction(label);
    criteria = criteria.filter(
      (crit) => crit.parameterId !== param.parameterId
    );
    currentCohortCriteriaStore.next([...criteria, param]);
    close();
  };

  const renderNumericalAttributes = () => {
    const { count, subtype } = node;
    return (
      numAttributes.length > 0 && (
        <React.Fragment>
          {(isMeasurement() || isObservation()) && (
            <div style={styles.label}>Numeric Values</div>
          )}
          {isCOPEOrMinuteSurvey && (
            <div>
              <CheckBox onChange={(v) => toggleAnyValueCheckbox(v)} /> Any value
              {count > -1 && (
                <span style={styles.badge}> {count.toLocaleString()}</span>
              )}
            </div>
          )}
          {!(isCOPEOrMinuteSurvey && anyValue) &&
            numAttributes.map((attr, a) => (
              <div key={a}>
                {isBloodPressure() && (
                  <div style={styles.label}>{attr.name}</div>
                )}
                {isCOPEOrMinuteSurvey && <div style={styles.orCircle}>OR</div>}
                <Dropdown
                  id={`numerical-dropdown-${a}`}
                  style={{ marginBottom: '0.5rem', width: '100%' }}
                  value={attr.operator}
                  options={options}
                  placeholder='Select Operator'
                  onChange={(e) => selectChange(a, e.value)}
                  appendTo='self'
                />
                <FlexRowWrap>
                  {![null, 'ANY'].includes(attr.operator) && (
                    <div style={{ width: '33%' }}>
                      <NumberInput
                        id={`numerical-input-${a}-0`}
                        style={{
                          padding: '0 0.25rem',
                          ...(hasUnits() ? { width: '70%' } : {}),
                        }}
                        value={attr.operands[0]}
                        min={attr.MIN}
                        max={attr.MAX}
                        onChange={(v) => inputChange(v, a, 0)}
                      />
                      {hasUnits() && <span> {PM_UNITS[subtype]}</span>}
                    </div>
                  )}
                  {attr.operator === Operator.BETWEEN && (
                    <React.Fragment>
                      <div style={{ padding: '0.2rem 1.5rem 0 1rem' }}>and</div>
                      <div style={{ width: '33%' }}>
                        <NumberInput
                          id={`numerical-input-${a}-1`}
                          style={{
                            padding: '0 0.25rem',
                            ...(hasUnits() ? { width: '70%' } : {}),
                          }}
                          value={attr.operands[1]}
                          min={attr.MIN}
                          max={attr.MAX}
                          onChange={(v) => inputChange(v, a, 1)}
                        />
                        {hasUnits() && <span> {PM_UNITS[subtype]}</span>}
                      </div>
                    </React.Fragment>
                  )}
                </FlexRowWrap>
                {hasRange() && ![null, 'ANY'].includes(attr.operator) && (
                  <div style={{ paddingTop: '0.2rem' }}>
                    Range: {attr.MIN.toLocaleString()} -{' '}
                    {attr.MAX.toLocaleString()}
                  </div>
                )}
              </div>
            ))}
        </React.Fragment>
      )
    );
  };

  const renderCategoricalAttributes = () => {
    return (
      catAttributes.length > 0 && (
        <React.Fragment>
          {isCOPEOrMinuteSurvey && (
            <div>
              <CheckBox onChange={(v) => toggleAnyVersionCheckbox(v)} /> Any
              version
              {node.count > -1 && (
                <span style={styles.badge}>{node.count.toLocaleString()}</span>
              )}
            </div>
          )}
          {!(isCOPEOrMinuteSurvey && anyVersion) && (
            <React.Fragment>
              {(numAttributes.length > 0 || isObservation()) && (
                <div style={styles.orCircle}>OR</div>
              )}
              {!isCOPEOrMinuteSurvey && (
                <div style={styles.label}>Categorical Values</div>
              )}
              {catAttributes.map((attr, a) => (
                <div key={a} style={styles.categorical}>
                  <CheckBox
                    checked={attr.checked}
                    style={{ marginRight: '3px' }}
                    onChange={(v) => checkboxChange(v, a)}
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
  };

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
            The COPE survey is longitudinal and will change over time. Use the
            following attributes to select data from one or more versions.
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
          {isPhysicalMeasurement()
            ? node.name
            : node.domainId.toString().toLowerCase()}{' '}
          Detail
        </h3>
      )}
      {isSurvey() && (
        <div style={{ ...styles.label, marginBottom: '0.75rem' }}>
          {node.subtype === CriteriaSubType.ANSWER
            ? `${ppiQuestions.getValue()[node.parentId].name} - ${node.name}`
            : node.name}
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
          {renderCategoricalAttributes()}
          {numAttributes.length > 0 && catAttributes.length > 0 && (
            <div style={{ position: 'relative' }}>
              <div style={styles.andCircle}>AND</div>
              <div style={styles.andDivider} />
            </div>
          )}
          {renderNumericalAttributes()}
        </div>
      ) : (
        <div>
          {(isMeasurement() || isObservation()) && (
            <div>
              <div style={styles.label}>{displayName()}</div>
              <CheckBox onChange={(v) => toggleAnyValueCheckbox(v)} /> Any value{' '}
              {isMeasurement() && <span> (lab exists)</span>}
              {!anyValue && numAttributes.length > 0 && (
                <div style={styles.orCircle}>OR</div>
              )}
            </div>
          )}
          {!anyValue && (
            <div style={{ minHeight: '15rem' }}>
              {renderNumericalAttributes()}
              {renderCategoricalAttributes()}
            </div>
          )}
        </div>
      )}
      <CalculateFooter
        addButtonText='ADD THIS'
        addFn={() => addParameterToSearchItem()}
        backFn={() => back()}
        calculateFn={() => requestPreview()}
        calculating={calculating}
        count={attributeCount}
        disableAdd={disableAddButton()}
        disableCalculate={disableCalculateButton()}
      />
    </div>
  );
});
