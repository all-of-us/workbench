node -e "import('./src/server.mjs').then(m => m.startServer($PROXY_PORT)).catch(e => console.error(e))"
