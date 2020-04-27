
import {
  Button, StyledAnchorTag
} from 'app/components/buttons';
import {FlexColumn} from 'app/components/flex';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles} from 'app/utils';

import * as React from 'react';

const OMOPTutorialsLink = 'https://www.ohdsi.org/past-events/2017-tutorials-' +
  'omop-common-data-model-and-standardized-vocabularies/';
const OMOPDataSetLink = 'https://www.ohdsi.org/data-standardization/the-common-data-model/';

const notReachedSelectorColor = colorWithWhiteness(colors.dark, 0.4);

export const panels = [
  {
    title: 'Introduction',
    shortTitle: 'Intro',
    content: <div>Welcome to the <i>All of Us</i> Researcher Workbench!<br/><br/>All workbench analyses
      happen in a “Workspace.” Within a Workspace you can select participants
      using the “Cohort Builder” tool. Another tool, the “Concept Set Builder,”
      allows you to select data types for analysis. The cohorts and concept sets
      you make can then be accessed from “Notebooks,” the analysis environment. <br/><br/>
      For illustration, let's consider research on 'Type 2 diabetes' for this quick tour.</div>,
    image: '/assets/images/quick-tour/homepage.png'
  },
  {
    title: 'Workspaces',
    shortTitle: 'Workspaces',
    content: <div>A Workspace is your place to store and analyze data for a specific project.
      You can share this Workspace with other users, allowing them to view or edit
      your work. The dataset referenced by a workspace is in
      {' '}<StyledAnchorTag href={OMOPDataSetLink} target='_blank'>
        OMOP common data model
      </StyledAnchorTag>{' '}
      format. Here are some
      {' '}<StyledAnchorTag href={OMOPTutorialsLink} target='_blank'>
        tutorials
      </StyledAnchorTag>{' '}
      to understand OMOP data model.
      <br/><br/>
      When you create your Workspace, you will be prompted
      to state your research purpose. For example, when you create a Workspace to study Type
      2 Diabetes, for research purpose you could enter: “I will use this Workspace to
      investigate the impact of Geography on use of different medications to treat
      Type 2Diabetes.”</div>,
    image: '/assets/images/quick-tour/workspaces.png'
  },
  {
    title: 'Cohorts',
    shortTitle: 'Cohorts',
    content: <div>A “Cohort” is a group of participants you are interested in researching.
      The Cohort Builder allows you to create and review cohorts and annotate
      participants in your study group.
      <br/><br/>
      For example, you can build a Cohort called “diabetes cases,” to include people
      who have been diagnosed with type II diabetes, using a combination of billing codes and
      laboratory values. You can also have a “controls” Cohort. Once you build your cohorts,
      you can go through and manually review the records for each participant and decide if
      you want to include or exclude them from your Cohort and make specific
      annotations/notes to each record.</div>,
    image: '/assets/images/quick-tour/cohorts.png'
  },
  {
    title: 'Concepts',
    shortTitle: 'Concepts',
    content: <div>Concepts describe information in a patient’s medical record, such as a
      condition they have, a prescription they are taking or their physical measurements.
      In the Workbench we refer to subject areas such as conditions, drugs, measurements
      etc. as “domains.” You can search for and save collections of concepts from a
      particular domain as a “Concept Set.”
      <br/><br/>
      For example, if you want to select height, weight and blood pressure information
      (concepts) from your “diabetes cases” Cohort, you can search for the 3 concepts
      from the “Measurements” domain and call it “biometrics” Concept Set. You can then
      use Notebooks to extract that information from your cohort.</div>,
    image: '/assets/images/quick-tour/concept-sets.png'
  },
  {
    title: 'Datasets',
    shortTitle: 'Datasets',
    content: <div>Datasets are analysis-ready tables that can be exported to a Notebook.
      You can build and preview a dataset for one or more cohorts by selecting the desired
      concept sets and values for the cohorts.
      <br/><br/>
      For example, select your cohort called “diabetes cases” and your concept set called
      “biometrics” and then the values from the concept set you want to see. Before exporting
      the dataset, you can preview the resulting data frame that will be sent to the notebook.</div>,
    image: '/assets/images/quick-tour/datasets.png'
  },
  {
    title: 'Notebooks',
    shortTitle: 'Notebooks',
    content: <div>A Notebook is a computational environment where you can analyze data with basic
      programming knowledge in R or Python. Several template Notebooks and resources
      are available within your Workspace that will guide you how to import your
      Cohort(s) and Concept Set(s) into the Notebook and can assist with basic analyses.
      <br/><br/>
      For example, you can launch a Notebook
      to import your “diabetes cases” Cohort and then select your “biometrics” Concept Set, to
      get biometrics data for the participants in your Cohort. You can then analyze the data to
      study correlation between hypertension and diabetes.</div>,
    image: '/assets/images/quick-tour/notebooks.png'
  }];

const styles = reactStyles({
  modalBackdrop: {
    position: 'fixed',
    top: 0,
    bottom: 0,
    right: 0,
    left: 0,
    backgroundColor: colors.dark,
    opacity: .8,
    zIndex: 1040,
  },
  mainStyling: {
    height: '79.23%',
    width: '80%',
    borderRadius: '8px',
    backgroundColor: colors.primary,
    boxShadow: '0 2px 5px 0 rgba(0,0,0,0.26), 0 2px 10px 0 rgba(0,0,0,0.16)',
    position: 'absolute',
    left: '10%',
    top: '1%',
    zIndex: 1050,
  },
  title: {
    color: colors.white,
    marginTop: '2%',
    marginLeft: '3%',
    fontSize: 21,
    width: '100%',
    fontWeight: 500
  },
  mainTitle: {
    color: colors.white,
    fontSize: 48,
    width: '100%',
    marginTop: '3%',
    marginLeft: '3%',
    fontWeight: 'bold'
  },
  breadcrumbs: {
    width: '100%',
    marginTop: '5%',
    justifyContent: 'center',
    display: 'flex',
    flexDirection: 'row'
  },
  circle: {
    border: `2px solid ${notReachedSelectorColor}`,
    borderRadius: '50%',
    height: '21.92px',
    width: '21.92px',
    left: '21.92px'
  },
  check: {
    minHeight: '10px',
    minWidth: '10px',
    marginLeft: '2px',
    marginTop: '-3px'
  },
  current: {
    minHeight: '12px',
    minWidth: '12px',
    marginLeft: '3px',
    marginBottom: '2px',
    backgroundColor: colors.secondary,
    borderRadius: '50%',
    display: 'inline-block'
  },
  connector: {
    border: `1px solid ${notReachedSelectorColor}`,
    boxSizing: 'border-box',
    height: '1px',
    width: '107px',
    position: 'relative',
    left: '21.92px',
    top: '-36px'
  },
  breadcrumbTitle: {
    transform: 'translate(-40%)',
    textAlign: 'center',
    color: colors.secondary
  },

  divider: {
    boxSizing: 'border-box',
    height: '2px',
    width: '90%',
    border: `0.5px solid ${colors.white}`,
    boxShadow: '0 2px 5px 0 rgba(0,0,0,0.26), 0 2px 10px 0 rgba(0,0,0,0.16)',
    margin: 'auto'
  },
  panel: {
    marginTop: '5%',
    width: '100%',
    height: '30%',
    display: 'flex'
  },
  panelTitle: {
    width: '100%',
    marginLeft: '5%',
    color: colors.white,
    fontSize: 28,
    fontWeight: 'bold'
  },
  panelContents: {
    paddingLeft: '5%',
    marginTop: '1%',
    color: colors.white,
    fontSize: 14,
    textAlign: 'left'
  },
  panelText: {
    marginRight: '2%',
    paddingTop: '.5%',
    fontSize: 16,
    lineHeight: '24px',
    whiteSpace: 'pre-line',
    textAlign: 'left'
  },
  panelRight: {
    marginRight: '2rem',
    marginBottom: '5%',
    maxHeight: '90%',
    height: 'min-content',
    position: 'relative',
    display: 'flex',
    justifyContent: 'flex-end'
  },
  panelImage: {
    width: '100%',
    objectFit: 'contain',
    objectPosition: 'right top'
  },
  panelGrid: {
    flex: 1,
    minHeight: 0,
    marginTop: '5%',
    display: 'grid'
  },
  controls: {
    width: '100%',
    display: 'flex',
    justifyContent: 'space-between'
  }
});


const completedStyles = {
  circleCompleted: {
    ...styles.circle,
    left: '0px',
    border: `2px solid ${colors.secondary}`,
  },
  connectorCompleted: {
    ...styles.connector,
    border: `1px solid ${colors.secondary}`
  }
};


export interface QuickTourReactState {
  selected: number;
  fullImage: boolean;
}

export interface QuickTourReactProps {
  closeFunction: Function;
}

export class QuickTourReact extends React.Component<QuickTourReactProps, QuickTourReactState> {
  checkImg = '/assets/images/check.svg';
  expandIcon = '/assets/icons/expand.svg';
  shrinkIcon = '/assets/icons/shrink.svg';

  constructor(props: QuickTourReactProps) {
    super(props);
    this.state = {selected: 0, fullImage: false};
  }

  previous(): void {
    this.setState({selected: this.state.selected - 1});
  }

  next(): void {
    const {selected} = this.state;

    if (selected === panels.length - 1) {
      this.close();
    } else {
      this.setState({selected: selected + 1});
    }
  }

  close(): void {
    this.setState({selected: 0});
    this.props.closeFunction();
  }

  selectPanel(panel: number): void {
    this.setState({selected: panel});
  }

  lastButtonText(): string {
    return (this.state.selected === panels.length - 1 ? 'Close' : 'Next');
  }

  toggleImage(): void {
    this.setState({fullImage: !this.state.fullImage});
  }

  render() {
    const {selected, fullImage} = this.state;

    return fullImage ?
      <div style={{...styles.mainStyling, height: '35%'}}>
        <div style={{position: 'relative', display: 'inline-block'}}
             data-test-id='full-image-wrapper'>
          <img src={panels[selected].image} style={{height: '100%', width: '100%'}}/>
          <div onClick={() => this.toggleImage()}
               data-test-id='shrink-icon'
               style={{position: 'absolute', right: '5%', bottom: '5%',
                 cursor: 'pointer', width: '28px'}}>
            <img src={this.shrinkIcon}/>
          </div>
        </div>
      </div> :
      <React.Fragment>
        <div style={styles.modalBackdrop}/>
        <FlexColumn style={styles.mainStyling} data-test-id='quick-tour-react' className='quickTourReact'>
          <div style={styles.title}><i>All of Us</i> Researcher Workbench</div>
          <div style={styles.mainTitle}>Quick Tour</div>
          <div style={styles.breadcrumbs}>
            {panels.map((p, i) => {
              return <React.Fragment key={i}>
                {/*We don't set the width on the last item, because there is no connector,
                so setting a width would offset the entire selection area by half a connector length*/}
                <div style={{width: i !== panels.length - 1 ? '128px' : null}}>
                  <div style={selected ? completedStyles.circleCompleted : styles.circle}
                       data-test-id={'breadcrumb' + i}
                       onClick={() => this.selectPanel(i)}>
                    {(i < selected) && <div style={styles.check}>
                        <img src={this.checkImg}/>
                    </div>}
                    {(i === selected) && <div style={styles.current}/>}
                  </div>
                  <div style={styles.breadcrumbTitle}>{p.shortTitle}</div>
                  {(i !== panels.length - 1) &&
                  <div style={i < selected ?
                    completedStyles.connectorCompleted : styles.connector}>
                  </div>}
                </div>
              </React.Fragment>;
            })}
          </div>
          <div style={{width: '100%', paddingTop: '5%'}}>
            <div style={styles.divider}/>
          </div>
          <div style={{
            ...styles.panelGrid,
            gridTemplateAreas: `'content image'
                                'controls controls'`,
            columnGap: '1rem',
            gridTemplateColumns: '0.75fr 0.25fr',
            gridTemplateRows: 'minmax(0, 1fr) 4rem'
          }}>
            <div style={{gridArea: 'content', overflowX: 'hidden', overflowY: 'auto'}}>
              <div style={styles.panelTitle}
                   data-test-id='panel-title'>
                {panels[selected].title}
              </div>
              <div style={styles.panelContents}>
                <div style={styles.panelText}>{panels[selected].content}</div>
              </div>
            </div>
            <div style={{...styles.panelRight, gridArea: 'image'}}>
              <img src={panels[selected].image} style={styles.panelImage}/>
              {(selected !== 0) &&
              <div style={{
                position: 'absolute', right: '5%',
                bottom: '5%', height: '1rem', width: '1rem'
              }}>
                  <div style={{position: 'absolute', zIndex: 2, cursor: 'pointer', width: '28px'}}
                       data-test-id='expand-icon'
                       onClick={() => this.toggleImage()}>
                      <img src={this.expandIcon}/>
                  </div>
              </div>}
            </div>
            <div style={{...styles.controls, alignItems: 'center', gridArea: 'controls', }}>
              <div style={{width: '50%'}}>
                {selected !== 0 &&
                <Button type='primaryOnDarkBackground' data-test-id='previous'
                        style={{marginLeft: '10%'}}
                        onClick={() => this.previous()}>Previous</Button>}
              </div>
              <div style={{display: 'flex', justifyContent: 'flex-end', width: '49%'}}>
                {selected !== (panels.length - 1) &&
                <Button type='primaryOnDarkBackground' data-test-id='close'
                        onClick={() => this.close()}
                        style={{marginLeft: '10%', marginRight: '0.25rem'}}>Close</Button>}
                <Button type='secondaryOnDarkBackground' data-test-id='next'
                        style={{marginRight: '10%'}}
                        onClick={() => this.next()}>{this.lastButtonText()}</Button>
              </div>
            </div>
          </div>
        </FlexColumn>
      </React.Fragment>;
  }
}
