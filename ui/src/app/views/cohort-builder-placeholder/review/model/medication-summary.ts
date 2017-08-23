export class MedicationSummary {
  constructor(public drugName = '',
              public mentions = '',
              public firstMention = '',
              public lastMention = '') {}
}
