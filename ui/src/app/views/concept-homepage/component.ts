import {Component, OnInit, ViewChild} from '@angular/core';


import {ConceptTableComponent} from 'app/views/concept-table/component';
import {ConceptAddModalComponent} from 'app/views/concept-add-modal/component';


@Component({
  styleUrls: ['../../styles/buttons.css',
    '../../styles/cards.css',
    '../../styles/headers.css',
    '../../styles/inputs.css',
    '../../styles/errors.css',
    './component.css'],
  templateUrl: './component.html',
})
export class ConceptHomepageComponent implements OnInit {
  searchTerm: string;
  standardConceptsOnly = false;
  searching = false;
  selectedDomain: string;
  addTextHovering = false;

  @ViewChild(ConceptTableComponent)
  conceptTable: ConceptTableComponent;

  @ViewChild(ConceptAddModalComponent)
  conceptAddModal: ConceptAddModalComponent;

  conceptDomainList = [
    {
      name: 'Conditions',
      conceptCount: 22668,
      participantCount: 946260,
    },
    {
      name: 'Drugs',
      conceptCount: 14325,
      participantCount: 353254,
    },
    {
      name: 'Measurements',
      conceptCount: 14325,
      participantCount: 353254,
    },
    {
      name: 'Procedures',
      conceptCount: 14325,
      participantCount: 353254,
    },
    {
      name: 'Demographics',
      conceptCount: 14325,
      participantCount: 353254,
    },
    {
      name: 'Visits',
      conceptCount: 14325,
      participantCount: 353254,
    },
  ];

  concepts = [
    {
      name: "non-small cell lung cancer",
      synonyms: [
        "lung cancer",
        "malignant tumor",
        "carcinoma of the lung"
      ],
      code: 25463,
      vocabulary: "SNOMED",
      count: 2596
    },
    {
      name: "Epidermal growth factor receptor positive non-small cell lung cancer",
      synonyms: [],
      code: 42696,
      vocabulary: "SNOMED",
      count: 209
    }
  ];

  constructor(
  ) {}

  ngOnInit(): void {}

  openAddModal(): void {
    this.conceptAddModal.open();
  }

  selectDomain(domain: string) {
    this.selectedDomain = domain;
  }


}
