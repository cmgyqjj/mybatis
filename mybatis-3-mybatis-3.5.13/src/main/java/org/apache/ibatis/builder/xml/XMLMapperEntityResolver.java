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

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import org.apache.ibatis.io.Resources;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Offline entity resolver for the MyBatis DTDs.
 *
 * @author Clinton Begin
 * @author Eduardo Macarron
 */
public class XMLMapperEntityResolver implements EntityResolver {
    //指定mybatis-config.xm文件和映射问题对应的DTD的SystemId
    private static final String IBATIS_CONFIG_SYSTEM = "ibatis-3-config.dtd";
    private static final String IBATIS_MAPPER_SYSTEM = "ibatis-3-mapper.dtd";
    private static final String MYBATIS_CONFIG_SYSTEM = "mybatis-3-config.dtd";
    private static final String MYBATIS_MAPPER_SYSTEM = "mybatis-3-mapper.dtd";
    //指定对应DTD文件的具体位置
    private static final String MYBATIS_CONFIG_DTD = "org/apache/ibatis/builder/xml/mybatis-3-config.dtd";
    private static final String MYBATIS_MAPPER_DTD = "org/apache/ibatis/builder/xml/mybatis-3-mapper.dtd";

    public XMLMapperEntityResolver() {
    }

    //resolveEntity()方法是EntityResolver中实现的方法，实现如下
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException {
        try {
            if (systemId != null) {
                String lowerCaseSystemId = systemId.toLowerCase(Locale.ENGLISH);
                if (lowerCaseSystemId.contains("mybatis-3-config.dtd") || lowerCaseSystemId.contains("ibatis-3-config.dtd")) {
                    return this.getInputSource("org/apache/ibatis/builder/xml/mybatis-3-config.dtd", publicId, systemId);
                }

                if (lowerCaseSystemId.contains("mybatis-3-mapper.dtd") || lowerCaseSystemId.contains("ibatis-3-mapper.dtd")) {
                    return this.getInputSource("org/apache/ibatis/builder/xml/mybatis-3-mapper.dtd", publicId, systemId);
                }
            }

            return null;
        } catch (Exception var4) {
            throw new SAXException(var4.toString());
        }
    }

    // 通过公开Id和系统Id去对应的映射路径里面拿到对应的DTD文件流
    private InputSource getInputSource(String path, String publicId, String systemId) {
        InputSource source = null;
        if (path != null) {
            try {
                InputStream in = Resources.getResourceAsStream(path);
                source = new InputSource(in);
                source.setPublicId(publicId);
                source.setSystemId(systemId);
            } catch (IOException var6) {
            }
        }

        return source;
    }
}
