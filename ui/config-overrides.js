const webpack = require('webpack');

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

    // removes the last minimizer CssMinimizerPlugin() which has a bug
    // see https://github.com/all-of-us/workbench/pull/6753#issuecomment-1150407055
    config.optimization.minimizer = config.optimization.minimizer.filter(m => m.options.minimizer.options.parse?.ecma);

    return config;
  },
  jest: function(config) {
    config.testMatch = ["**/*.spec.tsx"];
    return config;
  }
}
