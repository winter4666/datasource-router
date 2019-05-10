package com.github.winter4666.datasourcerouter;

/**
 * 存储多数据源用的lookupKey
 * @author wutian
 */
class ContextHolder {
	
	private static final ThreadLocal<String> contextHolder = new ThreadLocal<String>();
	
    static void setLookupKey(String lookupKey) {
	    contextHolder.set(lookupKey);
    }

    static String getLookupKey() {
	    return (String) contextHolder.get();
    }

    static void clearLookupKey() {
	    contextHolder.remove();
    }

}
