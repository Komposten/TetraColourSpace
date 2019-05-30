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
import java.util.List;

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

	
	public Backend(File dataFile, Logger logger) throws IOException, ParserConfigurationException, ParseException
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


	private void loadDataFromFile() throws IOException, ParserConfigurationException, ParseException
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
			logError("Error reading data file: %s", e, dataFile.getPath());
			throw e;
		}
		catch (ParserConfigurationException e)
		{
			logError("Error creating the XML parser!", e);
			throw e;
		}
		catch (SAXException e)
		{
			logError("Error parsing data file %s", e, dataFile.getPath());
			throw new ParseException(e);
		}
		catch (ParseException e)
		{
			String msg = "Error parsing data file " + dataFile.getPath() + ": " + e.getMessage();
			logger.log(Level.FATAL, msg);
			throw e;
		}
	}


	private void logError(String formatString, Throwable cause, Object... params)
	{
		String msg = String.format(formatString, params);
		logger.log(Level.FATAL, getClass().getSimpleName(), msg, cause, false);
	}


	private void loadConfig(Element root)
	{
		NodeList styleNodes = root.getElementsByTagName("style");
		if (styleNodes.getLength() > 0)
			style = new Style(styleNodes.item(0));
		else
			style = new Style();
	}
	
	
	private void loadGraph(Element root) throws ParseException
	{
		GraphLoader loader = new GraphLoader(style);
		if (!loader.load(root))
		{
			List<String> errors = loader.getErrors();
			
			StringBuilder message = new StringBuilder();
			for (String error : errors)
				message.append("\n").append(error);
			
			throw new ParseException(message.toString());
		}
		
		graph = loader.getResult();
	}
}
