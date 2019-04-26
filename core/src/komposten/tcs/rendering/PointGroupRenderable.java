package komposten.tcs.rendering;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;

import komposten.tcs.backend.data.Point;
import komposten.tcs.backend.data.PointGroup;
import komposten.tcs.backend.data.Shape;
import komposten.tcs.util.ModelInstanceFactory;
import komposten.tcs.util.ShapeFactory;
import komposten.tcs.util.TCSUtils;


public class PointGroupRenderable implements Disposable
{
	private Model model;
	private Mesh pointMesh;
	private List<Mesh> pointMeshes;
	
	private List<ModelInstance> pointModels;
	private Renderable pointRenderable;
	private List<Renderable> pointRenderables;

	public PointGroupRenderable(PointGroup data, int sphereSegments)
	{
		float size = data.getSize();
		
		createModel(data.getShape(), size, sphereSegments);
//		createModelInstances(data.getPoints());
		createPointMesh(data.getPoints());
	}


	private void createModel(Shape shape, float size, int sphereSegments)
	{
		ModelBuilder builder = new ModelBuilder();
		float diameter = size;
		
		switch (shape)
		{
			case BOX :
				model = ShapeFactory.createBox(builder, diameter*0.8f, GL20.GL_TRIANGLES);
				break;
			case PYRAMID :
				model = ShapeFactory.createTetrahedron(builder, diameter*0.9f, GL20.GL_TRIANGLES);
				break;
			case SPHERE :
			default :
				model = ShapeFactory.createSphere(builder, diameter, GL20.GL_TRIANGLES, sphereSegments);
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
	
	
	private void createPointMesh(List<Point> points)
	{
		//CURRENT Split into multiple meshes, since each mesh may only have Short.MAX_VALUE vertices!
		Mesh modelMesh = model.meshes.first();
		int stride = modelMesh.getVertexSize()/4;
		
		int verticesPerMesh = Short.MAX_VALUE;
		int modelsPerMesh = verticesPerMesh / modelMesh.getNumVertices();
		float meshesNeeded = points.size() / (float)modelsPerMesh;
		List<Mesh> meshes = new ArrayList<>(MathUtils.ceil(meshesNeeded));
		
		float[] vertices = new float[modelMesh.getNumVertices() * stride];
		short[] indices = new short[modelMesh.getNumIndices()];
		MeshBuilder builder = new MeshBuilder();
		
		boolean hasBegun = false;
		int modelCount = 0;
		
		System.out.println("Verts per mesh = " + verticesPerMesh);
		System.out.println("Models per mesh = " + modelsPerMesh);
		System.out.println("Verts per model = " + modelMesh.getNumVertices());
		System.out.println("Point count = " + points.size());
		System.out.println("Meshes needed = " + meshesNeeded);
		
		for (Point point : points)
		{
			if (modelCount >= modelsPerMesh || !hasBegun)
			{
				if (hasBegun)
					meshes.add(builder.end());
				else
					hasBegun = true;
				
				builder.begin(Usage.Position | Usage.Normal | Usage.ColorUnpacked, GL20.GL_TRIANGLES);
				
				int numModels = (int) (meshesNeeded > 1 ? modelsPerMesh : modelsPerMesh * meshesNeeded + 1);
				int numVertices = numModels * modelMesh.getNumVertices();
				int numIndices = numModels * modelMesh.getNumIndices();
				
				builder.ensureVertices(numVertices);
				builder.ensureIndices(numIndices);
				
				modelCount = 0;
				meshesNeeded--;
			}
			
			modelMesh.getVertices(vertices);
			modelMesh.getIndices(indices);
			
			for (int v = 0; v < modelMesh.getNumVertices(); v++)
			{
				vertices[v*stride+0] += point.getCoordinates().x;
				vertices[v*stride+1] += point.getCoordinates().y;
				vertices[v*stride+2] += point.getCoordinates().z;
				vertices[v*stride+3] = point.getColour().r;
				vertices[v*stride+4] = point.getColour().g;
				vertices[v*stride+5] = point.getColour().b;
				vertices[v*stride+6] = point.getColour().a;
			}
			
			builder.addMesh(vertices, indices);
			
			modelCount++;
		}
		
		meshes.add(builder.end());
		
		pointMeshes = meshes;
		
		pointRenderables = new ArrayList<>(pointMeshes.size());
		
		for (Mesh mesh : pointMeshes)
		{
			Renderable renderable = new Renderable();
			renderable.meshPart.set("points", mesh, 0, mesh.getNumIndices(), GL20.GL_TRIANGLES);
			renderable.material = TCSUtils.getMaterialForColour(Color.WHITE).copy();
			pointRenderables.add(renderable);
		}
	}
	
	
	float time;
	public void render(ModelBatch batch, Environment environment)
	{
		if (time > 1)
		{
			System.err.println(Gdx.graphics.getFramesPerSecond());
			time -= 1;
		}
		time += Gdx.graphics.getDeltaTime();
		
		if (pointModels != null)
			batch.render(pointModels, environment);
		
		if (pointRenderables != null)
		{
			for (Renderable renderable : pointRenderables)
			{
				renderable.environment = environment;
				batch.render(renderable);
			}
		}
	}


	@Override
	public void dispose()
	{
		model.dispose();
		pointMesh.dispose();
		
		for (Mesh mesh : pointMeshes)
			mesh.dispose();
	}
}
