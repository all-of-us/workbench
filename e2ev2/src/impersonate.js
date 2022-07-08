const assert = require('assert').strict
const jwt = require('jsonwebtoken')
const {getSaBearerToken} = require('./sa-bearer')
const {Right, Left, Task, ...u} = require('./utils')

const export_ = o => Object.keys(o).forEach(k => { module.exports[k] = o[k] })

const nl = '\n'

const nows = () => Math.floor(Date.now() / 1000)

const formatResHead = res => `${res.statusCode} ${res.statusMessage}\n`
  +Object.keys(res.headers).map(k => `${k}: ${res.headers[k]}`).join('\n')

const getBearerTokenForUser = (saBearerToken, projectName, user) => {
  const  headers = {
    'Authorization': `Bearer ${saBearerToken}`,
    'Content-Type': 'application/json'
  }
  const impersonator = `firecloud-admin@${projectName}.iam.gserviceaccount.com`
  const scope = `
    https://www.googleapis.com/auth/userinfo.profile
    https://www.googleapis.com/auth/userinfo.email
    https://www.googleapis.com/auth/cloud-billing`.trim().split(/\s+/).join(' ')
  const grantType = 'urn:ietf:params:oauth:grant-type:jwt-bearer'
  // from: https://accounts.google.com/.well-known/openid-configuration
  const tokenServerUrl = 'https://oauth2.googleapis.com/token'
  const iamPath = `projects/-/serviceAccounts/${impersonator}`
  const payload = {
    iat: nows(),
    exp: nows() + 60 * 60,
    aud: tokenServerUrl,
    iss: impersonator,
    sub: user,
    scope,
  }
  return u.httpReq(`https://iam.googleapis.com/v1/${iamPath}:signJwt`, {
    method: 'POST', headers, rejectNotOkay: true,
    body: JSON.stringify({payload: JSON.stringify(payload)})
  })
  .chain(u.readStream).map(u.parseJson).chain(Task.fromEither).map(x => x.signedJwt)
  .chain(assertion => u.httpReq(tokenServerUrl, {
    method: 'POST', headers, rejectNotOkay: true,
    body: JSON.stringify({grantType, assertion})
  }))
  .chain(u.readStream).map(u.parseJson).chain(Task.fromEither)
}
export_({getBearerTokenForUser})

if (require.main === module) {
  // assuming launched as `node script.js`
  const args = process.argv.slice(2)
  if (args.length < 3) {
    console.log('usage: node script.js service-account-key-json project-name user')
    process.exit(1)
  }
  getSaBearerToken(args[0]).map(x => x.access_token).chain(saBearerToken =>
    getBearerTokenForUser(saBearerToken, args[1], args[2]))
  .fork(
    e => { console.error(e); process.exit(1) },
    console.log
  )
}

