package komposten.tcs.backend;

import java.util.EnumMap;
import java.util.Map;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.badlogic.gdx.graphics.Color;

import komposten.tcs.util.TCSUtils;

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
	
	private Map<Colour, Color> colours;
			

	public Style()
	{
		this(null);
	}
	
	
	Style(Node styleNode)
	{
		colours = new EnumMap<>(Colour.class);
		
		loadDefaultStyle();
		if (styleNode != null)
			loadStyle(styleNode);
	}


	private void loadDefaultStyle()
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
				String id = idAttr.getNodeValue().trim();
				String value = child.getTextContent();
				
				if (type.equalsIgnoreCase("colour"))
				{
					Colour colour = Colour.valueOf(id.toUpperCase());
					
					if (colour != null)
						colours.put(colour, TCSUtils.getColourFromHex(value));
				}
			}
		}
	}
	
	
	public Color get(Colour key)
	{
		return colours.get(key);
	}
}
