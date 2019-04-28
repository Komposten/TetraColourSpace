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
package komposten.tcs.util;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.math.Vector3;


public class TCSUtils
{
	private static Map<Color, Material> materials = new HashMap<>();
	
	private TCSUtils() {}


	/**
	 * Creates a coordinate vector from the given colour metrics.
	 * 
	 * @param theta
	 * @param phi
	 * @param magnitude The absolute magnitude (i.e. not a relative value between
	 *          0 and 1).
	 * @return
	 */
	public static Vector3 createVectorFromAngles(float theta, float phi, float magnitude)
	{
		Vector3 coords = new Vector3(1, 0, 0);
		coords.rotateRad(Vector3.Y, theta);
		coords.rotateRad(new Vector3(-coords.z, 0, coords.x), phi);
		coords.setLength(magnitude);
		return coords;
	}


	public static Color getColourFromHex(String hexColour)
	{
		if (hexColour.startsWith("#"))
		{
			hexColour = hexColour.substring(1);
		}
		
		if (hexColour.matches("[0-9A-Fa-f]+"))
		{
			if (hexColour.length() == 3 || hexColour.length() == 4)
			{
				char[] chars = new char[hexColour.length()*2];
				
				for (int i = 0; i < hexColour.length(); i++)
					chars[i*2] = chars[i*2+1] = hexColour.charAt(i);
				
				hexColour = new String(chars);
			}
			
			if (hexColour.length() == 6)
			{
				hexColour += "FF";
			}
			
			if (hexColour.length() == 8)
			{
				int rgb = Integer.parseInt(hexColour.substring(0, 6), 16);
				int alpha = Integer.parseInt(hexColour.substring(6), 16);
				return new Color(rgb << 8 | alpha);
			}
		}
		
		throw new IllegalArgumentException(hexColour + " is not a valid hex colour!");
	}


	public static Material getMaterialForColour(Color colour)
	{
		return materials.computeIfAbsent(colour, c -> new Material(ColorAttribute.createDiffuse(colour)));
	}
}
