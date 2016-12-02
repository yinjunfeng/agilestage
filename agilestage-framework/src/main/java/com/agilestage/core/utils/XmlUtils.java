/**
 * Copyright (c) All rights reserved.
 */
package com.agilestage.core.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.SAXValidator;
import org.dom4j.io.XMLWriter;
import org.dom4j.util.XMLErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

/**
 * XML解析引擎
 * 
 * @author <a href="mailto:729824941@qq.com">fengxing</a>
 * 2016年11月29日
 */
public final class XmlUtils {

    private static final Logger log = LoggerFactory.getLogger(XmlUtils.class);

    private static String encoding = "UTF-8";

    private XmlUtils() {
    }

    /**
     * 使用SaxReader读取XML文件
     * 
     * @throws DocumentException
     */
    public static Document createDoc(final String filePath) throws DocumentException {
        SAXReader xmlReader = new SAXReader();
        File file = new File(filePath);
        return xmlReader.read(file);
    }

    /**
     * 使用SaxReader读取XML文件
     * 
     * @param url
     * @throws DocumentException
     */
    public static Document createDoc(final URL url) throws DocumentException {
        SAXReader xmlReader = new SAXReader();
        return xmlReader.read(url);
    }

    /**
     * 将磁盘文件解析成Document对象
     * 
     * @param file File：磁盘文件
     * @return Document
     * @throws DocumentException
     */
    public static Document createDoc(final File file) throws DocumentException {
        SAXReader saxReader = new SAXReader();
        return saxReader.read(file);
    }

    /**
     * 使用InputStream读取XML文件
     * 
     * @param is
     * @throws DocumentException
     */
    public static Document createDoc(final InputStream is) throws DocumentException {
        SAXReader xmlReader = new SAXReader();
        return xmlReader.read(is);
    }

    /**
     * createXml( String StrOXML)
     * 
     * @param StrOXML 构造XML字符串
     * @throws DocumentException
     */
    public static Document createDoc(final String strOfXml, final String encoding) throws IOException,
                                                                                  DocumentException {
        InputStream inputStream = new ByteArrayInputStream(strOfXml.getBytes(encoding));
        SAXReader saxReader = new SAXReader();

        return saxReader.read(inputStream);
    }

    /**
     * createXmlByStrInMem( String StrOXML)
     * 
     * @param StrOXML 构造XML字符串
     * @throws DocumentException
     */
    public static Document strToDoc(final String strOXML) throws DocumentException {
        return DocumentHelper.parseText(strOXML);
    }

    /**
     * 将Docment对象解析成脚本格式， 返回的是中文编码脚本
     * 
     * @param doc Document
     * @return String
     */
    public static String doc2Str(final Document doc) {
        if (null == doc) {
            return null;
        }
        return doc.asXML();
    }

    /**
     * @param element
     * @return
     */
    public static Element getRecursionElement(final Element element) {
        if (element.elements().size() > 0) {
            return getRecursionElement(element);
        }
        return element;
    }

    /**
     * 使用xsd校验xml文件
     * 
     * @param xsdFileName
     * @param doc
     * @return
     */
    public static ErrorHandler validateXMLByXSD(final String xsdFileName, final Document doc) {
        try {
            XMLErrorHandler errorHandler = new XMLErrorHandler();

            SAXParserFactory factory = SAXParserFactory.newInstance();

            factory.setValidating(true);
            factory.setNamespaceAware(true);

            SAXParser parser = factory.newSAXParser();

            Document xmlDocument = doc;
            parser.setProperty("http://java.sun.com/xml/jaxp/properties/schemaLanguage",
                               "http://www.w3.org/2001/XMLSchema");
            parser.setProperty("http://java.sun.com/xml/jaxp/properties/schemaSource", "file:" + xsdFileName);

            SAXValidator validator = new SAXValidator(parser.getXMLReader());

            validator.setErrorHandler(errorHandler);

            validator.validate(xmlDocument);

            if (errorHandler.getErrors().hasContent()) {
                log.warn("XSD validate failed !");

            } else {
                log.warn("Good! XSD validate success!");
            }

            return errorHandler;
        } catch (SAXException ex) {
            log.debug(ex.getMessage(), ex);
        } catch (ParserConfigurationException e) {
            log.debug(e.getMessage(), e);
        }
        return null;
    }

    /**
     * 把Document写到指定路径的xml文件
     * 
     * @param doc Document：要输出的Document对象
     * @param filePath String：输出的文件路径
     */
    public static void write(final Document doc, final String filePath) throws IOException {
        write(doc, new File(filePath));
    }

    /**
     * 把Document写到指定路径的xml文件
     * 
     * @param doc
     * @param filePath
     * @param format
     * @throws IOException
     */
    public static void write(final Document doc, final String filePath, final OutputFormat format) throws IOException {
        write(doc, new File(filePath), format);
    }

    /**
     * 把Document写到指定路径的xml文件
     * 
     * @param doc Document：要输出的Document对象
     * @param file File：输出的文件路径
     */
    public static void write(final Document doc, final File file) throws IOException {
        write(doc, new FileOutputStream(file));
    }

    /**
     * 把Document写到指定路径的xml文件
     * 
     * @param doc
     * @param file
     * @param format
     * @throws IOException
     */
    public static void write(final Document doc, final File file, final OutputFormat format) throws IOException {
        write(doc, new FileOutputStream(file), format);
    }

    /**
     * 把Document写到输出流
     * 
     * @param doc
     * @param outputStream
     * @throws IOException
     */
    public static void write(final Document doc, final OutputStream outputStream) throws IOException {
        if (doc == null) {
            return;
        }
        OutputFormat format = new OutputFormat();
        format.setEncoding(encoding);

        XMLWriter xmlWriter = new XMLWriter(outputStream, format);

        xmlWriter.write(doc);
        outputStream.close();
        xmlWriter.close();
    }

    /**
     * 把Document写到输出流
     * 
     * @param doc
     * @param outputStream
     * @param format
     * @throws IOException
     */
    public static void
            write(final Document doc, final OutputStream outputStream, final OutputFormat format) throws IOException {
        if (doc == null) {
            return;
        }
        XMLWriter xmlWriter = new XMLWriter(outputStream, format);
        xmlWriter.write(doc);
        outputStream.close();
        xmlWriter.close();
    }

    /**
     * xml文件根据xsl样式表文件生成html文件
     * 
     * @param xmlFile File
     * @param htmlFile File
     * @param xslFile File
     */
    public static void
            xmlToHtml(final File xmlFile, final File htmlFile, final File xslFile) throws FileNotFoundException,
                                                                                  TransformerException {
        TransformerFactory tFactory = TransformerFactory.newInstance();

        Transformer transformer = tFactory.newTransformer(new StreamSource(new FileInputStream(xslFile)));

        transformer.transform(new StreamSource(new FileInputStream(xmlFile)),
                              new StreamResult(new FileOutputStream(htmlFile)));
    }

    /**
     * Document根据xsl样式表文件生成html文件
     * 
     * @param doc Document
     * @param htmlFile File
     * @param xslFile File
     */
    public static void
            xmlToHtml(final Document doc, final File htmlFile, final File xslFile) throws FileNotFoundException,
                                                                                  TransformerException {
        if (doc == null) {
            return;
        }

        String xmlStr = doc2Str(doc);
        InputStream inputStream = new ByteArrayInputStream(xmlStr.getBytes());

        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer = tFactory.newTransformer(new StreamSource(new FileInputStream(xslFile)));

        transformer.transform(new StreamSource(inputStream), new StreamResult(new FileOutputStream(htmlFile)));
    }

}