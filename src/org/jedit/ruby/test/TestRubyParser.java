package org.jedit.ruby.test;

import junit.framework.TestCase;
import org.jedit.ruby.RubyParser;
import org.jedit.ruby.Member;
import org.jedit.ruby.RubyMembers;
import org.jedit.ruby.RubyCache;
import org.jruby.lexer.yacc.SourcePosition;

import java.util.List;
import java.util.ArrayList;

/**
 * @author robmckinnon at users.sourceforge.net
 */
public class TestRubyParser extends TestCase {
    
    public static final String ARR_DEF = "def []\n" +
            "end\n";

    public static final String DEF = "def red\n" +
            "end\n";

    public static final String EMPTY_CLASS = "class Green\n" +
            "end\n";

    public static final String CLASS = "class Green\n" +
            DEF +
            "end\n";

    public static final String ARR_CLASS = "class Green\n" +
            ARR_DEF +
            "end\n";

    public static final String CLASS_AND_DEF = "class Green\n" +
            "end\n" +
            DEF;

    public static final String EMPTY_MODULE = "module Blue\n" +
            "end\n";

    public static final String MODULE_METHOD = "module Blue\n" +
            "  def Blue.deep\n" +
            "  end\n" +
            "end\n";

    public static final String EMPTY_CLASS_IN_MODULE = "module Blue\n" +
            EMPTY_CLASS +
            "end\n";

    public static final String CLASS_IN_MODULE = "module Blue\n" +
            CLASS +
            "end\n";

    public static final String ARR_CLASS_IN_MODULE = "module Blue\n" +
            ARR_CLASS +
            "end\n";

    public static final String DEF_IN_MODULE = "module Blue\n" +
            DEF +
            "end\n";

    public void tearDown() {
        RubyCache.clear();
    }

    public void testParentOfDef() {
        List<Member> members = RubyParser.getMembersAsList(CLASS, null, null);
        Member method = members.get(0).getChildMembers()[0];
        assertTrue("Assert has parent", method.hasParentMember());
        assertEquals("Assert parent correct", "Green", method.getParentMember().getFullName());
    }

    private void addClassesToCache() {
        RubyCache.add(CLASS, "CLASS");
        RubyCache.add(ARR_CLASS, "ARR_CLASS");
    }

    private void addModuleToCache() {
        RubyCache.add(DEF_IN_MODULE, "DEF_IN_MODULE");
    }

    public void testGetClassByMethod() {
        addClassesToCache();
        assertFindByMethodCorrect("red", 0, "Green", 1);
        assertFindByMethodCorrect("[]", 0, "Green", 1);
    }

    public void testGetMethodByClass() {
        addClassesToCache();
        assertFindByClassCorrect("Green", 0, "[]", 2, "ARR_CLASS");
        assertFindByClassCorrect("Green", 1, "red", 2, "CLASS");
    }

    public void testGetClassByCombo() {
        addClassesToCache();
        addModuleToCache();
        assertFindByMethodCorrect("red", 0, "Blue", 2);
        assertFindByMethodCorrect("red", 1, "Green", 2);
        assertFindByMethodCorrect("[]", 0, "Green", 1);
    }

    public void testGetMethodByCombo() {
        addClassesToCache();
        addModuleToCache();
        assertFindByClassCorrect("Green", 0, "[]", 2, "ARR_CLASS");
        assertFindByClassCorrect("Green", 1, "red", 2, "CLASS");
        assertFindByClassCorrect("Blue", 0, "red", 1, "DEF_IN_MODULE");
    }

    private void assertFindByMethodCorrect(String method, int index, String parentName, int parentCount) {
        List<Member> members = RubyCache.getMembersWithMethod(method);
        assertEquals("Assert parent match correct for: " + method, parentCount, members.size());
        assertEquals("Assert name correct", parentName, members.get(index).getName());
    }

    private void assertFindByClassCorrect(String parentName, int index, String methodName, int methodCount, String filePath) {
        List<Member.Method> members = RubyCache.getMethodsOfMember(parentName);
        assertEquals("Assert child match correct", methodCount, members.size());
        assertEquals("Assert name correct", methodName, members.get(index).getName());
        assertEquals("Assert path correct", filePath, members.get(index).getFilePath());
    }

    public void testParseModuleMethod() {
        List<Member> members = RubyParser.getMembersAsList(MODULE_METHOD, null, null);
        assertCorrect(0, "Blue", null, 7, members);
        assertChildrenCorrect(members, "Blue.deep", 18, "Blue");
    }

    public void testBigFile() {
        List<Member> members = RubyParser.getMembersAsList(bigFile, null, new TestListener());
        members.toString();
    }

    public void testEmptyClassInModuleSize() {
        assertSizeCorrect(1, EMPTY_CLASS_IN_MODULE);
    }

    public void testEmptyClassInModule() {
        List<Member> members = RubyParser.getMembersAsList(EMPTY_CLASS_IN_MODULE, null, null);
        assertCorrect(0, "Blue", null, 7, members);
        assertChildrenCorrect(members, "Blue::Green", 18, "Blue");
    }

    public void testPreviousMemberBeforeModuleMember() {
        assertPreviousMemberCorrect(EMPTY_CLASS_IN_MODULE, 6, null);
    }

    public void testPreviousMemberAfterModuleMember() {
        assertPreviousMemberCorrect(EMPTY_CLASS_IN_MODULE, 8, "Blue");
    }

    public void testPreviousMemberAfterClassMember() {
        assertPreviousMemberCorrect(EMPTY_CLASS_IN_MODULE, 19, "Blue::Green");
    }

    public void testPreviousMemberAtMember() {
        assertPreviousMemberCorrect(EMPTY_CLASS_IN_MODULE, 7, "Blue");
    }

    public void testPreviousMemberInModuleClass() {
        assertPreviousMemberCorrect(bigFile, 2832, "with_http");
    }

    private List<Member> assertChildrenCorrect(List<Member> members, String name, int offset, String parentName) {
        members = getChildMembers(members);
        assertCorrect(0, name, parentName, offset, members);
        return members;
    }

    public void testClassInModuleSize() {
        assertSizeCorrect(1, CLASS_IN_MODULE);
    }

    public void testParseClassInModule() {
        List<Member> members = RubyParser.getMembersAsList(CLASS_IN_MODULE, null, null);
        assertCorrect(0, "Blue", null, 7, members);
        members = assertChildrenCorrect(members, "Blue::Green", 18, "Blue");
        assertChildrenCorrect(members, "red", 28, "Blue");
    }

    public void testParseArrClassInModule() {
        List<Member> members = RubyParser.getMembersAsList(ARR_CLASS_IN_MODULE, null, null);
        assertCorrect(0, "Blue", null, 7, members);
        members = assertChildrenCorrect(members, "Blue::Green", 18, "Blue");
        assertChildrenCorrect(members, "[]", 28, "Blue");
    }

    public void testDefInModuleSize() {
        assertSizeCorrect(1, DEF_IN_MODULE);
    }

    public void testParseDefInModule() {
        List<Member> members = RubyParser.getMembersAsList(DEF_IN_MODULE, null, null);
        assertCorrect(0, "Blue", null, 7, members);
        assertChildrenCorrect(members, "red", 16, "Blue");
    }

    public void testParseDefSize() {
        assertSizeCorrect(1, DEF);
    }

    public void testParseDef() {
        List<Member> members = RubyParser.getMembersAsList(DEF, null, null);
        assertCorrect(0, "red", null, 4, members);
    }

    public void testNextDef() {
        RubyMembers members = RubyParser.getMembers("\n" + DEF + "\n", null);
        Member nextMember = members.getNextMember(0);
        List<Member> list = new ArrayList<Member>();
        list.add(nextMember);
        assertCorrect(0, "red", null, 5, list);
    }

    public void testParseArrDefSize() {
        assertSizeCorrect(1, ARR_DEF);
    }

    public void testParseArrDef() {
        List<Member> members = RubyParser.getMembersAsList(ARR_DEF, null, null);
        assertCorrect(0, "[]", null, 4, members);
    }

    public void testParseEmptyClassSize() {
        assertSizeCorrect(1, EMPTY_CLASS);
    }

    public void testParseEmptyClass() {
        List<Member> members = RubyParser.getMembersAsList(EMPTY_CLASS, null, null);
        assertCorrect(0, "Green", null, 6, members);
    }

    public void testParseEmptyModuleSize() {
        assertSizeCorrect(1, EMPTY_MODULE);
    }

    public void testParseEmptyModule() {
        List<Member> members = RubyParser.getMembersAsList(EMPTY_MODULE, null, null);
        assertCorrect(0, "Blue", null, 7, members);
    }

    public void testParseClassSize() {
        assertSizeCorrect(1, CLASS);
    }

    public void testParseClass() {
        List<Member> members = RubyParser.getMembersAsList(CLASS, null, null);
        assertCorrect(0, "Green", null, 6, members);
        assertChildrenCorrect(members, "red", 16, "Green");
    }

    public void testParseArrClass() {
        List<Member> members = RubyParser.getMembersAsList(ARR_CLASS, null, null);
        assertCorrect(0, "Green", null, 6, members);
        assertChildrenCorrect(members, "[]", 16, "Green");
    }

    public void testParseClassAndDefSize() {
        assertSizeCorrect(2, CLASS_AND_DEF);
    }

    public void testParseClassAndDef() {
        List<Member> members = RubyParser.getMembersAsList(CLASS_AND_DEF, null, null);
        assertCorrect(0, "Green", null, 6, members);
        assertCorrect(1, "red", null, 20, members);
    }

    public void testClassMethodSize() {
        assertSizeCorrect(2, classMethodFile);
    }

    public void testClassMethodCall() {
        List<Member> members = RubyParser.getMembersAsList(classMethodFile, null, null);
        assertCorrect(0, "One", null, 6, members);
        assertChildrenCorrect(members, "to_yaml(opts)", 15, "One");
        assertCorrect(1, "Two", null, 74, members);
    }

    private void assertSizeCorrect(int resultSize, String content) {
        List<Member> members = RubyParser.getMembersAsList(content, null, null);
        assertEquals("assert result size is correct", resultSize, members.size());
    }

    private List<Member> getChildMembers(List<Member> members) {
        assertTrue("assert has child members", members.get(0).hasChildMembers());
        List<Member> childMembers = members.get(0).getChildMembersAsList();
        return childMembers;
    }

    private static class TestListener implements RubyParser.WarningListener {
        public void warn(SourcePosition position, String message) {
            System.out.println(message);
        }

        public void warn(String message) {
            System.out.println(message);
        }

        public void warning(SourcePosition position, String message) {
            System.out.println(message);
        }

        public void warning(String message) {
            System.out.println(message);
        }

        public void error(SourcePosition position, String message) {
            System.out.println(message);
        }
    }

    private void assertPreviousMemberCorrect(String text, int caretPosition, String expectedName) {
        RubyMembers members = RubyParser.getMembers(text, "", new TestListener(), false);
        Member member = members.getCurrentMember(caretPosition);

        if(expectedName == null) {
            assertNull("assert previous member is null", member);
        } else {
            assertEquals("assert previous member correct", expectedName, member.getFullName());
        }
    }

    private void assertCorrect(int index, String name, String parentName, int offset, List<Member> members) {
        try {
            Member member = members.get(index);
            assertEquals("Assert name correct", name, member.getFullName());
            assertEquals("Assert offset correct", offset, member.getStartOffset());

            List<Member> memberPath = member.getMemberPath();

            if(parentName == null) {
                assertEquals("assert empty list", 1, memberPath.size());
                assertEquals("assert self path member correct", name, memberPath.get(0).getFullName());
            } else {
                assertEquals("assert top path member correct", parentName, memberPath.get(0).getFullName());
            }
        } catch (Exception e) {
            fail("Member not in result: " + name);
        }
    }

    private static final String classMethodFile = "class One\n" +
            "\tdef to_yaml(opts)\n" +
            "\t\tself.class if Hash === opts\n" +
            "\tend\n" +
            "end\n" +
            "class Two\n" +
            "end";

    private static final String bigFile = "module HTTPCache\n" +
            "\n" +
            "\tVERSION = '0.1.0'\n" +
            "\tHOMEDIR = ENV['HOME'] || ENV['USERPROFILE'] || ENV['HOMEPATH']\n" +
            "\tCACHEDIR = File.join(HOMEDIR, '.httpcache')\n" +
            "\tDir.mkdir(CACHEDIR) unless File.exists? CACHEDIR\n" +
            "\n" +
            "\t##\n" +
            "\t# Obtains HTTP response based on the supplied url,\n" +
            "\t# and returns <code>CachedResponse</code>.\n" +
            "\t# Uses ETag and Last-Modified date to retrieve\n" +
            "\t# the content only when it has really changed.\n" +
            "\t# If not changed the cached content is returned.\n" +
            "\t#\n" +
            "\t# Params:\n" +
            "\t#  url, e.g. 'http://www.ruby-lang.org/'\n" +
            "\t#  proxy_address, optional e.g. 'localhost'\n" +
            "\t#  proxy_port, optional e.g. '8080'\n" +
            "\t#\n" +
            "\t# Example 1:\n" +
            "\t#  url = 'http://www.ruby-lang.org/'\n" +
            "\t#  cache = HTTPCache.get(url)\n" +
            "\t#  p cache.content\n" +
            "\t#\n" +
            "\t# Example 2:\n" +
            "\t#  # If you're running a proxy web server at port 8080\n" +
            "\t#  proxy_address = 'localhost'\n" +
            "\t#  proxy_port = '8080'\n" +
            "\t#  url = 'http://www.ruby-lang.org/'\n" +
            "\t#  cache = HTTPCache.get(url, proxy_address, proxy_port)\n" +
            "\t#  p cache.content\n" +
            "\t#\n" +
            "\tdef HTTPCache.get url, proxy_address=nil, proxy_port=nil\n" +
            "\t\turl_dir = Digest::MD5.hexdigest url\n" +
            "\t\tcache_path = File.join CACHEDIR, url_dir\n" +
            "\t\turi = URI.parse url\n" +
            "\n" +
            "\t\tretriever = Retriever.new uri, cache_path, proxy_address, proxy_port\n" +
            "\t\tretriever.retrieve\n" +
            "\tend\n" +
            "\n" +
            "\tclass Retriever\n" +
            "\n" +
            "\t\tdef initialize uri, cache_path, proxy_address, proxy_port\n" +
            "\t\t\t@uri = uri\n" +
            "\t\t\t@cache_path = cache_path\n" +
            "\t\t\t@header_file = @cache_path + '/header'\n" +
            "\t\t\t@content_file = @cache_path + '/content'\n" +
            "\t\t\t@proxy_address = proxy_address\n" +
            "\t\t\t@proxy_port = proxy_port\n" +
            "\t\t\t@request_param = {'Accept-Encoding' => 'gzip, deflate'}\n" +
            "\t\tend\n" +
            "\n" +
            "\t\tdef retrieve\n" +
            "\t\t\tif File.exists? @cache_path\n" +
            "\t\t\t\twith_http {|http| do_followup_request(http)}\n" +
            "\t\t\telse\n" +
            "\t\t\t\twith_http {|http| do_request(http)}\n" +
            "\t\t\tend\n" +
            "\t\tend\n" +
            "\n" +
            "\t\tprivate\n" +
            "\n" +
            "\t\tdef do_request http\n" +
            "\t\t\tcreate_cache http.request_get(@uri.request_uri, @request_param)\n" +
            "\t\tend\n" +
            "\n" +
            "\t\tdef do_followup_request http\n" +
            "\t\t\tcache = YAML::load(IO.read(@header_file))\n" +
            "\n" +
            "\t\t\tif cache['etag']\n" +
            "\t\t\t\t@request_param['If-None-Match'] = cache['etag']\n" +
            "\t\t\telsif cache['last-modified']\n" +
            "\t\t\t\t@request_param['If-Modified-Since'] = cache['last-modified']\n" +
            "\t\t\telsif cache['date']\n" +
            "\t\t\t\t@request_param['If-Modified-Since'] = cache['date']\n" +
            "\t\t\tend\n" +
            "\n" +
            "\t\t\tresponse = http.request_get(@uri.request_uri, @request_param)\n" +
            "\n" +
            "\t\t\tif response.code == '304' # not modified use cache\n" +
            "\t\t\t\tcache.content = IO.read(@content_file) if File.exists? @content_file\n" +
            "\t\t\telse\n" +
            "\t\t\t\tcache = create_cache response\n" +
            "\t\t\tend\n" +
            "\n" +
            "\t\t\tcache\n" +
            "\t\tend\n" +
            "\n" +
            "\t\tdef create_cache response\n" +
            "\t\t\tcache = CachedResponse.new @uri, response\n" +
            "\n" +
            "\t\t\tDir.mkdir @cache_path unless File.exists? @cache_path\n" +
            "\t\t\tFile.open(@header_file, 'w') do |file|\n" +
            "\t\t\t\tfile.write cache.to_yaml\n" +
            "\t\t\tend\n" +
            "\n" +
            "\t\t\tcache.content = response.get_content\n" +
            "\n" +
            "\t\t\tif cache.content\n" +
            "\t\t\t\t# ruby 1.8.1 bug prevents to_yaml working on cache.content\n" +
            "\t\t\t\tFile.open(@content_file, 'w') do |file|\n" +
            "\t\t\t\t\tfile.write cache.content\n" +
            "\t\t\t\tend\n" +
            "\t\t\tend\n" +
            "\n" +
            "\t\t\tcache\n" +
            "\t\tend\n" +
            "\n" +
            "\t\tdef with_http\n" +
            "\t\t\tif @proxy_address and @proxy_port\n" +
            "\t\t\t\tNet::HTTP::Proxy(@proxy_address, @proxy_port).start(@uri.host, @uri.port) do |http|\n" +
            "\t\t\t\t\tyield http\n" +
            "\t\t\t\tend\n" +
            "\t\t\telse\n" +
            "\t\t\t\thttp = Net::HTTP.new(@uri.host, @uri.port)\n" +
            "\t\t\t\tyield http\n" +
            "\t\t\tend\n" +
            "\t\tend\n" +
            "\tend\n" +
            "\n" +
            "\tclass CachedResponse\n" +
            "\t\tattr_accessor :uri, :http_version, :code, :message, :content\n" +
            "\t\tattr_writer :header\n" +
            "\n" +
            "\t\tdef initialize uri, response\n" +
            "\t\t\t@uri = uri.to_s\n" +
            "\t\t\t@http_version = response.http_version\n" +
            "\t\t\t@code = response.code\n" +
            "\t\t\t@message = response.message.strip\n" +
            "\t\t\t@header = response.to_hash\n" +
            "\t\t\t@content = nil\n" +
            "\t\tend\n" +
            "\n" +
            "\t\t##\n" +
            "\t\t# Returns retrieved content as a\n" +
            "\t\t# <code>REXML::Document</code>\n" +
            "\t\t# if it is possible to do so.\n" +
            "\t\tdef as_xml\n" +
            "\t\t\txml = nil\n" +
            "\t\t\ttype = self['content-type']\n" +
            "\t\t\tif @content and type\n" +
            "\t\t\t\tif type =~ /.*html/\n" +
            "\t\t\t\t\tparser = HTMLTree::XMLParser.new(false, false)\n" +
            "\t\t\t\t\tparser.feed(@content)\n" +
            "\t\t\t\t\t# then you have the tree built..\n" +
            "\t\t\t\t\txml = parser.document\n" +
            "\t\t\t\telsif type =~ /.*xml/\n" +
            "\t\t\t\t\txml = REXML::Document.new @content \n" +
            "\t\t\t\tend\n" +
            "\t\t\tend\n" +
            "\t\t\txml\n" +
            "\t\tend\n" +
            "\t\n" +
            "\t\t##\n" +
            "\t\t# Iterates for each header names and values.\n" +
            "\t\tdef each_header &block\n" +
            "\t\t\t@header.each &block\n" +
            "\t\tend\n" +
            "\n" +
            "\t\t##\n" +
            "\t\t# Returns HTTP response header as a string\n" +
            "\t\tdef header\n" +
            "\t\t\theader = %Q[HTTP/#{@http_version} #{@code} #{@message}\\n]\n" +
            "\t\t\teach_header do |key,value|\n" +
            "\t\t\t\theader << %Q[#{key}: #{value}\\n]\n" +
            "\t\t\tend\n" +
            "\t\t\theader\n" +
            "\t\tend\n" +
            "\n" +
            "\t\t##\n" +
            "\t\t# Returns the header field corresponding to the case-insensitive key.\n" +
            "\t\t# Example:\n" +
            "\t\t#  type = response['Content-Type']\n" +
            "\t\tdef [] key\n" +
            "\t\t\t@header[key.downcase]\n" +
            "\t\tend\n" +
            "\tend\n" +
            "\n" +
            "end\n" +
            "\n" +
            "module Zlib\n" +
            "\tclass GzipReader\n" +
            "\t\tdef GzipReader.unzip string\n" +
            "\t\t\tgz = Zlib::GzipReader.new(StringIO.new(string))\n" +
            "\t\t\tcontent = gz.read\n" +
            "\t\t\tgz.close\n" +
            "\t\t\tcontent\n" +
            "\t\tend\n" +
            "\tend\n" +
            "end\n" +
            "\n" +
            "module Net\n" +
            "\tclass HTTPResponse\n" +
            "\t\tdef get_content\n" +
            "\t\t\tcontent = self.body\n" +
            "\n" +
            "\t\t\tif content and (encoding = self['Content-Encoding'])\n" +
            "\t\t\t\tif encoding =~ /gzip/i\n" +
            "\t\t\t\t\tcontent = Zlib::GzipReader.unzip(content)\n" +
            "\t\t\t\telsif encoding =~ /deflate/i\n" +
            "\t\t\t\t\tcontent = Zlib::Inflate.inflate(content)\n" +
            "\t\t\t\tend\n" +
            "\t\t\tend\n" +
            "\n" +
            "\t\t\tcontent\n" +
            "\t\tend\n" +
            "\tend\n" +
            "end";
}
