const faker = require('faker/locale/en_US');

/**
 * Get fake user information.
 */
const fakeUser = {
  fname: faker.name.firstName(),
  lname: faker.name.lastName(),
  job: faker.name.jobTitle(),
  phone: faker.phone.phoneNumber(),
  company: faker.company.companyName(),
  word: faker.lorem.words(5),
  message: faker.lorem.words(),
  email: faker.internet.email(),
  state: faker.address.stateAbbr(),
  country: 'U.S.A'
};

/**
 * Get a lowercase random alphanumeric string
 */
const randomString = (len) => {
  let s = '';
  while (s.length < len) { s += Math.random().toString(36).substr(2, len - s.length); }
  return s.toLowerCase();
};

module.exports = {
  fakeUser,
  randomString
};
