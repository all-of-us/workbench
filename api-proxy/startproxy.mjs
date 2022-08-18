import {startServer} from './src/server.mjs'

startServer(process.env.PROXY_PORT).catch(e => { console.error(e); process.exit(1) })
