package mytest;

import org.apache.ibatis.builder.xml.XMLMapperEntityResolver;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.IOException;

/**
 * @author:qjj
 * @create: 2023-12-27 10:40
 * @Description: XML读取实验，一个模拟XPathParser手动实现流程的测试
 */

public class testXML {
    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
//        开启验证
        documentBuilderFactory.setValidating(true);
        documentBuilderFactory.setNamespaceAware(false);
        documentBuilderFactory.setIgnoringComments(true);
        documentBuilderFactory.setIgnoringElementContentWhitespace(false);
        documentBuilderFactory.setCoalescing(false);
        documentBuilderFactory.setExpandEntityReferences(true);
//        构建
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        //设置异常处理对象
        documentBuilder.setErrorHandler(new ErrorHandler() {
            @Override
            public void error(SAXParseException exception) throws SAXException {
                System.out.println("error:" + exception.getMessage());
            }

            @Override
            public void fatalError(SAXParseException exception) throws SAXException {
                System.out.println("fatalError:" + exception.getMessage());
            }

            @Override
            public void warning(SAXParseException exception) throws SAXException {
                System.out.println("WARN:" + exception.getMessage());
            }
        });
//        读取XML
        Document document = documentBuilder.parse("/Users/qiingjiajun/git project/mybatis/mybatis-3-mybatis-3.5.13/src/test/resources/mytest/book.xml");
        XPathFactory factory = XPathFactory.newInstance();
        XPath xPath = factory.newXPath();
//        定义规则，用来筛选作者为Neal Stephenson的书的标题
        System.out.println("查询作者为Neal Stephenson的书的标题");
        XPathExpression expr = xPath.compile("//book[author='Neal Stephenson']/title/text()");
        Object result = expr.evaluate(document, XPathConstants.NODESET);
        NodeList nodes = (NodeList) result;
        for (int i = 0; i < nodes.getLength(); i++) {
            System.out.println(nodes.item(i).getNodeValue());
        }
//        查询1997年之后的书名
        System.out.println("查询1997年之后的书名");
        XPathExpression expr2 = xPath.compile("//book[@year>1997]/title/text()");
        Object result2 = expr2.evaluate(document, XPathConstants.NODESET);
        NodeList nodes2 = (NodeList) result2;
        for (int i = 0; i < nodes2.getLength(); i++) {
            System.out.println(nodes2.item(i).getNodeValue());
        }
    }
}
