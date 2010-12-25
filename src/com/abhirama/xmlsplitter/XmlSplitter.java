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

public class XmlSplitter {
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

    SMOutputDocument outputDocument = createNewOutputDocument(new File("foo.xml"));

    Stack<SMOutputElement> outputElements = new Stack<SMOutputElement>();
    SMOutputElement outputRootElement = writeElement(documentRootElement, outputDocument);
    outputElements.push(outputRootElement);

    while (this.xmlEventReader.hasNext()) {
      XMLEvent xmlEvent = xmlEventReader.nextEvent();
      if (xmlEvent.isStartElement()) {
        StartElement startElement = xmlEvent.asStartElement();
        outputRootElement = writeElement(startElement, outputElements.peek());
        outputElements.push(outputRootElement);
      }

      if (xmlEvent.isCharacters()) {
        Characters characters = xmlEvent.asCharacters();
        writeCharacters(characters, outputElements.peek());
      }

      if (xmlEvent.isEndElement()) {
        outputElements.pop();
      }
    }

    outputDocument.closeRootAndWriter();
  }

  protected SMOutputElement writeElement(StartElement startElement, SMOutputDocument outputDocument) throws XMLStreamException {
    SMOutputElement outputElement = writeElementTag(startElement, outputDocument);
    opNameSpaces(startElement, outputElement);
    opAttributes(startElement, outputElement);
    return outputElement;
  }

  protected SMOutputElement writeElement(StartElement startElement, SMOutputElement outputElement) throws XMLStreamException {
    SMOutputElement innerOutputElement = writeElementTag(startElement, outputElement);
    opNameSpaces(startElement, innerOutputElement);
    opAttributes(startElement, innerOutputElement);
    return innerOutputElement;
  }

  protected void writeCharacters(Characters characters, SMOutputElement outputElement) throws XMLStreamException {
    outputElement.addCharacters(characters.getData());
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

  protected SMOutputElement writeElementTag(StartElement startElement, SMOutputDocument outputDocument) throws XMLStreamException {
    String elementName = getNameFromQName(startElement.getName());
    SMOutputElement outputElement = outputDocument.addElement(elementName);
    return outputElement;
  }

  protected SMOutputElement writeElementTag(StartElement startElement, SMOutputElement outputElement) throws XMLStreamException {
    String elementName = getNameFromQName(startElement.getName());
    SMOutputElement innerOutputElement = outputElement.addElement(elementName);
    return innerOutputElement;
  }

  protected void opNameSpaces(StartElement startElement, SMOutputElement outputElement) throws XMLStreamException {
    Iterator nameSpaceIterator = startElement.getNamespaces();

    while (nameSpaceIterator.hasNext()) {
      Namespace namespace = ((Namespace) nameSpaceIterator.next());
      org.codehaus.staxmate.out.SMNamespace smNamespace = outputElement.getNamespace(namespace.getNamespaceURI(), namespace.getPrefix());
      outputElement.predeclareNamespace(smNamespace);
    }
  }

  protected void opAttributes(StartElement startElement, SMOutputElement outputElement) throws XMLStreamException {
    Iterator attributeIterator = startElement.getAttributes();

    while (attributeIterator.hasNext()) {
      Attribute attribute = ((Attribute) attributeIterator.next());
      String attributeName = getNameFromQName(attribute.getName());
      String attributeValue = attribute.getValue();
      outputElement.addAttribute(attributeName,  attributeValue);
    }
  }

  protected String getNameFromQName(QName qName) {
    String elementName = "";

    String prefix = qName.getPrefix();
    String localPart = qName.getLocalPart();
    if (isEmpty(prefix)) {
      elementName = localPart;
    } else {
      elementName = prefix + ":" + localPart;
    }

    return elementName;
  }

  public static boolean isEmpty(String string) {
    return string == null || string.equals("");
  }

  public static boolean isNotEmpty(String string) {
    return !isEmpty(string);
  }

  public static void main(String[] args) throws XMLStreamException, FileNotFoundException {
    XmlSplitter xmlSplitter = new XmlSplitter("files\\ipo.xml");
    xmlSplitter.init();
    xmlSplitter.split();
  }
}
