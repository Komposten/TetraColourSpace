package komposten.tcs.backend;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import com.github.quickhull3d.QuickHull3D;

import komposten.tcs.backend.Style.Colour;
import komposten.tcs.backend.data.Point;
import komposten.tcs.backend.data.PointGroup;
import komposten.tcs.backend.data.Shape;
import komposten.tcs.backend.data.Volume;
import komposten.tcs.util.TCSUtils;
import komposten.utilities.logging.Level;
import komposten.utilities.logging.Logger;
import komposten.utilities.tools.MathOps;
import komposten.utilities.tools.Regex;

public class Backend
{
	private Logger logger;
	private File dataFile;
	private Style style;
	
	private List<PointGroup> dataGroups;
	private List<Volume> dataVolumes;

	
	public Backend(File dataFile, Logger logger)
	{
		this.dataFile = dataFile;
		this.logger = logger;

		dataGroups = new ArrayList<>();
		dataVolumes = new ArrayList<>();
		
		loadDataFromFile();
	}
	
	
	public Style getStyle()
	{
		return style;
	}
	
	
	public List<PointGroup> getDataGroups()
	{
		return dataGroups;
	}
	
	
	public List<Volume> getDataVolumes()
	{
		return dataVolumes;
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
			loadPointData(root);
			loadVolumeData(root);
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


	private void loadPointData(Element root)
	{
		NodeList groups = root.getElementsByTagName("group");
		for (int g = 0; g < groups.getLength(); g++)
		{
			Element group = (Element) groups.item(g);
			
			Node groupNameAttr = group.getAttributeNode("name");
			Node groupShapeAttr = group.getAttributeNode("shape");
			
			String groupName = getAttributeValue(groupNameAttr, "Group " + (g+1));
			String shapeName = getAttributeValue(groupShapeAttr, "sphere");
			Shape shape = Shape.fromString(shapeName);
			
			if (shape == null)
			{
				shape = Shape.SPHERE;
				logger.log(Level.WARNING, "Invalid point shape: \"" + shapeName + "\"");
			}
			
			PointGroup pointGroup = new PointGroup(groupName, shape);
			pointGroup.setPoints(createGroupPoints(group, pointGroup));
			dataGroups.add(pointGroup);
		}
	}


	private List<Point> createGroupPoints(Element group, PointGroup pointGroup)
	{
		List<Point> pointList = new LinkedList<>();
		
		NodeList points = group.getElementsByTagName("point");
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
			Point point = new Point(name, coords, metrics, colour, pointGroup);
			pointList.add(point);
		}
		
		return pointList;
	}


	private void loadVolumeData(Element root)
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
			
			int[][] faces = null;
			double[] coords = new double[pointStrings.length*3];
			for (int j = 0; j < pointStrings.length; j++)
			{
				String pointString = pointStrings[j].trim();
				Vector3 vector = getCoordinatesFromLine(pointString);
				coords[j*3+0] = vector.x;
				coords[j*3+1] = vector.y;
				coords[j*3+2] = vector.z;
			}
			
			if (coords.length >= 12)
			{
				QuickHull3D quickHull = new QuickHull3D();
				quickHull.build(coords);
				
				coords = new double[quickHull.getNumVertices()*3];
				quickHull.getVertices(coords);
				
				faces = quickHull.getFaces();
			}
			else if (coords.length == 9)
			{
				faces = new int[1][];
				faces[0] = new int[] { 0, 1, 2 };
			}
			else
			{
				faces = new int[0][];
			}
			
			Volume volume = new Volume(coords, faces, colour);
			dataVolumes.add(volume);
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
