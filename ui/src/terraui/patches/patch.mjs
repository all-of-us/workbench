import * as fs from 'fs'

const [,, regex, fns, fpath] = process.argv
const fn = eval(fns)

fs.promises.readFile(fpath, 'utf8')
.then(c => c.replace(new RegExp(regex, 'g'), fn))
.then(c => fs.promises.writeFile(fpath, c))

