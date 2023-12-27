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

/**
 * @author Clinton Begin
 */
public class GenericTokenParser {

    //    占位符开始标记
    private final String openToken;
    //    占位符结束标记
    private final String closeToken;
//    TokenHandler接口会按一定的业务逻辑解析占位符
    private final TokenHandler handler;

    public GenericTokenParser(String openToken, String closeToken, TokenHandler handler) {
        this.openToken = openToken;
        this.closeToken = closeToken;
        this.handler = handler;
    }

//    处理占位符逻辑
    public String parse(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        // 取到占位符起始标记的位置
        int start = text.indexOf(openToken);
//        如果没找到直接返回，说明没有占位符
        if (start == -1) {
            return text;
        }
        char[] src = text.toCharArray();
        int offset = 0;
//        用来记录解析后的字符串
        final StringBuilder builder = new StringBuilder();
        StringBuilder expression = null;
        do {
            if (start > 0 && src[start - 1] == '\\') {
//                开始标记已经转义，于是遇到转义符开始记录，直接将前面的字符集以及开始标记追加到后面
                builder.append(src, offset, start - offset - 1).append(openToken);
                offset = start + openToken.length();
            } else {
//                已经找到了开始标记，然后现在找结束标记
                // found open token. let's search close token.
                if (expression == null) {
                    expression = new StringBuilder();
                } else {
                    expression.setLength(0);
                }
                builder.append(src, offset, start - offset);
                offset = start + openToken.length();
                int end = text.indexOf(closeToken, offset);
//                如果找到了结束标记
                while (end > -1) {
//                    追加结束标记前面的字符串
                    if ((end <= offset) || (src[end - 1] != '\\')) {
                        expression.append(src, offset, end - offset);
                        break;
                    }
//                    结束标记已经转义，于是遇到转义符开始记录，直接将前面的字符集以及开始标记追加到后面
                    expression.append(src, offset, end - offset - 1).append(closeToken);
                    offset = end + closeToken.length();
                    end = text.indexOf(closeToken, offset);
                }
                if (end == -1) {
                    // close token was not found.
                    builder.append(src, start, src.length - start);
                    offset = src.length;
                } else {
//                    拼出来完整内容
                    builder.append(handler.handleToken(expression.toString()));
                    offset = end + closeToken.length();
                }
            }
            start = text.indexOf(openToken, offset);
        } while (start > -1);
        if (offset < src.length) {
            builder.append(src, offset, src.length - offset);
        }
        return builder.toString();
    }
}
