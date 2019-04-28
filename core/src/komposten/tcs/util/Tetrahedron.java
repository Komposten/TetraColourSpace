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

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

public class Tetrahedron
{
	public final Vector3 mediumPos;
	public final Vector3 longPos;
	public final Vector3 shortPos;
	public final Vector3 uvPos;
	public final Vector3 achroPos;

	public Tetrahedron(float size)
	{
		float pi = MathUtils.PI;
		float circleThird = MathUtils.PI2/3;
		float deg110 = (float) Math.toRadians(109.5);
		float magnitude = 0.75f * size;
		mediumPos = TCSUtils.createVectorFromAngles(pi/2, pi/2 - deg110, magnitude);
		longPos = TCSUtils.createVectorFromAngles(pi/2 - circleThird, pi/2 - deg110, magnitude);
		shortPos = TCSUtils.createVectorFromAngles(pi/2 + circleThird, pi/2 - deg110, magnitude);
		uvPos = TCSUtils.createVectorFromAngles(0, pi/2, magnitude);
		achroPos = Vector3.Zero.cpy();
	}
}