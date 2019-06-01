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
import java.util.Map.Entry;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;

import komposten.tcs.backend.Style.Colour;
import komposten.tcs.backend.Style.Setting;
import komposten.tcs.backend.data.GraphData;
import komposten.tcs.backend.data.Point;
import komposten.tcs.backend.data.PointGroup;
import komposten.tcs.backend.data.Volume;
import komposten.tcs.util.TCSUtils;
import komposten.utilities.tools.MathOps;


public class GraphWriter
{
	/**
	 * Writes the specified {@link GraphData} to the specified file.
	 * @param graph The <code>GraphData</code> to save.
	 * @param style The graph's style.
	 * @param file The destination file.
	 * @throws IOException If the XML model could not be created or saved to the destination file. 
	 */
	public void write(GraphData graph, Style style, File file) throws IOException
	{
		try
		{
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			docBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			Document doc = docBuilderFactory.newDocumentBuilder().newDocument();

			Element dataElement = doc.createElement("data");
			doc.appendChild(dataElement);
			
			addStyleElement(style, doc, dataElement);
			addPointElements(graph, style, doc, dataElement);
			addVolumeElements(graph, doc, dataElement);

			saveToFile(doc, file);
		}
		catch (ParserConfigurationException e)
		{
			throw new IOException("Could not configure the XML parser!", e);
		}
	}


	private void addStyleElement(Style style, Document doc, Element parent)
	{
		if (!style.getChangedColours().isEmpty() || !style.getChangedSettings().isEmpty())
		{
			Element styleElement = doc.createElement("style");
			parent.appendChild(styleElement);
	
			for (Entry<Colour, Color> entry : style.getChangedColours().entrySet())
			{
				Element colourElement = doc.createElement("colour");
				colourElement.setAttribute("id", entry.getKey().name().toLowerCase());
				colourElement.setTextContent("#" + entry.getValue().toString());
				styleElement.appendChild(colourElement);
			}
	
			for (Entry<Setting, Number> entry : style.getChangedSettings().entrySet())
			{
				Element settingElement = doc.createElement("setting");
				settingElement.setAttribute("id", entry.getKey().name().toLowerCase());
				settingElement.setTextContent(entry.getValue().toString());
				styleElement.appendChild(settingElement);
			}
		}
	}


	private void addPointElements(GraphData graph, Style style, Document doc,
			Element parent)
	{
		for (PointGroup group : graph.getDataGroups())
		{
			Element groupElement = doc.createElement("group");
			groupElement.setAttribute("name", group.getName());
			groupElement.setAttribute("shape", group.getShape().toString());
			
			if (!MathOps.equals(group.getSize(), style.get(Setting.POINT_SIZE).floatValue(), 0.00001f))
				groupElement.setAttribute("size", Float.toString(group.getSize()));

			for (Point point : group.getPoints())
			{
				Element pointElement = doc.createElement("point");
				pointElement.setAttribute("name", point.getName());
				pointElement.setAttribute("colour", "#" + point.getColour().toString());
				pointElement.setAttribute("position", vectorToString(point.getMetrics()));
				groupElement.appendChild(pointElement);
			}

			parent.appendChild(groupElement);
		}
	}


	private void addVolumeElements(GraphData graph, Document doc, Element parent)
	{
		for (Volume volume : graph.getDataVolumes())
		{
			Element volumeElement = doc.createElement("volume");
			volumeElement.setAttribute("colour", "#" + volume.getColour().toString());
			volumeElement.setTextContent(volumeToString(volume));
			
			parent.appendChild(volumeElement);
		}
	}


	private String vectorToString(Vector3 vector)
	{
		return String.format("%f,%f,%f", vector.x, vector.y, vector.z);
	}


	private String volumeToString(Volume volume)
	{
		StringBuilder builder = new StringBuilder();
		
		double[] coords = volume.getCoordinates();
		for (int i = 0; i < coords.length; i += 3)
		{
			Vector3 metrics = TCSUtils.getMetricsForCoordinates(
					(float) coords[i], (float) coords[i + 1],
					(float) coords[i + 2]);
			builder.append(metrics.x).append(',')
			.append(metrics.y).append(',')
			.append(metrics.z).append('\n');
		}
		
		return builder.toString();
	}


	private void saveToFile(Document doc, File file) throws IOException
	{
		try
		{
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(file);
			
			transformer.transform(source, result);
		}
		catch (TransformerConfigurationException e)
		{
			throw new IOException("Could not create the XML transformer!", e);
		}
		catch (TransformerException e)
		{
			throw new IOException("Could not save the XML to " + file.getName() + "!", e);
		}
	}
}
