package komposten.tcs.util;

import static komposten.tcs.util.TestUtils.assertVectorEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.math.Vector3;


class ModelInstanceFactoryTest
{
	@Test
	void create()
	{
		Model model = buildModel();
		Vector3 position = new Vector3(1, 2, 3);
		Color colour = Color.RED;

		ModelInstance instance = ModelInstanceFactory.create(model, position, colour);

		assertEquals(model, instance.model);
		assertVectorEquals(position, instance.transform.getTranslation(new Vector3()));

		instance.nodes.forEach(node -> {
			node.parts.forEach(part -> {
				ColorAttribute attr = (ColorAttribute) part.material.get(ColorAttribute.Diffuse);
				assertEquals(colour, attr.color);
			});
		});
	}


	private Model buildModel()
	{
		Model model = new Model();
		
		Node node1 = new Node();
		Node node2 = new Node();
		
		NodePart part1 = new NodePart(new MeshPart(), new Material());
		NodePart part2 = new NodePart(new MeshPart(), new Material());
		NodePart part3 = new NodePart(new MeshPart(), new Material());
		NodePart part4 = new NodePart(new MeshPart(), new Material());
		
		model.nodes.add(node1);
		model.nodes.add(node2);
		
		node1.parts.add(part1);
		node1.parts.add(part2);
		node2.parts.add(part3);
		node2.parts.add(part4);
		
		return model;
	}

}
