import {Component, OnInit} from '@angular/core';

@Component({
  selector: 'app-quick-tour-modal',
  styleUrls:['./component.css'],
  templateUrl: './component.html'
})

export class QuickTourModalComponent implements OnInit {

  panelTitles: String[];
  panels = new Map<String, String>();
  checkImg = '/assets/images/check.svg';
  selected: String;
  completed: String[];
  selectedIndex: number;
  learning = false;

  constructor(){}

  ngOnInit(): void {
    this.panelTitles = ["Intro", "Workspaces", "Concepts",
      "Cohorts", "Notebooks"];
    this.panels.set("Intro", "Lots of content here");
    this.panels.set("Workspaces", "Lots more content here");
    this.panels.set("Concepts", "Even more content");
    this.panels.set("Cohorts", "Still more content");
    this.panels.set("Notebooks", "Finally, last of the content");
    this.selected = "Intro";
    this.selectedIndex = 0;
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

}
