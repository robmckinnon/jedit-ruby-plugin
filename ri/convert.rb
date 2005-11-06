require 'rdoc_to_java.rb'

base_dir = "/home/b/apps/versions/jedit/4.2/plugins/RubyPlugin/ri/"
result_dir = base_dir + "java-xml"
serializer = JavaXmlSerializer.new(base_dir, result_dir)
serializer.convert_dir(base_dir, "1.8/system")
serializer.convert_dir(base_dir, "1.8/stdlib")
serializer.convert_dir(base_dir, "1.8/gems")

