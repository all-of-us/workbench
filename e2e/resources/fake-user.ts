const faker = require('faker/locale/en_US');

/**
 * Get a fake user information.
 */
export const fakeUser = {
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

export const makeString = (charLimit?: number) => {
  let loremStr = faker.lorem.paragraphs();
  if (charLimit === undefined) {
    return loremStr;
  }
  if (loremStr.length > charLimit) {
    loremStr = loremStr.slice(0, charLimit - 1);
  }
  return loremStr
};
