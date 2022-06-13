const webpack = require('webpack');
const CssMinimizerPlugin = require('css-minimizer-webpack-plugin');

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

    config.optimization.minimizer = config.optimization.minimizer.map(m =>
      // CssMinimizerPlugin is unable to minimize clr-min-ui.css (b/c already minimized?)
      // so we exclude that file
      // see https://github.com/all-of-us/workbench/pull/6753#issuecomment-1150407055
      m.options.minimizer.options.parse?.ecma ? new CssMinimizerPlugin({ exclude: /clr-ui\.min.\.css$/i }) : m
    );

    return config;
  },
  jest: function(config) {
    config.testMatch = ["**/*.spec.tsx"];
    return config;
  }
}
