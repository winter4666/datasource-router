# datasource-router
基于spring的`AbstractRoutingDataSource`，封装了一些工具类和注解，使多数据源的切换变得更加简单自然

## 简介
代码基本实现思路来源于文章[Dynamic DataSource Routing](https://spring.io/blog/2007/01/23/dynamic-datasource-routing/)，再此基础之上，利用spring的aop特性，简化了切换多数据源的方式。

## 使用方法
1. 修改spring配置，启用`@AspectJ`支持
```xml
<aop:aspectj-autoproxy/>
```

2. 使用`RoutingDataSource`作为数据源
```xml
<bean id="dataSource" class="com.github.winter4666.datasourcerouter.RoutingDataSource">
    <property name="targetDataSources">
        <map key-type="java.lang.String">
            <entry key="db1" value-ref="db1DataSource"/>
            <entry key="db2" value-ref="db2DataSource"/>
        </map>
    </property>
    <property name="defaultTargetDataSource" ref="db1"/>
</bean>
```

3. `DSRAspect`注入spring
```xml
<bean id="dataSourceRouter" class="com.github.winter4666.datasourcerouter.DataSourceRouter"/>
    
<bean id="sharedTransactionTemplate" class="com.github.winter4666.datasourcerouter.DSRTransactionTemplate">
    <property name="transactionManager" ref="txManager"/>
    <property name="dataSourceRouter" ref="dataSourceRouter"/>
</bean>
   
<bean class="com.github.winter4666.datasourcerouter.DSRAspect">
    <property name="dataSourceRouter" ref="dataSourceRouter"/>
    <property name="dsrTransactionTemplate" ref="sharedTransactionTemplate"/>
</bean>
```

5. 使用注解切换数据源

在dao层的类或方法上，加上`@DataSource`注解，方法执行访问数据库时，将使用`@DataSource`注解所指定的数据源。
```java
@DataSource("db2")
@Repository
public class UserDao {
	
	public User getUserById(Long id) {
		//通过用户id查找用户
	}

}
```
```java
@Repository
public class UserDao {
	
	@DataSource("db2")
	public User getUserById(Long id) {
		//通过用户id查找用户
	}

}
```
在service层的类或方法上，加上`@DSRTransactional`注解，使用多数据源版本的声明式事务
```java
@DSRTransactional("db2")
@Service
public class UserServiceImpl implements UserService{
	
	public void transfer(long fromUserId,long toUserId,int amount) {
		//转账
	}

}
```
```java
@Service
public class UserServiceImpl implements UserService{
	
	@DSRTransactional("db2")
	public void transfer(long fromUserId,long toUserId,int amount) {
		//转账
	}

}
```

6. 也可以直接使用`DataSourceRouter`类和`DSRTransactionTemplate`类，通过编程的方式切换数据源，在某些场景下，这样比使用注解灵活。
```java
@Repository
public class UserDao {
	
	@Autowired
	private DataSourceRouter dataSourceRouter;
	
	public User getUserById(Long id) {
		return dataSourceRouter.accessDb("db2", new DataSourceCallback<User>() {
		
			public User doWithDataSource() {
				//通过用户id查找用户
			}
			
		});
	}

}
```
```java
@Service
public class UserServiceImpl implements UserService{
	
	private DSRTransactionTemplate transactionTemplate;
	
	public void transfer(long fromUserId,long toUserId,int amount) {
		transactionTemplate.execute("db2", new TransactionCallbackWithoutResult() {
			
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				//转账
			}
		});
	}

}
```