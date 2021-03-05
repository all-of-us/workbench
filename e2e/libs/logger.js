const winston = require('winston');

// Only log message (no timestamp and log level) to Console.
const simpleLogger = winston.createLogger({
  level: 'info',
  format: winston.format.combine(
    winston.format( (info) => {
      info.level = info.level.toUpperCase();
      return info;
    })(),
    winston.format.printf( (info) => {return `${info.message}`; }),
  ),
  transports: [
    new winston.transports.Console({handleExceptions: true})
  ],
  exitOnError: false
});

// Default: Log timestamp, log level and message to Console.
const logger = winston.createLogger({
  level: 'info',
  format: winston.format.combine(
    winston.format.splat(),
    winston.format( (info) => {
      info.level = info.level.toUpperCase();
      return info;
    })(),
    winston.format.timestamp({ format: 'YYYY-MM-DD HH:mm:ss:ms' }),
    winston.format.printf( (info) => {return `[${info.timestamp}] ${info.level} -- ${info.message}`; }),
  ),
  transports: [
    new winston.transports.Console({handleExceptions: true})
  ],
  exitOnError: false
});

module.exports = { logger, simpleLogger}
