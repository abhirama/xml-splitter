/**
 * Created by IntelliJ IDEA.
 * User: abhirama
 * Date: Dec 25, 2010
 * Time: 4:43:36 PM
 * To change this template use File | Settings | File Templates.
 */
package com.abhirama.xmlsplitter;

import javax.xml.namespace.QName;

public class Util {
  public static String getNameFromQName(QName qName) {
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

}
