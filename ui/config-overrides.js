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

    // there are 2 active minimizers:
    // * TerserPlugin for JS - this works and reduces our size by ~ 2/3
    // * CSSMnimizerPlugin - this has bug when trying to minimize clr-min-ui.css (b/c already minimized?)
    //    see https://github.com/all-of-us/workbench/pull/6753#issuecomment-1150407055
    //    we re-init this plugin by excluding the problematic file from its config

    config.optimization.minimizer = config.optimization.minimizer.map(m =>
      // this matches TerserPlugin - keep it as-is 
      m.options.minimizer.options.parse?.ecma 
        ? m 
        : new CssMinimizerPlugin({ exclude: /clr-ui\.min\.css$/i })
    );

    return config;
  },
  jest: function(config) {
    config.testMatch = ["**/*.spec.tsx"];
    return config;
  }
}
