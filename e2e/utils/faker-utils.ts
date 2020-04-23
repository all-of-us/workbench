const faker = require('faker/locale/en_US');

/**
 * Get a fake user information.
 */
export const user = {
  fname: faker.name.firstName(),
  lname: faker.name.lastName(),
  job: faker.name.jobTitle(),
  phone: faker.phone.phoneNumber(),
  company: faker.company.companyName(),
  word: faker.lorem.words(5),
  message: faker.random.words(),
  email: faker.internet.email(),
  state: faker.address.stateAbbr(),
  country: 'U.S.A'
};
