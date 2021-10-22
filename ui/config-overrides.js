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
    return config;
  },
  jest: function(config) {
    config.testMatch = ["**/*.spec.tsx"];
    return config;
  }
}
