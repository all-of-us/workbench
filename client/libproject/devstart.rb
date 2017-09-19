require "optparse"
require "fileutils"
require_relative "../../libproject/utils/common"
require_relative "../../libproject/workbench"
require_relative "../../libproject/swagger"

def swagger_regen()
  Workbench::Swagger.download_swagger_codegen_cli

  common = Common.new
  common.run_inline %W{
      java -jar #{Workbench::Swagger::SWAGGER_CODEGEN_CLI_JAR}
      generate --lang python --input-spec #{Workbench::Swagger::SWAGGER_SPEC} --output py/tmp}
  move_opts = {:force => true, :verbose => true}
  FileUtils.mv('py/tmp/swagger_client', 'py/aou_workbench_client/', move_opts)
  FileUtils.mv('py/tmp/docs', 'py/swagger_docs', move_opts)
  FileUtils.mv('py/tmp/README.md', 'py/README.swagger.md', move_opts)
  FileUtils.mv('py/tmp/requirements.txt', 'py/swagger-requirements.txt', move_opts)
  FileUtils.remove_dir('py/tmp')
  # TODO(markfickett) Automatically check out generated-py-client when running the above.
  common.status "Only commit generated files on the generated-py-client branch."
  common.status "Publish a version with `git tag pyclient-vN-N-rcN` and `git push --tags`."
end

def pylint()
  py_module_root = File.join(Workbench::WORKBENCH_ROOT, 'client', 'py', 'aou_workbench_client')
  rc_file_path = File.join(Workbench::WORKBENCH_ROOT, 'libproject', 'pylintrc')

  # As well as the client Python module, lint setup.py and other support files.
  Dir.chdir(File.join(Workbench::WORKBENCH_ROOT, 'client', 'py'))
  support_py_files = Dir.glob('*.py').map(&File.method(:realpath))

  enabled = "bad-indentation,broad-except,bare-except,logging-too-many-args," +
      "unused-argument,redefined-outer-name,redefined-builtin," +
      "superfluous-parens,syntax-error,trailing-whitespace,unused-import," +
      "unused-variable," +
      "undefined-variable,bad-whitespace,line-too-long,unused-import," +
      "unused-variable"

  common = Common.new
  common.run_inline %W{pylint --rcfile #{rc_file_path} --reports=n --score=n
      --ignore=swagger_client --disable=all --enable=#{enabled}
      #{py_module_root}} + support_py_files
end

Common.register_command({
  :invocation => "swagger-regen",
  :description => "rebuilds the Swagger-generated client libraries",
  :fn => Proc.new { |*args| swagger_regen(*args) }
})

Common.register_command({
  :invocation => "pylint",
  :description => "Lint Python",
  :fn => Proc.new { |*args| pylint(*args) }
})
