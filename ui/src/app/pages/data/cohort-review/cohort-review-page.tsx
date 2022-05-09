import * as fp from 'lodash/fp';
import * as React from 'react';
import { useParams } from 'react-router';

const { useEffect, useState } = React;

import { Button } from 'app/components/buttons';
import { FadeBox } from 'app/components/containers';
import { ClrIcon } from 'app/components/icons';
import { SpinnerOverlay } from 'app/components/spinners';
import { withSpinnerOverlay } from 'app/components/with-spinner-overlay';
import { CohortReviewListItem } from 'app/pages/data/cohort-review/cohort-review-list-item';
import { visitsFilterOptions } from 'app/services/review-state.service';
import { datatableStyles } from 'app/styles/datatable';
import {
  cohortBuilderApi,
  cohortReviewApi,
  cohortsApi,
} from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { useNavigation } from 'app/utils/navigation';
import { MatchParams } from 'app/utils/stores';
import { reactStyles, withCurrentWorkspace } from 'app/utils';
import {
  CriteriaType,
  Domain,
  FilterColumns as Columns,
  PageFilterRequest as Request,
  SortOrder,
} from 'generated/fetch';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';

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
    overflow: 'auto',
  },
  description: {
    margin: '0 0 0.25rem',
    color: '#000000',
  },
  columnHeader: {
    background: '#f4f4f4',
    color: colors.primary,
    fontWeight: 600,
  },
  columnBody: {
    background: '#ffffff',
    padding: '0.5rem 0.5rem 0.3rem 0.75rem',
    verticalAlign: 'top',
    textAlign: 'left',
    borderLeft: 0,
    borderRight: 0,
    borderBottom: 'none',
    lineHeight: '0.6rem',
    cursor: 'pointer',
  },
  sortIcon: {
    marginTop: '4px',
    color: '#2691D0',
    fontSize: '0.5rem',
    float: 'right',
  },
  tableBody: {
    textAlign: 'left',
    lineHeight: '0.75rem',
  },
});
const reverseColumnEnum = {
  participantId: Columns.PARTICIPANTID,
  sexAtBirth: Columns.SEXATBIRTH,
  gender: Columns.GENDER,
  race: Columns.RACE,
  ethnicity: Columns.ETHNICITY,
  birthDate: Columns.BIRTHDATE,
  deceased: Columns.DECEASED,
  status: Columns.STATUS,
};
const fields = [
  { field: 'participantId', name: 'Participant ID' },
  { field: 'birthDate', name: 'Date of Birth' },
  { field: 'deceased', name: 'Deceased' },
  { field: 'sexAtBirth', name: 'Sex at Birth' },
  { field: 'gender', name: 'Gender' },
  { field: 'race', name: 'Race' },
  { field: 'ethnicity', name: 'Ethnicity' },
  { field: 'status', name: 'Status' },
];
const rows = 25;

const mockReviews = [
  {
    cohortReviewId: 11898,
    cohortId: 83520,
    cdrVersionId: 3,
    etag: '"2"',
    creationTime: 1645126803000,
    lastModifiedTime: 1645126803000,
    cohortDefinition:
      '{"includes":[{"id":"includes_dfi3vfsss","items":[{"id":"items_ff6utxbmb","type":"CONDITION","searchParameters":[{"parameterId":"param427402564572001true","name":"Disease","domain":"CONDITION","type":"SNOMED","group":true,"attributes":[],"ancestorData":false,"standard":true,"conceptId":4274025},{"parameterId":"param4047779123946008true","name":"Disorder by body site","domain":"CONDITION","type":"SNOMED","group":true,"attributes":[],"ancestorData":false,"standard":true,"conceptId":4047779},{"parameterId":"param35207668I10false","name":"Essential (primary) hypertension","domain":"CONDITION","type":"ICD10CM","group":false,"attributes":[],"ancestorData":false,"standard":false,"conceptId":35207668},{"parameterId":"param1567956E11false","name":"Type 2 diabetes mellitus","domain":"CONDITION","type":"ICD10CM","group":true,"attributes":[],"ancestorData":false,"standard":false,"conceptId":1567956},{"parameterId":"param44833556401false","name":"Essential hypertension","domain":"CONDITION","type":"ICD9CM","group":true,"attributes":[],"ancestorData":false,"standard":false,"conceptId":44833556},{"parameterId":"param44821949401.9false","name":"Unspecified essential hypertension","domain":"CONDITION","type":"ICD9CM","group":false,"attributes":[],"ancestorData":false,"standard":false,"conceptId":44821949}],"modifiers":[{"name":"AGE_AT_EVENT","operator":"GREATER_THAN_OR_EQUAL_TO","operands":[22]},{"name":"NUM_OF_OCCURRENCES","operator":"GREATER_THAN_OR_EQUAL_TO","operands":[2]},{"name":"ENCOUNTERS","operator":"IN","operands":["9202"]}]},{"id":"items_qz0kq7af7","type":"MEASUREMENT","searchParameters":[{"parameterId":"param301372102415514240641582","name":"Aspartate aminotransferase [Enzymatic activity/volume] in Serum or Plasma (>= 1,000, Abnormally high, Abnormal)","domain":"MEASUREMENT","type":"LOINC","group":false,"attributes":[{"name":"NUM","operator":"GREATER_THAN_OR_EQUAL_TO","operands":[1000]},{"name":"CAT","operator":"IN","operands":["4155142","40641582"]}],"ancestorData":false,"standard":true,"conceptId":3013721}],"modifiers":[]}],"temporal":false},{"id":"includes_0zq5t7mrc","items":[{"id":"items_8ydps29tt","type":"PERSON","searchParameters":[{"parameterId":"AGE-18-52","name":"Current Age In Range 18 - 52","domain":"PERSON","type":"AGE","group":false,"attributes":[{"name":"AGE","operator":"BETWEEN","operands":["18","52"]}],"ancestorData":false,"standard":true}],"modifiers":[]},{"id":"items_x32tljzzl","type":"DEVICE","searchParameters":[{"parameterId":"param2720870Q9967true","name":"Low osmolar contrast material, 300-399 mg/ml iodine concentration, per ml","domain":"DEVICE","type":"HCPCS","group":false,"attributes":[],"ancestorData":false,"standard":true,"conceptId":2720870},{"parameterId":"param45768258706294009true","name":"Basic diagnostic x-ray system","domain":"DEVICE","type":"SNOMED","group":false,"attributes":[],"ancestorData":false,"standard":true,"conceptId":45768258}],"modifiers":[]}],"temporal":false},{"id":"includes_l7tob4esx","items":[{"id":"items_la243uzn9","type":"VISIT","searchParameters":[{"parameterId":"param9202true","name":"Outpatient Visit","domain":"VISIT","type":"VISIT","group":false,"attributes":[],"ancestorData":false,"standard":true,"conceptId":9202}],"modifiers":[]},{"id":"items_aiy484ka5","type":"ARRAY_DATA","searchParameters":[{"parameterId":"","name":"Global Diversity Array","domain":"ARRAY_DATA","type":"","group":false,"attributes":[]}],"modifiers":[]}],"temporal":false},{"id":"includes_533cdx843","items":[{"id":"items_8eqffnj2h","type":"SURVEY","searchParameters":[{"parameterId":"param3000000004","name":"In what country were you born? - USA","domain":"SURVEY","type":"PPI","group":false,"attributes":[{"name":"CAT","operator":"IN","operands":["1586136"]}],"ancestorData":false,"standard":false,"conceptId":1586135,"subtype":"ANSWER"},{"parameterId":"param3000000330","name":"Overall Health","domain":"SURVEY","type":"PPI","group":true,"attributes":[],"ancestorData":false,"standard":false,"conceptId":1585710,"subtype":"SURVEY"},{"parameterId":"param158586402","name":"How old were you when you first started regular cigarette smoking every day? - Select a value (>= 22)","domain":"SURVEY","type":"PPI","group":false,"attributes":[{"name":"NUM","operator":"GREATER_THAN_OR_EQUAL_TO","operands":[22]}],"ancestorData":false,"standard":false,"conceptId":1585864,"subtype":"ANSWER"},{"parameterId":"param13331022100000002","name":"In the past month, have recommendations for socially distancing caused stress for you? ( May 2020)","domain":"SURVEY","type":"PPI","group":true,"attributes":[{"name":"SURVEY_VERSION_CONCEPT_ID","operator":"IN","operands":["2100000002"]}],"ancestorData":false,"standard":false,"conceptId":1333102,"subtype":"QUESTION"},{"parameterId":"param13328642100000003","name":"Thinking about your current social habits, in the last 5 days: I have stayed home all day (aside from time spent outdoors, but never closer than 6 feet from people who are not from my home). - A few days (1-2 days) ( June 2020)","domain":"SURVEY","type":"PPI","group":false,"attributes":[{"name":"SURVEY_VERSION_CONCEPT_ID","operator":"IN","operands":["2100000003"]},{"name":"CAT","operator":"IN","operands":["1332864"]}],"ancestorData":false,"standard":false,"conceptId":1332748,"subtype":"ANSWER"}],"modifiers":[]},{"id":"items_gjalpln6v","type":"WHOLE_GENOME_VARIANT","searchParameters":[{"parameterId":"","name":"Whole Genome Sequence","domain":"WHOLE_GENOME_VARIANT","type":"PPI","group":false,"attributes":[]}],"modifiers":[]}],"temporal":false},{"id":"includes_05o8gyd7c","items":[{"id":"items_yvshrudcd","type":"PHYSICAL_MEASUREMENT","searchParameters":[{"parameterId":"param2000000003","name":"Normal (Systolic <= 120 / Diastolic <= 80)","domain":"PHYSICAL_MEASUREMENT","type":"PPI","group":false,"attributes":[{"conceptId":903118,"name":"NUM","operands":["120"],"operator":"LESS_THAN_OR_EQUAL_TO"},{"conceptId":903115,"name":"NUM","operands":["80"],"operator":"LESS_THAN_OR_EQUAL_TO"}],"ancestorData":false,"standard":false}],"modifiers":[]},{"id":"items_pmzqfcnjx","type":"FITBIT","searchParameters":[{"parameterId":"","name":"Has any Fitbit data","domain":"FITBIT","type":"PPI","group":false,"attributes":[]}],"modifiers":[]}],"temporal":false},{"id":"includes_6xaia7m9p","items":[{"id":"items_16kfp3lzn","type":"OBSERVATION","searchParameters":[{"parameterId":"param304357945401-7true","name":"Postal code [Location]","domain":"OBSERVATION","type":"LOINC","group":false,"attributes":[],"ancestorData":false,"standard":true,"conceptId":3043579},{"parameterId":"param407662292000000011","name":"Current state [PhenX] (State Information Suppressed for Privacy)","domain":"OBSERVATION","type":"LOINC","group":false,"attributes":[{"name":"CAT","operator":"IN","operands":["2000000011"]}],"ancestorData":false,"standard":true,"conceptId":40766229}],"modifiers":[]}],"temporal":false}],"excludes":[{"id":"excludes_qvf5lobnb","items":[{"id":"items_bsukh7zfs","type":"PERSON","searchParameters":[{"parameterId":"","name":"Deceased","domain":"PERSON","type":"DECEASED","group":false,"attributes":[]}],"modifiers":[]}],"temporal":false}],"dataFilters":[]}',
    cohortName: 'Multi Review 1',
    description: '',
    matchedParticipantCount: 8683,
    reviewSize: 123,
    reviewedCount: 0,
    reviewStatus: 'CREATED',
    participantCohortStatuses: null,
  },
  {
    cohortReviewId: 11421,
    cohortId: 81339,
    cdrVersionId: 3,
    etag: '"2"',
    creationTime: 1643819959000,
    lastModifiedTime: 1643819959000,
    cohortDefinition:
      '{"includes":[{"id":"includes_aa5r7a759","items":[{"id":"items_4vwo67udr","type":"CONDITION","searchParameters":[{"parameterId":"param4042140118234003true","name":"Finding by site","domain":"CONDITION","type":"SNOMED","group":true,"attributes":[],"ancestorData":false,"standard":true,"conceptId":4042140},{"parameterId":"param427402564572001true","name":"Disease","domain":"CONDITION","type":"SNOMED","group":true,"attributes":[],"ancestorData":false,"standard":true,"conceptId":4274025}],"modifiers":[]}],"temporal":false}],"excludes":[],"dataFilters":[]}',
    cohortName: 'Multi Review 2',
    description: '',
    matchedParticipantCount: 99426,
    reviewSize: 222,
    reviewedCount: 0,
    reviewStatus: 'CREATED',
    participantCohortStatuses: null,
  },
  {
    cohortReviewId: 11860,
    cohortId: 83342,
    cdrVersionId: 3,
    etag: '"2"',
    creationTime: 1645043277000,
    lastModifiedTime: 1645043277000,
    cohortDefinition:
      '{"includes":[{"id":"includes_6qfw7k6ug","items":[{"id":"items_mbm5ckfao","type":"PERSON","searchParameters":[{"parameterId":"AGE-18-33","name":"Current Age In Range 18 - 33","domain":"PERSON","type":"AGE","group":false,"attributes":[{"name":"AGE","operator":"BETWEEN","operands":["18","33"]}],"ancestorData":false,"standard":true},{"parameterId":"AGE_AT_CONSENT-18-33","name":"Age at Consent In Range 18 - 33","domain":"PERSON","type":"AGE","group":false,"attributes":[{"name":"AGE_AT_CONSENT","operator":"BETWEEN","operands":["18","33"]}],"ancestorData":false,"standard":true},{"parameterId":"AGE_AT_CDR-18-33","name":"Age at CDR Date In Range 18 - 33","domain":"PERSON","type":"AGE","group":false,"attributes":[{"name":"AGE_AT_CDR","operator":"BETWEEN","operands":["18","33"]}],"ancestorData":false,"standard":true}],"modifiers":[]},{"id":"items_6hvn21s6q","type":"PERSON","searchParameters":[{"parameterId":"","name":"Deceased","domain":"PERSON","type":"DECEASED","group":false,"attributes":[]}],"modifiers":[]}],"temporal":false},{"id":"includes_gts99ssa1","items":[{"id":"items_qjsb83prp","type":"PERSON","searchParameters":[{"parameterId":"param38003564","name":"Ethnicity - Not Hispanic or Latino","domain":"PERSON","type":"ETHNICITY","group":false,"ancestorData":false,"standard":true,"conceptId":38003564},{"parameterId":"param38003563","name":"Ethnicity - Hispanic or Latino","domain":"PERSON","type":"ETHNICITY","group":false,"ancestorData":false,"standard":true,"conceptId":38003563},{"parameterId":"param1586148","name":"Ethnicity - Race Ethnicity None Of These","domain":"PERSON","type":"ETHNICITY","group":false,"ancestorData":false,"standard":true,"conceptId":1586148},{"parameterId":"param903079","name":"Ethnicity - Prefer Not To Answer","domain":"PERSON","type":"ETHNICITY","group":false,"ancestorData":false,"standard":true,"conceptId":903079},{"parameterId":"param903096","name":"Ethnicity - Skip","domain":"PERSON","type":"ETHNICITY","group":false,"ancestorData":false,"standard":true,"conceptId":903096}],"modifiers":[]},{"id":"items_vsu4o82fq","type":"PERSON","searchParameters":[{"parameterId":"param45878463","name":"Gender Identity - Female","domain":"PERSON","type":"GENDER","group":false,"ancestorData":false,"standard":true,"conceptId":45878463},{"parameterId":"param45880669","name":"Gender Identity - Male","domain":"PERSON","type":"GENDER","group":false,"ancestorData":false,"standard":true,"conceptId":45880669},{"parameterId":"param2000000002","name":"Gender Identity - Not man only, not woman only, prefer not to answer, or skipped","domain":"PERSON","type":"GENDER","group":false,"ancestorData":false,"standard":true,"conceptId":2000000002}],"modifiers":[]},{"id":"items_9i24yqvzv","type":"PERSON","searchParameters":[{"parameterId":"param8527","name":"Race - White","domain":"PERSON","type":"RACE","group":false,"ancestorData":false,"standard":true,"conceptId":8527},{"parameterId":"param8516","name":"Race - Black or African American","domain":"PERSON","type":"RACE","group":false,"ancestorData":false,"standard":true,"conceptId":8516},{"parameterId":"param","name":"Race - Unknown","domain":"PERSON","type":"RACE","group":false,"ancestorData":false,"standard":true,"conceptId":0},{"parameterId":"param2000000008","name":"Race - More than one population","domain":"PERSON","type":"RACE","group":false,"ancestorData":false,"standard":true,"conceptId":2000000008},{"parameterId":"param8515","name":"Race - Asian","domain":"PERSON","type":"RACE","group":false,"ancestorData":false,"standard":true,"conceptId":8515},{"parameterId":"param2000000001","name":"Race - Another single population","domain":"PERSON","type":"RACE","group":false,"ancestorData":false,"standard":true,"conceptId":2000000001},{"parameterId":"param45882607","name":"Race - None of these","domain":"PERSON","type":"RACE","group":false,"ancestorData":false,"standard":true,"conceptId":45882607}],"modifiers":[]},{"id":"items_v3bluc7e6","type":"PERSON","searchParameters":[{"parameterId":"param45878463","name":"Sex Assigned at Birth - Female","domain":"PERSON","type":"SEX","group":false,"ancestorData":false,"standard":true,"conceptId":45878463},{"parameterId":"param45880669","name":"Sex Assigned at Birth - Male","domain":"PERSON","type":"SEX","group":false,"ancestorData":false,"standard":true,"conceptId":45880669},{"parameterId":"param2000000009","name":"Sex Assigned at Birth - Not male, not female, prefer not to answer, or skipped","domain":"PERSON","type":"SEX","group":false,"ancestorData":false,"standard":true,"conceptId":2000000009}],"modifiers":[]}],"temporal":false}],"excludes":[],"dataFilters":[]}',
    cohortName: 'Multi Review 3',
    description: '',
    matchedParticipantCount: 50345,
    reviewSize: 234,
    reviewedCount: 0,
    reviewStatus: 'CREATED',
    participantCohortStatuses: null,
  },
];

export const CohortReviewPage = fp.flow(
  withCurrentWorkspace(),
  withSpinnerOverlay()
)(({ workspace, hideSpinner, showSpinner }) => {
  const { ns, wsid, cid } = useParams<MatchParams>();
  const [navigate, navigateByUrl] = useNavigation();
  const [cohort, setCohort] = useState(undefined);
  const [cohortReviews, setCohortReviews] = useState(undefined);
  const [activeReview, setActiveReview] = useState(undefined);
  const [loading, setLoading] = useState(true);
  const [loadingParticipants, setLoadingParticipants] = useState(false);

  const loadCohortAndReviews = async () => {
    const [cohortResponse, cohortReviewResponse] = await Promise.all([
      cohortsApi().getCohort(ns, wsid, +cid),
      cohortReviewApi().getCohortReviewsByCohortId(ns, wsid, +cid),
    ]);
    setCohort(cohortResponse);
    if (cid === '90837') {
      setCohortReviews(mockReviews);
      setActiveReview(mockReviews[0]);
      getParticipantData(+mockReviews[0].cohortId);
    } else {
      setCohortReviews(cohortReviewResponse.items);
      if (cohortReviewResponse.items.length > 0) {
        setActiveReview(cohortReviewResponse.items[0]);
        getParticipantData();
      } else {
        hideSpinner();
      }
    }
    setLoading(false);
  };

  const getVisitsFilterOptions = () => {
    cohortBuilderApi()
      .findCriteriaBy(
        ns,
        wsid,
        Domain[Domain.VISIT],
        CriteriaType[CriteriaType.VISIT]
      )
      .then((response) => {
        visitsFilterOptions.next([
          { value: null, label: 'Any' },
          ...response.items.map((option) => {
            return { value: option.name, label: option.name };
          }),
        ]);
      });
  };

  useEffect(() => {
    loadCohortAndReviews();
    if (!visitsFilterOptions.getValue()) {
      getVisitsFilterOptions();
    }
  }, []);

  const getParticipantData = (testCid?) => {
    setLoadingParticipants(true);
    showSpinner();
    const query = {
      page: 0,
      pageSize: rows,
      sortColumn: reverseColumnEnum.participantId,
      sortOrder: SortOrder.Asc,
      filters: { items: [] },
    } as Request;
    cohortReviewApi()
      .getParticipantCohortStatuses(
        ns,
        wsid,
        testCid || +cid,
        +workspace.cdrVersionId,
        query
      )
      .then((response) => {
        setActiveReview(response.cohortReview);
        setLoadingParticipants(false);
        hideSpinner();
      });
  };

  const columns = fields.map((col) => {
    const header = (
      <React.Fragment>
        <span style={styles.columnHeader}>{col.name}</span>
      </React.Fragment>
    );
    return (
      <Column
        style={styles.tableBody}
        bodyStyle={styles.columnBody}
        key={col.field}
        field={col.field}
        header={header}
        sortable
      />
    );
  });

  return (
    <FadeBox style={{ margin: 'auto', paddingTop: '1rem', width: '95.7%' }}>
      {loading ? (
        <SpinnerOverlay />
      ) : (
        <React.Fragment>
          <style>{datatableStyles}</style>
          <div>
            <button
              style={styles.backBtn}
              type='button'
              onClick={() =>
                navigateByUrl(`workspaces/${ns}/${wsid}/data/cohorts/build`, {
                  queryParams: { cohortId: cid },
                })
              }
            >
              Back to cohort
            </button>
            <h4 style={styles.title}>
              Review Sets for {cohort.name}
              <Button
                style={{ float: 'right', height: '1.3rem' }}
                disabled={loading}
                onClick={() =>
                  navigate([
                    'workspaces',
                    ns,
                    wsid,
                    'data',
                    'cohorts',
                    cid,
                    'review',
                    'cohort-description',
                  ])
                }
              >
                Cohort Description
              </Button>
            </h4>
            <div style={styles.description}>{cohort.description}</div>
          </div>
          <div style={{ display: 'flex' }}>
            <div
              style={{
                border: `1px solid ${colorWithWhiteness(colors.black, 0.8)}`,
                borderRadius: '3px',
                flex: '0 0 20%',
              }}
            >
              <div
                style={{
                  borderBottom: `1px solid ${colorWithWhiteness(
                    colors.black,
                    0.8
                  )}`,
                  color: colors.primary,
                  fontSize: '16px',
                  fontWeight: 600,
                  padding: '0.5rem',
                }}
              >
                Review Sets
                <ClrIcon
                  shape='plus-circle'
                  class='is-solid'
                  size={18}
                  style={{ cursor: 'pointer', marginLeft: '0.5rem' }}
                />
              </div>
              <div style={{ padding: '0.25rem' }}>
                {cohortReviews.map((cohortReview, cr) => (
                  <CohortReviewListItem
                    key={cr}
                    cohortReview={cohortReview}
                    onUpdate={() => loadCohortAndReviews()}
                    onSelect={() => {
                      setActiveReview(cohortReview);
                      getParticipantData(+cohortReview.cohortId);
                    }}
                    selected={
                      activeReview.cohortReviewId ===
                      cohortReview.cohortReviewId
                    }
                  />
                ))}
              </div>
            </div>
            <div style={{ flex: '0 0 80%', marginLeft: '0.25rem' }}>
              {!!activeReview?.participantCohortStatuses && (
                <DataTable
                  style={{ fontSize: '12px' }}
                  value={activeReview.participantCohortStatuses}
                  first={0}
                  lazy
                  rows={rows}
                  scrollable
                  scrollHeight='calc(100vh - 350px)'
                >
                  {columns}
                </DataTable>
              )}
            </div>
          </div>
        </React.Fragment>
      )}
    </FadeBox>
  );
});
