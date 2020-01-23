#Api Rollback Safety
In AppEngine, it is very easy to rollback just the API. This can be difficult to do in
conjunction with some database changes. One example of such changes is dropping tables.
If you drop a table, but need to rollback the API, the server will begin to complain because
it is looking for tables which no longer exist. Thus, we need to have some care with 
database changes that are not compatible against all versions of our application.

##Process for Incompatible Database Changes
If you find yourself needing to make backwards incompatible database changes (e.g., dropping
a table), you should stagger the release of the API changes with the release of the database
changes. For example, with dropping a table, first merge in and release a change to the server
code that removes all references to the table to be dropped. Open a PR (but do not merge) that
drops the table. When the release removing the references to the dropped table is released,
merge the changelog that drops the table. This way, the API server can be safely rolled back
one version without the database changes making the previous version unusable. 

###Pros
* This allows the person with the most context on the change to hit the merge button/resolve
any conflicts.

###Cons
* This requires any engineer making these sorts of changes to be watching the release process
carefully 