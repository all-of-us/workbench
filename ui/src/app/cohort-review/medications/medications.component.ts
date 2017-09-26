import { Component, OnInit } from '@angular/core';
import { MedicationSummary } from '../model';

@Component({
  selector: 'app-medications',
  templateUrl: './medications.component.html',
  styleUrls: ['./medications.component.css']
})
export class MedicationsComponent implements OnInit {

  public medicationSummary: MedicationSummary[];

  constructor() { }

  ngOnInit() {
    this.medicationSummary = [new MedicationSummary('ASA', '9', '2002-12-14', '2006-11-05'),
      new MedicationSummary('Albuterol', '1', '1997-02-15', '1997-02-15'),
      new MedicationSummary('Altace', '8', '2004-02-17', '2006-09-16'),
      new MedicationSummary('Altace', '8', '2004-02-17', '2006-09-16'),
      new MedicationSummary('Altace', '8', '2004-02-17', '2006-09-16'),
      new MedicationSummary('Altace', '8', '2004-02-17', '2006-09-16'),
      new MedicationSummary('Altace', '8', '2004-02-17', '2006-09-16'),
      new MedicationSummary('Altace', '8', '2004-02-17', '2006-09-16'),
      new MedicationSummary('Altace', '8', '2004-02-17', '2006-09-16'),
      new MedicationSummary('Altace', '8', '2004-02-17', '2006-09-16'),
      new MedicationSummary('Altace', '8', '2004-02-17', '2006-09-16'),
      new MedicationSummary('Altace', '8', '2004-02-17', '2006-09-16'),
      new MedicationSummary('Altace', '8', '2004-02-17', '2006-09-16'),
      new MedicationSummary('Altace', '8', '2004-02-17', '2006-09-16'),
      new MedicationSummary('Altace', '8', '2004-02-17', '2006-09-16'),
      new MedicationSummary('Altace', '8', '2004-02-17', '2006-09-16'),
      new MedicationSummary('Altace', '8', '2004-02-17', '2006-09-16'),
      new MedicationSummary('Altace', '8', '2004-02-17', '2006-09-16'),
      new MedicationSummary('Altace', '8', '2004-02-17', '2006-09-16'),
      new MedicationSummary('Altace', '8', '2004-02-17', '2006-09-16'),
      new MedicationSummary('Altace', '8', '2004-02-17', '2006-09-16'),
      new MedicationSummary('Altace', '8', '2004-02-17', '2006-09-16'),
      new MedicationSummary('Altace', '8', '2004-02-17', '2006-09-16'),
      new MedicationSummary('Altace', '8', '2004-02-17', '2006-09-16'),
      new MedicationSummary('Altace', '8', '2004-02-17', '2006-09-16'),
      new MedicationSummary('Altace', '8', '2004-02-17', '2006-09-16'),
      new MedicationSummary('Altace', '8', '2004-02-17', '2006-09-16'),
      new MedicationSummary('Altace', '8', '2004-02-17', '2006-09-16'),
      new MedicationSummary('Altace', '8', '2004-02-17', '2006-09-16'),
      new MedicationSummary('Altace', '8', '2004-02-17', '2006-09-16'),
      new MedicationSummary('Altace', '8', '2004-02-17', '2006-09-16'),
      new MedicationSummary('Altace', '8', '2004-02-17', '2006-09-16'),
      new MedicationSummary('Altace', '8', '2004-02-17', '2006-09-16'),
      new MedicationSummary('Altace', '8', '2004-02-17', '2006-09-16'),
      new MedicationSummary('Altace', '8', '2004-02-17', '2006-09-16')];
  }

}
