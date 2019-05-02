/*
 * Copyright 2019 Jakob Hjelm
 * 
 * This file is part of TetraColourSpace.
 * 
 * TetraColourSpace is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package komposten.tcs.backend;

import java.io.File;
import java.io.IOException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import komposten.tcs.backend.data.GraphData;
import komposten.utilities.logging.Level;
import komposten.utilities.logging.Logger;

public class Backend
{
	private Logger logger;
	private File dataFile;
	private Style style;
	private GraphData graph;

	
	public Backend(File dataFile, Logger logger)
	{
		this.dataFile = dataFile;
		this.logger = logger;

		loadDataFromFile();
	}
	
	
	public Style getStyle()
	{
		return style;
	}
	
	
	public GraphData getGraph()
	{
		return graph;
	}


	private void loadDataFromFile()
	{
		try
		{
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			docBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			Document document = docBuilder.parse(dataFile);
			Element root = document.getDocumentElement();
			
			loadConfig(root);
			loadGraph(root);
		}
		catch (IOException e)
		{
			String msg = "Error reading data file: " + dataFile.getPath();
			logger.log(Level.FATAL, getClass().getSimpleName(), msg, e, false);
		}
		catch (ParserConfigurationException e)
		{
			String msg = "Error creating the XML parser!";
			logger.log(Level.FATAL, getClass().getSimpleName(), msg, e, false);
		}
		catch (SAXException e)
		{
			String msg = "Error parsing data file: " + dataFile.getPath();
			logger.log(Level.FATAL, getClass().getSimpleName(), msg, e, false);
		}
	}


	private void loadConfig(Element root)
	{
		NodeList styleNodes = root.getElementsByTagName("style");
		if (styleNodes.getLength() > 0)
			style = new Style(styleNodes.item(0));
		else
			style = new Style();
	}
	
	
	private void loadGraph(Element root)
	{
		GraphLoader loader = new GraphLoader(style, logger);
		graph = loader.load(root);
	}
}
