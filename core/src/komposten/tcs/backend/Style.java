package komposten.tcs.backend;

import java.util.EnumMap;
import java.util.Map;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.badlogic.gdx.graphics.Color;

import komposten.tcs.util.TCSUtils;
import komposten.utilities.tools.MathOps;

public class Style
{
	public enum Colour
	{
		BACKGROUND,
		TEXT,
		CROSSHAIR,
		WL_LONG,
		WL_MEDIUM,
		WL_SHORT,
		WL_UV,
		ACHRO,
		METRIC_LINE,
		METRIC_FILL,
		HIGHLIGHT,
		SELECTION
	}
	
	public enum Setting
	{
		POINT_SIZE,
		CORNER_SIZE,
		SPHERE_QUALITY
	}
	
	private Map<Colour, Color> colours;
	private Map<Setting, Number> settings;

	public Style()
	{
		this(null);
	}
	
	
	Style(Node styleNode)
	{
		colours = new EnumMap<>(Colour.class);
		settings = new EnumMap<>(Setting.class);
		
		loadDefaults();
		if (styleNode != null)
			loadStyle(styleNode);
	}


	private void loadDefaults()
	{
		colours.put(Colour.BACKGROUND, new Color(.12f, .12f, .12f, 1f));
		colours.put(Colour.TEXT, new Color(.89f, .89f, .89f, 1f));
		colours.put(Colour.CROSSHAIR, null);
		colours.put(Colour.WL_LONG, Color.RED);
		colours.put(Colour.WL_MEDIUM, Color.GREEN);
		colours.put(Colour.WL_SHORT, Color.BLUE);
		colours.put(Colour.WL_UV, Color.VIOLET);
		colours.put(Colour.ACHRO, Color.GRAY);
		colours.put(Colour.METRIC_LINE, new Color(.89f, .89f, .89f, 1f));
		colours.put(Colour.METRIC_FILL, new Color(.445f, .445f, .445f, 1f));
		colours.put(Colour.HIGHLIGHT, Color.CORAL);
		colours.put(Colour.SELECTION, Color.DARK_GRAY);

		settings.put(Setting.POINT_SIZE, 0.02f);
		settings.put(Setting.CORNER_SIZE, 0.03f);
		settings.put(Setting.SPHERE_QUALITY, 25);
	}


	private void loadStyle(Node styleNode)
	{
		NodeList childNodes = styleNode.getChildNodes();
		
		for (int j = 0; j < childNodes.getLength(); j++)
		{
			Node child = childNodes.item(j);
			
			if (child.getNodeType() == Node.TEXT_NODE)
				continue;
			
			String type = child.getNodeName().toLowerCase();
			Node idAttr = child.getAttributes().getNamedItem("id");
			
			if (idAttr != null)
			{
				String id = idAttr.getNodeValue().trim().toUpperCase();
				String value = child.getTextContent();
				
				if (type.equalsIgnoreCase("colour"))
				{
					loadColour(id, value);
				}
				else if (type.equalsIgnoreCase("setting"))
				{
					loadSetting(id, value);
				}
			}
		}
	}


	private void loadColour(String id, String value)
	{
		Colour colour = Colour.valueOf(id);
		
		if (colour != null)
			colours.put(colour, TCSUtils.getColourFromHex(value));
	}


	private void loadSetting(String id, String value)
	{
		Setting setting = Setting.valueOf(id);
		Number number = getNumberFromString(value);
		
		switch (setting)
		{
			case POINT_SIZE :
				if (number.doubleValue() <= 0)
					throw new IllegalArgumentException("POINT_SIZE must be positive and non-zero!");
				break;
			case CORNER_SIZE :
				if (number.doubleValue() <= 0)
					throw new IllegalArgumentException("CORNER_SIZE must be positive and non-zero!");
				break;
			case SPHERE_QUALITY :
				if (number.intValue() <= 5)
					number = 5;
				break;
			default :
				return;
		}
		
		settings.put(setting, number);
	}
	
	
	private Number getNumberFromString(String value)
	{
		if (MathOps.isDouble(value))
			return Double.valueOf(value);
		else if (value.matches("-?\\d+"))
			return Integer.valueOf(value);
		
		throw new IllegalArgumentException(value + " is not a valid double or integer!");
	}


	public Color get(Colour key)
	{
		return colours.get(key);
	}
	
	
	public Number get(Setting key)
	{
		return settings.get(key);
	}
}
