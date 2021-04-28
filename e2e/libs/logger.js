const winston = require('winston');
const util = require('util');

const combineMessageAndSplat = () => {
  return {
    transform: (info, _opts) => {
      //combine message and args if any
      info.message = util.format(info.message, ...(info[Symbol.for('splat')] || []));
      return info;
    }
  };
};

const timeNow = () => {
  return new Date().toLocaleString('en-US', {
    timeZone: 'America/New_York',
    hour12: false
  });
};

// Log to Console.
const logger = winston.createLogger({
  level: process.env.LOG_LEVEL || 'info',
  format: winston.format.combine(
    combineMessageAndSplat(),
    winston.format.timestamp({ format: timeNow }),
    winston.format.printf((info) => {
      return `[${info.timestamp}] - ${info.message}`;
    })
  ),
  transports: [
    new winston.transports.Console({
      level: process.env.LOG_LEVEL || 'info',
      handleExceptions: true
    })
  ],
  exitOnError: false
});

module.exports = { logger };
