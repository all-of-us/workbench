import {Component, DoCheck, Input, OnInit} from '@angular/core';
import * as React from 'react';
import * as ReactDOM from 'react-dom';

const OMOPTutorialsLink = 'https://www.ohdsi.org/past-events/2017-tutorials-' +
    'omop-common-data-model-and-standardized-vocabularies/';
const OMOPDataSetLink = 'https://www.ohdsi.org/data-standardization/the-common-data-model/';

const panels = [
    {
      title: 'Intro',
      content: <div>Welcome to the All of Us Research Workbench!<br/><br/>All workbench analyses
        happen in a “Workspace.” Within a Workspace you can select participants
        using the “Cohort Builder” tool.  Another tool, the “Concept Set Builder,”
        allows you to select data types for analysis.  The cohorts and concept sets
        you make can then be accessed from “Notebooks,” the analysis environment. <br/><br/>
        For illustration, let's consider research on 'Type 2 diabetes' for this quick tour.</div>,
      image: '/assets/images/intro.png'
    },
    {
      title: 'Workspaces',
      content: <div>A Workspace is your place to store and analyze data for a specific project.
       You can share this Workspace with other users, allowing them to view or edit
       your work. The dataset referenced by a workspace is in
        <a className='link' href={OMOPDataSetLink} target='_blank'> OMOP common data model</a>
          format. Here are some
        <a className='link' href={OMOPTutorialsLink} target='_blank'> tutorials</a>
        to understand OMOP data model.
        <br/><br/>
        When you create your Workspace, you will be prompted
        to state your research purpose.  For example, when you create a Workspace to study Type
        2 Diabetes, for research purpose you could enter: “I will use this Workspace to
        investigate the impact of Geography on use of different medications to treat
        Type 2Diabetes.”</div>,
      image: '/assets/images/workspaces.png'
    },
    {
      title: 'Cohorts',
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
      image: '/assets/images/cohorts.png'
    },
    {
      title: 'Concepts',
      content: <div>Concepts describe information in a patient’s medical record, such as a
          condition they have, a  prescription they are taking or their physical measurements.
          In the Workbench we refer to subject areas such as conditions, drugs, measurements
          etc. as “domains.” You can search for and save collections of concepts from a
          particular domain as a “Concept Set.”
          <br/><br/>
          For example, if you want to select height, weight and blood pressure information
          (concepts) from your “diabetes cases” Cohort, you can search for the 3 concepts
          from the “Measurements” domain and call it “biometrics” Concept Set. You can then
          use Notebooks to extract that information from your cohort.</div>,
      image: '/assets/images/concepts.png'
    },
    {
      title: 'Notebooks',
      content: <div>A Notebook is a computational environment where you can analyze data with basic
          programming knowledge in R or Python. Several template Notebooks and resources
          are available within your Workspace that will guide you how to import your
          Cohort(s) and Concept Set(s) into the Notebook and can assist with basic analyses.
          <br/><br/>
         For example, you can launch a Notebook
          to import your “diabetes cases” Cohort and then select your “biometrics” Concept Set, to
          get biometrics data for the participants in your Cohort. You can then analyze the data to
          study correlation between hypertension and diabetes.</div>,
      image: '/assets/images/notebooks.png'
    }];

type QuickTourState = {
    selected: number,
    fullImage: boolean,
    numPanels: number
}

type QuickTourProps = {
    learning: boolean,
    closeFunction: Function
}

class QuickTourReact extends React.Component<QuickTourProps, QuickTourState> {
  state: QuickTourState;
  props: QuickTourProps;

  checkImg = '/assets/images/check.svg';
  expandIcon = '/assets/icons/expand.svg';
  shrinkIcon = '/assets/icons/shrink.svg';

  constructor(props: Object) {
    super(props);
    this.state = {selected: 0, fullImage: false, numPanels: 5};
  }

  previous(): void {
    this.setState(state => {
      return {selected: state.selected - 1};
    });
  }

  next(): void {
    if (this.state.selected === 4) {
      this.close();
      return;
    }
    this.setState(state => {
      return {selected: state.selected + 1};
    });
  }

  close(): void {
    this.setState({selected: 0});
    this.props.closeFunction();
  }

  selectPanel(panel: number): void {
    this.setState({selected: panel});
  }

  lastButtonText(): string {
    return (this.state.selected === 4 ? 'Close' : 'Next');
  }

  toggleImage(): void {
    this.setState({fullImage: !this.state.fullImage});
  }

  render() {
      return  <>
        <div className={this.props.learning ? 'modal-backdrop' : undefined}></div>
        {this.props.learning && !this.state.fullImage &&
        <div className='main' id='quick-tour'>
          <div className='title'>All of Us Researcher Workbench</div>
          <div className='intro'>Quick Tour</div>
          <div className='breadcrumbs'>
              {panels.map((p, i) => {
                  return <><div className='breadcrumb-component'>
                    <div className={'circle' + (i <= this.state.selected ? ' completed' : '')}
                         onClick={() => this.selectPanel(i)}>
                        {(i < this.state.selected) && <div className='check'>
                            <img src={this.checkImg}/>
                        </div>}
                        {(i ===  this.state.selected) && <div className='current'></div>}

                    </div>
                    <div className='breadcrumb-title'>{p.title}</div>
                    {(i !== this.state.numPanels - 1) &&
                    <div className={'connector' + ((i < this.state.selected) ? ' completed' : '')}>
                    </div>}
                  </div></>;
              })}
          </div>
          <div style={{width: '100%', paddingTop: '5%'}}>
             <div className='divider'></div>
          </div>
          <div className='panel'>
            <div className='panel-left'>
              <div className='panel-title'>{panels[this.state.selected].title}</div>
              <div className='panel-contents'>
                <div className='panel-text'>{panels[this.state.selected].content}</div>
              </div>
            </div>
            <div className='panel-right'>
              <img src={panels[this.state.selected].image} className='panel-image'/>
                {(this.state.selected !== 0) &&
                <div style={{position: 'absolute', right: '5%',
                    bottom: '5%', height: '1rem', width: '1rem'}}>
                    <div className='resize-icon' style={{position: 'absolute', zIndex: 2}}
                         onClick={() => this.toggleImage()}>
                        <img src={this.expandIcon}/>
                    </div>
                </div>}

            </div>
          </div>
          <div className='controls'>
            <div className='left'>
              {this.state.selected !== 0 &&
              <button type='button' className='btn btn-close' id='previous'
                      onClick={() => this.previous()}>Previous</button>}
            </div>
            <div className='right'>
              {this.state.selected !== (this.state.numPanels - 1) &&
              <button type='button' className='btn btn-close' id='close'
                      onClick={() => this.close()}>Close</button>}
              <button type='button' className='btn btn-primary' id='next'
                      onClick={() => this.next()}>{this.lastButtonText()}</button>
            </div>
          </div>
        </div>}
        {this.props.learning && this.state.fullImage && <div className='main full-image'>
          <div className='full-image-wrapper'>
            <img src={panels[this.state.selected].image} style={{height: '100%', width: '100%'}}/>
              <div className='resize-icon' onClick={() => this.toggleImage()}
                   style={{position: 'absolute', right: '5%', bottom: '5%'}}>
                  <img src={this.shrinkIcon}/>
              </div>
          </div>
        </div>}
      </>;
  }
}

@Component({
  selector: 'app-quick-tour-modal',
  styleUrls: ['./component.css'],
  templateUrl: './component.html'
})

export class QuickTourModalComponent implements DoCheck, OnInit {

  @Input('learning')
  learning: boolean;
  @Input('onClose')
  public onClose: Function;

  constructor() {}

  ngOnInit(): void {
    ReactDOM.render(React.createElement(QuickTourReact,
        {learning: this.learning, closeFunction: this.onClose}),
        document.getElementById('quick-tour'));
  }

  ngDoCheck(): void {
    ReactDOM.render(React.createElement(QuickTourReact,
        {learning: this.learning, closeFunction: this.onClose}),
        document.getElementById('quick-tour'));
  }



}
