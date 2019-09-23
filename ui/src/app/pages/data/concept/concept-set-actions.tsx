import {Component} from '@angular/core';
import {Button} from 'app/components/buttons';
import {ActionCardBase} from 'app/components/card';
import {FadeBox} from 'app/components/containers';
import {SpinnerOverlay} from 'app/components/spinners';
import {conceptSetsApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {navigate, navigateByUrl, urlParamsStore} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {ConceptSet} from 'generated/fetch';
import * as React from 'react';

const styles = reactStyles({
  conceptSetsHeader: {
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
    title: 'Create another Concept Set',
    description: `Create another concept set for the same or a different domain.`,
    action: 'newConceptSet'
  }, {
    title: 'Create a Data Set',
    description: `Create an analyze ready data set that can be exported to notebooks.`,
    action: 'dataSet'
  }
];

interface State {
  conceptSet: ConceptSet;
  conceptSetLoading: boolean;
}

interface Props {
  workspace: WorkspaceData;
}

export const ConceptSetActions = withCurrentWorkspace()(
  class extends React.Component<Props, State> {
    constructor(props: any) {
      super(props);
      this.state = {
        conceptSet: undefined,
        conceptSetLoading: false,
      };
    }

    componentDidMount(): void {
      const csid = urlParamsStore.getValue().csid;
      this.setState({conceptSetLoading: true});
      if (csid) {
        const {namespace, id} = this.props.workspace;
        conceptSetsApi().getConceptSet(namespace, id, csid).then(cs => {
          if (cs) {
            this.setState({conceptSet: cs, conceptSetLoading: false});
          } else {
            navigate(['workspaces', namespace, id, 'data', 'concepts']);
          }
        });
      }
    }

    navigateTo(action: string): void {
      const {namespace, id} = this.props.workspace;
      const {conceptSet} = this.state;
      let url = `/workspaces/${namespace}/${id}/`;
      switch (action) {
        case 'conceptSet':
          url += `data/concepts/sets/${conceptSet.id}`;
          break;
        case 'newConceptSet':
          url += `data/concepts`;
          break;
        case 'notebook':
          url += `notebooks`;
          break;
        case 'dataSet':
          url += `data/data-sets`;
          break;
      }
      navigateByUrl(url);
    }

    render() {
      const {conceptSet, conceptSetLoading} = this.state;
      return <FadeBox style={{margin: 'auto', marginTop: '1rem', width: '95.7%'}}>
        {conceptSetLoading && <SpinnerOverlay />}
        {conceptSet && <React.Fragment>
          <h3 style={styles.conceptSetsHeader}>Concept Set Saved Successfully</h3>
          <div style={{marginTop: '0.25rem'}}>
            The concept set
            <a style={{color: colors.accent, margin: '0 4px'}}
               onClick={() => this.navigateTo('conceptSet')}>
              {conceptSet.name}
            </a>
            has been saved.
          </div>
          <h3 style={{...styles.conceptSetsHeader, marginTop: '1.5rem'}}>What Next?</h3>
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
export class ConceptSetActionsComponent extends ReactWrapperBase {

  constructor() {
    super(ConceptSetActions, []);
  }
}
