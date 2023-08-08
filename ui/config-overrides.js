const webpack = require('webpack');

// removes the minimizer CssMinimizerPlugin() which has a bug
// see https://github.com/all-of-us/workbench/pull/6753#issuecomment-1150407055
const removeCssMinimizer = (original) => {
  const filtered = original.filter(m => m.options.minimizer.options.parse?.ecma);
  const minimizersRemoved = original.length = filtered.length;

  if (minimizersRemoved !== 1) {
    throw new Error(`expected to remove exactly one minimizer (CssMinimizerPlugin), actually removed ${minimizersRemoved}`);
  }

  return filtered;
}

module.exports = {
  webpack: function(config, env) {
    if (!process.env.REACT_APP_ENVIRONMENT) {
      throw new Error(`REACT_APP_ENVIRONMENT is not set. It must be set in order to use yarn start or dev-up. For example REACT_APP_ENVIRONMENT=local yarn dev-up`);
    }

    if (!config.plugins) {
      config.plugins = []
    }
    config.plugins.push(
        new webpack.NormalModuleReplacementPlugin(
            /src\/environments\/environment\.ts/,
            `environment.${process.env.REACT_APP_ENVIRONMENT}.ts`
        )
    );
    
    config.optimization.minimizer = removeCssMinimizer(config.optimization.minimizer);

    // Workaround for:
    /*
      ERROR in ./node_modules/@terra-ui-packages/components/lib/es/index.js 1:0-44
      Module not found: Error: Can't resolve './Interactive' in '/home/dmohs/b/r/aou/interactive/ui/node_modules/@terra-ui-packages/components/lib/es'
      Did you mean 'Interactive.js'?
      BREAKING CHANGE: The request './Interactive' failed to resolve only because it was resolved as fully specified
      (probably because the origin is strict EcmaScript Module, e. g. a module with javascript mimetype, a '*.mjs' file, or a '*.js' file where the package.json contains '"type": "module"').
      The extension in the request is mandatory for it to be fully specified.
      Add the extension to the request.
    */
    config.module.rules[1].oneOf.forEach(rule => {
      if (rule.loader && rule.loader.includes('babel-loader')) {
        rule.resolve = { fullySpecified: false }
      }
    })

    // Useful for debugging if you're fast with Ctrl-C and can hit it before the screen is cleared.
    // console.dir(config.module.rules, {depth:10})

    return config;
  },
  jest: function(config) {
    config.testMatch = ["**/*.spec.tsx"];
    return config;
  }
}
