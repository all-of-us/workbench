import * as fs from 'fs'
import * as nu from 'util'

const create = logPath => {
  const logStream = logPath ? fs.createWriteStream(logPath) : process.stderr
  console.log('logging to: '+(logPath || 'stderr'))
  let lastStamp = -Infinity

  const truncate = ms => Math.floor(ms / 60e3) * 60e3

  const needsStamp = nowMs => lastStamp + 60e3 < nowMs

  const maybeStamp = nowMs => {
    if (needsStamp(nowMs)) {
      logStream.write(new Date(nowMs).toISOString().slice(0, 16)+'Z\n')
      lastStamp = truncate(nowMs)
    }
  }

  const log = (...formatArgs) => {
    const nowMs = Date.now()
    maybeStamp(nowMs)
    const stampStart = truncate(nowMs)
    const delta = nowMs - stampStart
    logStream.write('+'+delta.toString().padEnd(6, ' '))
    logStream.write(nu.format(...formatArgs)+'\n')
  }
  return {log}
}

export default create
