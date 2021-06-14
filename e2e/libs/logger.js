const winston = require('winston');

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
    winston.format.prettyPrint(),
    winston.format.splat(),
    winston.format.timestamp({ format: timeNow }),
    winston.format.printf((info) => {
      return `[${info.timestamp}] - ${info.message}`;
    })
  ),
  transports: [new winston.transports.Console({ handleExceptions: true })],
  exitOnError: false
});

module.exports = { logger };
