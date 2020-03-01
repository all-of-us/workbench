const faker = require('faker/locale/en_US');
const configs = require('../workbench-config');

const newUserName = `aoutestuser${Math.floor(Math.random() * 1000)}${Math.floor(Date.now() / 1000)}`;

const defaultFieldValues = [{
  label: 'New Username',
  value: newUserName
}, {
  label: 'First Name',
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


module.exports = {
  defaultFieldValues,
};
