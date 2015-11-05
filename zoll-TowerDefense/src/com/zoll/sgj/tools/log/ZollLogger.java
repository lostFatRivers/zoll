package com.zoll.sgj.tools.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ZollLogger {
	private static final Logger logger = LoggerFactory.getLogger("CONSOLE");
	
	public static void log(Object obj) {
		logger.info(obj.toString());
	}
}
