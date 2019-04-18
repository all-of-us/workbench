def evaluate_env_vars(str, env)
  env.each { |k, v| str.gsub! "$" + k, v }
  return str
end

xml = File.read("src/main/webapp/WEB-INF/appengine-web.xml.template")

env = {}
for line in File.read("db/vars.env").split("\n").reject { |c| c.empty? }
  line = line.strip()
  if line[0] == '#'
    next
  end

  var, val = line.split("=", 2)
  if var == "DB_HOST" && ENV["OVERWRITE_WORKBENCH_DB_HOST"]
    val = "localhost"
  else
    val = evaluate_env_vars(val, env)
  end

  env[var] = val
  xml.gsub! "${#{var}}", val
end

File.write("src/main/webapp/WEB-INF/appengine-web.xml", xml)