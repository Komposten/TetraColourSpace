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
