package de.fuberlin.wiwiss.d2rq.sql;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import junit.framework.TestCase;
import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.algebra.AliasMap;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.ColumnRenamerMap;
import de.fuberlin.wiwiss.d2rq.algebra.RelationName;
import de.fuberlin.wiwiss.d2rq.algebra.AliasMap.Alias;

public class SQLSyntaxTest extends TestCase {
	private final static Attribute foo_col1 = new Attribute(null, "foo", "col1");
	private final static Attribute bar_col2 = new Attribute(null, "bar", "col2");
	private final static Alias fooAsBar = new Alias(
			new RelationName(null, "foo"),
			new RelationName(null, "bar"));
	
	public void testParseRelationNameNoSchema() {
		RelationName r = SQL.parseRelationName("table");
		assertEquals("table", r.tableName());
		assertNull(r.schemaName());
	}
	
	public void testParseRelationNameWithSchema() {
		RelationName r = SQL.parseRelationName("schema.table");
		assertEquals("table", r.tableName());
		assertEquals("schema", r.schemaName());
	}
	
	public void testParseInvalidRelationName() {
		try {
			SQL.parseRelationName(".");
			fail();
		} catch (D2RQException ex) {
			assertEquals(D2RQException.SQL_INVALID_RELATIONNAME, ex.errorCode());
		}
	}

	public void testParseInvalidAttributeName() {
		try {
			SQL.parseAttribute("column");
			fail("not fully qualified name -- should have failed");
		} catch (D2RQException ex) {
			assertEquals(D2RQException.SQL_INVALID_ATTRIBUTENAME, ex.errorCode());
		}
	}

	public void testFindColumnInEmptyExpression() {
		assertEquals(Collections.EMPTY_SET, SQL.findColumnsInExpression("1+2"));
	}
	
	public void testNumbersInExpressionsAreNotColumns() {
		assertEquals(Collections.EMPTY_SET, SQL.findColumnsInExpression("1.2"));
	}
	
	public void testFindColumnInColumnName() {
		assertEquals(Collections.singleton(foo_col1),
				SQL.findColumnsInExpression("foo.col1"));
	}
	
	public void testFindColumnsInExpression() {
		assertEquals(new HashSet(Arrays.asList(new Attribute[]{foo_col1, bar_col2})),
				SQL.findColumnsInExpression("foo.col1 + bar.col2 = 135"));
	}
	
	public void testFindColumnsInExpressionWithSchema() {
		assertEquals(new HashSet(Arrays.asList(new Attribute[]{
				new Attribute("s1", "t1", "c1"), 
				new Attribute("s2", "t2", "c2")})),
				SQL.findColumnsInExpression("s1.t1.c1 + s2.t2.c2 = 135"));
	}

	public void testReplaceColumnsInExpressionWithAliasMap() {
		Alias alias = new Alias(new RelationName(null, "foo"), new RelationName(null, "bar"));
		AliasMap fooAsBar = new AliasMap(Collections.singleton(alias));
		assertEquals("bar.col1", 
				SQL.replaceColumnsInExpression("foo.col1", fooAsBar));
		assertEquals("LEN(bar.col1) > 0", 
				SQL.replaceColumnsInExpression("LEN(foo.col1) > 0", fooAsBar));
		assertEquals("baz.col1", 
				SQL.replaceColumnsInExpression("baz.col1", fooAsBar));
		assertEquals("fooo.col1", 
				SQL.replaceColumnsInExpression("fooo.col1", fooAsBar));
		assertEquals("ofoo.col1", 
				SQL.replaceColumnsInExpression("ofoo.col1", fooAsBar));
	}
	
	public void testReplaceColumnsWithSchemaInExpressionWithAliasMap() {
		Alias alias = new Alias(new RelationName("schema", "foo"), new RelationName("schema", "bar"));
		AliasMap fooAsBar = new AliasMap(Collections.singleton(alias));
		assertEquals("schema.bar.col1", 
				SQL.replaceColumnsInExpression("schema.foo.col1", fooAsBar));
	}
	
	public void testReplaceColumnsInExpressionWithColumnReplacer() {
		Map map = new HashMap();
		map.put(foo_col1, bar_col2);
		ColumnRenamerMap col1ToCol2 = new ColumnRenamerMap(map);
		assertEquals("bar.col2", 
				SQL.replaceColumnsInExpression("foo.col1", col1ToCol2));
		assertEquals("LEN(bar.col2) > 0", 
				SQL.replaceColumnsInExpression("LEN(foo.col1) > 0", col1ToCol2));
		assertEquals("foo.col3", 
				SQL.replaceColumnsInExpression("foo.col3", col1ToCol2));
		assertEquals("foo.col11", 
				SQL.replaceColumnsInExpression("foo.col11", col1ToCol2));
		assertEquals("ofoo.col1", 
				SQL.replaceColumnsInExpression("ofoo.col1", col1ToCol2));
	}

	public void testParseAliasIsCaseInsensitive() {
		assertEquals(fooAsBar, SQL.parseAlias("foo AS bar"));
		assertEquals(fooAsBar, SQL.parseAlias("foo as bar"));
	}

	public void testParseAlias() {
		assertEquals(
				new Alias(new RelationName(null, "table1"), new RelationName("schema", "table2")),
				SQL.parseAlias("table1 AS schema.table2"));
	}
	
	public void testParseInvalidAlias() {
		try {
			SQL.parseAlias("asdf");
		} catch (D2RQException ex) {
			assertEquals(D2RQException.SQL_INVALID_ALIAS, ex.errorCode());
		}
	}
}