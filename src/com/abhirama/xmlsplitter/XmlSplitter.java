/**
 * Created by IntelliJ IDEA.
 * User: abhirama
 * Date: Dec 24, 2010
 * Time: 9:59:27 PM
 * To change this template use File | Settings | File Templates.
 */
package com.abhirama.xmlsplitter;

import org.codehaus.staxmate.SMOutputFactory;
import org.codehaus.staxmate.out.SMOutputDocument;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Stack;

public class XmlSplitter implements XMLEventObserver {
  protected File xmlFile;
  protected XMLEventReader xmlEventReader;
  protected StartElement documentRootElement;
  protected XMLDocumentWriter xmlDocumentWriter;
  protected long expectedFileSize; //in bytes

  public XmlSplitter(File xmlFile) {
    this.xmlFile = xmlFile;
  }

  public XmlSplitter(String fileName) {
    this.xmlFile = new File(fileName);
  }

  public long getExpectedFileSize() {
    return expectedFileSize;
  }

  public void setExpectedFileSize(long expectedFileSize) {
    this.expectedFileSize = expectedFileSize;
  }

  public void init() throws XMLStreamException, FileNotFoundException {
    xmlEventReader = XMLInputFactory.newInstance().createXMLEventReader(new FileInputStream(this.xmlFile));
  }

  public SMOutputDocument createNewOutputDocument(File outputFile) throws XMLStreamException {
    SMOutputFactory outf = new SMOutputFactory(XMLOutputFactory.newInstance());
    SMOutputDocument doc = outf.createOutputDocument(outputFile);

    // (optional) 3: enable indentation (note spaces after backslash!)
    doc.setIndentation("\n  ", 1, 1);
    return doc;
  }

  protected Stack<String> elementName = new Stack<String>();

  public void split() throws XMLStreamException {
    assignDocumentRootElement();
    splitHelper();
  }

  protected void splitHelper() throws XMLStreamException {
    XMLEvent xmlEvent = loopUntilStartElement();
    elementName.push(Util.getNameFromQName(xmlEvent.asStartElement().getName()));
    initializeXmlDocumentWriter();
    xmlDocumentWriter.write();
  }

  protected void initializeXmlDocumentWriter() throws XMLStreamException {
    xmlDocumentWriter = new XMLDocumentWriter(new File(System.currentTimeMillis() + ".xml"));
    xmlDocumentWriter.init();
    xmlDocumentWriter.setDocumentRootElement(this.documentRootElement);
    xmlDocumentWriter.setXmlEventReader(this.xmlEventReader);
    xmlDocumentWriter.setXmlEventObserver(this);
  }

  protected XMLEvent loopUntilStartElement() throws XMLStreamException {
    XMLEvent xmlEvent = this.xmlEventReader.peek();

    while (!xmlEvent.isStartElement()) {
      if (xmlEvent.isEndDocument()) {
        System.exit(0);
      }

      this.xmlEventReader.nextEvent();
      xmlEvent = xmlEventReader.peek();
    }
    return xmlEvent;
  }

  public void notifyElementEndEvent(XMLEvent xmlEvent) {
    try {
      EndElement endElement = xmlEvent.asEndElement();

      if (elementName.peek().equals(Util.getNameFromQName(endElement.getName()))) {
        xmlDocumentWriter.flushOutput();

        if (xmlDocumentWriter.getXmlFile().length() > this.expectedFileSize) {
          elementName.pop();
          xmlDocumentWriter.close();
          splitHelper();
        }
      }
    } catch (XMLStreamException e) {
      throw new RuntimeException(e);
    }
  }

  public void notifyDocumentEndEvent(XMLEvent xmlEvent) {
    try {
      xmlDocumentWriter.close();
    } catch (XMLStreamException e) {
      throw new RuntimeException(e);
    }
  }

  protected void assignDocumentRootElement() throws XMLStreamException {
    while (this.xmlEventReader.hasNext()) {
      XMLEvent xmlEvent = xmlEventReader.nextEvent();
      if (xmlEvent.isStartElement()) {
        documentRootElement = xmlEvent.asStartElement();
        break;
      }
    }
  }

  public static void main(String[] args) throws XMLStreamException, FileNotFoundException {
    XmlSplitter xmlSplitter = new XmlSplitter("files\\ipo.xml");
    xmlSplitter.setExpectedFileSize(1000);
    xmlSplitter.init();
    xmlSplitter.split();
  }
}
