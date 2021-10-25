import dotenv from 'dotenv';
dotenv.config({ path: '.env.base' });
dotenv.config({ path: process.env.DOTENV_CONFIG_PATH });
