const faker = require('faker/locale/en_US');

export const makeString = (charLimit?: number) => {
  let str = faker.lorem.paragraphs();
  if (str.length > charLimit) {
    str = str.slice(0, charLimit - 1);
  }
  return str
};
