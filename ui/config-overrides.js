const webpack = require('webpack');

module.exports = function override(config, env) {
  let environmentFilePath = 'environment.localtest.ts'
  if (process.env.REACT_APP_ENVIRONMENT === 'local' ) {
    environmentFilePath = 'environment.local.ts'
  } else if (process.env.REACT_APP_ENVIRONMENT === 'localtest') {
    environmentFilePath = 'environment.localtest.ts'
  } else if (process.env.REACT_APP_ENVIRONMENT === 'test') {
    environmentFilePath = 'environment.test.ts'
  } else if (process.env.REACT_APP_ENVIRONMENT === 'staging') {
    environmentFilePath = 'environment.staging.ts'
  } else if (process.env.REACT_APP_ENVIRONMENT === 'stable') {
    environmentFilePath = 'environment.stable.ts'
  } else if (process.env.REACT_APP_ENVIRONMENT === 'perf') {
    environmentFilePath = 'environment.perf.ts'
  } else if (process.env.REACT_APP_ENVIRONMENT === 'preprod') {
    environmentFilePath = 'environment.preprod.ts'
  } else if (process.env.REACT_APP_ENVIRONMENT === 'prod') {
    environmentFilePath = 'environment.prod.ts'
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
}
