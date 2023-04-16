package com.flyinghead.alienfront;

public class Utils
{
	public static byte[] hexStringToBytes(String s)
	{
		byte[] bytes = new byte[s.length() / 2];
		for (int i = 0; i < s.length(); i += 2)
			bytes[i / 2] = Integer.valueOf(s.substring(i, i + 2), 16).byteValue();
		return bytes;
	}
}
