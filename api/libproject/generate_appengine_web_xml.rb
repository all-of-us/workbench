xml = File.read("src/main/webapp/WEB-INF/appengine-web.xml.template")
ENV.each { |k, v| xml.gsub! "${#{k}}", v }
File.write("src/main/webapp/WEB-INF/appengine-web.xml", xml)
