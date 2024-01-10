/*
 *    Copyright 2009-2023 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.builder.xml;

import java.io.InputStream;
import java.io.Reader;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.builder.BaseBuilder;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.datasource.DataSourceFactory;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.loader.ProxyFactory;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.io.VFS;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.reflection.ReflectorFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
import org.apache.ibatis.session.AutoMappingBehavior;
import org.apache.ibatis.session.AutoMappingUnknownColumnBehavior;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.LocalCacheScope;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.type.JdbcType;

/**
 * @author Clinton Begin
 * @author Kazuki Shimizu
 */
public class XMLConfigBuilder extends BaseBuilder {

    //   标识是否已经解析过了
    private boolean parsed;
    //    用于解析mybatis-config.xml的XPathParser，前面讲过，这个相当于Mybatis对XML处理的那些Xpath实现的封装
    private final XPathParser parser;
    //    标识＜environment ＞配置的名称，默认读取＜environment> 标签的 default 属性
    private String environment;
    //    默认反射工厂，负责创建和缓存反射对象
    private final ReflectorFactory localReflectorFactory = new DefaultReflectorFactory();

    public XMLConfigBuilder(Reader reader) {
        this(reader, null, null);
    }

    public XMLConfigBuilder(Reader reader, String environment) {
        this(reader, environment, null);
    }

    public XMLConfigBuilder(Reader reader, String environment, Properties props) {
        this(Configuration.class, reader, environment, props);
    }

    public XMLConfigBuilder(Class<? extends Configuration> configClass, Reader reader, String environment,
                            Properties props) {
        this(configClass, new XPathParser(reader, true, props, new XMLMapperEntityResolver()), environment, props);
    }

    public XMLConfigBuilder(InputStream inputStream) {
        this(inputStream, null, null);
    }

    public XMLConfigBuilder(InputStream inputStream, String environment) {
        this(inputStream, environment, null);
    }

    public XMLConfigBuilder(InputStream inputStream, String environment, Properties props) {
        this(Configuration.class, inputStream, environment, props);
    }

    public XMLConfigBuilder(Class<? extends Configuration> configClass, InputStream inputStream, String environment,
                            Properties props) {
        this(configClass, new XPathParser(inputStream, true, props, new XMLMapperEntityResolver()), environment, props);
    }

    private XMLConfigBuilder(Class<? extends Configuration> configClass, XPathParser parser, String environment,
                             Properties props) {
        super(newConfig(configClass));
        ErrorContext.instance().resource("SQL Mapper Configuration");
        this.configuration.setVariables(props);
        this.parsed = false;
        this.environment = environment;
        this.parser = parser;
    }

    public Configuration parse() {
        if (parsed) {
            throw new BuilderException("Each XMLConfigBuilder can only be used once.");
        }
        parsed = true;
//        在配置文件中查找"/configuration"节点，并且尝试解析成XNode对象
        parseConfiguration(parser.evalNode("/configuration"));
        return configuration;
    }

    private void parseConfiguration(XNode root) {
        try {
//            解析不同的配置节点
            propertiesElement(root.evalNode("properties"));
            Properties settings = settingsAsProperties(root.evalNode("settings"));
            loadCustomVfs(settings);
            loadCustomLogImpl(settings);
            typeAliasesElement(root.evalNode("typeAliases"));
            pluginElement(root.evalNode("plugins"));
            objectFactoryElement(root.evalNode("objectFactory"));
            objectWrapperFactoryElement(root.evalNode("objectWrapperFactory"));
            reflectorFactoryElement(root.evalNode("reflectorFactory"));
            settingsElement(settings);
            environmentsElement(root.evalNode("environments"));
            databaseIdProviderElement(root.evalNode("databaseIdProvider"));
            typeHandlerElement(root.evalNode("typeHandlers"));
            mapperElement(root.evalNode("mappers"));
        } catch (Exception e) {
            throw new BuilderException("Error parsing SQL Mapper Configuration. Cause: " + e, e);
        }
    }

    private Properties settingsAsProperties(XNode context) {
        if (context == null) {
            return new Properties();
        }
        Properties props = context.getChildrenAsProperties();
        // Check that all settings are known to the configuration class
        MetaClass metaConfig = MetaClass.forClass(Configuration.class, localReflectorFactory);
        for (Object key : props.keySet()) {
            if (!metaConfig.hasSetter(String.valueOf(key))) {
                throw new BuilderException(
                    "The setting " + key + " is not known.  Make sure you spelled it correctly (case sensitive).");
            }
        }
        return props;
    }

    private void loadCustomVfs(Properties props) throws ClassNotFoundException {
        String value = props.getProperty("vfsImpl");
        if (value != null) {
            String[] clazzes = value.split(",");
            for (String clazz : clazzes) {
                if (!clazz.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Class<? extends VFS> vfsImpl = (Class<? extends VFS>) Resources.classForName(clazz);
                    configuration.setVfsImpl(vfsImpl);
                }
            }
        }
    }

    private void loadCustomLogImpl(Properties props) {
        Class<? extends Log> logImpl = resolveClass(props.getProperty("logImpl"));
        configuration.setLogImpl(logImpl);
    }

    private void typeAliasesElement(XNode parent) {
        if (parent != null) {
//            获取下面的子节点
            for (XNode child : parent.getChildren()) {
//                处理<package>节点
                if ("package".equals(child.getName())) {
//                    获取指定的包名
                    String typeAliasPackage = child.getStringAttribute("name");
//                    通过TypeAliasRegistry扫描指定包内的所有类，并且解析@Alias，完成注册别名
                    configuration.getTypeAliasRegistry().registerAliases(typeAliasPackage);
                } else {
                    String alias = child.getStringAttribute("alias");
                    String type = child.getStringAttribute("type");
                    try {
                        Class<?> clazz = Resources.classForName(type);
                        if (alias == null) {
                            typeAliasRegistry.registerAlias(clazz);
                        } else {
                            typeAliasRegistry.registerAlias(alias, clazz);
                        }
                    } catch (ClassNotFoundException e) {
                        throw new BuilderException("Error registering typeAlias for '" + alias + "'. Cause: " + e, e);
                    }
                }
            }
        }
    }

    private void pluginElement(XNode parent) throws Exception {
//        判断当前节点不为空
        if (parent != null) {
//            便利其下的子节点，相当于便利下面的插件标签
            for (XNode child : parent.getChildren()) {
//                获取插件的interceptor属性，这个属性是指定拦截器的全限定名
                String interceptor = child.getStringAttribute("interceptor");
//                获取插件的属性，这个属性是插件的配置信息
                Properties properties = child.getChildrenAsProperties();
//                创建插件实例，并且设置属性，这里是使用构造方法创建的
                Interceptor interceptorInstance = (Interceptor) resolveClass(interceptor).getDeclaredConstructor()
                    .newInstance();
//                设置插件的属性
                interceptorInstance.setProperties(properties);
//                将插件添加到configuration的interceptorChain中
//                interceptorChain底层是一个List<Interceptor>
                configuration.addInterceptor(interceptorInstance);
            }
        }
    }

    private void objectFactoryElement(XNode context) throws Exception {
        if (context != null) {
//            获取objectFactory的type属性，这个属性是指定ObjectFactory的实现类的全限定名
            String type = context.getStringAttribute("type");
//            获取objectFactory的子节点，这些子节点的属性会被设置到ObjectFactory中
            Properties properties = context.getChildrenAsProperties();
//            通过构造器的方式创建ObjectFactory实例，并且设置属性
            ObjectFactory factory = (ObjectFactory) resolveClass(type).getDeclaredConstructor().newInstance();
//            设置ObjectFactory的属性
            factory.setProperties(properties);
//            设置configuration的objectFactory属性
            configuration.setObjectFactory(factory);
        }
    }

    private void objectWrapperFactoryElement(XNode context) throws Exception {
        if (context != null) {
            String type = context.getStringAttribute("type");
            ObjectWrapperFactory factory = (ObjectWrapperFactory) resolveClass(type).getDeclaredConstructor().newInstance();
            configuration.setObjectWrapperFactory(factory);
        }
    }

    private void reflectorFactoryElement(XNode context) throws Exception {
        if (context != null) {
            String type = context.getStringAttribute("type");
            ReflectorFactory factory = (ReflectorFactory) resolveClass(type).getDeclaredConstructor().newInstance();
            configuration.setReflectorFactory(factory);
        }
    }

    private void propertiesElement(XNode context) throws Exception {
        if (context != null) {
//            解析<properties>的子节点的name和value属性，并且记录到Properties中
            Properties defaults = context.getChildrenAsProperties();
//            解析<properties>标签的resource和url，这两个属性用于确定properties配置文件的位置
            String resource = context.getStringAttribute("resource");
            String url = context.getStringAttribute("url");
//            url和resource不能同时存在，否则会报异常
            if (resource != null && url != null) {
                throw new BuilderException(
                    "The properties element cannot specify both a URL and a resource based property file reference.  Please specify one or the other.");
            }
//            加载resource和url到Properties中
            if (resource != null) {
                defaults.putAll(Resources.getResourceAsProperties(resource));
            } else if (url != null) {
                defaults.putAll(Resources.getUrlAsProperties(url));
            }
//            与configuration中的variables集合合并
            Properties vars = configuration.getVariables();
            if (vars != null) {
                defaults.putAll(vars);
            }
//            更新XPathParse和Config的variables字段
            parser.setVariables(defaults);
            configuration.setVariables(defaults);
        }
    }

    private void settingsElement(Properties props) {
        configuration
            .setAutoMappingBehavior(AutoMappingBehavior.valueOf(props.getProperty("autoMappingBehavior", "PARTIAL")));
        configuration.setAutoMappingUnknownColumnBehavior(
            AutoMappingUnknownColumnBehavior.valueOf(props.getProperty("autoMappingUnknownColumnBehavior", "NONE")));
        configuration.setCacheEnabled(booleanValueOf(props.getProperty("cacheEnabled"), true));
        configuration.setProxyFactory((ProxyFactory) createInstance(props.getProperty("proxyFactory")));
        configuration.setLazyLoadingEnabled(booleanValueOf(props.getProperty("lazyLoadingEnabled"), false));
        configuration.setAggressiveLazyLoading(booleanValueOf(props.getProperty("aggressiveLazyLoading"), false));
        configuration.setMultipleResultSetsEnabled(booleanValueOf(props.getProperty("multipleResultSetsEnabled"), true));
        configuration.setUseColumnLabel(booleanValueOf(props.getProperty("useColumnLabel"), true));
        configuration.setUseGeneratedKeys(booleanValueOf(props.getProperty("useGeneratedKeys"), false));
        configuration.setDefaultExecutorType(ExecutorType.valueOf(props.getProperty("defaultExecutorType", "SIMPLE")));
        configuration.setDefaultStatementTimeout(integerValueOf(props.getProperty("defaultStatementTimeout"), null));
        configuration.setDefaultFetchSize(integerValueOf(props.getProperty("defaultFetchSize"), null));
        configuration.setDefaultResultSetType(resolveResultSetType(props.getProperty("defaultResultSetType")));
        configuration.setMapUnderscoreToCamelCase(booleanValueOf(props.getProperty("mapUnderscoreToCamelCase"), false));
        configuration.setSafeRowBoundsEnabled(booleanValueOf(props.getProperty("safeRowBoundsEnabled"), false));
        configuration.setLocalCacheScope(LocalCacheScope.valueOf(props.getProperty("localCacheScope", "SESSION")));
        configuration.setJdbcTypeForNull(JdbcType.valueOf(props.getProperty("jdbcTypeForNull", "OTHER")));
        configuration.setLazyLoadTriggerMethods(
            stringSetValueOf(props.getProperty("lazyLoadTriggerMethods"), "equals,clone,hashCode,toString"));
        configuration.setSafeResultHandlerEnabled(booleanValueOf(props.getProperty("safeResultHandlerEnabled"), true));
        configuration.setDefaultScriptingLanguage(resolveClass(props.getProperty("defaultScriptingLanguage")));
        configuration.setDefaultEnumTypeHandler(resolveClass(props.getProperty("defaultEnumTypeHandler")));
        configuration.setCallSettersOnNulls(booleanValueOf(props.getProperty("callSettersOnNulls"), false));
        configuration.setUseActualParamName(booleanValueOf(props.getProperty("useActualParamName"), true));
        configuration.setReturnInstanceForEmptyRow(booleanValueOf(props.getProperty("returnInstanceForEmptyRow"), false));
        configuration.setLogPrefix(props.getProperty("logPrefix"));
        configuration.setConfigurationFactory(resolveClass(props.getProperty("configurationFactory")));
        configuration.setShrinkWhitespacesInSql(booleanValueOf(props.getProperty("shrinkWhitespacesInSql"), false));
        configuration.setArgNameBasedConstructorAutoMapping(
            booleanValueOf(props.getProperty("argNameBasedConstructorAutoMapping"), false));
        configuration.setDefaultSqlProviderType(resolveClass(props.getProperty("defaultSqlProviderType")));
        configuration.setNullableOnForEach(booleanValueOf(props.getProperty("nullableOnForEach"), false));
    }

    private void environmentsElement(XNode context) throws Exception {
        if (context != null) {
//            <environments default="development">
//                <environment id="development">
//                  <transactionManager type="JDBC">
//                    <property name="" value=""/>
//                  </transactionManager>
//                  <dataSource type="UNPOOLED">
//                    <property name="driver" value="${driver}"/>
//                    <property name="url" value="${url}"/>
//                    <property name="username" value="${username}"/>
//                    <property name="password" value="${password}"/>
//                  </dataSource>
//                </environment>
//              </environments>
            if (environment == null) {
//                读一下看看有没有配置default属性作为默认环境
                environment = context.getStringAttribute("default");
            }
//            遍历environment节点
            for (XNode child : context.getChildren()) {
//                获取environment节点的id属性，相当于是名字吧
                String id = child.getStringAttribute("id");
//                判断是否是指定的环境
                if (isSpecifiedEnvironment(id)) {
//                    解析transactionManager节点
                    TransactionFactory txFactory = transactionManagerElement(child.evalNode("transactionManager"));
//                    解析dataSource节点
                    DataSourceFactory dsFactory = dataSourceElement(child.evalNode("dataSource"));
                    DataSource dataSource = dsFactory.getDataSource();
//                    创建Environment.Builder对象，并且设置id、transactionFactory、dataSource
//                    链式调用
                    Environment.Builder environmentBuilder = new Environment.Builder(id).transactionFactory(txFactory)
                        .dataSource(dataSource);
                    configuration.setEnvironment(environmentBuilder.build());
                    break;
                }
            }
        }
    }

    private void databaseIdProviderElement(XNode context) throws Exception {
//          <databaseIdProvider type="DB_VENDOR">
//            <property name="Apache Derby" value="derby"/>
//          </databaseIdProvider>
        DatabaseIdProvider databaseIdProvider = null;
        if (context != null) {
            String type = context.getStringAttribute("type");
            // 为了保证兼容性修改type的值
            if ("VENDOR".equals(type)) {
                type = "DB_VENDOR";
            }
//            获取子节点的属性，然后通过构造方法实例化，并且赋值属性
            Properties properties = context.getChildrenAsProperties();
            databaseIdProvider = (DatabaseIdProvider) resolveClass(type).getDeclaredConstructor().newInstance();
            databaseIdProvider.setProperties(properties);
        }
//        把databaseId记录到configuration的databaseId字段中
        Environment environment = configuration.getEnvironment();
        if (environment != null && databaseIdProvider != null) {
            String databaseId = databaseIdProvider.getDatabaseId(environment.getDataSource());
            configuration.setDatabaseId(databaseId);
        }
    }

    private TransactionFactory transactionManagerElement(XNode context) throws Exception {
        if (context != null) {
            String type = context.getStringAttribute("type");
            Properties props = context.getChildrenAsProperties();
            TransactionFactory factory = (TransactionFactory) resolveClass(type).getDeclaredConstructor().newInstance();
            factory.setProperties(props);
            return factory;
        }
        throw new BuilderException("Environment declaration requires a TransactionFactory.");
    }

    private DataSourceFactory dataSourceElement(XNode context) throws Exception {
        if (context != null) {
            String type = context.getStringAttribute("type");
            Properties props = context.getChildrenAsProperties();
            DataSourceFactory factory = (DataSourceFactory) resolveClass(type).getDeclaredConstructor().newInstance();
            factory.setProperties(props);
            return factory;
        }
        throw new BuilderException("Environment declaration requires a DataSourceFactory.");
    }

    private void typeHandlerElement(XNode parent) {
        if (parent != null) {
            for (XNode child : parent.getChildren()) {
                if ("package".equals(child.getName())) {
                    String typeHandlerPackage = child.getStringAttribute("name");
                    typeHandlerRegistry.register(typeHandlerPackage);
                } else {
                    String javaTypeName = child.getStringAttribute("javaType");
                    String jdbcTypeName = child.getStringAttribute("jdbcType");
                    String handlerTypeName = child.getStringAttribute("handler");
                    Class<?> javaTypeClass = resolveClass(javaTypeName);
                    JdbcType jdbcType = resolveJdbcType(jdbcTypeName);
                    Class<?> typeHandlerClass = resolveClass(handlerTypeName);
                    if (javaTypeClass != null) {
                        if (jdbcType == null) {
                            typeHandlerRegistry.register(javaTypeClass, typeHandlerClass);
                        } else {
                            typeHandlerRegistry.register(javaTypeClass, jdbcType, typeHandlerClass);
                        }
                    } else {
                        typeHandlerRegistry.register(typeHandlerClass);
                    }
                }
            }
        }
    }

    private void mapperElement(XNode parent) throws Exception {
//          <mappers>
//            <mapper resource="org/apache/ibatis/builder/xsd/BlogMapper.xml"/>
//            <mapper url="file:./src/test/resources/org/apache/ibatis/builder/xsd/NestedBlogMapper.xml"/>
//            <mapper class="org.apache.ibatis.builder.xsd.CachedAuthorMapper"/>
//            <package name="org.apache.ibatis.builder.mapper"/>
//          </mappers>
        if (parent != null) {
//            遍历<mappers>的子节点
            for (XNode child : parent.getChildren()) {
//                判断是不是有一个<package>节点，如果有的话，就扫描指定的包
                if ("package".equals(child.getName())) {
                    String mapperPackage = child.getStringAttribute("name");
                    configuration.addMappers(mapperPackage);
                } else {
//                    获取<mapper>节点的resource、url、class属性
                    String resource = child.getStringAttribute("resource");
                    String url = child.getStringAttribute("url");
                    String mapperClass = child.getStringAttribute("class");
                    if (resource != null && url == null && mapperClass == null) {
//                        如果resource不为空，url和mapperClass为空，那么就是通过resource加载Mapper
//                        Resources.getResourceAsStream
                        ErrorContext.instance().resource(resource);
                        try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
                            XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, resource,
                                configuration.getSqlFragments());
                            mapperParser.parse();
                        }
                    } else if (resource == null && url != null && mapperClass == null) {
//                        如果resource为空，url不为空，mapperClass为空，那么就是通过url加载Mapper
                        ErrorContext.instance().resource(url);
                        try (InputStream inputStream = Resources.getUrlAsStream(url)) {
                            XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, url,
                                configuration.getSqlFragments());
                            mapperParser.parse();
                        }
                    } else if (resource == null && url == null && mapperClass != null) {
//                        如果resource为空，url为空，mapperClass不为空，那么就是通过mapperClass加载Mapper
                        Class<?> mapperInterface = Resources.classForName(mapperClass);
                        configuration.addMapper(mapperInterface);
                    } else {
                        throw new BuilderException(
                            "A mapper element may only specify a url, resource or class, but not more than one.");
                    }
                }
            }
        }
    }

    private boolean isSpecifiedEnvironment(String id) {
        if (environment == null) {
            throw new BuilderException("No environment specified.");
        }
        if (id == null) {
            throw new BuilderException("Environment requires an id attribute.");
        }
        return environment.equals(id);
    }

    private static Configuration newConfig(Class<? extends Configuration> configClass) {
        try {
            return configClass.getDeclaredConstructor().newInstance();
        } catch (Exception ex) {
            throw new BuilderException("Failed to create a new Configuration instance.", ex);
        }
    }

}
