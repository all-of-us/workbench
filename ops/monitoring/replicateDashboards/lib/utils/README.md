# Project Management Tools

- I have some code in some language.
- I need to perform various tasks with this code (e.g., compile, deploy).
- I want to write scripts to do these tasks to make them easier and document them.
- I do not want to write these scripts in bash.

Enter this package. Start by:

```bash
curl https://raw.githubusercontent.com/dmohs/project-management/master/.sample-project.rb > project.rb && chmod +x project.rb
```
then:
```bash
./project.rb
```
Now add tasks to the `.project` folder a la https://github.com/dmohs/react-cljs/blob/master/.project/install.rb.
