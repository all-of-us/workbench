const assert = require('assert').strict
const jwt = require('jsonwebtoken')
const {Right, Left, Task, ...u} = require('./utils')

const export_ = o => Object.keys(o).forEach(k => { module.exports[k] = o[k] })

const nl = '\n'

const nows = () => Math.floor(Date.now() / 1000)

const formatResHead = res => `${res.statusCode} ${res.statusMessage}\n`
  +Object.keys(res.headers).map(k => `${k}: ${res.headers[k]}`).join('\n')

const getSaBearerToken = serviceAccountKeyJson =>
  Task.of(serviceAccountKeyJson)
  .map(u.parseJson).chain(Task.fromEither).chain(saKey => {
    // Scopes copied from what `gcloud auth print-access-token` gets.
    const scope = `
      https://www.googleapis.com/auth/cloud-platform
      https://www.googleapis.com/auth/compute
      https://www.googleapis.com/auth/appengine.admin
      https://www.googleapis.com/auth/userinfo.email
      openid`.trim().split(/\s+/).join(' ')
    const payload = {
      iss: saKey.client_email,
      scope,
      aud: saKey.token_uri,
      iat: nows(),
      exp: nows() + 3600,
    }
    const signedJwt = jwt.sign(payload, saKey.private_key, {algorithm: 'RS256'})
    const grant_type = 'urn:ietf:params:oauth:grant-type:jwt-bearer'
    return u.httpReq(saKey.token_uri, {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({assertion: signedJwt, grant_type })
    })
  })
  .chain(u.readStream).map(u.parseJson).chain(Task.fromEither)
  // odd choice here by Google to end these with a stream of periods, but stripping seams fine
  .map(x => ({...x, access_token: x.access_token.replace(/[.]+$/, '')}))
export_({getSaBearerToken})

if (require.main === module) {
  // assuming launched as `node script.js`
  const args = process.argv.slice(2)
  if (args.length < 2) {
    console.log('usage: node script.js service-account-key-json')
    process.exit(1)
  }
  getSaBearerToken(args[0]).fork(
    e => { console.error(e); process.exit(1) },
    console.log
  )
}
