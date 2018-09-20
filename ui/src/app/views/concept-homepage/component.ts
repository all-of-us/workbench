import {Component, OnInit} from '@angular/core';


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

  constructor(
  ) {}

  ngOnInit(): void {}
}
