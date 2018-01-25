import * as d3 from 'd3';
import {ChartInfo} from 'generated';


function makeCounter(arr) {
  return arr.reduce((counter, prop) => {
    counter[prop] = 0;
    return counter;
  }, {});
}

export default class ChartInfoContainer {
  raw: ChartInfo[];
  races: Array<ChartInfo['race']>;
  genders: Array<ChartInfo['gender']>;
  ageRanges: Array<ChartInfo['ageRange']>;

  /*
   * At construction, the unique set of races, genders, and ageRanges present
   * in the dataset is computed and stored, with any decoding that may be necessary.
   * The raw data set is also stored.
   */
  constructor(data) {
    this.raw = data;
    const races = new Set();
    const genders = new Set();
    const ageRanges = new Set();

    data.forEach(({race, gender, ageRange}) => {
      races.add(race);
      genders.add(this.decode(gender));
      ageRanges.add(ageRange);
    });

    this.races = Array.from(races.values());
    this.genders = Array.from(genders.values());
    this.ageRanges = Array.from(ageRanges.values());
  }

  /*
   * Helper function to translate not-very-human-readable codes into more human
   * readable representations
   */
  decode(code: string): string {
    return { 'M': 'Male', 'F': 'Female'}[code];
  }

  /**
   * Returns a tabular type of view on the data, suitable for feeding to d3.stack
   * Each row is an object with race to count maps and the gender/ageRange as key,
   * and annotated with a total.
   * Example row: {Asian: 12, White: 12, total: 24, genderAgeRange: 'Female > 65'}
   */
  asStack() {
    const genderXage = d3.cross(this.genders, this.ageRanges);
    const stacked = genderXage.map(([gender, ageRange]) => {
      const counter = makeCounter(this.races);
      const genderAgeRange = `${gender} ${ageRange}`;
      let total = 0;
      this.raw
        .filter(d => d.ageRange === ageRange)
        .filter(d => this.decode(d.gender) === gender)
        .forEach(({race, count}) => {
          total += count;
          counter[race] += count;
        });
      return {genderAgeRange, total, ...counter};
    });

    return stacked;
  }

  /*
   * Returns a mapping {prop value => count} for the given property,
   * distributed across all other properties in the chart info set
   */
  withTotals(prop: keyof ChartInfo) {
    const arr = this[prop + 's'];
    const counts = makeCounter(arr);
    arr.forEach(value => {
      this.raw.filter(d => d[prop] === value)
        .forEach(({count}) => counts[value] += count);
    });
    return counts;
  }
}
