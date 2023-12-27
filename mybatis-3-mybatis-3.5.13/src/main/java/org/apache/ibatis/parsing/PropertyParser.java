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
package org.apache.ibatis.parsing;

import java.util.Properties;

/**
 * @author Clinton Begin
 * @author Kazuki Shimizu
 */
public class PropertyParser {

    private static final String KEY_PREFIX = "org.apache.ibatis.parsing.PropertyParser.";
    /**
     * 在mybatis-config.xml文件中<properties>节点下配置是否开启默认值的配置项，相当于这些是那个配置文件里面的<>中的标签
     * <p>
     * The default value is {@code false} (indicate disable a default value on placeholder) If you specify the
     * {@code true}, you can specify key and default value on placeholder (e.g. {@code ${db.username:postgres}}).
     * </p>
     *
     * @since 3.4.2
     */
    public static final String KEY_ENABLE_DEFAULT_VALUE = KEY_PREFIX + "enable-default-value";

    /**
     * 配置占位符与默认值之间的默认分隔符的对应配置项，相当于这些是那个配置文件里面的<>中的标签
     * <p>
     * The default separator is {@code ":"}.
     * </p>
     *
     * @since 3.4.2
     */
    public static final String KEY_DEFAULT_VALUE_SEPARATOR = KEY_PREFIX + "default-value-separator";

    //  默认情况下关闭默认值功能
    private static final String ENABLE_DEFAULT_VALUE = "false";
    //  默认情况下默认分隔符为：
    private static final String DEFAULT_VALUE_SEPARATOR = ":";

    private PropertyParser() {
        // Prevent Instantiation
    }

    public static String parse(String string, Properties variables) {
        VariableTokenHandler handler = new VariableTokenHandler(variables);
//        占位符格式  ${}
        GenericTokenParser parser = new GenericTokenParser("${", "}", handler);
        return parser.parse(string);
    }

    private static class VariableTokenHandler implements TokenHandler {
//        <properties>节点下配置下配置的键值对
        private final Properties variables;
//        是否支持默认值功能
        private final boolean enableDefaultValue;
//        默认值的分隔符
        private final String defaultValueSeparator;

        private VariableTokenHandler(Properties variables) {
            this.variables = variables;
            this.enableDefaultValue = Boolean.parseBoolean(getPropertyValue(KEY_ENABLE_DEFAULT_VALUE, ENABLE_DEFAULT_VALUE));
            this.defaultValueSeparator = getPropertyValue(KEY_DEFAULT_VALUE_SEPARATOR, DEFAULT_VALUE_SEPARATOR);
        }

        private String getPropertyValue(String key, String defaultValue) {
            return variables == null ? defaultValue : variables.getProperty(key, defaultValue);
        }

        @Override
        public String handleToken(String content) {
//            配置判空
            if (variables != null) {
                String key = content;
//                判断是否支持默认值
                if (enableDefaultValue) {
//                    查找分隔符
                    final int separatorIndex = content.indexOf(defaultValueSeparator);
                    String defaultValue = null;
                    if (separatorIndex >= 0) {
//                        获取占位符名称
                        key = content.substring(0, separatorIndex);
//                        获取默认值
                        defaultValue = content.substring(separatorIndex + defaultValueSeparator.length());
                    }
                    if (defaultValue != null) {
//                        在配置集合中查找占位符，这里相当于是使用默认值，但是配置了额外值，那么就先使用额外配置的值
                        return variables.getProperty(key, defaultValue);
                    }
                }
                if (variables.containsKey(key)) {
//                    在配置集合中查找占位符
                    return variables.getProperty(key);
                }
            }
            return "${" + content + "}";
        }
    }

}
