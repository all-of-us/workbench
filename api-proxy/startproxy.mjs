import {startServer} from './src/server.mjs'

startServer(...process.argv.slice(2)).catch(e => { console.error(e); process.exit(1) })
