export const runServer = async port => {
  const sm = await import('$/src/server.mjs?'+Date.now())
  const server = sm.create()
  server.listen(port)
  server.on('request', sm.createReqHandler(async (req, res) => {
    const rhm = await import('$/src/reqhandler.mjs?'+Date.now())
    return rhm.handleReq(req, res)
  }))
}
