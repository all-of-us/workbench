import {Component} from '@angular/core';
import {Clickable} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {ClrIcon} from 'app/components/icons';
import {TooltipTrigger} from 'app/components/popups';
import {reactStyles, ReactWrapperBase} from 'app/utils';
import {ConceptSetsList} from 'app/views/concept-set-list/component';
import {ConceptWrapper} from 'app/views/concepts/component';
import * as React from 'react';

const styles = reactStyles({
  headerLinks: {
    color: '#2691D0',
    fontSize: '20px',
    lineHeight: '24px',
    height: '1.5rem',
    padding: '0 .5rem',
    textAlign: 'center',
    letterSpacing: '0.12rem',
    textDecoration: 'none'
  },
  infoIcon: {
    color: '#2691D0',
    cursor: 'pointer',
    marginLeft: '0.2rem',
    height: '20px',
    width: '20px'
  }
});

const activatedStyles = reactStyles({
  headerActivated: {
    ...styles.headerLinks,
    fontWeight: 700,
    borderBottom: 'solid 3px'
  }
});

const tooltipContent = [<ul><u>What is a concept?</u><ul/>
      Concepts describe information in a patient’s medical record,
      such as a condition they have, a  prescription they are
  taking or their physical measurements. <p/>
  <u>What is a concept set?</u><ul/>
      You can search for and save collections of concepts from a particular domain as a
      “Concept set”. For example, you can search for height, weight and blood pressure
      concepts from “Measurements” domain and call it “biometrics” concept set.<p/>
  <u>How to use a concept set </u><ul/>
      You can use Notebooks to extract data defined in your “concept set” from your “cohort”.
      For example, you can launch a Notebook to import your “diabetes cases” cohort
      and then select your “biometrics” concept set, to get biometrics data for
  the participants in your cohort.</ul>];

export class ConceptHomepage extends React.Component<{}, {showConcepts: boolean}> {
  constructor(props) {
    super(props);
    this.state = {
      showConcepts: true
    };
  }

  render() {
    const {showConcepts} = this.state;
    return <React.Fragment>
      <FadeBox style={{margin: 'auto', marginTop: '1rem', width: '95.7%'}}>
        <div style={{display: 'flex', flexDirection: 'row'}}>
          <Clickable style={showConcepts ? activatedStyles.headerActivated : styles.headerLinks}
            onClick={() => this.setState({showConcepts: true})}
            data-test-id='concepts-link'>
            Concepts
          </Clickable>
          <Clickable
            style={{...(showConcepts ? styles.headerLinks : activatedStyles.headerActivated),
              marginLeft: '1rem'}}
            onClick={() => this.setState({showConcepts: false})}
            data-test-id='concept-sets-link'>
            Concept Sets
          </Clickable>
          <TooltipTrigger content={tooltipContent}>
            <ClrIcon shape='info' className='is-solid' style={styles.infoIcon}/>
          </TooltipTrigger>
        </div>
        {showConcepts && <ConceptWrapper/>}
        {!showConcepts && <ConceptSetsList/>}
      </FadeBox>
    </React.Fragment>;
  }
}

@Component({
  template: '<div #root></div>'
})
export class ConceptHomepageComponent extends ReactWrapperBase {
  constructor() {
    super(ConceptHomepage, []);
  }
}
