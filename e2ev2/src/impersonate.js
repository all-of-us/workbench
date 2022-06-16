const assert = require('assert').strict
const fs = require('fs')
const https = require('https')
const jwt = require('jsonwebtoken')
const {Right, Left, Task} = require('./fp')

const nl = '\n'

const nows = () => Math.floor(Date.now() / 1000)

const readFile = (path, options) =>
  Task((rej, res) => fs.readFile(path, options, (err, data) => err ? rej(err) : res(data)))

const parseJson = s => { try { return Right(JSON.parse(s)) } catch (e) { return Left(e) } }

const httpReq = (url, options) => Task((rej, res) => {
  const req = https.request(url, options, response => {
    if (options.rejectNotOkay && (response.statusCode < 200 || response.statusCode >= 300)) {
      return rej(new Error(`${url} responded with status ${response.statusCode}`))
    }
    res(response)
  })
  if (options.body) { req.write(options.body) }
  req.end()
})

const readStream = s => Task((rej, res) => {
  const chunks = []
  s.on('data', chunk => chunks.push(chunk))
  s.on('end', () => res(Buffer.concat(chunks)))
})

const formatResHead = res => `${res.statusCode} ${res.statusMessage}\n`
  +Object.keys(res.headers).map(k => `${k}: ${res.headers[k]}`).join('\n')

const getBearerToken = (projectName, user) => {
  const bToken = process.env.BEARER_TOKEN?.trim().replace(/[.]+$/, '')
  if (!bToken) { throw 'BEARER_TOKEN not set' }
  const  headers = {
    'Authorization': `Bearer ${bToken}`,
    'Content-Type': 'application/json'
  }
  const saEmail = `firecloud-admin@${projectName}.iam.gserviceaccount.com`
  const scope = `
    https://www.googleapis.com/auth/userinfo.profile
    https://www.googleapis.com/auth/userinfo.email
    https://www.googleapis.com/auth/cloud-billing`.trim().split(/\s+/).join(' ')
  const grantType = 'urn:ietf:params:oauth:grant-type:jwt-bearer'
  // from: https://accounts.google.com/.well-known/openid-configuration
  const tokenServerUrl = 'https://oauth2.googleapis.com/token'
  const iamPath = `projects/-/serviceAccounts/${saEmail}`
  const payload = {
    iat: nows(),
    exp: nows() + 60 * 60,
    aud: tokenServerUrl,
    iss: saEmail,
    sub: user,
    scope,
  }
  return httpReq(`https://iam.googleapis.com/v1/${iamPath}:signJwt`, {
    method: 'POST', headers, rejectNotOkay: true,
    body: JSON.stringify({payload: JSON.stringify(payload)})
  })
  // .map(res => { console.log(formatResHead(res)); return res })
  .chain(readStream).map(parseJson).chain(Task.fromEither).map(x => x.signedJwt)
  .chain(assertion => httpReq(tokenServerUrl, {
    method: 'POST', headers, rejectNotOkay: true,
    body: JSON.stringify({grantType, assertion})
  }))
  // .map(res => { console.log(formatResHead(res)); return res })
  .chain(readStream).map(parseJson).chain(Task.fromEither).map(x => x.access_token)
}
module.exports.getBearerToken = getBearerToken

if (require.main === module) {
  const args = process.argv.slice(2)
  if (args.length < 2) {
    console.log('usage: command project-name user')
    process.exit(1)
  }
  getBearerToken(...args).fork(
    e => { console.error(e); process.exit(1) },
    console.log
  )
}

// Doesn't quite work:
const getSaBearerToken =
  readFile('sa-key.json', {encoding: 'utf8'})
  .map(parseJson).chain(Task.fromEither).chain(saKey => {
    const adminEmail = `firecloud-admin@${saKey.project_id}.iam.gserviceaccount.com`
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
      aud: 'https://oauth2.googleapis.com/token',
      iat: nows(),
      exp: nows() + 3600,
    }
    const signedJwt = jwt.sign(payload, saKey.private_key, {algorithm: 'RS256'})
    const url = `https://iamcredentials.googleapis.com/v1/projects/-/serviceAccounts/`
      +`${saKey.project_id}@${saKey.project_id}.iam.gserviceaccount.com:signJwt`
    return httpReq(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${process.env.POBT.trim()}`
      },
      body: JSON.stringify({payload: signedJwt})
    })
  })
  // .map(res => { console.log(formatResHead(res)); return res })
  .chain(readStream).map(b => '---\n'+b.toString())

