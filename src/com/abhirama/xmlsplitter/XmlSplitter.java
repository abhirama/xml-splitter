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
import org.codehaus.staxmate.out.SMOutputElement;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Iterator;
import java.util.Stack;

public class XmlSplitter implements XMLEventObserver {
  protected File xmlFile;
  protected XMLEventReader xmlEventReader;
  protected StartElement documentRootElement;

  public XmlSplitter(File xmlFile) {
    this.xmlFile = xmlFile;
  }

  public XmlSplitter(String fileName) {
    this.xmlFile = new File(fileName);
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

  public void split() throws XMLStreamException {
    assignDocumentRootElement();
    XMLDocumentWriter xmlDocumentWriter = new XMLDocumentWriter(new File("foo.xml"));
    xmlDocumentWriter.init();
    xmlDocumentWriter.setDocumentRootElement(this.documentRootElement);
    xmlDocumentWriter.setXmlEventReader(this.xmlEventReader);
    xmlDocumentWriter.setXmlEventObserver(this);
    xmlDocumentWriter.write();
  }

  public void notifyElementEndEvent(XMLDocumentWriter xmlDocumentWriter) {
  }

  public void notifyDocumentEndEvent(XMLDocumentWriter xmlDocumentWriter) {
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
    xmlSplitter.init();
    xmlSplitter.split();
  }
}
