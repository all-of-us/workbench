import {Component, OnInit} from '@angular/core';

@Component({
  selector: 'app-quick-tour-modal',
  styleUrls: ['./component.css'],
  templateUrl: './component.html'
})

export class QuickTourModalComponent implements OnInit {

  panelTitles: String[];
  panels = new Map<String, String>();
  panelImages = new Map<String, String>();
  checkImg = '/assets/images/check.svg';
  selected: String;
  completed: String[];
  selectedIndex: number;
  learning = false;
  fullImage = false;

  constructor() {}

  ngOnInit(): void {
    this.setPanels();
    this.selectedIndex = 0;
    this.selected = this.panelTitles[this.selectedIndex];
  }

  open(): void {
    this.learning = true;
  }

  close(): void {
    this.learning = false;
  }

  previous(): void {
    this.selectedIndex = this.selectedIndex - 1;
    this.completed = this.panelTitles.slice(0, this.selectedIndex);
    this.selected = this.panelTitles[this.selectedIndex];
  }

  next(): void {
    this.completed = this.panelTitles.slice(0, this.selectedIndex);
    this.selectedIndex = this.selectedIndex + 1;
    this.selected = this.panelTitles[this.selectedIndex];
  }

  selectPanel(panel: string): void {
    this.selectedIndex = this.panelTitles.indexOf(panel);
    this.completed = this.panelTitles.slice(0, this.selectedIndex);
    this.selected = panel;
  }

  selectedTitle(title: string) {
    if (title === 'Intro') {
      return 'Introduction';
    } else {
      return title;
    }
  }

  toggleImage(): void {
    this.fullImage = !this.fullImage;
  }

  setPanels(): void {
    this.panelTitles = ['Intro', 'Workspaces', 'Cohorts', 'Concepts',
      'Notebooks'];
    this.panels.set(this.panelTitles[0],
      'Welcome to the All of Us Research Workbench!\n\nAll workbench analyses ' +
      'happen in a “Workspace.” Within a Workspace you can select participants ' +
      'using the “Cohort Builder” tool.  Another tool, the “Concept Set Builder,” ' +
      'allows you to select data types for analysis.  The cohorts and concept sets ' +
      'you make can then be accessed from “Notebooks,” the analysis environment.\n\n' +
      'For illustration, let\'s consider research on "Type 2 diabetes" for this quick tour.');
    this.panelImages.set(this.panelTitles[0], '/assets/images/intro.png');
    this.panels.set(this.panelTitles[1],
      'A Workspace is your place to store and analyze data for a specific project. ' +
      'You can share this Workspace with other users, allowing them to view or edit ' +
      'your work. The dataset referenced by a workspace is in ' +
      '<a class="link" href="https://www.ohdsi.org/data-standardization/the-common-data-model/" ' +
      'target="_blank">OMOP common data model</a> format. Here are some ' +
      '<a class="link" href="https://www.ohdsi.org/past-events/2017-tutorials-omop-common-data' +
      '-model-and-standardized-vocabularies/" target="_blank">tutorials</a> to ' +
      'understand OMOP data model.\n\nWhen you create your Workspace, you will be prompted ' +
      'to state your research purpose.  For example, when you create a Workspace to study Type ' +
      '2 Diabetes, for research purpose you could enter: “I will use this Workspace to ' +
      'investigate the impact of Geography on use of different medications to treat Type 2 ' +
      'Diabetes.”');
    this.panelImages.set(this.panelTitles[1], '/assets/images/workspaces.png');
    this.panels.set(this.panelTitles[2],
      'A “Cohort” is a group of participants you are interested in researching. The Cohort ' +
      'Builder allows you to create and review cohorts and annotate participants in your study ' +
      'group.\n\nFor example, you can build a Cohort called “diabetes cases,” to include people ' +
      'who have been diagnosed with type II diabetes, using a combination of billing codes and ' +
      'laboratory values. You can also have a “controls” Cohort. Once you build your cohorts, ' +
      'you can go through and manually review the records for each participant and decide if ' +
      'you want to include or exclude them from your Cohort and make specific ' +
      'annotations/notes to each record.');
    this.panelImages.set(this.panelTitles[2], '/assets/images/cohorts.png');
    this.panels.set(this.panelTitles[3],
      'Concepts describe information in a patient’s medical record, such as a condition they ' +
      'have, a  prescription they are taking or their physical measurements. In the Workbench we ' +
      'refer to subject areas such as conditions, drugs, measurements etc. as “domains.” You can ' +
      'search for and save collections of concepts from a particular domain as a “Concept Set.” ' +
      '\n\n' +
      'For example, if you want to select height, weight and blood pressure information ' +
      '(concepts) from your “diabetes cases” Cohort, you can search for the 3 concepts ' +
      'from the “Measurements” domain and call it “biometrics” Concept Set. You can then ' +
      'use Notebooks to extract that information from your cohort.');
    this.panelImages.set(this.panelTitles[3], '/assets/images/concepts.png');
    this.panels.set(this.panelTitles[4],
      'A Notebook is a computational environment where you can analyze data with basic ' +
      'programming knowledge in R or Python. Several template Notebooks and resources ' +
      'are available within your Workspace that will guide you how to import your ' +
      'Cohort(s) and Concept Set(s) into the ' +
      'Notebook and can assist with basic analyses. \n\nFor example, you can launch a Notebook ' +
      'to import your “diabetes cases” Cohort and then select your “biometrics” Concept Set, to ' +
      'get biometrics data for the participants in your Cohort. You can then analyze the data to ' +
      'study correlation between hypertension and diabetes.');
    this.panelImages.set(this.panelTitles[4], '/assets/images/notebooks.png');
  }

}
