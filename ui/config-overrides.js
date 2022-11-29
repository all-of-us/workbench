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

    // Remove noisy warning about compiled included code. Stolen from:
    // https://medium.com/tomincode/hiding-critical-dependency-warnings-from-webpack-c76ccdb1f6c1
    /*
    config.plugins.push(
      new webpack.ContextReplacementPlugin(
        /[/]ui[/]src[/]terraui[/]/,
        (data) => {
          const msg = 'require function is used in a way in which dependencies cannot be '
            +'statically extracted'
          const noiseIndex = data.dependencies.findIndex(d => d.critical === msg)
          if (noiseIndex !== -1) {
            // Must modify the object rather than return a modified copy or result is ignored.
            delete data.dependencies[noiseIndex].critical
          }
          return data;
        },
      ),
    )
    */

    return config;
  },
  jest: function(config) {
    config.testMatch = ["**/*.spec.tsx"];
    return config;
  }
}
