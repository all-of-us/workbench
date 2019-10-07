import {Component} from '@angular/core';
import {Button} from 'app/components/buttons';
import {ActionCardBase} from 'app/components/card';
import {FadeBox} from 'app/components/containers';
import {SpinnerOverlay} from 'app/components/spinners';
import {cohortsApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {currentCohortStore, navigate, navigateByUrl, urlParamsStore} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {Cohort} from 'generated/fetch';
import * as React from 'react';

const styles = reactStyles({
  cohortsHeader: {
    color: colors.primary,
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
    fontSize: '18px', fontWeight: 600, lineHeight: '22px', color: colors.primary,
    wordBreak: 'break-word', textOverflow: 'ellipsis', overflow: 'hidden',
    display: '-webkit-box', WebkitLineClamp: 3, WebkitBoxOrient: 'vertical'
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

const actionCards = [
  {
    title: 'Create another Cohort',
    description: `Create another cohort for your analysis.`,
    action: 'newCohort'
  }, {
    title: 'Create Review Sets',
    description: `The review set feature allows you to select a subset of your cohort to review
       participants row-level data and add notes and annotations.`,
    action: 'review'
  }, {
    title: 'Create a Dataset',
    description: `Create an analysis ready dataset that can be exported to notebooks.`,
    action: 'dataSet'
  },
];

interface Props {
  workspace: WorkspaceData;
}

interface State {
  cohort: Cohort;
  cohortLoading: boolean;
}

const CohortActions = withCurrentWorkspace()(
  class extends React.Component<Props, State> {
    constructor(props: any) {
      super(props);
      this.state = {cohort: currentCohortStore.getValue(), cohortLoading: false};
    }

    componentDidMount(): void {
      const {cohort} = this.state;
      if (!cohort) {
        const cid = urlParamsStore.getValue().cid;
        this.setState({cohortLoading: true});
        if (cid) {
          const {namespace, id} = this.props.workspace;
          cohortsApi().getCohort(namespace, id, cid).then(c => {
            if (c) {
              currentCohortStore.next(c);
              this.setState({cohort: c, cohortLoading: false});
            } else {
              navigate(['workspaces', namespace, id, 'data', 'cohorts']);
            }
          });
        }
      }
    }

    navigateTo(action: string): void {
      const {cohort} = this.state;
      const {namespace, id} = this.props.workspace;
      let url = `/workspaces/${namespace}/${id}/`;
      switch (action) {
        case 'cohort':
          url += `data/cohorts/build?cohortId=${cohort.id}`;
          break;
        case 'review':
          url += `data/cohorts/${cohort.id}/review`;
          break;
        case 'notebook':
          url += 'notebooks';
          break;
        case 'dataSet':
          url += 'data/data-sets';
          break;
        case 'newCohort':
          url += `data/cohorts/build`;
      }
      navigateByUrl(url);
    }

    render() {
      const {cohort, cohortLoading} = this.state;
      return <FadeBox style={{margin: 'auto', marginTop: '1rem', width: '95.7%'}}>
        {cohortLoading && <SpinnerOverlay />}
        {cohort && <React.Fragment>
          <h3 style={styles.cohortsHeader}>Cohort Saved Successfully</h3>
          <div style={{marginTop: '0.25rem'}}>
            The cohort
             <a
               style={{color: colors.accent, margin: '0 4px'}}
               onClick={() => this.navigateTo('cohort')}>
                {cohort.name}
             </a>
             has been saved.
          </div>
          <h3 style={{...styles.cohortsHeader, marginTop: '1.5rem'}}>What Next?</h3>
          <div style={styles.cardArea}>
            {actionCards.map((card, i) => {
              return <ActionCardBase key={i} style={styles.card}>
                <div style={{display: 'flex', flexDirection: 'column', alignItems: 'flex-start'}}>
                  <div style={{display: 'flex', flexDirection: 'row', alignItems: 'flex-start'}}>
                    <div style={styles.cardName}>{card.title}</div>
                  </div>
                  <div style={styles.cardDescription}>{card.description}</div>
                </div>
                <div>
                  <Button
                    type='primary'
                    style={styles.cardButton}
                    onClick={() => this.navigateTo(card.action)}>
                    {card.title}
                  </Button>
                </div>
              </ActionCardBase>;
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
