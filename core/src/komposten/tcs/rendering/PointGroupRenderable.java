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
package komposten.tcs.rendering;

import java.util.ArrayList;
import java.util.List;

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


public abstract class PointGroupRenderable implements Disposable
{
	protected final Environment environment;
	protected final Model model;

	public PointGroupRenderable(PointGroup data, int sphereSegments, Environment environment)
	{
		float size = data.getSize();
		
		this.environment = environment;
		this.model = createModel(data.getShape(), size, sphereSegments);
		
		createDataPoints(data.getPoints());
	}
	

	protected abstract void createDataPoints(List<Point> points);


	private Model createModel(Shape shape, float size, int sphereSegments)
	{
		ModelBuilder builder = new ModelBuilder();
		float diameter = size;
		
		switch (shape)
		{
			case BOX :
				return ShapeFactory.createBox(builder, diameter*0.8f, GL20.GL_TRIANGLES);
			case PYRAMID :
				return ShapeFactory.createTetrahedron(builder, diameter*0.9f, GL20.GL_TRIANGLES);
			case SPHERE :
			default :
				return ShapeFactory.createSphere(builder, diameter, GL20.GL_TRIANGLES, sphereSegments);
		}
	}
	

	public abstract void render(ModelBatch batch);
	
	
	@Override
	public void dispose()
	{
		model.dispose();
	}
	
	
	public static class MeshPerPoint extends PointGroupRenderable
	{
		private List<ModelInstance> pointModels;
		
		
		public MeshPerPoint(PointGroup data, int sphereSegments, Environment environment)
		{
			super(data, sphereSegments, environment);
		}
		
		
		@Override
		protected void createDataPoints(List<Point> points)
		{
			pointModels = new ArrayList<>(points.size());
			for (Point point : points)
			{
				ModelInstance instance = ModelInstanceFactory.create(model,
						point.getCoordinates(), point.getColour());
				pointModels.add(instance);
			}
		}
		
		
		@Override
		public void render(ModelBatch batch)
		{
			if (pointModels != null)
				batch.render(pointModels, environment);
		}
	}

	
	
	public static class MeshPerGroup extends PointGroupRenderable
	{
		private List<Mesh> pointMeshes;
		private List<Renderable> pointRenderables;
		
		
		public MeshPerGroup(PointGroup data, int sphereSegments, Environment environment)
		{
			super(data, sphereSegments, environment);
		}
		
		
		@Override
		protected void createDataPoints(List<Point> points)
		{
			Mesh modelMesh = model.meshes.first();
			
			int floatsPerVertex = modelMesh.getVertexSize()/4;
			int modelsPerMesh = Short.MAX_VALUE / modelMesh.getNumVertices();
			float meshesNeeded = points.size() / (float)modelsPerMesh;
			List<Mesh> meshes = new ArrayList<>(MathUtils.ceil(meshesNeeded));
			
			float[] vertices = new float[modelMesh.getNumVertices() * floatsPerVertex];
			short[] indices = new short[modelMesh.getNumIndices()];
			
			int modelCount = 0;
			int modelsInCurrentMesh = modelsPerMesh;
			
			MeshBuilder builder = new MeshBuilder();
			builder.begin(Usage.Position | Usage.Normal | Usage.ColorUnpacked, GL20.GL_TRIANGLES);
			
			for (Point point : points)
			{
				if (modelCount >= modelsPerMesh)
				{
					meshes.add(builder.end());
					builder.begin(Usage.Position | Usage.Normal | Usage.ColorUnpacked, GL20.GL_TRIANGLES);
					
					if (meshesNeeded > 1)
						modelsInCurrentMesh = (int) (modelsPerMesh * meshesNeeded + 1);
					
					builder.ensureVertices(modelsInCurrentMesh * modelMesh.getNumVertices());
					builder.ensureIndices(modelsInCurrentMesh * modelMesh.getNumIndices());
					
					modelCount = 0;
					meshesNeeded--;
				}
				
				modelMesh.getVertices(vertices);
				modelMesh.getIndices(indices);
				
				for (int v = 0; v < modelMesh.getNumVertices(); v++)
				{
					vertices[v*floatsPerVertex+0] += point.getCoordinates().x;
					vertices[v*floatsPerVertex+1] += point.getCoordinates().y;
					vertices[v*floatsPerVertex+2] += point.getCoordinates().z;
					vertices[v*floatsPerVertex+3] = point.getColour().r;
					vertices[v*floatsPerVertex+4] = point.getColour().g;
					vertices[v*floatsPerVertex+5] = point.getColour().b;
					vertices[v*floatsPerVertex+6] = point.getColour().a;
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
				renderable.environment = environment;
				pointRenderables.add(renderable);
			}
		}
		
		
		@Override
		public void render(ModelBatch batch)
		{
			if (pointRenderables != null)
			{
				for (Renderable renderable : pointRenderables)
				{
					batch.render(renderable);
				}
			}
		}
	
	
		@Override
		public void dispose()
		{
			super.dispose();
			
			for (Mesh mesh : pointMeshes)
				mesh.dispose();
		}
	}
}
