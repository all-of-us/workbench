require('dotenv').config({ path: process.env.DOTENV_CONFIG_PATH });

// Note: If DOTENV_CONFIG_PATH is undefined, ".env" config file is used.
console.log('');
console.log(`DOTENV_CONFIG_PATH: ${process.env.DOTENV_CONFIG_PATH}`);

const u = process.env.USER_NAME.split('@');
console.log(`USER_NAME: ${u}`);

const r = process.env.READER_USER.split('@');
console.log(`READER_USER: ${r}`);

const w = process.env.WRITER_USER.split('@');
console.log(`WRITER_USER: ${w}`);

const c = process.env.COLLABORATOR_USER.split('@');
console.log(`COLLABORATOR_USER: ${c}`);

const a = process.env.ACCESS_TEST_USER.split('@');
console.log(`ACCESS_TEST_USER: ${a}`);
console.log('');
