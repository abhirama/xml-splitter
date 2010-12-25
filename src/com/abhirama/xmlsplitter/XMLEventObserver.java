/**
 * Created by IntelliJ IDEA.
 * User: abhirama
 * Date: Dec 25, 2010
 * Time: 2:19:16 PM
 * To change this template use File | Settings | File Templates.
 */
package com.abhirama.xmlsplitter;

public interface XMLEventObserver {
  public void notifyElementEndEvent(XMLDocumentWriter xmlDocumentWriter);
  public void notifyDocumentEndEvent(XMLDocumentWriter xmlDocumentWriter);
}
