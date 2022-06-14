import * as React from 'react';
import { useParams } from 'react-router';
import * as fp from 'lodash/fp';

const { useEffect, useState } = React;

import { Domain } from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { ComboChart } from 'app/components/combo-chart.component';
import { ClrIcon } from 'app/components/icons';
import { SpinnerOverlay } from 'app/components/spinners';
import { ParticipantsCharts } from 'app/pages/data/cohort-review/participants-charts';
import {
  cohortBuilderApi,
  cohortReviewApi,
  cohortsApi,
} from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles } from 'app/utils';

const styles = reactStyles({
  activeButton: {

  },
  domainButton: {
    background: colors.accent,
    borderRadius: '3px',
    color: colors.white,
    height: '1rem',
  }
});

enum OverviewPanels {
  DEMOGRAPHICS,
  CONDITIONS,
  PROCEDURES,
  MEDICATIONS,
  LABS,
}

const domainTabs = [
  {
    displayText: 'DEMOGRAPHICS',
    domain: Domain.PERSON,
  },
  {
    displayText: 'CONDITIONS',
    domain: Domain.CONDITION,
  },
  {
    displayText: 'PROCEDURES',
    domain: Domain.PROCEDURE,
  },
  {
    displayText: 'MEDICATIONS',
    domain: Domain.DRUG,
  },
  {
    displayText: 'LABS',
    domain: Domain.LAB,
  },
];

export const CohortReviewOverview = ({ cohort }) => {
  const [activeTab, setActiveTab] = useState(OverviewPanels.DEMOGRAPHICS);
  const [loading, setLoading] = useState(true);
  const [panelOpen, setPanelOpen] = useState(false);

  return (
    <div>
      <div>
        <h3>Overview</h3>
        <ClrIcon shape='angle' direction={panelOpen ? 'up' : 'down'} />
      </div>
      {panelOpen && (
        <div>{loading ? <SpinnerOverlay /> : <div style={{ display: 'flex' }}>
          <div style={{ flex: '0 0 20%', marginLeft: '0.25rem' }}>
            {domainTabs.map((domainTab, index) => <div key={index}>
              <Button type=
            </div>)}
          </div>
          <div style={{ flex: '0 0 80%', marginLeft: '0.25rem' }}></div>
        </div>}</div>
      )}
    </div>
  );
};
