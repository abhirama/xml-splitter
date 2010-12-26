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
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class XmlSplitter implements XMLEventObserver {
  protected File xmlFile;
  protected XMLEventReader xmlEventReader;
  protected StartElement documentRootElement;
  protected XMLDocumentWriter xmlDocumentWriter;
  protected long expectedFileSize; //in bytes
  protected int fileCounter = 0;
  protected File splitFileDirectory;

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
    splitHelper();
  }

  static int bar = 0;
  protected void splitHelper() throws XMLStreamException {
    loopUntilStartElement();
    System.out.println("Creating doc writer");
    initializeXmlDocumentWriter();
    System.out.println("Done creating doc writer");
    fileCounter++;
    System.out.println("Started to write");
    System.out.println("--------" + ++bar);
    xmlDocumentWriter.write();
  }

  protected void initializeXmlDocumentWriter() throws XMLStreamException {
    xmlDocumentWriter = new XMLDocumentWriter(getSplitFile());
    System.out.println("Initializing doc writer");
    xmlDocumentWriter.init();
    System.out.println("Done initializing doc writer");
    xmlDocumentWriter.setDocumentRootElement(this.documentRootElement);
    xmlDocumentWriter.setXmlEventReader(this.xmlEventReader);
    xmlDocumentWriter.setXmlEventObserver(this);
  }

  protected File getSplitFile() {
    return new File(splitFileDirectory, getSplitFileName());
  }

  protected String getSplitFileName() {
    String locXmlFileName = this.xmlFile.getName();
    String splitFileName = locXmlFileName.substring(0, locXmlFileName.lastIndexOf(".")) + "_" + fileCounter + ".xml";
    System.out.println(splitFileName);
    return splitFileName;
  }

  protected void loopUntilStartElement() throws XMLStreamException {
    XMLEvent xmlEvent = xmlEventReader.peek();

    System.out.println("<>");
    while (!xmlEvent.isStartElement()) {
      System.out.println("looping");
      if (xmlEvent.isEndDocument() || !xmlEventReader.hasNext()) {
        System.out.println("End reached - 1");
        System.out.println("Document ends:" + xmlEvent.isEndDocument());
        System.exit(0);
      }

      this.xmlEventReader.nextEvent();
      xmlEvent = xmlEventReader.peek();
    }
    System.out.println("</>");
  }

  public void notifyElementStartEvent(XMLEvent xmlEvent) {
    nodePositionCounter++;
  }

  int foo = 0;

  public void notifyElementEndEvent(XMLEvent xmlEvent) {
   if (foo == 251818) {
     int k = 0;  
   }

    //System.out.println(++foo);

    nodePositionCounter--;
    try {
      if (nodePositionCounter == 0) {
        xmlDocumentWriter.flushOutput();

        if (xmlDocumentWriter.getXmlFile().length() > this.expectedFileSize) {
          xmlDocumentWriter.close();
          System.out.println(xmlEvent.asEndElement().getName().getLocalPart()); //todo has to be removed
          splitHelper();
        }
      }
    } catch (XMLStreamException e) {
      throw new RuntimeException(e);
    }
  }

  public void notifyDocumentEndEvent(XMLEvent xmlEvent) {
    System.out.println("End reached - 0");
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
    XmlSplitter xmlSplitter = new XmlSplitter("C:\\files\\DrugBank\\foo\\drugbank.xml");
    //XmlSplitter xmlSplitter = new XmlSplitter("files\\ipo.xml");
    //xmlSplitter.setExpectedFileSize(10000000);
    //xmlSplitter.setExpectedFileSize(1000);
    xmlSplitter.setExpectedFileSize(0);
    xmlSplitter.setSplitFileDirectory(new File("C:\\files\\DrugBank\\foo"));
    xmlSplitter.init();
    xmlSplitter.split();
  }
}
