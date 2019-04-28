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
package komposten.tcs.backend.data;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;

/**
 * Stores information about a data point.
 */
public class Point
{
	private PointGroup group;
	private String name;
	private Vector3 coordinates;
	private Vector3 metrics;
	private Color colour;		
	
	/**
	 * @param name The name of the point.
	 * @param coordinates The position of the point in the colour space.
	 * @param metrics The tetrachromatic colour metrics for the point. <code>x, y,</code> and
	 * <code>z</code> represent theta, phi and r respectively.
	 * @param colour The colour of the point.
	 * @param group The {@link PointGroup group} the point belongs to.
	 */
	public Point(String name, Vector3 coordinates, Vector3 metrics, Color colour, PointGroup group)
	{
		this.group = group;
		this.name = name;
		this.coordinates = coordinates;
		this.metrics = metrics;
		this.colour = colour;
	}
	
	
	public PointGroup getGroup()
	{
		return group;
	}


	public String getName()
	{
		return name;
	}


	public Vector3 getCoordinates()
	{
		return coordinates;
	}


	/**
	 * @return The tetrachromatic colour metrics for the point. <code>x, y,</code> and
	 * <code>z</code> represent theta, phi and r respectively.
	 */
	public Vector3 getMetrics()
	{
		return metrics;
	}

	
	public Color getColour()
	{
		return colour;
	}
	
	
	@Override
	public String toString()
	{
		return String.format("%s|(%.03f, %.03f, %.03f)[%s]", name, metrics.x, metrics.y, metrics.z, colour);
	}
}