import * as React from 'react';
import { useParams } from 'react-router';

const { useEffect, useState } = React;

import { AgeType, Domain, GenderOrSexType } from 'generated/fetch';

import { ComboChart } from 'app/components/combo-chart.component';
import { ClrIcon } from 'app/components/icons';
import { SpinnerOverlay } from 'app/components/spinners';
import { cohortBuilderApi } from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import { MatchParams } from 'app/utils/stores';

import { ParticipantsCharts } from './participants-charts';

const styles = reactStyles({
  domainButton: {
    background: colors.accent,
    border: 'none',
    borderRadius: '3px',
    color: colors.white,
    cursor: 'pointer',
    fontSize: '10px',
    fontWeight: 600,
    height: '1.25rem',
    width: '5rem',
  },
  get activeButton() {
    return {
      ...this.domainButton,
      background: colors.white,
      border: `1px solid ${colors.accent}`,
      color: colors.accent,
    };
  },
  overviewContainer: {
    borderRadius: '3px',
    boxShadow: '0 0 2px 0 rgba(0,0,0,0.12), 0 3px 2px 0 rgba(0,0,0,0.12)',
    marginBottom: '0.5rem',
  },
  overviewHeader: {
    cursor: 'pointer',
    fontWeight: 600,
    lineHeight: '2rem',
    margin: '0 0.5rem',
  },
  graphContainer: {
    flex: '0 0 80%',
    position: 'relative',
  },
  tabsContainer: {
    flex: '0 0 20%',
    marginTop: '-1rem',
    padding: '2rem 0 0 1rem',
  },
});

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
  const { ns, wsid } = useParams<MatchParams>();
  const [activeTab, setActiveTab] = useState(domainTabs[0].domain);
  const [demoChartData, setDemoChartData] = useState(undefined);
  const [loading, setLoading] = useState(true);
  const [panelOpen, setPanelOpen] = useState(false);

  useEffect(() => {
    cohortBuilderApi()
      .findDemoChartInfo(
        ns,
        wsid,
        GenderOrSexType[GenderOrSexType.GENDER],
        AgeType[AgeType.AGE],
        JSON.parse(cohort.criteria)
      )
      .then((demoChartInfo) => {
        setDemoChartData(demoChartInfo.items);
        setLoading(false);
      });
  }, []);

  return (
    <div style={styles.overviewContainer}>
      <h3
        style={styles.overviewHeader}
        onClick={() => setPanelOpen((prevPanelOpen) => !prevPanelOpen)}
      >
        Overview
        <ClrIcon
          style={{ marginLeft: '0.5rem' }}
          shape='angle'
          dir={panelOpen ? 'up' : 'down'}
        />
      </h3>
      {panelOpen && (
        <div>
          {loading ? (
            <SpinnerOverlay />
          ) : (
            <div style={{ display: 'flex' }}>
              <div style={styles.tabsContainer}>
                {domainTabs.map(({ displayText, domain }, index) => (
                  <div key={index} style={{ marginBottom: '0.5rem' }}>
                    <button
                      style={
                        activeTab === domain
                          ? styles.activeButton
                          : styles.domainButton
                      }
                      onClick={() => setActiveTab(domain)}
                    >
                      {displayText}
                    </button>
                  </div>
                ))}
              </div>
              <div style={styles.graphContainer}>
                {activeTab === Domain.PERSON ? (
                  <ComboChart mode={'stacked'} data={demoChartData} />
                ) : (
                  <ParticipantsCharts cohortId={cohort.id} domain={activeTab} />
                )}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
