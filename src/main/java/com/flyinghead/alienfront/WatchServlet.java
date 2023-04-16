package com.flyinghead.alienfront;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/Watch")
public class WatchServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		/*
		String content = request.getReader().readLine();
		if (content != null)
		{
			String[] params = content.split("&");
			for (int i = 0; i < params.length; i++)
			{
				String[] nameValue = params[i].split("=");
				if (nameValue.length == 2)
				{
					log(nameValue[0]);
					byte[] value = Utils.hexStringToBytes(nameValue[1]);
					for (byte b : value) {
						b = unscramble(b);
						log("unscramble " + Integer.toHexString((int)b & 0xff) + " " + (char)b);
					}
				}
			}
		}
		*/
		response.getWriter()
//			.append("Address=192.168.1.30\n")
//			.append("Port=17777\n")
//			.append("Response=20\n")
			.append("END\n");
	}
	
	private byte unscramble(byte b) {
		int i = ~b;
		i = ((i >> 5) & 7) | ((i << 3) & 0xf1);
		return (byte)i;
	}
}
