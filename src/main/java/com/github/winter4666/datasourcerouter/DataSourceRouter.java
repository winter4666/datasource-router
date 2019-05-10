package com.github.winter4666.datasourcerouter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 多数据源路由器
 * @author wutian
 */
public class DataSourceRouter {
	
	private final static Logger logger = LoggerFactory.getLogger(DataSourceRouter.class);
	
	private Map<Class<? extends LookupKeyBuilder>, LookupKeyBuilder> lookupKeyBuilders = new HashMap<>();
	
	private ExecutorService executorService;
	
	public DataSourceRouter() {
		
	}
	
	public DataSourceRouter(Integer concurrentAccessWorker) {
		executorService = Executors.newFixedThreadPool(concurrentAccessWorker,new ThreadFactory() {
			
			@Override
			public Thread newThread(Runnable r) {
                Thread t = Executors.defaultThreadFactory().newThread(r);
                t.setDaemon(true);
				return t;
			}
		});
	}
	
	private LookupKeyBuilder getLookupKeyBuilder(Class<? extends LookupKeyBuilder> lookupKeyBuilderClass) {
		try {
			LookupKeyBuilder lookupKeyBuilder = lookupKeyBuilders.get(lookupKeyBuilderClass);
			if(lookupKeyBuilder == null) {
				lookupKeyBuilder = lookupKeyBuilderClass.newInstance();
				lookupKeyBuilders.put(lookupKeyBuilderClass, lookupKeyBuilder);
			}
			return lookupKeyBuilder;
		} catch (InstantiationException | IllegalAccessException e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException("getLookupKey error,lookupKeyBuilderClass=" + lookupKeyBuilderClass.getName());
		}
	}
	
	/**
	 * 直接访问lookupKey指定的数据源
	 * @param lookupKey
	 * @param dataSourceCallback
	 * @return
	 */
	public final <T> T accessDb(String lookupKey,DataSourceCallback<T> dataSourceCallback) {
    	if(lookupKey == null) {
    		throw new RuntimeException("lookupKey is null,but lookupKey is required");
    	}
		String existedLookupKey = (String)ContextHolder.getLookupKey();
		if(existedLookupKey != null) {
			if(existedLookupKey.equals(lookupKey)) {
				return dataSourceCallback.doWithDataSource();
			} else {
				throw new RuntimeException("accessDb failed,lookupKey " + existedLookupKey + " in context is existed,but expected lookupKey is " + lookupKey);
			}
		} else {
			ContextHolder.setLookupKey(lookupKey);
	        try {
	        	return dataSourceCallback.doWithDataSource();
	        } finally {
	        	ContextHolder.clearLookupKey();;
	        }
		}
	}
	
	/**
	 * 通过param和lookupKeyBuilderClass生成lookupKey，然后访问该数据源
	 * @param param
	 * @param lookupKeyBuilderClass
	 * @param dataSourceCallback
	 * @return
	 */
	public <T> T accessDb(Object param,Class<? extends LookupKeyBuilder> lookupKeyBuilderClass,DataSourceCallback<T> dataSourceCallback) {
		return accessDb(getLookupKeyBuilder(lookupKeyBuilderClass).getLookupKey(param), dataSourceCallback);
	}
	
	/**
	 * 访问lookupKeyBuilderClass中存在的多个数据源，直到获取到一个不为空的返回结果为止，若所有数据源都返回空的结果，则该方法返回空值
	 * <p/>
	 * 该方法将通过判断executorService是否为空来选择访问数据源的方式，有并发访问和串行访问两种方式：并发访问效率更高，需要使用线程池；串行访问速度更慢，但无需另外启动线程
	 * @param lookupKeyBuilderClass
	 * @param dataSourceCallback
	 * @return
	 */
	public <T> T accessDbsForOneResult(Class<? extends LookupKeyBuilder> lookupKeyBuilderClass,DataSourceCallback<T> dataSourceCallback) {
		if(executorService == null) {
			return serialAccessDbsForOneResult(lookupKeyBuilderClass, dataSourceCallback);
		} else {
			return concurrentAccessDbsForOneResult(lookupKeyBuilderClass, dataSourceCallback);
		}
	}
	
	private <T> T serialAccessDbsForOneResult(Class<? extends LookupKeyBuilder> lookupKeyBuilderClass, DataSourceCallback<T> dataSourceCallback) {
    	for(String lookupKey : getLookupKeyBuilder(lookupKeyBuilderClass).getAllLookupKeys()) {
        	T data = accessDb(lookupKey, dataSourceCallback);
        	if(data != null) {
        		return data;
        	}
    	}
    	return null;
	}
	
	private <T> T concurrentAccessDbsForOneResult(Class<? extends LookupKeyBuilder> lookupKeyBuilderClass,final DataSourceCallback<T> dataSourceCallback) {
			List<String> lookupKeys = getLookupKeyBuilder(lookupKeyBuilderClass).getAllLookupKeys();
			CompletionService<T> completionService = new ExecutorCompletionService<>(executorService);
			for(final String lookupKey : lookupKeys) {
				completionService.submit(new Callable<T>() {
		
					@Override
					public T call() throws Exception {
						return accessDb(lookupKey, dataSourceCallback);
					}
				});
			}
			
			try {
				for(int i = 0;i < lookupKeys.size();i++) {
					Future<T> future = completionService.take();
					T data = future.get();
					if(data != null) {
						return data;
					}
				}
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException("concurrentAccessDbsForOneResult failed",e);
			}
			return null;
	}
	
	/**
	 * 访问lookupKeyBuilderClass中存在的多个数据源，并将所有结果合并成一个列表，然后一起返回
	 * <p/>
	 * 该方法将通过判断executorService是否为空来选择访问数据源的方式，有并发访问和串行访问两种方式：并发访问效率更高，需要使用线程池；串行访问速度更慢，但无需另外启动线程
	 * @param lookupKeyBuilderClass
	 * @param dataSourceCallback
	 * @return
	 */
    public <T> List<T> accessAllDbsAndMergeResults(Class<? extends LookupKeyBuilder> lookupKeyBuilderClass,DataSourceCallback<?> dataSourceCallback) {
		if(executorService == null) {
			return serialAccessAllDbsAndMergeResults(lookupKeyBuilderClass, dataSourceCallback);
		} else {
			return concurrentAccessAllDbsAndMergeResults(lookupKeyBuilderClass, dataSourceCallback);
		}
    }
    
    @SuppressWarnings("unchecked")
    private <T> List<T> serialAccessAllDbsAndMergeResults(Class<? extends LookupKeyBuilder> lookupKeyBuilderClass, DataSourceCallback<?> dataSourceCallback) {
    	List<T> list = new ArrayList<T>();
    	for(String lookupKey : getLookupKeyBuilder(lookupKeyBuilderClass).getAllLookupKeys()) {
    		Object result = accessDb(lookupKey, dataSourceCallback);
    		if(result != null) {
    			if(result instanceof List) {
    				list.addAll((List<T>)result);
    			} else {
    				list.add((T)result);
    			}
    		}
    	}
    	return list;
    }
    
    @SuppressWarnings("unchecked")
    private <T> List<T> concurrentAccessAllDbsAndMergeResults(Class<? extends LookupKeyBuilder> lookupKeyBuilderClass,final DataSourceCallback<?> dataSourceCallback) {
		List<String> lookupKeys = getLookupKeyBuilder(lookupKeyBuilderClass).getAllLookupKeys();
		List<Callable<Object>> tasks = new ArrayList<>();
		for(final String lookupKey : lookupKeys) {
			tasks.add(new Callable<Object>() {

				@Override
				public Object call() throws Exception {
					return  accessDb(lookupKey, dataSourceCallback);
				}
				
			});
		}
		
		try {
	    	List<T> list = new ArrayList<T>();
	    	for(Future<Object> future : executorService.invokeAll(tasks)) {
	    		Object result = future.get();
	    		if(result != null) {
	    			if(result instanceof List) {
	    				list.addAll((List<T>)result);
	    			} else {
	    				list.add((T)result);
	    			}
	    		}
	    	}
	    	return list;
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException("concurrentAccessAllDbsAndMergeResults failed",e);
		}
    	
    }
	
    public interface DataSourceCallback<T> {
    	T doWithDataSource();
    }

}
