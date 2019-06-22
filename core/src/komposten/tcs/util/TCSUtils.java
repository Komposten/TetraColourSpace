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

import komposten.utilities.tools.MathOps;


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
	public static Vector3 getCoordinatesForMetrics(float theta, float phi, float magnitude)
	{
		if (magnitude < 0)
			throw new IllegalArgumentException("the magnitude must not be a negative value!");
		
		Vector3 coords = new Vector3(1, 0, 0);
		coords.rotateRad(Vector3.Y, theta);
		coords.rotateRad(new Vector3(-coords.z, 0, coords.x), phi);
		coords.setLength(magnitude);
		return coords;
	}
	
	
	/**
	 * Calculates the colour metrics (theta, phi and r) for a given set of
	 * Cartesian coordinates.
	 * 
	 * @return A vector containing theta, phi and r (absolute magnitude) as x, y,
	 *         and z, respectively.
	 */
	public static Vector3 getMetricsForCoordinates(Vector3 coordinates)
	{
		float r = coordinates.len();
		float theta = -MathOps.angle(0, 0, coordinates.x, coordinates.z);
		
		coordinates.rotateRad(Vector3.Y, -theta);
		float phi = MathOps.angle(0, 0, coordinates.x, coordinates.y);
		
		return new Vector3(theta, phi, r);
	}
	
	
	public static Vector3 getMetricsForCoordinates(float x, float y, float z)
	{
		return getMetricsForCoordinates(new Vector3(x, y, z));
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
	
	
	public static float getDistanceModifier(float fieldOfView)
	{
		/*
		 * Scaling is based on the following data:
		 * 
		 * FOV	 DIST
		 *   1	70.00
		 *  10	 7.00
		 *  30	 2.50
		 *  67	 1.00
		 *  90	 0.75
		 * 110	 0.62
		 * 150	 0.40
		 * =========
		 * Equation: y = e^(-1.018*log(x) + 4.285)
		 */
		
		return (float) (Math.exp(4.285) * Math.exp(-1.018*Math.log(fieldOfView)));
	}
	
	
	public static float getZoomModifier(float fieldOfView)
	{
		/*
		 * Scaling is based on the following data:
		 * 
		 * FOV	VEL
		 *   1	200
		 *  10	40
		 *  30	10
		 *  67	5
		 *  90	3
		 * 110	2.5
		 * 150	2.15
		 * =========
		 * Equation: y = e^(-0.945*log(x) + 3.886)
		 */
		
		return (float) (Math.exp(3.886) * Math.exp(-0.945*Math.log(fieldOfView)));
	}
}
