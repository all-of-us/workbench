const handleReq = (req, res) => {
  if (!(req.url.pathname === '/'
    && req.url.search === ''
    && req.method === 'GET')) { return }
  res.mwrite(`AllOfUs Workbench API`).mend()
}
export default handleReq
