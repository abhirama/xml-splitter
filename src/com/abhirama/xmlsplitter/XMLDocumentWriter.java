/**
 * Created by IntelliJ IDEA.
 * User: abhirama
 * Date: Dec 25, 2010
 * Time: 1:26:38 PM
 * To change this template use File | Settings | File Templates.
 */
package com.abhirama.xmlsplitter;

import org.codehaus.staxmate.SMOutputFactory;
import org.codehaus.staxmate.out.SMOutputDocument;
import org.codehaus.staxmate.out.SMOutputElement;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.*;
import java.io.File;
import java.util.Iterator;
import java.util.Stack;

public class XMLDocumentWriter {
  protected File xmlFile;
  protected StartElement documentRootElement;
  protected XMLEventReader xmlEventReader;
  protected SMOutputDocument outputDocument;
  protected Stack<SMOutputElement> outputElements = new Stack<SMOutputElement>();
  protected XMLEventObserver xmlEventObserver;

  public XMLDocumentWriter(File xmlFile) {
    this.xmlFile = xmlFile;
  }

  public XMLDocumentWriter(String xmlFileName) {
    this.xmlFile = new File(xmlFileName);
  }

  public StartElement getDocumentRootElement() {
    return documentRootElement;
  }

  public void setDocumentRootElement(StartElement documentRootElement) {
    this.documentRootElement = documentRootElement;
  }

  public XMLEventReader getXmlEventReader() {
    return xmlEventReader;
  }

  public void setXmlEventReader(XMLEventReader xmlEventReader) {
    this.xmlEventReader = xmlEventReader;
  }

  public XMLEventObserver getXmlEventObserver() {
    return xmlEventObserver;
  }

  public void setXmlEventObserver(XMLEventObserver xmlEventObserver) {
    this.xmlEventObserver = xmlEventObserver;
  }

  public File getXmlFile() {
    return xmlFile;
  }

  public void setXmlFile(File xmlFile) {
    this.xmlFile = xmlFile;
  }

  public void init() throws XMLStreamException {
    this.outputDocument = createNewOutputDocument();
  }

  public void close() throws XMLStreamException {
    this.outputDocument.closeRootAndWriter();
  }

  public void write() throws XMLStreamException {
    SMOutputElement outputRootElement = writeElement(documentRootElement, outputDocument);
    outputElements.push(outputRootElement);

    while (xmlEventReader.hasNext()) {
      processEvent();
    }
  }

  protected void processEvent() throws XMLStreamException {
    XMLEvent xmlEvent = xmlEventReader.nextEvent();
    if (xmlEvent.isStartElement()) {
      StartElement startElement = xmlEvent.asStartElement();
      SMOutputElement outputRootElement = writeElement(startElement, outputElements.peek());
      outputElements.push(outputRootElement);
      xmlEventObserver.notifyElementStartEvent(xmlEvent);
    }

    if (xmlEvent.isCharacters()) {
      Characters characters = xmlEvent.asCharacters();
      writeCharacters(characters, outputElements.peek());
    }

    if (xmlEvent.isEndElement()) {
      outputElements.pop();
      xmlEventObserver.notifyElementEndEvent(xmlEvent);
    }

    if (xmlEvent.isEndDocument()) {
      xmlEventObserver.notifyDocumentEndEvent(xmlEvent);
    }

    //todo - remove
    this.flushOutput();
  }

  static int count = 0;
  public SMOutputDocument createNewOutputDocument() throws XMLStreamException {
    System.out.println("No of docs created:" + ++count);

    if (count == 717) {
      int i = 10;  
    }

    XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
    System.out.println("Created xml output factory");
    SMOutputFactory outf = new SMOutputFactory(xmlOutputFactory);
    System.out.println("Created outf");
    SMOutputDocument doc = outf.createOutputDocument(this.xmlFile);
    System.out.println("Initlized file");

    // (optional) 3: enable indentation (note spaces after backslash!)
    doc.setIndentation("\n  ", 1, 1);
    return doc;
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

  protected SMOutputElement writeElementTag(StartElement startElement, SMOutputDocument outputDocument) throws XMLStreamException {
    String elementName = Util.getNameFromQName(startElement.getName());
    SMOutputElement outputElement = outputDocument.addElement(elementName);
    return outputElement;
  }

  protected SMOutputElement writeElementTag(StartElement startElement, SMOutputElement outputElement) throws XMLStreamException {
    String elementName = Util.getNameFromQName(startElement.getName());
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
      String attributeName = Util.getNameFromQName(attribute.getName());
      String attributeValue = attribute.getValue();
      outputElement.addAttribute(attributeName, attributeValue);
    }
  }

  protected void flushOutput() throws XMLStreamException {
    this.outputDocument.getContext().flushWriter();
  }


}
