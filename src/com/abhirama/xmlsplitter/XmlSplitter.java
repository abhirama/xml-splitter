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
import org.xml.sax.SAXException;
import org.w3c.dom.Document;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.Source;
import javax.xml.validation.Validator;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.Stack;

public class XmlSplitter {
  protected File xmlFile;
  protected XMLEventReader xmlEventReader;
  protected StartElement documentRootElement;
  protected long expectedFileSize; //in bytes
  protected int fileCounter = 0;
  protected File splitFileDirectory;
  protected Stack<SMOutputElement> outputElements = new Stack<SMOutputElement>();
  protected SMOutputDocument outputDocument;
  protected File currentXMLOPFile;
  protected File schemaFile;

  protected int nodePositionCounter = 0;

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

  public File getSplitFileDirectory() {
    return splitFileDirectory;
  }

  public void setSplitFileDirectory(File splitFileDirectory) {
    this.splitFileDirectory = splitFileDirectory;
  }

  public File getSchemaFile() {
    return schemaFile;
  }

  public void setSchemaFile(File schemaFile) {
    this.schemaFile = schemaFile;
  }

  public void init() throws XMLStreamException, FileNotFoundException {
    xmlEventReader = XMLInputFactory.newInstance().createXMLEventReader(new FileInputStream(this.xmlFile));
  }

  public void split() throws XMLStreamException, IOException, SAXException, ParserConfigurationException {
    assignDocumentRootElement();
    assignCurrentXMLOPFile();
    assignCurrentOPDocument(currentXMLOPFile);
    opDocumentRootElement();
    processEvents();
  }

  protected void loopUntilStartElement() throws XMLStreamException {
    XMLEvent xmlEvent = xmlEventReader.peek();

    while (!xmlEvent.isStartElement()) {
      if (xmlEvent.isEndDocument() || !xmlEventReader.hasNext()) {
        System.exit(0);
      }

      this.xmlEventReader.nextEvent();
      xmlEvent = xmlEventReader.peek();
    }
  }

  protected void opDocumentRootElement() throws XMLStreamException {
    SMOutputElement outputRootElement = opElement(documentRootElement, outputDocument);
    outputElements.push(outputRootElement);
  }

  protected void processEvents() throws XMLStreamException, IOException, SAXException, ParserConfigurationException {
    while (xmlEventReader.hasNext()) {
      XMLEvent xmlEvent = xmlEventReader.nextEvent();
      if (xmlEvent.isStartElement()) {
        StartElement startElement = xmlEvent.asStartElement();
        SMOutputElement outputRootElement = opElement(startElement, outputElements.peek());
        outputElements.push(outputRootElement);
        nodePositionCounter++;
      }

      if (xmlEvent.isCharacters()) {
        Characters characters = xmlEvent.asCharacters();
        opCharacters(characters, outputElements.peek());
      }

      if (xmlEvent.isEndElement()) {
        outputElements.pop();
        if (outputElements.size() == 1) { //this is the root element that we added in the begining
          flushOutput(); //doing this so that we can get the file size

          if (currentXMLOPFile.length() > this.expectedFileSize) {
            closeOpDocument();
            validateSplitXmlDocument();
            outputElements.pop(); //we make this emty bcos we will be adding a new root element in the next step
            loopUntilStartElement(); //we do this so that empty xml file is not created if we have reached the end of the doc
            assignCurrentXMLOPFile();
            assignCurrentOPDocument(currentXMLOPFile);
            opDocumentRootElement();
          }
        }
      }

      if (xmlEvent.isEndDocument()) {
        closeOpDocument();
      }
    }
  }

  protected void validateSplitXmlDocument() throws IOException, SAXException, ParserConfigurationException {
    if (schemaFile != null) {
      // parse an XML document into a DOM tree
      DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document document = parser.parse(this.currentXMLOPFile);

      // create a SchemaFactory capable of understanding WXS schemas
      SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

      // load a WXS schema, represented by a Schema instance
      Source schemaStream = new StreamSource(schemaFile);
      Schema schema = factory.newSchema(schemaStream);

      // create a Validator instance, which can be used to validate an instance document
      Validator validator = schema.newValidator();

      // validate the DOM tree
      validator.validate(new DOMSource(document));
    }
  }

  public void closeOpDocument() throws XMLStreamException {
    this.outputDocument.closeRootAndWriter();
  }

  protected SMOutputElement opElement(StartElement startElement, SMOutputDocument outputDocument) throws XMLStreamException {
    SMOutputElement outputElement = opElementTag(startElement, outputDocument);
    opNameSpaces(startElement, outputElement);
    opAttributes(startElement, outputElement);
    return outputElement;
  }

  protected SMOutputElement opElement(StartElement startElement, SMOutputElement outputElement) throws XMLStreamException {
    SMOutputElement innerOutputElement = opElementTag(startElement, outputElement);
    opNameSpaces(startElement, innerOutputElement);
    opAttributes(startElement, innerOutputElement);
    return innerOutputElement;
  }

  protected void opCharacters(Characters characters, SMOutputElement outputElement) throws XMLStreamException {
    outputElement.addCharacters(characters.getData());
  }

  protected SMOutputElement opElementTag(StartElement startElement, SMOutputDocument outputDocument) throws XMLStreamException {
    String elementName = Util.getNameFromQName(startElement.getName());
    SMOutputElement outputElement = outputDocument.addElement(elementName);
    return outputElement;
  }

  protected SMOutputElement opElementTag(StartElement startElement, SMOutputElement outputElement) throws XMLStreamException {
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

  protected void assignDocumentRootElement() throws XMLStreamException {
    while (this.xmlEventReader.hasNext()) {
      XMLEvent xmlEvent = xmlEventReader.nextEvent();
      if (xmlEvent.isStartElement()) {
        documentRootElement = xmlEvent.asStartElement();
        break;
      }
    }
  }

  public void assignCurrentOPDocument(File outputFile) throws XMLStreamException {
    SMOutputFactory smOutputFactory = new SMOutputFactory(XMLOutputFactory.newInstance());
    outputDocument = smOutputFactory.createOutputDocument(outputFile);
    // (optional) 3: enable indentation (note spaces after backslash!)
    outputDocument.setIndentation("\n  ", 1, 1);
  }

  protected void assignCurrentXMLOPFile() {
    currentXMLOPFile = new File(splitFileDirectory, getSplitFileName());
  }

  protected String getSplitFileName() {
    String locXmlFileName = this.xmlFile.getName();
    fileCounter = fileCounter + 1;
    String splitFileName = locXmlFileName.substring(0, locXmlFileName.lastIndexOf(".")) + "_" + fileCounter + ".xml";
    return splitFileName;
  }

  public static void main(String[] args) throws XMLStreamException, IOException, SAXException, ParserConfigurationException {
    XmlSplitter xmlSplitter = new XmlSplitter("C:\\files\\DrugBank\\drugbank.xml\\drugbank.xml");
    //XmlSplitter xmlSplitter = new XmlSplitter("files\\ipo.xml");
    //xmlSplitter.setExpectedFileSize(10000000);
    //xmlSplitter.setExpectedFileSize(1000);
    xmlSplitter.setExpectedFileSize(0);
    //xmlSplitter.setSchemaFile(new File("C:\\files\\DrugBank\\drugbank.xml\\drugbank.xsd"));
    xmlSplitter.setSplitFileDirectory(new File("C:\\files\\DrugBank\\foo"));
    xmlSplitter.init();
    xmlSplitter.split();
  }
}
