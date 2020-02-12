const faker = require('faker/locale/en_US');

/**
 * Get a fake user information.
 */
const fakeUser = {
  fname: faker.name.firstName(),
  lname: faker.name.lastName(),
  job: faker.name.jobTitle(),
  phone: faker.phone.phoneNumber(),
  company: faker.Company.companyName,
  word: faker.lorem.words(5),
  message: faker.random.words(),
  email: faker.internet.email(),
  state: faker.Address.state_abbr(),
  country: 'U.S.A'
};


module.exports = {
  fakeUser,

};
