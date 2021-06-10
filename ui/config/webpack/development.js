process.env.NODE_ENV = process.env.NODE_ENV || 'development';

const environment = require('./environment');

environment.config.merge({
  devServer: {
    hot: false,
    inline: false,
    liveReload: false
  }
});

module.exports = environment.toWebpackConfig();
