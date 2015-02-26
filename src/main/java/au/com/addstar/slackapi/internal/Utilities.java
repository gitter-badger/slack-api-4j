package au.com.addstar.slackapi.internal;

import java.util.Collections;
import java.util.Map;

import com.google.gson.JsonElement;

public class Utilities
{
	/**
	 * Parses the element as a unix timestamp 
	 * @param element The element to convert
	 * @return The time in milliseconds
	 */
	public static long getAsTimestamp(JsonElement element)
	{
		String raw = element.getAsString();
		
		long time;
		if (raw.contains("."))
		{
			double precTime = Double.parseDouble(raw);
			time = (long)(precTime * 1000);
		}
		else
			time = Long.parseLong(raw) * 1000;
		
		return time;
	}
	
	/**
	 * So I dont have to force type Collections.emptyMap() for parameters
	 */
	public static final Map<String, Object> EMPTY_MAP = Collections.emptyMap();
}