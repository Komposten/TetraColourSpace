package komposten.tcs.backend;

import java.util.Arrays;

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
import komposten.utilities.logging.Level;
import komposten.utilities.logging.Logger;
import komposten.utilities.tools.MathOps;
import komposten.utilities.tools.Regex;

public class GraphLoader
{
	private Style style;
	private Logger logger;


	public GraphLoader(Style style, Logger logger)
	{
		this.style = style;
		this.logger = logger;
	}
	
	
	public GraphData load(Element dataElement)
	{
		GraphData graph = new GraphData();
		
		loadPointData(dataElement, graph);
		loadVolumeData(dataElement, graph);
		
		return graph;
	}


	private void loadPointData(Element root, GraphData graph)
	{
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
				logger.log(Level.WARNING, "Invalid point shape: \"" + shapeName + "\"");
			}
			
			int groupIndex = graph.addGroup(groupName, shape, size);
			createGroupPoints(group, groupIndex, graph);
		}
	}


	private void createGroupPoints(Element groupData, int groupIndex, GraphData graph)
	{
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
			Vector3 coords = TCSUtils.createVectorFromAngles(metrics.x, metrics.y, metrics.z);
			graph.addPoint(name, coords, metrics, colour, groupIndex);
		}
	}


	private void loadVolumeData(Element root, GraphData graph)
	{
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
				coords[j*3+0] = vector.x;
				coords[j*3+1] = vector.y;
				coords[j*3+2] = vector.z;
			}

			graph.addVolume(coords, colour);
		}
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

		return TCSUtils.createVectorFromAngles(metrics.x, metrics.y, metrics.z);
	}
	
	
	private Vector3 getColourSpaceMetricsFromLine(String line)
	{
		String[] values = Regex.getMatches(MathOps.doubleRegex, line);
		float[] floats = new float[values.length];
		
		for (int i = 0; i < floats.length; i++)
			floats[i] = Float.parseFloat(values[i]);

		float theta = floats[0];
		float phi = floats[1];
		float magnitude = floats[2];
		
		return new Vector3(theta, phi, magnitude);
	}
}
