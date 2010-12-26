/**
 * Created by IntelliJ IDEA.
 * User: abhirama
 * Date: Dec 25, 2010
 * Time: 2:19:16 PM
 * To change this template use File | Settings | File Templates.
 */
package com.abhirama.xmlsplitter;

import javax.xml.stream.events.XMLEvent;

public interface XMLEventObserver {
  public void notifyElementStartEvent(XMLEvent xmlEvent);
  public void notifyElementEndEvent(XMLEvent xmlEvent);
  public void notifyDocumentEndEvent(XMLEvent xmlEvent);
}
