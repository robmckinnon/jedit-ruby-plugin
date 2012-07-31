# encoding: utf-8
require 'erb'
require 'uri'
require 'morph'
require 'mechanize'
require 'fileutils'
require 'rexml/document'

class MethodData
  include Morph
end

class NodeData
  include Morph
end

def setup
  MethodData.new.namespace = 'namespace'
  MethodData.new.block_params = 'block_params'
  MethodData.new.params = 'params'
  NodeData.new.namespace = 'namespace'
  NodeData.new.superclass = 'superclass'
  @agent = Mechanize.new
  @agent.user_agent_alias = 'Mac Safari'

  @items = {}
end

def write_docs d
  erb = ERB.new(IO.read("./cdesc.erb"))
  result = erb.result(binding)
  result_dir = "./java-xml/1.9.3/#{d.name}"

  FileUtils.mkdir_p(result_dir)

  file = %Q[#{result_dir}/#{d.name}.xml]

  File.open(file.to_s, "w") do |file|
    file.puts result
  end
end

def get_ruby_stdlib
  get_ruby_core

  base_uri = URI.parse('http://www.ruby-doc.org/stdlib-1.9.3/toc.html')

  response = @agent.get base_uri.to_s

  packages = response.links.select {|x| x.uri.to_s[/index.html/] }

  packages.each do |item|
    process_page base_uri.merge(item.uri), item.text
  end

  @items.each {|k,v| write_docs v }
end


def get_ruby_core
  setup

  base_uri = URI.parse('http://www.ruby-doc.org/core-1.9.3/')

  process_page base_uri
end

def process_page base_uri, package=nil
  begin
    response = @agent.get base_uri.to_s
  rescue Exception => e
    puts e.to_s
    return
  end

  classes = response.links.select {|x| x.uri.to_s[/\.html$/] && !x.uri.to_s[/_(rb|c|txt).html/] }

  classes.each do |item|
    uri = base_uri.merge(item.uri)
    puts uri.to_s
    d = get_docs uri, package

    if @items[d.name]
      @items[d.name].c_methods = @items[d.name].c_methods + d.c_methods
      @items[d.name].i_methods = @items[d.name].i_methods + d.i_methods
    else
      @items[d.name] = d
    end

    nil
  end

  nil
end

def normalize(text)
  if text
    text.gsub!('&lt;', '|lt;')
    REXML::Text.normalize(text)
  else
    text
  end
end

def get_docs uri, package
  @r = @agent.get uri

  docs = NodeData.new
  docs.namespace = get_namespace
  docs.superclass = ''

  name = @r.at('h1.class') || @r.at('h1.module')

  docs.name = normalize(name.text.sub('Syck', 'YAML'))
  docs.html_comment = normalize(@r.at('#description').inner_html)
  docs.full_name = docs.name

  docs.i_methods = get_methods('instance', docs.name, package)
  docs.c_methods = get_methods('class', docs.name, package)

  docs.attributes = []
  docs.constants = []
  docs.includes = []
  docs
end

def get_namespace
  if node = @r.at('.namespace-list-section')
    node.at('a').text.sub('Syck', 'YAML')
  else
    ''
  end
end

def get_methods type, class_name, package
  links = @r.links.select do |x|
    begin
      x.uri.to_s[/^#method-#{type[0,1]}/]
    rescue
      false
    end
  end

  methods = links.map { |link| get_method(link.text, link.uri.to_s, type, class_name, package) }
  methods.compact
end

def convert_call(text)
  URI.decode( URI.encode(text).sub('%E2%86%92','aarrow') ).sub('aarrow','->').strip
end

def remove_source_code p
  p.at('.method-source-code') && p.at('.method-source-code').remove
end

def description_blank? p
  text = p.at('.method-description').text.strip
  if text.size == 0
    true
  else
    text[/^YAML\:\:Syck/]
  end
end

def get_description p, package
  html = p.at('.method-description').inner_html
  if package
    package = 'yaml' if package == 'syck'
    html = "<p>require '#{package}'</p><br />\n#{html}"
  end
  normalize(html)
end

def get_method name, anchor, type, class_name, package
  puts anchor

  p = @r.at("a[@name='#{anchor.sub('#','')}']").parent

  aliases = []

  if calls = (p/'.method-callseq')
    calls = calls.map {|call| convert_call(call.text) }
  else
    calls = [p.at('.method-name').text]

    if p.at('.aliases')
      aliases = p.at('.aliases').search('a').map{|x| x.text}

      aliases = aliases.map {|x| Morph.from_hash('alias' => { 'name' => normalize(x) }) }
    end
  end

  method = MethodData.new
  method.name = normalize(name.sub(/^(#|::)/,''))

  remove_source_code p
  if description_blank?(p) && package
    return nil
  end

  method.html_comment = get_description(p, package)
  method.aliases = aliases
  if package
    if p.at('.method-name')
      method.block_params = normalize("#{p.at('.method-name').text}#{p.at('.method-args').text}")
    else
      method.block_params = normalize(calls.join("\n"))
    end
    method.params = ''
  else
    method.block_params = normalize(calls.join("\n"))
    method.params = p.at('.method-args') ? normalize(p.at('.method-args').text) : ''
  end
  method.full_name = normalize("#{class_name}#{name}".sub('Syck', 'YAML'))
  method.namespace = class_name.sub('Syck', 'YAML')
  method.visibility = 'public'
  method.is_singleton = (type == 'class')
  method
end

# d = get_ruby_core
d = get_ruby_stdlib
