const faker = require('faker/locale/en_US');
const configs = require('../workbench-config');

export const defaultFields = [{
  label: 'Disease-focused research',
  value: faker.random.word()
}, {
  label: 'Methods development/validation study',
  value: 'tester1'
}, {
  label: 'Last Name',
  value: 'Puppeteerdriver'
}, {
  label: 'Email Address',
  value: configs.registrationContactEmail
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
  label: 'Zip Code',
  value: faker.address.zipCode()
}, {
  label: 'Country',
  value: 'U.S.A'
},
];
