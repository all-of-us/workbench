import * as React from 'react';

import { ResearchPurpose } from 'generated/fetch';

import {
  getSelectedPopulations,
  getSelectedPrimaryPurposeItems,
} from 'app/utils/research-purpose';

import { WorkspaceInfoField } from './workspace-info-field';

interface Props {
  researchPurpose: ResearchPurpose;
}

export const ResearchPurposeSection = ({ researchPurpose }: Props) => {
  const {
    reasonForAllOfUs,
    intendedStudy,
    anticipatedFindings,
    populationDetails,
  } = researchPurpose;
  return (
    <>
      <h3>Research Purpose</h3>
      <div className='research-purpose' style={{ marginTop: '1.5rem' }}>
        <WorkspaceInfoField labelText='Primary purpose of project'>
          {getSelectedPrimaryPurposeItems(researchPurpose).map(
            (researchPurposeItem, i) => (
              <div key={i}>{researchPurposeItem}</div>
            )
          )}
        </WorkspaceInfoField>
        <WorkspaceInfoField labelText='Reason for choosing All of Us'>
          {reasonForAllOfUs}
        </WorkspaceInfoField>
        <WorkspaceInfoField labelText='Area of intended study'>
          {intendedStudy}
        </WorkspaceInfoField>
        <WorkspaceInfoField labelText='Anticipated findings'>
          {anticipatedFindings}
        </WorkspaceInfoField>
        {populationDetails.length > 0 && (
          <WorkspaceInfoField labelText='Population area(s) of focus'>
            {getSelectedPopulations(researchPurpose)}
          </WorkspaceInfoField>
        )}
      </div>
    </>
  );
};
