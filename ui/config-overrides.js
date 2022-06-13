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
    let environmentFilePath = 'environment.localtest.ts'
    if (process.env.REACT_APP_ENVIRONMENT) {
      environmentFilePath = `environment.${process.env.REACT_APP_ENVIRONMENT}.ts`
    }

    if (!config.plugins) {
      config.plugins = []
    }
    config.plugins.push(
        new webpack.NormalModuleReplacementPlugin(
            /src\/environments\/environment\.ts/,
            environmentFilePath
        )
    );
   config.optimization.minimizer = removeCssMinimizer(config.optimization.minimizer);

    return config;
  },
  jest: function(config) {
    config.testMatch = ["**/*.spec.tsx"];
    return config;
  }
}
