package com.flyinghead.alienfront;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RankingServletTest {
	private RankingServlet rankingServlet;
	private static final String DB_URL = "jdbc:h2:/tmp/ranking.db";

	@Before
	public void setupGuestBookServlet() throws ServletException
	{
		new File(DB_URL.substring(8) + ".mv.db").delete();
		rankingServlet = new RankingServlet();
		ServletConfig servletConfig = mock(ServletConfig.class);
		ServletContext servletContext = mock(ServletContext.class);
		when(servletConfig.getServletContext()).thenReturn(servletContext);
		doNothing().when(servletContext).log("");
		when(servletConfig.getInitParameter("databaseUrl")).thenReturn(DB_URL);
		rankingServlet.init(servletConfig);
	}

	@After
	public void tearDownHelper() {
		rankingServlet.destroy();
		new File(DB_URL.substring(8) + ".mv.db").delete();
	}

	@Test
	public void testGetRanking() throws IOException, ServletException, SQLException
	{
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		StringWriter stringWriter = new StringWriter();
		when(request.getReader()).thenReturn(new BufferedReader(new StringReader("request=2")));
	    when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));

		rankingServlet.doPost(request, response);
		
		assertEquals("***&&&", stringWriter.toString());
		
		Connection conn = DriverManager.getConnection(DB_URL, "sa", "");
		Statement stmt = conn.createStatement();
		stmt.execute("INSERT INTO RANKING (SCORE, PLAYER_NAME, ARCADE_NAME, CITY, STATE, DATE, IP_ADDR, EXTRA) "
				+ "VALUES (123456, 'TESTUSER', 'AMNESIA', 'GENEVA', 'GE', CURRENT_TIMESTAMP, '192.168.1.1', '0&1&2&3')");
		stmt.close();
		conn.close();

		request = mock(HttpServletRequest.class);
		response = mock(HttpServletResponse.class);
		stringWriter = new StringWriter();
		when(request.getReader()).thenReturn(new BufferedReader(new StringReader("request=2")));
		when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));
		rankingServlet.doPost(request, response);
		
		assertEquals("***123456:TESTUSER:AMNESIA:GENEVA:GE&&&", stringWriter.toString());
	}

	@Test
	public void testSetScore() throws IOException, ServletException, SQLException
	{
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		StringWriter stringWriter = new StringWriter();
		when(request.getReader()).thenReturn(new BufferedReader(new StringReader("request=1 1abd6000ac23d95f49dc1f9e6b928884e0bae15e4a161ebb305d0a1b45ed003f80de2d00e945d5a2856e55d001fa5e043aa69b158ee77a71913e2a683af67dae")));
	    when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));

		rankingServlet.doPost(request, response);
		
		Connection conn = DriverManager.getConnection(DB_URL, "sa", "");
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery("SELECT TOP 10 SCORE, PLAYER_NAME, ARCADE_NAME, CITY, STATE FROM RANKING ORDER BY SCORE DESC");
		assertTrue(rs.next());
		assertEquals(137000, rs.getInt(1));
		assertEquals("FLY2", rs.getString(2));
		assertEquals("FLYCAST", rs.getString(3));
		assertEquals("PARIS", rs.getString(4));
		assertEquals("", rs.getString(5));
		stmt.close();
		conn.close();
	}
}
