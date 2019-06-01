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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;

import komposten.tcs.backend.Style.Colour;
import komposten.tcs.backend.Style.Setting;
import komposten.tcs.backend.data.GraphData;
import komposten.tcs.backend.data.Shape;
import komposten.tcs.util.TCSUtils;
import komposten.utilities.tools.MathOps;
import komposten.utilities.tools.Regex;

public class GraphLoader
{
	private Style style;

	private GraphData result;
	private List<String> errors;

	public GraphLoader(Style style)
	{
		this.style = style;
	}
	
	
	public GraphData getResult()
	{
		return result;
	}
	
	
	public List<String> getErrors()
	{
		return errors;
	}
	
	
	public boolean load(Element dataElement)
	{
		errors = new ArrayList<>();
		result = new GraphData();
		
		boolean success = true;
		if (!loadPointData(dataElement, result))
			success = false;
		if (!loadVolumeData(dataElement, result))
			success = false;
		
		return success;
	}


	private boolean loadPointData(Element root, GraphData graph)
	{
		boolean success = true;
		float defaultSize = style.get(Setting.POINT_SIZE).floatValue();
		
		NodeList groups = root.getElementsByTagName("group");
		for (int g = 0; g < groups.getLength(); g++)
		{
			Element group = (Element) groups.item(g);
			
			Node groupNameAttr = group.getAttributeNode("name");
			Node groupShapeAttr = group.getAttributeNode("shape");
			Node groupSizeAttr = group.getAttributeNode("size");
			
			String groupName = getAttributeValue(groupNameAttr, "Group " + (g+1));
			String shapeName = getAttributeValue(groupShapeAttr, "sphere");
			String sizeString = getAttributeValue(groupSizeAttr, "");
			
			Shape shape = Shape.fromString(shapeName);
			float size = (MathOps.isDouble(sizeString) ? Double.valueOf(sizeString).floatValue() : defaultSize);
			
			if (shape == null)
			{
				shape = Shape.SPHERE;
				errors.add("Invalid point shape: \"" + shapeName + "\"");
				success = false;
			}
			
			int groupIndex = graph.addGroup(groupName, shape, size);
			if (!createGroupPoints(group, groupIndex, graph))
				success = false;
			
		}

		return success;
	}


	private boolean createGroupPoints(Element groupData, int groupIndex, GraphData graph)
	{
		boolean success = true;
		NodeList points = groupData.getElementsByTagName("point");
		for (int i = 0; i < points.getLength(); i++)
		{
			Node pointNode = points.item(i);
			
			NamedNodeMap attributes = pointNode.getAttributes();
			Node colourAttr = attributes.getNamedItem("colour");
			Node nameAttr = attributes.getNamedItem("name");
			Node positionAttr = attributes.getNamedItem("position");
			
			String colourHex = getAttributeValue(colourAttr, style.get(Colour.TEXT).toString());
			String name = getAttributeValue(nameAttr, "Point " + (i+1));
			String position = positionAttr.getNodeValue();
			
			Color colour = TCSUtils.getColourFromHex(colourHex);
			
			Vector3 metrics = getColourSpaceMetricsFromLine(position);
			
			if (metrics != null)
			{
				Vector3 coords = TCSUtils.getCoordinatesForMetrics(metrics.x, metrics.y, metrics.z);
				graph.addPoint(name, coords, metrics, colour, groupIndex);
			}
			else
			{
				success = false;
			}
		}
		
		return success;
	}


	private boolean loadVolumeData(Element root, GraphData graph)
	{
		boolean success = true;
		NodeList volumes = root.getElementsByTagName("volume");
		for (int i = 0; i < volumes.getLength(); i++)
		{
			Node volumesNode = volumes.item(i);
			
			NamedNodeMap attributes = volumesNode.getAttributes();
			Node colourAttr = attributes.getNamedItem("colour");
			
			String colourHex = getAttributeValue(colourAttr, style.get(Colour.TEXT).toString());
			Color colour = TCSUtils.getColourFromHex(colourHex);
	
			String[] pointStrings = volumesNode.getFirstChild().getTextContent().trim().split("[\n\r]+");
			pointStrings = Arrays.stream(pointStrings).filter(x -> !x.trim().isEmpty()).toArray(l -> new String[l]);
			
			double[] coords = new double[pointStrings.length*3];
			for (int j = 0; j < pointStrings.length; j++)
			{
				String pointString = pointStrings[j].trim();
				Vector3 vector = getCoordinatesFromLine(pointString);
				
				if (vector != null)
				{
					coords[j*3+0] = vector.x;
					coords[j*3+1] = vector.y;
					coords[j*3+2] = vector.z;
				}
				else
				{
					success = false;
				}
			}
			
			graph.addVolume(coords, colour);
		}
		
		return success;
	}
	
	
	private String getAttributeValue(Node attribute, String defaultValue)
	{
		if (attribute != null)
			return attribute.getNodeValue().trim();
		else
			return defaultValue;
	}


	private Vector3 getCoordinatesFromLine(String line)
	{
		Vector3 metrics = getColourSpaceMetricsFromLine(line);

		if (metrics == null)
			return null;
		else
			return TCSUtils.getCoordinatesForMetrics(metrics.x, metrics.y, metrics.z);
	}
	
	
	private Vector3 getColourSpaceMetricsFromLine(String line)
	{
		String[] values = Regex.getMatches(MathOps.doubleRegex, line);
		
		if (values.length >= 3)
		{
			float[] floats = new float[values.length];
			
			for (int i = 0; i < floats.length; i++)
				floats[i] = Float.parseFloat(values[i]);
	
			float theta = floats[0];
			float phi = floats[1];
			float magnitude = floats[2];
			
			return new Vector3(theta, phi, magnitude);
		}
		else
		{
			errors.add("Colour metrics must contain 3 values! [" + line + "]");
			return null;
		}
	}
}
