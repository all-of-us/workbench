const faker = require('faker/locale/en_US');

const newUserName = `aoutestuser${Math.floor(Math.random() * 1000)}${Math.floor(Date.now() / 1000)}`;

export const defaultFieldValues = [{
  label: 'New Username',
  value: newUserName
}, {
  label: 'First Name',
  value: 'tester1'
}, {
  label: 'Last Name',
  value: 'Puppeteerdriver'
}, {
  label: 'Street Address 1',
  value: faker.address.streetName()
}, {
  label: 'City',
  value: faker.address.city()
}, {
  label: 'State',
  value: faker.address.stateAbbr()
}, {
  label: 'Zip code',
  value: faker.address.zipCode()
}, {
  label: 'Country',
  value: 'U.S.A'
},
];
