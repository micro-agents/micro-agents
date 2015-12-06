/*******************************************************************************
 * µ² - Micro-Agent Platform, core of the Otago Agent Platform (OPAL),
 * developed at the Information Science Department, 
 * University of Otago, Dunedin, New Zealand.
 * 
 * This file is part of the aforementioned software.
 * 
 * µ² is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * µ² is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with the Micro-Agents Framework.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.nzdis.micro.bootloader;

import java.io.File;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * MicroConfigLoader is the Configuration loader of the micro-agent platform.
 * Once called (by the platform itself), no configuration changes will have
 * effect.
 * 
 * @author <a href="cfrantz@infoscience.otago.ac.nz">Christopher Frantz</a> 
 * @version $Revision: 1.0 $ $Date: 2010/11/14 00:00:00 $
 * 
 */
public class MicroConfigLoader {

	private static String platformFilename = MicroBootProperties.platformFileName;
	private static MicroPropertiesMap props = new MicroPropertiesMap();
	private final static String configLoaderName = "Configuration Loader: "; 
	
	protected static void setConfigFileName(String fileName){
		platformFilename = fileName;
	}
	
	public static MicroPropertiesMap getConfiguration(String configSection){

		props.clear();
		File file = new File(platformFilename);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = null;
		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(db == null){
			System.err.println(configLoaderName + "Error when attempting to create XML DocumentBuilder. Aborting reading of properties from XML configuration file.");
			return props;
		}
		Document doc = null;
		try {
			doc = db.parse(file);
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println(configLoaderName + "File '" + platformFilename + "' could not be found. Starting with default configuration.");
			return props;
			//e.printStackTrace();
		}
		if(doc == null){
			System.err.println(configLoaderName + "Null reference on document created by XML DocumentBuilder. Aborting reading of properties from XML configuration file.");
			return props;
		}
		Element docElement = doc.getDocumentElement();
		return parseElement(docElement, configSection);
	}
	
	private static MicroPropertiesMap parseElement(Element e, String configSection) {

	    NodeList list = e.getChildNodes();
	    for(int i=0; i<list.getLength(); i++) {
	    	Node n = list.item(i);
			if(n instanceof Element) {
    	        Element en = (Element)n;
    	        //System.out.println(list.item(i).getAttributes().getNamedItem("name").getNodeValue().equals(configSection));
    	        if(!n.getNodeName().equals("a"))
    	          throw new IllegalStateException("Tag <a> </a> expected.");
    	        String name = en.getAttribute("name");
    	        String type = en.getAttribute("type");
    	        NodeList children = en.getChildNodes();
    	        if(type.equals("Configuration")) {
    	          for(int ii=0; ii<list.getLength(); ii++) {
    	            Node nn=children.item(ii);
    	            if(nn instanceof Element && list.item(i).getAttributes().getNamedItem("name").getNodeValue().equals(configSection)) {
    	              parseElement((Element) nn, configSection);
    	            }
    	          }
    	        }
    	        else {
    	          // content is some sort of string
    	          Node ncontent = children.item(0);
    	          String content = "";
    	          if (ncontent != null && ncontent.getNodeName().equals("#text")) {
    	            content = ncontent.getNodeValue().trim();
    	            props.put(name, content);
    	          }
    	       }
	    	}
	    }
	    return props;
	}

}
