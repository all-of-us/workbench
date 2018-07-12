import {Injectable} from '@angular/core';
import {Map} from 'immutable';
import {Epic} from 'redux-observable';
import {Observable} from 'rxjs/Observable';

/* tslint:disable:ordered-imports */
import {
  BEGIN_CRITERIA_REQUEST,
  BEGIN_ALL_CRITERIA_REQUEST,
  CANCEL_CRITERIA_REQUEST,

  BEGIN_COUNT_REQUEST,
  CANCEL_COUNT_REQUEST,

  BEGIN_CHARTS_REQUEST,
  CANCEL_CHARTS_REQUEST,

  BEGIN_PREVIEW_REQUEST,
  BEGIN_ATTR_PREVIEW_REQUEST,

  RootAction,
  ActionTypes,
} from './actions/types';

import {
  loadCriteriaRequestResults,
  criteriaRequestError,

  loadCountRequestResults,
  countRequestError,

  loadChartsRequestResults,
  chartsRequestError,

  loadPreviewRequestResults,
  loadAttributePreviewRequestResults,
  previewRequestError,
} from './actions/creators';

import {CohortSearchState} from './store';
/* tslint:enable:ordered-imports */

import {CohortBuilderService} from 'generated';

type CSEpic = Epic<RootAction, CohortSearchState>;
type CritRequestAction = ActionTypes[typeof BEGIN_CRITERIA_REQUEST];
type CountRequestAction = ActionTypes[typeof BEGIN_COUNT_REQUEST];
type ChartRequestAction = ActionTypes[typeof BEGIN_CHARTS_REQUEST];
type PreviewRequestAction = ActionTypes[typeof BEGIN_ATTR_PREVIEW_REQUEST];
type AttributePreviewRequestAction = ActionTypes[typeof BEGIN_PREVIEW_REQUEST];
const compare = (obj) => (action) => Map(obj).isSubset(Map(action));

/**
 * CohortSearchEpics
 *
 * Exposes functions (called `epics` by redux-observable) that listen in on the
 * stream of dispatched actions (exposed as an Observable) and attach handlers
 * to certain of them; this allows us to dispatch actions asynchronously.  This is
 * the interface between the application state and the backend API.
 *
 * TODO: clean up these funcs using the new lettable operators
 */
@Injectable()
export class CohortSearchEpics {
  testResults = [
    {
      conceptId: 9203,
      name: 'Emergency Room Visit',
      code: '',
      count: null,
      hasAttributes: false,
      selectable: true,
      subtype: '',
      type: 'VISIT',
      id: 316400,
      parentId: 0,
      predefinedAttributes: null,
      domainId: 'Visit',
      group: true
    },
    {
      conceptId: 8870,
      name: 'Emergency Room - Hospital',
      code: '',
      count: null,
      hasAttributes: false,
      selectable: true,
      subtype: '',
      type: 'VISIT',
      id: 316425,
      parentId: 316400,
      predefinedAttributes: null,
      domainId: 'Visit',
      group: false
    },
    {
      conceptId: 581381,
      name: 'Emergency Room Critical Care Facility',
      code: '',
      count: null,
      hasAttributes: false,
      selectable: true,
      subtype: '',
      type: 'VISIT',
      id: 316427,
      parentId: 316400,
      predefinedAttributes: null,
      domainId: 'Visit',
      group: false
    },
    {
      conceptId: 262,
      name: 'Emergency Room and Inpatient Visit',
      code: '',
      count: null,
      hasAttributes: false,
      selectable: true,
      subtype: '',
      type: 'VISIT',
      id: 316401,
      parentId: 0,
      predefinedAttributes: null,
      domainId: 'Visit',
      group: true
    },
    {
      conceptId: 9201,
      name: 'Inpatient Visit',
      code: '',
      count: null,
      hasAttributes: false,
      selectable: true,
      subtype: '',
      type: 'VISIT',
      id: 316402,
      parentId: 0,
      predefinedAttributes: null,
      domainId: 'Visit',
      group: true
    },
    {
      conceptId: 8920,
      name: 'Comprehensive Inpatient Rehabilitation Facility',
      code: '',
      count: null,
      hasAttributes: false,
      selectable: true,
      subtype: '',
      type: 'VISIT',
      id: 316420,
      parentId: 316402,
      predefinedAttributes: null,
      domainId: 'Visit',
      group: false
    },
    {
      conceptId: 581383,
      name: 'Inpatient Cardiac Care Facility',
      code: '',
      count: null,
      hasAttributes: false,
      selectable: true,
      subtype: '',
      type: 'VISIT',
      id: 316439,
      parentId: 316402,
      predefinedAttributes: null,
      domainId: 'Visit',
      group: false
    },
    {
      conceptId: 581379,
      name: 'Inpatient Critical Care Facility',
      code: '',
      count: null,
      hasAttributes: false,
      selectable: true,
      subtype: '',
      type: 'VISIT',
      id: 316441,
      parentId: 316402,
      predefinedAttributes: null,
      domainId: 'Visit',
      group: false
    },
    {
      conceptId: 8717,
      name: 'Inpatient Hospital',
      code: '',
      count: null,
      hasAttributes: false,
      selectable: true,
      subtype: '',
      type: 'VISIT',
      id: 316443,
      parentId: 316402,
      predefinedAttributes: null,
      domainId: 'Visit',
      group: false
    },
    {
      conceptId: 581382,
      name: 'Inpatient Intensive Care Facility',
      code: '',
      count: null,
      hasAttributes: false,
      selectable: true,
      subtype: '',
      type: 'VISIT',
      id: 316444,
      parentId: 316402,
      predefinedAttributes: null,
      domainId: 'Visit',
      group: false
    },
    {
      conceptId: 8970,
      name: 'Inpatient Long-term Care',
      code: '',
      count: null,
      hasAttributes: false,
      selectable: true,
      subtype: '',
      type: 'VISIT',
      id: 316446,
      parentId: 316402,
      predefinedAttributes: null,
      domainId: 'Visit',
      group: false
    },
    {
      conceptId: 581384,
      name: 'Inpatient Nursery',
      code: '',
      count: null,
      hasAttributes: false,
      selectable: true,
      subtype: '',
      type: 'VISIT',
      id: 316448,
      parentId: 316402,
      predefinedAttributes: null,
      domainId: 'Visit',
      group: false
    },
    {
      conceptId: 8971,
      name: 'Inpatient Psychiatric Facility',
      code: '',
      count: null,
      hasAttributes: false,
      selectable: true,
      subtype: '',
      type: 'VISIT',
      id: 316449,
      parentId: 316402,
      predefinedAttributes: null,
      domainId: 'Visit',
      group: false
    },
    {
      conceptId: 8892,
      name: 'Other Inpatient Care',
      code: '',
      count: null,
      hasAttributes: false,
      selectable: true,
      subtype: '',
      type: 'VISIT',
      id: 316461,
      parentId: 316402,
      predefinedAttributes: null,
      domainId: 'Visit',
      group: false
    },
    {
      conceptId: 8863,
      name: 'Skilled Nursing Facility',
      code: '',
      count: null,
      hasAttributes: false,
      selectable: true,
      subtype: '',
      type: 'VISIT',
      id: 316475,
      parentId: 316402,
      predefinedAttributes: null,
      domainId: 'Visit',
      group: false
    },
    {
      conceptId: 32037,
      name: 'Intensive Care',
      code: '',
      count: null,
      hasAttributes: false,
      selectable: true,
      subtype: '',
      type: 'VISIT',
      id: 316403,
      parentId: 0,
      predefinedAttributes: null,
      domainId: 'Visit',
      group: true
    },
    {
      conceptId: 581381,
      name: 'Emergency Room Critical Care Facility',
      code: '',
      count: null,
      hasAttributes: false,
      selectable: true,
      subtype: '',
      type: 'VISIT',
      id: 316426,
      parentId: 316403,
      predefinedAttributes: null,
      domainId: 'Visit',
      group: false
    },
    {
      conceptId: 581383,
      name: 'Inpatient Cardiac Care Facility',
      code: '',
      count: null,
      hasAttributes: false,
      selectable: true,
      subtype: '',
      type: 'VISIT',
      id: 316440,
      parentId: 316403,
      predefinedAttributes: null,
      domainId: 'Visit',
      group: false
    },
    {
      conceptId: 581379,
      name: 'Inpatient Critical Care Facility',
      code: '',
      count: null,
      hasAttributes: false,
      selectable: true,
      subtype: '',
      type: 'VISIT',
      id: 316442,
      parentId: 316403,
      predefinedAttributes: null,
      domainId: 'Visit',
      group: false
    },
    {
      conceptId: 581382,
      name: 'Inpatient Intensive Care Facility',
      code: '',
      count: null,
      hasAttributes: false,
      selectable: true,
      subtype: '',
      type: 'VISIT',
      id: 316445,
      parentId: 316403,
      predefinedAttributes: null,
      domainId: 'Visit',
      group: false
    },
    {
      conceptId: 581380,
      name: 'Outpatient Critical Care Facility',
      code: '',
      count: null,
      hasAttributes: false,
      selectable: true,
      subtype: '',
      type: 'VISIT',
      id: 316462,
      parentId: 316403,
      predefinedAttributes: null,
      domainId: 'Visit',
      group: false
    },
    {
      conceptId: 42898160,
      name: 'Long Term Care Visit',
      code: '',
      count: null,
      hasAttributes: false,
      selectable: true,
      subtype: '',
      type: 'VISIT',
      id: 316404,
      parentId: 0,
      predefinedAttributes: null,
      domainId: 'Visit',
      group: true
    },
    {
      conceptId: 8970,
      name: 'Inpatient Long-term Care',
      code: '',
      count: null,
      hasAttributes: false,
      selectable: true,
      subtype: '',
      type: 'VISIT',
      id: 316447,
      parentId: 316404,
      predefinedAttributes: null,
      domainId: 'Visit',
      group: false
    },
    {
      conceptId: 9202,
      name: 'Outpatient Visit',
      code: '',
      count: null,
      hasAttributes: false,
      selectable: true,
      subtype: '',
      type: 'VISIT',
      id: 316405,
      parentId: 0,
      predefinedAttributes: null,
      domainId: 'Visit',
      group: true
    },
    {
      conceptId: 581478,
      name: 'Ambulance Visit',
      code: '',
      count: null,
      hasAttributes: false,
      selectable: true,
      subtype: '',
      type: 'VISIT',
      id: 316408,
      parentId: 316405,
      predefinedAttributes: null,
      domainId: 'Visit',
      group: true
    },
    {
      conceptId: 8850,
      name: 'Ambulance - Air or Water',
      code: '',
      count: null,
      hasAttributes: false,
      selectable: true,
      subtype: '',
      type: 'VISIT',
      id: 316413,
      parentId: 316408,
      predefinedAttributes: null,
      domainId: 'Visit',
      group: false
    },
    {
      conceptId: 8668,
      name: 'Ambulance - Land',
      code: '',
      count: null,
      hasAttributes: false,
      selectable: true,
      subtype: '',
      type: 'VISIT',
      id: 316414,
      parentId: 316408,
      predefinedAttributes: null,
      domainId: 'Visit',
      group: false
    },
    {
      conceptId: 581476,
      name: 'Home Visit',
      code: '',
      count: null,
      hasAttributes: false,
      selectable: true,
      subtype: '',
      type: 'VISIT',
      id: 316409,
      parentId: 316405,
      predefinedAttributes: null,
      domainId: 'Visit',
      group: true
    },
    {
      conceptId: 32036,
      name: 'Laboratory Visit',
      code: '',
      count: null,
      hasAttributes: false,
      selectable: true,
      subtype: '',
      type: 'VISIT',
      id: 316410,
      parentId: 316405,
      predefinedAttributes: null,
      domainId: 'Visit',
      group: true
    },
    {
      conceptId: 581477,
      name: 'Office Visit',
      code: '',
      count: null,
      hasAttributes: false,
      selectable: true,
      subtype: '',
      type: 'VISIT',
      id: 316411,
      parentId: 316405,
      predefinedAttributes: null,
      domainId: 'Visit',
      group: true
    },
    {
      conceptId: 581458,
      name: 'Pharmacy visit',
      code: '',
      count: null,
      hasAttributes: false,
      selectable: true,
      subtype: '',
      type: 'VISIT',
      id: 316406,
      parentId: 0,
      predefinedAttributes: null,
      domainId: 'Visit',
      group: true
    },
    {
      conceptId: 581479,
      name: 'Rehabilitation Visit',
      code: '',
      count: null,
      hasAttributes: false,
      selectable: true,
      subtype: '',
      type: 'VISIT',
      id: 316407,
      parentId: 0,
      predefinedAttributes: null,
      domainId: 'Visit',
      group: true
    }
  ];
  constructor(private service: CohortBuilderService) {}

  fetchCriteria: CSEpic = (action$) => (
    action$.ofType(BEGIN_CRITERIA_REQUEST).mergeMap(
      ({cdrVersionId, kind, parentId}: CritRequestAction) => {
        const _type = kind.match(/^DEMO.*/i) ? 'DEMO' : kind;
        return this.service.getCriteriaByTypeAndParentId(cdrVersionId, _type, parentId)
          .map(result => loadCriteriaRequestResults(kind, parentId, result.items))
          .race(action$
            .ofType(CANCEL_CRITERIA_REQUEST)
            .filter(compare({kind, parentId}))
            .first())
          .catch(e => Observable.of(criteriaRequestError(kind, parentId, e)));
      }
    )
  )

  fetchAllCriteria: CSEpic = (action$) => (
    action$.ofType(BEGIN_ALL_CRITERIA_REQUEST).mergeMap(
      ({cdrVersionId, kind, parentId}: CritRequestAction) => {
        const _type = kind.match(/^DEMO.*/i) ? 'DEMO' : kind;
        return this.service.getCriteriaByTypeAndParentId(cdrVersionId, _type, parentId)
          .map(result => loadCriteriaRequestResults(kind, parentId, this.testResults))
          .race(action$
            .ofType(CANCEL_CRITERIA_REQUEST)
            .filter(compare({kind, parentId}))
            .first())
          .catch(e => Observable.of(criteriaRequestError(kind, parentId, e)));
      }
    )
  )

  fetchCount: CSEpic = (action$) => (
    action$.ofType(BEGIN_COUNT_REQUEST).mergeMap(
      ({cdrVersionId, entityType, entityId, request}: CountRequestAction) =>
      this.service.countParticipants(cdrVersionId, request)
        .map(response => typeof response === 'number' ? response : 0)
        .map(count => loadCountRequestResults(entityType, entityId, count))
        .race(action$
          .ofType(CANCEL_COUNT_REQUEST)
          .filter(compare({entityType, entityId}))
          .first())
        .catch(e => Observable.of(countRequestError(entityType, entityId, e)))
    )
  )

  previewCount: CSEpic = (action$) => (
    action$.ofType(BEGIN_PREVIEW_REQUEST).switchMap(
      ({cdrVersionId, request}: PreviewRequestAction) =>
      this.service.countParticipants(cdrVersionId, request)
        .map(response => typeof response === 'number' ? response : 0)
        .map(count => loadPreviewRequestResults(count))
        .catch(e => Observable.of(previewRequestError(e)))
    )
  )

  attributePreviewCount: CSEpic = (action$) => (
    action$.ofType(BEGIN_ATTR_PREVIEW_REQUEST).switchMap(
      ({cdrVersionId, request}: AttributePreviewRequestAction) =>
      this.service.countParticipants(cdrVersionId, request)
        .map(response => typeof response === 'number' ? response : 0)
        .map(count => loadAttributePreviewRequestResults(count))
        .catch(e => Observable.of(previewRequestError(e)))
    )
  )

  fetchChartData: CSEpic = (action$) => (
    action$.ofType(BEGIN_CHARTS_REQUEST).mergeMap(
      ({cdrVersionId, entityType, entityId, request}: ChartRequestAction) =>
      this.service.getChartInfo(cdrVersionId, request)
        .map(result => loadChartsRequestResults(entityType, entityId, result.items))
        .race(action$
          .ofType(CANCEL_CHARTS_REQUEST)
          .filter(compare({entityType, entityId}))
          .first())
        .catch(e => Observable.of(chartsRequestError(entityType, entityId, e)))
    )
  )
}
