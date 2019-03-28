import {Component} from '@angular/core';
import {Button} from 'app/components/buttons';
import {CohortActionCardBase} from 'app/components/card';
import {FadeBox} from 'app/components/containers';
import {cohortsApi} from 'app/services/swagger-fetch-clients';
import {WorkspaceData} from 'app/services/workspace-storage.service';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {currentCohortStore, navigate, navigateByUrl, urlParamsStore} from 'app/utils/navigation';
import {Cohort} from 'generated/fetch';
import * as React from 'react';

const styles = reactStyles({
  cohortsHeader: {
    color: '#2F2E7E',
    fontSize: '20px',
    lineHeight: '24px',
    fontWeight: 600,
    marginTop: 0,
  },
  cardArea: {
    display: 'flex',
    alignItems: 'center',
    width: '100%'
  },
  card: {
    marginTop: '0.5rem',
    justifyContent: 'space-between',
    marginRight: '1rem',
    padding: '0.75rem 0.75rem 0rem 0.75rem',
    boxShadow: '0 0 0 0'
  },
  cardName: {
    fontSize: '18px', fontWeight: 600, lineHeight: '22px', color: '#2F2E7E',
    cursor: 'pointer', wordBreak: 'break-all', textOverflow: 'ellipsis',
    overflow: 'hidden', display: '-webkit-box', WebkitLineClamp: 3,
    WebkitBoxOrient: 'vertical'
  },
  cardDescription: {
    marginTop: '0.5rem', textOverflow: 'ellipsis', overflow: 'hidden', display: '-webkit-box',
    WebkitLineClamp: 4, WebkitBoxOrient: 'vertical'
  },
  cardButton: {
    margin: '1rem 0',
    height: '2rem'
  }
});

const disabledButton = {
  ...styles.cardButton,
  cursor: 'not-allowed'
};

const actionCards = [
  {
    title: 'Create Review Sets',
    description: `The review set feature allows you to select a subset of your cohort to review
       participants row-level data and add notes and annotations.`,
    action: 'review'
  },
  {
    title: 'Export to a Notebook',
    description: `Data can be exported to a cloud-based Jupyter notebook for analysis using R or
       Python programming language.`,
    action: 'notebook'
  },
  {
    title: 'Create a Dataset',
    description: `Here, you can build build and preview a dataset for one or more cohorts by
       selecting the desired concept sets and values for the cohorts.`,
    action: 'dataset'
  },
];

export const CohortActions = withCurrentWorkspace()(
  class extends React.Component<{workspace: WorkspaceData}, {cohort: Cohort}> {
    constructor(props: any) {
      super(props);
      this.state = {cohort: currentCohortStore.getValue()};
    }

    componentDidMount(): void {
      const {cohort} = this.state;
      if (!cohort) {
        const cid = urlParamsStore.getValue().cid;
        if (cid) {
          const {namespace, id} = this.props.workspace;
          cohortsApi().getCohort(namespace, id, cid).then(c => {
            if (c) {
              currentCohortStore.next(c);
              this.setState({cohort: c});
            } else {
              navigate(['workspaces', namespace, id, 'cohorts']);
            }
          });
        }
      }
    }

    navigateTo = (action: string): void => {
      const {cohort} = this.state;
      const {namespace, id} = this.props.workspace;
      let url = `/workspaces/${namespace}/${id}/`;
      switch (action) {
        case 'cohort':
          url += `cohorts/build?cohortId=${cohort.id}`;
          break;
        case 'review':
          url += `cohorts/${cohort.id}/review`;
          break;
        case 'notebook':
          url += 'notebooks';
          break;
        case 'dataset':
          url += 'data/datasets';
          break;
      }
      navigateByUrl(url);
    }

    render() {
      const {cohort} = this.state;
      return <FadeBox style={{margin: 'auto', marginTop: '1rem', width: '95.7%'}}>
        {cohort && <React.Fragment>
          <h3 style={styles.cohortsHeader}>Cohort Saved Successfully</h3>
          <div style={{marginTop: '0.25rem'}}>
            The cohort
             <a
               style={{color: '#5DAEE1', margin: '0 4px'}}
               onClick={() => this.navigateTo('cohort')}>
                {cohort.name}
             </a>
             has been saved and can now be used in analysis and concept sets.
          </div>
          <h3 style={{...styles.cohortsHeader, marginTop: '1.5rem'}}>What Next?</h3>
          <div style={styles.cardArea}>
            {actionCards.map((card, i) => {
              const disabled = card.action === 'notebook';
              return <CohortActionCardBase key={i} style={styles.card}>
                <div style={{display: 'flex', flexDirection: 'column', alignItems: 'flex-start'}}>
                  <div style={{display: 'flex', flexDirection: 'row', alignItems: 'flex-start'}}>
                    <div style={styles.cardName}>{card.title}</div>
                  </div>
                  <div style={styles.cardDescription}>{card.description}</div>
                </div>
                <div>
                  <Button
                    type='primary'
                    style={disabled ? disabledButton : styles.cardButton}
                    disabled={disabled}
                    onClick={() => this.navigateTo(card.action)}>
                    {card.title}
                  </Button>
                </div>
              </CohortActionCardBase>;
            })}
          </div>
        </React.Fragment>}
      </FadeBox>;
    }
  }
);

@Component({
  template: '<div #root></div>'
})
export class CohortActionsComponent extends ReactWrapperBase {

  constructor() {
    super(CohortActions, []);
  }
}
