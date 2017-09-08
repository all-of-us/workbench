require "open-uri"
require_relative "utils/common"

SWAGGER_CODEGEN_CLI_JAR = "libproject/swagger-codegen-cli.jar"

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
