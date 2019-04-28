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

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;

public class ModelInstanceFactory
{
	private ModelInstanceFactory() {}
	
	public static ModelInstance create(Model model, Vector3 position, Color colour)
	{
		Material material = TCSUtils.getMaterialForColour(colour);
		
		ModelInstance instance = new ModelInstance(model);
		instance.transform.setTranslation(position);
		setMaterial(material, instance);
		return instance;
	}


	private static void setMaterial(Material material, ModelInstance instance)
	{
		instance.nodes.forEach(node -> node.parts.forEach(part -> part.material = material));
	}
}
