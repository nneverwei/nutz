package com.zzh.json;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zzh.lang.Files;
import com.zzh.lang.Lang;
import com.zzh.lang.Streams;
import com.zzh.lang.stream.CharInputStream;
import com.zzh.lang.stream.CharOutputStream;

import junit.framework.TestCase;

@SuppressWarnings("unchecked")
public class JsonTest extends TestCase {

	public void testBoolean() {
		assertTrue(Json.fromJson(boolean.class, Lang.inr("true")));
		assertFalse(Json.fromJson(boolean.class, Lang.inr("false")));
		assertTrue(((Boolean) Json.fromJson(Lang.inr("true"))).booleanValue());
		assertFalse(((Boolean) Json.fromJson(Lang.inr("false"))).booleanValue());
	}

	public void testFloat() {
		assertEquals(2.3f, Json.fromJson(float.class, Lang.inr("2.3")));
		assertEquals(2.3f, ((Double) Json.fromJson(Lang.inr("2.3"))).floatValue());
		assertEquals(.3f, Json.fromJson(float.class, Lang.inr(".3")));
	}

	public void testLongg() {
		assertEquals(87L, Json.fromJson(long.class, Lang.inr("87")).longValue());
		assertEquals(87L, ((Long) Json.fromJson(Lang.inr("87L"))).longValue());
	}

	@SuppressWarnings("deprecation")
	public void testDatetime() {
		java.util.Date date = Json.fromJson(java.util.Date.class, Lang
				.inr("\"2008-05-16 14:35:43\""));
		assertEquals(108, date.getYear());
		assertEquals(4, date.getMonth());
		assertEquals(16, date.getDate());
		assertEquals(14, date.getHours());
		assertEquals(35, date.getMinutes());
		assertEquals(43, date.getSeconds());
	}

	public void testSimpleAbc() {
		String s = "{\"id\":45,\"name\":'xyz'}";
		Abc abc = Json.fromJson(Abc.class, Lang.inr(s));
		assertEquals(45, abc.id);
		assertEquals("xyz", abc.name);
	}

	public void testAllTypesInMap() throws FileNotFoundException {
		File f = Files.findFile("com/zzh/json/types.txt");
		Map<String, Object> map = (Map<String, Object>) Json.fromJson(new InputStreamReader(
				new FileInputStream(f)));
		assertTrue((Boolean) map.get("true"));
		assertFalse((Boolean) map.get("false"));
		assertNull(map.get("null"));
		assertTrue(34 == (Integer) map.get("int"));
		assertTrue(67L == (Long) map.get("long"));
		assertTrue(7.69 == (Double) map.get("double"));
		assertTrue(8.79f == (Float) map.get("float"));
		List<?> ary = (List<?>) map.get("array");
		assertEquals(2, ary.size());
		assertEquals("abc", ary.get(0));
		List<?> coll = ary;
		assertTrue(45 == (Integer) coll.get(1));
	}

	public void testSimpleMap() {
		String s = "{id:45,m:{x:1},name:'xyz'}";
		Map map = (Map) Json.fromJson(Lang.inr(s));
		assertEquals(45, map.get("id"));
		assertEquals("xyz", map.get("name"));
	}

	public void testSimpleMap2() {
		String s = "{f:false,t:true,H:30}";
		Map map = (Map) Json.fromJson(Lang.inr(s));
		assertTrue((Boolean) map.get("t"));
		assertFalse((Boolean) map.get("f"));
		assertEquals(30, map.get("H"));
	}

	public void testSimpleMap3() {
		String s = "{ary:[1,2],t:true,H:30}";
		Map map = (Map) Json.fromJson(Lang.inr(s));
		List<?> list = (List<?>) map.get("ary");
		assertEquals(2, list.size());
		assertTrue((Boolean) map.get("t"));
		assertEquals(30, map.get("H"));
	}

	public void testMap() throws FileNotFoundException {
		File f = Files.findFile("com/zzh/json/map.txt");
		Map<String, Object> map = Json.fromJson(HashMap.class, new InputStreamReader(
				new FileInputStream(f)));
		assertEquals("value1", map.get("a1"));
		assertEquals(35, map.get("a2"));
		assertEquals((double) 4.7, map.get("a3"));
		Map m1 = (Map) map.get("m1");
		assertEquals(12, m1.get("x"));
		assertEquals(45, m1.get("y"));
		Map m12 = (Map) m1.get("m12");
		assertEquals("haha", m12.get("w1"));
		assertEquals("fuck", m12.get("w2"));
		Map m2 = (Map) map.get("m2");
		assertEquals("good", m2.get("today"));
		assertEquals("nice", m2.get("tomy"));
	}

	@SuppressWarnings("deprecation")
	public void testSimplePersonObject() throws Exception {
		File f = Files.findFile("com/zzh/json/simplePerson.txt");
		Person p = Json.fromJson(Person.class, new InputStreamReader(new FileInputStream(f)));
		assertEquals("youoo", p.getName());
		assertEquals("YouChunSheng", p.getRealname());
		assertEquals(69, p.getAge());
		assertEquals(40, p.getBirthday().getYear());
		assertEquals(7, p.getBirthday().getMonth());
		assertEquals(15, p.getBirthday().getDate());
	}

	public void testPersonObject() throws Exception {
		File f = Files.findFile("com/zzh/json/person.txt");
		Person p = Json.fromJson(Person.class, new InputStreamReader(new FileInputStream(f)));
		StringBuilder sb = new StringBuilder();
		Writer w = new OutputStreamWriter(new CharOutputStream(sb));
		w.write(p.dump());
		w.write("\n");
		w.write(p.getFather().dump());
		w.write("\n");
		w.write(p.getCompany().getName());
		w.write("\n");
		w.write(p.getCompany().getCreator().dump());
		w.close();
		f = Files.findFile("com/zzh/json/person.expect.txt");

		assertTrue(Streams.equals(new CharInputStream(sb), new FileInputStream(f)));
	}

	public void testSimpleArray() throws Exception {
		String[] expAry = { "abc", "bbc", "fff" };
		String s = String.format("[%s]", Lang.concatBy("\"%s\"", ',', expAry));
		String[] reAry = Json.fromJson(String[].class, Lang.inr(s));
		assertTrue(Arrays.equals(expAry, reAry));

	}

	public void testSimpleArraySingleInteger() throws Exception {
		String s = "[2]";
		int[] ary = Json.fromJson(int[].class, Lang.inr(s));
		assertEquals(1, ary.length);
		assertEquals(2, ary[0]);
	}

	@SuppressWarnings("deprecation")
	public void testSimpleArraySingleDate() throws Exception {
		String s = "[\"2008-8-1\"]";
		java.sql.Date[] ary = Json.fromJson(java.sql.Date[].class, Lang.inr(s));
		assertEquals(1, ary.length);
		assertEquals(108, ary[0].getYear());
		assertEquals(7, ary[0].getMonth());
		assertEquals(1, ary[0].getDate());
	}

	public void testSimpleArraySingleObject() throws Exception {
		String s = "[{\"id\":24,\"name\":\"RRR\"}]";
		Abc[] ary = Json.fromJson(Abc[].class, Lang.inr(s));
		assertEquals(1, ary.length);
		assertEquals(24, ary[0].id);
		assertEquals("RRR", ary[0].name);
	}

	public void testSimpleObjectArray() throws Exception {
		String s = "[{\"id\":3,\"name\":\"A\"},{\"id\":10,\"name\":\"B\"}]";
		Abc[] ary = Json.fromJson(Abc[].class, Lang.inr(s));
		assertEquals(2, ary.length);
		assertEquals(3, ary[0].id);
		assertEquals(10, ary[1].id);
		assertEquals("A", ary[0].name);
		assertEquals("B", ary[1].name);
	}

	public void testNiceModeSimple() throws Exception {
		String s = "{id:45,name:\"x{y:12,t:'yzy'}z\"}";
		Abc abc = Json.fromJson(Abc.class, Lang.inr(s));
		assertEquals(45, abc.id);
		assertEquals("x{y:12,t:'yzy'}z", abc.name);

		s = "{id:45,name:'\"X\"'}";
		abc = Json.fromJson(Abc.class, Lang.inr(s));
		assertEquals(45, abc.id);
		assertEquals("\"X\"", abc.name);
	}

	@SuppressWarnings("deprecation")
	public void testParseNullFieldObject() throws Exception {
		File f = Files.findFile("com/zzh/json/personNull.txt");
		Person p = Json.fromJson(Person.class, new InputStreamReader(new FileInputStream(f)));
		assertEquals("youoo", p.getName());
		assertEquals("YouChunSheng", p.getRealname());
		assertEquals(69, p.getAge());
		assertEquals(40, p.getBirthday().getYear());
		assertEquals(7, p.getBirthday().getMonth());
		assertEquals(15, p.getBirthday().getDate());
	}

	public void testPrintJsonObject() throws Exception {
		File f = Files.findFile("com/zzh/json/person.txt");
		Person p = Json.fromJson(Person.class, new InputStreamReader(new FileInputStream(f)));
		String json = Json.toJson(p, JsonFormat.nice());
		Person p2 = Json.fromJson(Person.class, Lang.inr(json));
		assertEquals(p.getName(), p2.getName());
		assertEquals(p.getRealname(), p2.getRealname());
		assertEquals(p.getAge(), p2.getAge());
		assertEquals(p.getBirthday(), p2.getBirthday());
		assertEquals(p.getFather().getName(), p2.getFather().getName());
		assertEquals(p.getFather().getRealname(), p2.getFather().getRealname());
		assertEquals(p.getFather().getAge(), p2.getFather().getAge());
		assertEquals(p.getFather().getBirthday(), p2.getFather().getBirthday());
		assertEquals(p.getCompany().getName(), p2.getCompany().getName());
		assertEquals(p.getCompany().getCreator().getName(), p2.getCompany().getCreator().getName());
		assertEquals(p.getCompany().getCreator().getRealname(), p2.getCompany().getCreator()
				.getRealname());
		assertEquals(p.getCompany().getCreator().getAge(), p2.getCompany().getCreator().getAge());
		assertEquals(p.getCompany().getCreator().getFather(), p2.getCompany().getCreator()
				.getFather());
		assertEquals(p.getCompany().getCreator().getBirthday(), p2.getCompany().getCreator()
				.getBirthday());
	}

	public void testFilterField() throws Exception {
		File f = Files.findFile("com/zzh/json/person.txt");
		Person p = Json.fromJson(Person.class, new InputStreamReader(new FileInputStream(f)));
		String json = Json.toJson(p, JsonFormat.nice().setActivedFields("[name]"));
		Person p2 = Json.fromJson(Person.class, Lang.inr(json));
		assertEquals(p.getName(), p2.getName());
		assertNull(p2.getRealname());
		assertNull(p2.getBirthday());
		assertNull(p2.getFather());
		assertNull(p2.getCompany());
		assertEquals(0, p2.getAge());
	}

	public void testFilterField2() throws Exception {
		File f = Files.findFile("com/zzh/json/person.txt");
		Person p = Json.fromJson(Person.class, new InputStreamReader(new FileInputStream(f)));
		String json = Json.toJson(p, JsonFormat.nice().setIgnoreFields(
				"[realname][father][company]"));
		Person p2 = Json.fromJson(Person.class, Lang.inr(json));
		assertNull(p2.getRealname());
		assertEquals(p.getName(), p2.getName());
		assertEquals(p.getAge(), p2.getAge());
		assertEquals(p.getBirthday(), p2.getBirthday());
	}

	public static class Project {
		public int id;
		public String name;
		public String alias;
	}

	public void testOutpuProjectsAsList() throws Exception {
		String exp = "{\"id\":1,\"name\":\"nutz\",\"alias\":\"nutz\"}";
		Project p = new Project();
		p.id = 1;
		p.name = "nutz";
		p.alias = "nutz";
		assertEquals(exp, Json.toJson(p, JsonFormat.compact()));
	}

	public void testUndefined() throws Exception {
		String exp = "{id:45,name:'GG',alias:undefined}";
		Project p = Json.fromJson(Project.class, Lang.inr(exp));
		assertEquals(45, p.id);
		assertEquals("GG", p.name);
		assertNull(p.alias);
	}

	public static class X {
		public int id;
		public XT type;
	}

	public static enum XT {
		A, B
	}

	public void testEnumOutput() throws Exception {
		X x = new X();
		x.id = 5;
		x.type = XT.B;
		assertEquals("{\"id\":5,\"type\":\"B\"}", Json.toJson(x, JsonFormat.compact()));
	}

}