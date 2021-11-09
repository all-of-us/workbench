require('dotenv').config({ path: process.env.DOTENV_CONFIG_PATH });

console.log('');

// Note: If DOTENV_CONFIG_PATH is undefined, ".env" config file is used.
[
  'DOTENV_CONFIG_PATH',
  'USER_NAME',
  'READER_USER',
  'WRITER_USER',
  'COLLABORATOR_USER',
  'ACCESS_TEST_USER',
  'ADMIN_TEST_USER',
  'EGRESS_TEST_USER'
].forEach((k) => {
  console.log(`${k}: ${process.env[k]}`);
});
console.log('');
