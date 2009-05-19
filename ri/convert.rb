require 'rdoc_to_java.rb'

base_dir = "/Users/x/apps/jedit/plugins/RubyPlugin/ri/"
result_dir = base_dir + "java-xml"
serializer = JavaXmlSerializer.new(base_dir, result_dir)
#serializer.convert_dir(base_dir, "yaml/1.8.4")
#serializer.convert_dir(base_dir, "yaml/rails_1_0_0")
#serializer.convert_dir(base_dir, "yaml/rails_1_1_2")
#serializer.convert_dir(base_dir, "yaml/rails_1_2_3")
# serializer.convert_dir(base_dir, "yaml/rails_2_0_2")
serializer.convert_dir(base_dir, "yaml/rails_2_3_2")
