import { Injectable } from '@angular/core';

@Injectable()
export class TooltipService {
  ageChartHelpText = 'The age at occurrence bar chart provides a binned age \n' +
    'distribution for participants at the time the medical concept ' +
    'being queried occurred in their records. \n' +
    'If an individual’s record contains more than one mention of a concept, \n' +
    'the age at occurrence is included for each mention. \n' +
    'As a result, a participant may be counted more ' +
    'than once in the distribution. ';
  sourcesChartHelpText = 'Individual health records often contain medical ' +
    'information that means the same thing ' +
    'but may be recorded in many different ways. \n' +
    'The sources represent the many different ways that the standard medical concept ' +
    'returned in the search results has been recorded in patient records. \n' +
    'The sources bar chart provides the top 10 source concepts from the All of Us data.';
  matchingConceptsHelptText = 'Medical concepts are similar to medical terms; ' +
    'they capture medical information\n' +
    'in an individual’s records and may sometimes have values associated with them.\n' +
    'For example, “height” is a medical concept that has a measurement value (in centimeters).\n' +
    'These concepts are categorized into different domains. ' +
    'Domains are types of medical information.\n' +
    'The Data Browser searches the All of Us public data for medical concepts that\n' +
    'match the keyword or code entered in the search bar.\n' +
    'The Data Browser counts how many participants have at least\n' +
    'one mention of the matching medical concepts in their records.\n' +
    'Matching medical concepts that have the highest participant counts ' +
    'are returned at the top of the list.';
  constructor() { }

}
