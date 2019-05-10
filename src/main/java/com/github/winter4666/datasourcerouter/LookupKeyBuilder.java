package com.github.winter4666.datasourcerouter;

import java.util.List;

/**
 * 构建lookupKey
 * @author wutian
 */
public interface LookupKeyBuilder {
	
	String getLookupKey(Object param);
	
	List<String> getAllLookupKeys();

}
