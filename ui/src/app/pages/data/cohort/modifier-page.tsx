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

const mockVisits: Criteria[] = [
  {
    id: 6000000001,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Ambulatory Clinic / Center',
    count: 226,
    parentCount: 0,
    childCount: 226,
    group: false,
    selectable: true,
    conceptId: 38004207,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
  {
    id: 6000000002,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Ambulatory Dental Clinic / Center',
    count: 1,
    parentCount: 0,
    childCount: 1,
    group: false,
    selectable: true,
    conceptId: 38004218,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
  {
    id: 6000000003,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Ambulatory Endoscopy Cinic / Center',
    count: 1220,
    parentCount: 0,
    childCount: 1220,
    group: false,
    selectable: true,
    conceptId: 38004222,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
  {
    id: 6000000004,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Ambulatory Hearing and Speech Clinic / Center',
    count: 1,
    parentCount: 0,
    childCount: 1,
    group: false,
    selectable: true,
    conceptId: 38004227,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
  {
    id: 6000000005,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Ambulatory Infusion Therapy Clinic / Center',
    count: 987,
    parentCount: 0,
    childCount: 987,
    group: false,
    selectable: true,
    conceptId: 38004228,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
  {
    id: 6000000006,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Ambulatory Magnetic Resonance Imaging (MRI) Clinic / Center',
    count: 1630,
    parentCount: 0,
    childCount: 1630,
    group: false,
    selectable: true,
    conceptId: 38004238,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
  {
    id: 6000000007,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Ambulatory Mammography Clinic / Center',
    count: 2032,
    parentCount: 0,
    childCount: 2032,
    group: false,
    selectable: true,
    conceptId: 38004251,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
  {
    id: 6000000008,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Ambulatory Occupational Medicine Clinic / Center',
    count: 413,
    parentCount: 0,
    childCount: 413,
    group: false,
    selectable: true,
    conceptId: 38004267,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
  {
    id: 6000000009,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Ambulatory Oncological Radiation Clinic / Center',
    count: 154,
    parentCount: 0,
    childCount: 154,
    group: false,
    selectable: true,
    conceptId: 38004269,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
  {
    id: 6000000010,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Ambulatory Oncology Clinic / Center',
    count: 887,
    parentCount: 0,
    childCount: 887,
    group: false,
    selectable: true,
    conceptId: 38004268,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
  {
    id: 6000000011,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Ambulatory Ophthalmologic Surgery Clinic / Center',
    count: 134,
    parentCount: 0,
    childCount: 134,
    group: false,
    selectable: true,
    conceptId: 38004262,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
  {
    id: 6000000012,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Ambulatory Pain Clinic / Center',
    count: 26,
    parentCount: 0,
    childCount: 26,
    group: false,
    selectable: true,
    conceptId: 38004249,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
  {
    id: 6000000013,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Ambulatory Physical Therapy Clinic / Center',
    count: 324,
    parentCount: 0,
    childCount: 324,
    group: false,
    selectable: true,
    conceptId: 38004246,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
  {
    id: 6000000014,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Ambulatory Podiatric Clinic / Center',
    count: 28,
    parentCount: 0,
    childCount: 28,
    group: false,
    selectable: true,
    conceptId: 38004245,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
  {
    id: 6000000015,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Ambulatory Radiology Clinic / Center',
    count: 14031,
    parentCount: 0,
    childCount: 14031,
    group: false,
    selectable: true,
    conceptId: 38004250,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
  {
    id: 6000000016,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Ambulatory Recovery Care Clinic / Center',
    count: 1,
    parentCount: 0,
    childCount: 1,
    group: false,
    selectable: true,
    conceptId: 38004258,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
  {
    id: 6000000017,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Ambulatory Rehabilitation Visit',
    count: 17011,
    parentCount: 0,
    childCount: 17011,
    group: false,
    selectable: true,
    conceptId: 581479,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
  {
    id: 6000000018,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Ambulatory Research Clinic / Center',
    count: 575,
    parentCount: 0,
    childCount: 575,
    group: false,
    selectable: true,
    conceptId: 38004259,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
  {
    id: 6000000019,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Ambulatory Sleep Disorder Diagnostic Clinic / Center',
    count: 12,
    parentCount: 0,
    childCount: 12,
    group: false,
    selectable: true,
    conceptId: 38004264,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
  {
    id: 6000000020,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Ambulatory Surgical Center',
    count: 6752,
    parentCount: 0,
    childCount: 6752,
    group: false,
    selectable: true,
    conceptId: 8883,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
  {
    id: 6000000021,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Behavioral Disturbances Assisted Living Facility',
    count: 191,
    parentCount: 0,
    childCount: 191,
    group: false,
    selectable: true,
    conceptId: 38004303,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
  {
    id: 6000000022,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Case Management Visit',
    count: 1669,
    parentCount: 0,
    childCount: 1669,
    group: false,
    selectable: true,
    conceptId: 38004193,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
  {
    id: 6000000023,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Comprehensive Outpatient Rehabilitation Facility',
    count: 15,
    parentCount: 0,
    childCount: 15,
    group: false,
    selectable: true,
    conceptId: 8947,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
  {
    id: 6000000024,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Emergency Room - Hospital',
    count: 4064,
    parentCount: 0,
    childCount: 4064,
    group: false,
    selectable: true,
    conceptId: 8870,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
  {
    id: 6000000025,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Emergency Room Visit',
    count: 120088,
    parentCount: 0,
    childCount: 120088,
    group: false,
    selectable: true,
    conceptId: 9203,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
  {
    id: 6000000026,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Emergency Room and Inpatient Visit',
    count: 25660,
    parentCount: 0,
    childCount: 25660,
    group: false,
    selectable: true,
    conceptId: 262,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
  {
    id: 6000000027,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Health examination',
    count: 569,
    parentCount: 0,
    childCount: 569,
    group: false,
    selectable: true,
    conceptId: 32693,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
  {
    id: 6000000028,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Home Health Agency',
    count: 197,
    parentCount: 0,
    childCount: 197,
    group: false,
    selectable: true,
    conceptId: 38004519,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
  {
    id: 6000000029,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Home Visit',
    count: 14912,
    parentCount: 0,
    childCount: 14912,
    group: false,
    selectable: true,
    conceptId: 581476,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
  {
    id: 6000000030,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Hospice',
    count: 77,
    parentCount: 0,
    childCount: 77,
    group: false,
    selectable: true,
    conceptId: 8546,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
  {
    id: 6000000031,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Hospital',
    count: 1827,
    parentCount: 0,
    childCount: 1827,
    group: false,
    selectable: true,
    conceptId: 38004515,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
  {
    id: 6000000032,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Inpatient Hospital',
    count: 4181,
    parentCount: 0,
    childCount: 4181,
    group: false,
    selectable: true,
    conceptId: 8717,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
  {
    id: 6000000033,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Inpatient Visit',
    count: 103001,
    parentCount: 0,
    childCount: 103001,
    group: false,
    selectable: true,
    conceptId: 9201,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
  {
    id: 6000000034,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Intensive Care',
    count: 44,
    parentCount: 0,
    childCount: 44,
    group: false,
    selectable: true,
    conceptId: 32037,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
  {
    id: 6000000035,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Laboratory Visit',
    count: 63612,
    parentCount: 0,
    childCount: 63612,
    group: false,
    selectable: true,
    conceptId: 32036,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
  {
    id: 6000000036,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Mammography Center',
    count: 2,
    parentCount: 0,
    childCount: 2,
    group: false,
    selectable: true,
    conceptId: 38004677,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
  {
    id: 6000000037,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Mass Immunization Center',
    count: 4083,
    parentCount: 0,
    childCount: 4083,
    group: false,
    selectable: true,
    conceptId: 8858,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
  {
    id: 6000000038,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Non-hospital institution Visit',
    count: 6994,
    parentCount: 0,
    childCount: 6994,
    group: false,
    selectable: true,
    conceptId: 42898160,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
  {
    id: 6000000039,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Nursing Facility',
    count: 89,
    parentCount: 0,
    childCount: 89,
    group: false,
    selectable: true,
    conceptId: 8676,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
  {
    id: 6000000040,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Observation Room',
    count: 3641,
    parentCount: 0,
    childCount: 3641,
    group: false,
    selectable: true,
    conceptId: 581385,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
  {
    id: 6000000041,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Office Visit',
    count: 108905,
    parentCount: 0,
    childCount: 108905,
    group: false,
    selectable: true,
    conceptId: 581477,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
  {
    id: 6000000042,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Outpatient Hospital',
    count: 13067,
    parentCount: 0,
    childCount: 13067,
    group: false,
    selectable: true,
    conceptId: 8756,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
  {
    id: 6000000043,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Outpatient Visit',
    count: 341056,
    parentCount: 0,
    childCount: 341056,
    group: false,
    selectable: true,
    conceptId: 9202,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
  {
    id: 6000000044,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Pharmacy visit',
    count: 7918,
    parentCount: 0,
    childCount: 7918,
    group: false,
    selectable: true,
    conceptId: 581458,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
  {
    id: 6000000045,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Place of Employment-Worksite',
    count: 62,
    parentCount: 0,
    childCount: 62,
    group: false,
    selectable: true,
    conceptId: 581475,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
  {
    id: 6000000046,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Radiation Therapy Center',
    count: 59,
    parentCount: 0,
    childCount: 59,
    group: false,
    selectable: true,
    conceptId: 38004696,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
  {
    id: 6000000047,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Rehabilitation Hospital',
    count: 92,
    parentCount: 0,
    childCount: 92,
    group: false,
    selectable: true,
    conceptId: 38004285,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
  {
    id: 6000000048,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Telehealth',
    count: 46070,
    parentCount: 0,
    childCount: 46070,
    group: false,
    selectable: true,
    conceptId: 5083,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
  {
    id: 6000000049,
    parentId: -1,
    type: 'VISIT',
    subtype: '',
    code: '',
    name: 'Urgent Care Facility',
    count: 744,
    parentCount: 0,
    childCount: 744,
    group: false,
    selectable: true,
    conceptId: 8782,
    domainId: 'VISIT',
    hasAttributes: false,
    path: '',
    value: '',
    hasHierarchy: false,
    hasAncestorData: false,
    standard: true,
  },
];

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
    height: '1rem',
    lineHeight: '1rem',
    background: '#0079b8',
    color: '#ffffff',
    fontSize: '10px',
    padding: '0 0.25rem',
    borderRadius: '10px',
    marginTop: '0.25rem',
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
    name: ModifierType.AGE_AT_EVENT as ModifierType,
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
        value: Operator.GREATER_THAN_OR_EQUAL_TO,
      },
      {
        label: 'Less Than or Equal To',
        value: Operator.LESS_THAN_OR_EQUAL_TO,
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
        Domain.PHYSICAL_MEASUREMENT,
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
            operator: (
              [
                ModifierType.CATI,
                ModifierType.ENCOUNTERS,
              ] as Array<ModifierType>
            ).includes(mod.name)
              ? +existing.operands[0]
              : existing.operator,
            values:
              (mod.name as ModifierType) === ModifierType.EVENT_DATE
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
          name: ModifierType.NUM_OF_OCCURRENCES,
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
              value: Operator.GREATER_THAN_OR_EQUAL_TO,
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
          name: ModifierType.EVENT_DATE,
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
              value: Operator.LESS_THAN_OR_EQUAL_TO,
            },
            {
              label: 'Is On or After',
              value: Operator.GREATER_THAN_OR_EQUAL_TO,
            },
            {
              label: 'Is Between',
              value: Operator.BETWEEN,
            },
          ],
        });
      }
      if (addEncounters()) {
        let encountersOptions = mockVisits;
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
        if (
          !(
            [ModifierType.CATI, ModifierType.ENCOUNTERS] as Array<ModifierType>
          ).includes(item.name)
        ) {
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
      if (
        (
          [ModifierType.CATI, ModifierType.ENCOUNTERS] as Array<ModifierType>
        ).includes(name)
      ) {
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
            case ModifierType.EVENT_DATE:
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
        <div className='p-clearfix' style={{ display: 'flex', width: '100%' }}>
          <div
            style={{
              whiteSpace: 'nowrap',
              overflow: 'hidden',
              textOverflow: 'ellipsis',
            }}
            title={opt.label}
          >
            {opt.label}
          </div>
          &nbsp;
          <div style={styles.count}>
            {visitCounts[opt.value].toLocaleString()}
          </div>
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
                  {name === ModifierType.EVENT_DATE && (
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
                      panelStyle={
                        name === ModifierType.ENCOUNTERS && { width: '400px' }
                      }
                      itemTemplate={(e) => optionTemplate(e, name)}
                    />
                    {operator &&
                      !(
                        [
                          ModifierType.CATI,
                          ModifierType.ENCOUNTERS,
                        ] as Array<ModifierType>
                      ).includes(name) && (
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
