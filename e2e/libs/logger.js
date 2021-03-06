const winston = require('winston');
const { createLogger, format } = winston;

const options = {
  console: {
    handleExceptions: true,
    prettyPrint : true,
    json: false,
  }
};

// Used by jest-custom-reporter to read Jest console messages and save to file.
const fileLogger = createLogger({
  level: process.env.LOG_LEVEL || "info",
  format: format.combine(
    format.printf( (info) => {return `${info.message}`; })
  ),
  exitOnError: false
});

// Log timestamp, log level and message to Console.
const logger = createLogger({
  level: process.env.LOG_LEVEL || "info",
  format: format.combine(
    format.prettyPrint(),
    format.splat(),
    format.timestamp({ format: 'YYYY-MM-DD HH:mm:ss' }),
    format.printf( (info) => {
      return `${info.level.toUpperCase()}: [${info.timestamp}] - ${info.message}`;
    }),
  ),
  transports: [
    new winston.transports.Console(options.console),
  ],
  exitOnError: false
});

module.exports = { logger, simpleLogger: fileLogger}
