package com.flyinghead.alienfront;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import javax.crypto.Cipher;
import javax.crypto.spec.RC5ParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

@WebServlet("/Ranking")
public class RankingServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection conn;
	private String databaseUrl = "jdbc:h2:/var/lib/tomcat9/lib/ranking.db";
       
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String content = request.getReader().readLine();
		if ("request=2".equals(content))
		{
			PrintWriter writer = response.getWriter().append("***");
			try {
				Connection conn = getConnection();
				Statement stmt = conn.createStatement();
				try {
					ResultSet rs = stmt.executeQuery("SELECT TOP 10 SCORE, PLAYER_NAME, ARCADE_NAME, CITY, STATE FROM RANKING ORDER BY SCORE DESC");
					boolean first = true;
					while (rs.next())
					{
						if (!first)
							writer.append('&');
						first = false;
						writer.append(Integer.toString(rs.getInt("SCORE"))).append(':')
							.append(rs.getString("PLAYER_NAME")).append(':')
							.append(rs.getString("ARCADE_NAME")).append(':')
							.append(rs.getString("CITY")).append(':')
							.append(rs.getString("STATE"));
					}
				} finally {
					stmt.close();
				}
				writer.append("&&&");
			} catch (SQLException e) {
				log("SQL Error: ", e);
				throw new ServletException(e);
			}
		}
		else if (content != null && content.startsWith("request=1 "))
		{
			// FLY2&FLYCAST&PARIS&&192.168.167.2&137000&0&0&1&7&4&577&0&12
			// userName & arcadeName & City & State & ipAddress & score & 0 & 0 & 1 & 7 & 4 & 577 & 0 & 12
			content = content.substring(10);
			byte[] bytes = Utils.hexStringToBytes(content);
			try {
				bytes = rc5Decrypt(bytes);
			} catch (GeneralSecurityException e) {
				log("RC5 decryption error", e);
				throw new ServletException(e);
			}
			String s = new String(bytes, "UTF-8");
			log("Ranking request=1 " + s);
			String[] parts = s.split("&");
			if (parts.length > 5 && parts[0].length() > 0 && parts[0].length() <= 7)
			{
				try {
					Connection conn = getConnection();
					PreparedStatement stmt = conn.prepareStatement("INSERT INTO RANKING (PLAYER_NAME, SCORE, ARCADE_NAME, CITY, STATE, IP_ADDR, DATE, EXTRA) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
					try {
						stmt.setString(1, parts[0]);				// player name
						stmt.setInt(2, Integer.parseInt(parts[5]));	// score
						stmt.setString(3, parts[1]);				// arcade name
						stmt.setString(4, parts[2]);				// city
						stmt.setString(5, parts[3]);				// state
						stmt.setString(6, request.getRemoteAddr());	// IP address
						stmt.setTimestamp(7, new Timestamp(System.currentTimeMillis()));
						StringBuffer sb = new StringBuffer();
						for (int i = 6; i < parts.length; i++) {
							if (sb.length() != 0)
								sb.append('&');
							sb.append(parts[i]);
						}
						stmt.setString(8, sb.toString());			// extra
						stmt.execute();
					} finally {
						stmt.close();
					}
				} catch (SQLException e) {
					log("SQL Error: ", e);
					throw new ServletException(e);
				}
			}
		}
		else {
			response.sendError(500, "Unsupported request: " + content);
		}
	}
	
	private byte[] rc5Decrypt(byte[] cipher) throws GeneralSecurityException
	{
		Cipher c = Cipher.getInstance("RC5/ECB/NoPadding");
		byte[] keyBytes = { 0x01, (byte)0xD3, (byte)0xB4, (byte)0x90, (byte)0xAB, 0x32, 0x2D, (byte)0xC7 };
		SecretKeySpec rc5Key = new SecretKeySpec(keyBytes, "RC5");
		RC5ParameterSpec params = new RC5ParameterSpec(0, 12, 32);
		c.init(Cipher.DECRYPT_MODE, rc5Key, params);
		return c.doFinal(cipher);
	}
	
	private Connection getConnection() throws SQLException
	{
		if (conn == null)
		{
			try {
				Class.forName("org.h2.Driver");
			} catch (ClassNotFoundException e) {
				throw new SQLException("Can't load H2 jdbc driver", e);
			}
			conn = DriverManager.getConnection(databaseUrl, "sa", "");
			ResultSet tables = conn.getMetaData().getTables(null, null, "RANKING", null);
			if (!tables.next())
			{
				Statement stmt = conn.createStatement();
				try {
					stmt.execute(
						"CREATE TABLE RANKING(ID INT, PLAYER_NAME VARCHAR, SCORE INT, ARCADE_NAME VARCHAR, CITY VARCHAR, STATE VARCHAR, IP_ADDR VARCHAR, DATE TIMESTAMP, EXTRA VARCHAR)");
					stmt.execute(
						"CREATE INDEX ON RANKING (SCORE DESC)");
				} finally {
					stmt.close();
				}
			}
		}
		return conn;
	}

	@Override
	public void init() throws ServletException {
		Security.addProvider(new BouncyCastleProvider());
		String url = getServletConfig().getInitParameter("databaseUrl");
		if (url != null)
			databaseUrl = url;
	}

	@Override
	public void destroy() {
		if (conn != null)
			try {
				conn.close();
				conn = null;
			} catch (SQLException e) {
				log("database close error", e);
			}
		Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
		super.destroy();
	}
}
