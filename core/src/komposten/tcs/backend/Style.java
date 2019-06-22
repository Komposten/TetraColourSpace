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

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;

import komposten.tcs.util.TCSUtils;
import komposten.utilities.tools.MathOps;

public class Style
{
	public static final int RENDER_MODE_FAST = 0;
	public static final int RENDER_MODE_SLOW = 1;
	
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
		SPHERE_QUALITY,
		RENDER_MODE,
		FIELD_OF_VIEW
	}
	
	private Map<Colour, Color> colours;
	private Map<Setting, Number> settings;
	
	private Set<Colour> isDefaultColour;
	private Set<Setting> isDefaultSetting;

	public Style()
	{
		this(null);
	}
	
	
	Style(Node styleNode)
	{
		colours = new EnumMap<>(Colour.class);
		settings = new EnumMap<>(Setting.class);
		isDefaultColour = EnumSet.noneOf(Colour.class);
		isDefaultSetting = EnumSet.noneOf(Setting.class);
		
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
		settings.put(Setting.RENDER_MODE, RENDER_MODE_FAST);
		settings.put(Setting.FIELD_OF_VIEW, 67);
	}


	private void loadStyle(Node styleNode)
	{
		NodeList childNodes = styleNode.getChildNodes();
		
		for (int j = 0; j < childNodes.getLength(); j++)
		{
			Node child = childNodes.item(j);
			
			if (child.getNodeType() != Node.ELEMENT_NODE)
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
		{
			Color colourObj = TCSUtils.getColourFromHex(value);
			
			if (!colourObj.equals(colours.get(colour)))
			{
				colours.put(colour, colourObj);
				isDefaultColour.add(colour);
			}
		}
	}


	private void loadSetting(String id, String value)
	{
		Setting setting = Setting.valueOf(id);
		
		Number number = getNumberFromString(setting, value);
		
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
			case RENDER_MODE :
				if (number.intValue() != RENDER_MODE_FAST
						&& number.intValue() != RENDER_MODE_SLOW)
					throw new IllegalArgumentException(number.intValue() + " is an invalid render mode!");
				break;
			case FIELD_OF_VIEW :
				if (number.intValue() <= 0)
					throw new IllegalArgumentException("FIELD_OF_VIEW must be positive and non-zero!");
				break;
			default :
				return;
		}
		
		if (!MathUtils.isEqual(number.floatValue(), settings.get(setting).floatValue()))
		{
			settings.put(setting, number);
			isDefaultSetting.add(setting);
		}
	}
	
	
	private Number getNumberFromString(Setting setting, String value)
	{
		if (MathOps.isDouble(value))
		{
			return Double.valueOf(value);
		}
		else if (value.matches("-?\\d+"))
		{
			return Integer.valueOf(value);
		}
		else if (setting == Setting.RENDER_MODE)
		{
			if (value.equalsIgnoreCase("fast"))
				return RENDER_MODE_FAST;
			else if (value.equalsIgnoreCase("slow"))
				return RENDER_MODE_SLOW;
		}
		
		throw new IllegalArgumentException(value + " is not a valid double, integer or rendering mode!");
	}
	
	
	public boolean has(Colour key)
	{
		return colours.containsKey(key);
	}
	
	
	public boolean has(Setting key)
	{
		return settings.containsKey(key);
	}


	public Color get(Colour key)
	{
		return colours.get(key);
	}
	
	
	public Number get(Setting key)
	{
		return settings.get(key);
	}
	
	
	public Map<Colour, Color> getChangedColours()
	{
		Map<Colour, Color> result = new EnumMap<>(Colour.class);
		
		for (Colour colour : isDefaultColour)
			result.put(colour, colours.get(colour));
		
		return result;
	}
	
	
	public Map<Setting, Number> getChangedSettings()
	{
		Map<Setting, Number> result = new EnumMap<>(Setting.class);
		
		for (Setting setting : isDefaultSetting)
			result.put(setting, settings.get(setting));
		
		return result;
	}
}
