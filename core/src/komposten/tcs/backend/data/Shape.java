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

public enum Shape
{
	SPHERE(16),
	PYRAMID(17),
	BOX(15);
	
	private int value;

	private Shape(int value)
	{
		this.value = value;
	}
	
	public static Shape fromString(String string)
	{
		for (Shape shape : values())
		{
			if (string.equalsIgnoreCase(shape.name()) ||
					string.equals(Integer.toString(shape.value)))
				return shape;
		}
		
		return null;
	}
}