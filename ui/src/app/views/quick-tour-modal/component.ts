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

  setPanels(): void {
    this.panelTitles = ['Intro', 'Workspaces', 'Concepts',
      'Cohorts', 'Notebooks'];
    this.panels.set(this.panelTitles[0],
      'Welcome to the All of Us Workbench. This space contains a variety ' +
      'of cloud-based tools that will allow you to access and analyze program ' +
      'data.');
    this.panelImages.set(this.panelTitles[0], '/assets/images/intro.png');
    this.panels.set(this.panelTitles[1],
      'A workspace is your place to store and analyze data for a specific ' +
      'project. Each workspace is a separate Google bucket that serves as a ' +
      'dedicated space for file storage. You can share this workspace with other' +
      'users, allowing them to view or edit your work.');
    this.panelImages.set(this.panelTitles[1], '/assets/images/workspaces.png');
    this.panels.set(this.panelTitles[2],
      'Medical concepts are similar to medical terms; they describe information ' +
      'in a patient\'s medical record, such as a condition they have, a doctor\'s ' +
      'diagnosis, a prescription they are taking, or a procedure or measurement ' +
      'the doctor performed. In the Workbench we refer to conditions, procedures, ' +
      'drugs, measurements as domains.');
    this.panelImages.set(this.panelTitles[2], '/assets/images/concepts.png');
    this.panels.set(this.panelTitles[3],
      'The cohort builder allows you to create, review, and annotate groups of ' +
      'study participants, called "cohorts", without programming knowledge. You ' +
      'can use the cohort builder to include or exclude participants who selected ' +
      'specific answers to survey questions or participants with select physical ' +
      'measurements or demographics.');
    this.panelImages.set(this.panelTitles[3], '/assets/images/cohorts.png');
    this.panels.set(this.panelTitles[4],
      'A Notebook is where you will perform analyses on the cohort(s) that you ' +
      'created in the cohort builder using the concept set(s) you created in ' +
      'concept set builder.');
    this.panelImages.set(this.panelTitles[4], '/assets/images/notebooks.png');
  }

}
