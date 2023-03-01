const path = require('path')

require('esbuild').build({
  loader: { '.jpg': 'file', },
  bundle: true,
  format: 'esm',
  outdir: '../out', // relative to repo directory
  // Without a sourcemap, browser debugging information is garbage.
  sourcemap: true,
  plugins: [
    require('esbuild-plugin-svgr')(),
    require('esbuild-plugin-alias')({
      'src/configStore': path.resolve('src/configStoreEmpty.js'),
    }),
  ],
  external: ['react', 'lodash'],
  entryPoints: ['src/pages/Environments.js'],
}).catch(e => { process.exit(1) })
// Errors are reported already, so no need to log e.
