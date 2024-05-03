import * as fp from 'lodash/fp';

import { WorkspaceResource } from 'generated/fetch';

import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import {
  getTypeString,
  isCohort,
  isCohortReview,
  isConceptSet,
  isDataSet,
  isNotebook,
} from 'app/utils/resources';

const styles = reactStyles({
  resourceType: {
    height: '22px',
    width: '9rem',
    paddingLeft: '10px',
    paddingRight: '10px',
    borderRadius: '2px',
    display: 'flex',
    justifyContent: 'left',
    color: colors.white,
    fontFamily: 'Montserrat, sans-serif',
    fontSize: '12px',
    fontWeight: 500,
  },
});

export const StyledResourceType = (props: { resource: WorkspaceResource }) => {
  const { resource } = props;

  function getColor(): string {
    return fp.cond([
      [isCohort, () => colors.resourceCardHighlights.cohort],
      [isCohortReview, () => colors.resourceCardHighlights.cohortReview],
      [isConceptSet, () => colors.resourceCardHighlights.conceptSet],
      [isDataSet, () => colors.resourceCardHighlights.dataSet],
      [isNotebook, () => colors.resourceCardHighlights.notebook],
    ])(resource);
  }

  return (
    <div
      data-test-id='card-type'
      style={{ ...styles.resourceType, backgroundColor: getColor() }}
    >
      {fp.startCase(fp.camelCase(getTypeString(resource)))}
    </div>
  );
};
