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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class PropertyParserTest {

    /**
    *@Param:
    *@return:
    *@Author: qjj
    *@describe: 测试默认值的正确使用
    */
    @Test
    void replaceToVariableValue() {
//        这里相当于在模拟在mybatis-config.xml文件里面写<properties>标签
        Properties props = new Properties();
        props.setProperty(PropertyParser.KEY_ENABLE_DEFAULT_VALUE, "true");
        props.setProperty("key", "value");
        props.setProperty("tableName", "members");
        props.setProperty("orderColumn", "member_id");
        props.setProperty("a:b", "c");
//        先设置允许使用默认值，然后解析一下，断言看看${key:aaa}中的aaa有没有被设置成默认值
        Assertions.assertThat(PropertyParser.parse("${key}", props)).isEqualTo("value");
        Assertions.assertThat(PropertyParser.parse("${key:aaaa}", props)).isEqualTo("value");
        Assertions.assertThat(PropertyParser.parse("SELECT * FROM ${tableName:users} ORDER BY ${orderColumn:id}", props))
            .isEqualTo("SELECT * FROM members ORDER BY member_id");
//        再修改一下不允许使用默认值，然后断言一下试试，看看有没有用到默认值
        props.setProperty(PropertyParser.KEY_ENABLE_DEFAULT_VALUE, "false");
        Assertions.assertThat(PropertyParser.parse("${a:b}", props)).isEqualTo("c");
//        最后连这个配置都不写了，就是想测试一下，如果不写允许不允许使用默认值，默认情况下使用不使用默认值
//        经过测试，默认没有使用默认值
        props.remove(PropertyParser.KEY_ENABLE_DEFAULT_VALUE);
        Assertions.assertThat(PropertyParser.parse("${a:b}", props)).isEqualTo("c");

    }

    /**
    *@Param:
    *@return:
    *@Author: qjj
    *@describe: 测试没有配置<properties>标签的时候，自身的值能不能正确展示
    */
    @Test
    void notReplace() {
        Properties props = new Properties();
        props.setProperty(PropertyParser.KEY_ENABLE_DEFAULT_VALUE, "true");
        Assertions.assertThat(PropertyParser.parse("${key}", props)).isEqualTo("${key}");
        Assertions.assertThat(PropertyParser.parse("${key}", null)).isEqualTo("${key}");

        props.setProperty(PropertyParser.KEY_ENABLE_DEFAULT_VALUE, "false");
        Assertions.assertThat(PropertyParser.parse("${a:b}", props)).isEqualTo("${a:b}");

        props.remove(PropertyParser.KEY_ENABLE_DEFAULT_VALUE);
        Assertions.assertThat(PropertyParser.parse("${a:b}", props)).isEqualTo("${a:b}");

    }

    /**
    *@Param:
    *@return:
    *@Author: qjj
    *@describe: 测试一下默认值代替过程中遇到的分号，空格等问题
    */
    @Test
    void applyDefaultValue() {
        Properties props = new Properties();
        props.setProperty(PropertyParser.KEY_ENABLE_DEFAULT_VALUE, "true");
        Assertions.assertThat(PropertyParser.parse("${key:default}", props)).isEqualTo("default");
        Assertions.assertThat(PropertyParser.parse("SELECT * FROM ${tableName:users} ORDER BY ${orderColumn:id}", props))
            .isEqualTo("SELECT * FROM users ORDER BY id");
        Assertions.assertThat(PropertyParser.parse("${key:}", props)).isEmpty();
        Assertions.assertThat(PropertyParser.parse("${key: }", props)).isEqualTo(" ");
        Assertions.assertThat(PropertyParser.parse("${key::}", props)).isEqualTo(":");
    }

    /**
    *@Param:
    *@return:
    *@Author: qjj
    *@describe: 测试修改分割符之后能不能正确使用
    */
    @Test
    void applyCustomSeparator() {
        Properties props = new Properties();
        props.setProperty(PropertyParser.KEY_ENABLE_DEFAULT_VALUE, "true");
        props.setProperty(PropertyParser.KEY_DEFAULT_VALUE_SEPARATOR, "?:");
        Assertions.assertThat(PropertyParser.parse("${key?:default}", props)).isEqualTo("default");
        Assertions
            .assertThat(PropertyParser.parse(
                "SELECT * FROM ${schema?:prod}.${tableName == null ? 'users' : tableName} ORDER BY ${orderColumn}", props))
            .isEqualTo("SELECT * FROM prod.${tableName == null ? 'users' : tableName} ORDER BY ${orderColumn}");
        Assertions.assertThat(PropertyParser.parse("${key?:}", props)).isEmpty();
        Assertions.assertThat(PropertyParser.parse("${key?: }", props)).isEqualTo(" ");
        Assertions.assertThat(PropertyParser.parse("${key?::}", props)).isEqualTo(":");
    }

}
