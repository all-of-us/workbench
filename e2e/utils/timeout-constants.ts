const oneMinute = 60 * 1000; // in milliseconds

// a timeout appropriate for environment creation
export const environmentTimeout = 15 * oneMinute;

// a timeout appropriate for tests which need to create two Jupyter runtimes but also do other things
export const twoRuntimesTimeout = 3 * environmentTimeout;
