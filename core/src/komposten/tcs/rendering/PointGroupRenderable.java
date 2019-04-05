package komposten.tcs.rendering;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.utils.Disposable;

import komposten.tcs.backend.data.Point;
import komposten.tcs.backend.data.PointGroup;
import komposten.tcs.backend.data.Shape;
import komposten.tcs.util.ModelInstanceFactory;
import komposten.tcs.util.ShapeFactory;


public class PointGroupRenderable implements Disposable
{
	private Model model;
	
	private List<ModelInstance> pointModels;

	public PointGroupRenderable(PointGroup data, float size)
	{
		createModel(data.getShape(), size);
		createModelInstances(data.getPoints());
	}


	private void createModel(Shape shape, float size)
	{
		ModelBuilder builder = new ModelBuilder();
		float diameter = size;
		
		switch (shape)
		{
			case BOX :
				model = ShapeFactory.createBox(builder, diameter, GL20.GL_TRIANGLES);
				break;
			case PYRAMID :
				model = ShapeFactory.createTetrahedron(builder, diameter, GL20.GL_TRIANGLES);
				break;
			case SPHERE :
			default :
				model = ShapeFactory.createSphere(builder, diameter, GL20.GL_TRIANGLES, World.SPHERE_SEGMENTS);
				break;
		}
	}


	private void createModelInstances(List<Point> points)
	{
		pointModels = new ArrayList<>(points.size());
		for (Point point : points)
		{
			ModelInstance instance = ModelInstanceFactory.create(model,
					point.getCoordinates(), point.getColour());
			pointModels.add(instance);
		}
	}
	
	
	public void render(ModelBatch batch, Environment environment)
	{
		batch.render(pointModels, environment);
	}


	@Override
	public void dispose()
	{
		model.dispose();
	}
}
