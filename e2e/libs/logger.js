const winston = require('winston');
const { createLogger, format } = winston;

// Log to Console.
const logger = createLogger({
  level: process.env.LOG_LEVEL || 'info',
  format: format.combine(
    format.prettyPrint(),
    format.splat(),
    format.timestamp({ format: 'YYYY-MM-DD HH:mm:ss' }),
    format.printf((info) => {
      return `${info.level.toUpperCase()}: [${info.timestamp}] - ${info.message}`;
    })
  ),
  transports: [new winston.transports.Console({ handleExceptions: true })],
  exitOnError: false
});

module.exports = { logger };
