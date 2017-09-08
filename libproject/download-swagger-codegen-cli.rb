require "open-uri"
require_relative "utils/common"
require_relative "workbench-common"

SWAGGER_CODEGEN_CLI_JAR = File.join(WORKBENCH_ROOT, "libproject", "swagger-codegen-cli.jar")
SWAGGER_SPEC = File.join(
    WORKBENCH_ROOT, "api", "src", "main", "resources", "workbench.yaml")

def download(url, path)
  File.open(path, "wb") do |f|
    IO.copy_stream(open(url), f)
  end
end

def download_swagger_codegen_cli()
  unless File.exist?(SWAGGER_CODEGEN_CLI_JAR)
    common = Common.new
    jar_url = "https://storage.googleapis.com" +
      "/swagger-codegen-cli/swagger-codegen-cli-2.3.0-20170814.101630-90.jar"
    common.status "#{jar_url} > #{SWAGGER_CODEGEN_CLI_JAR}..."
    download(jar_url, SWAGGER_CODEGEN_CLI_JAR)
  end
end
