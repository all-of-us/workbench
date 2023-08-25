const { createProxyMiddleware } = require("http-proxy-middleware");

import { environment } from 'environments/environment';

module.exports = (app) => {
  // Local proxy for Core Tanagra endpoints
  app.use(
      '/v2',
      createProxyMiddleware({
        target: environment.tanagraApiUrl || '',
        changeOrigin: true,
      })
  );
};