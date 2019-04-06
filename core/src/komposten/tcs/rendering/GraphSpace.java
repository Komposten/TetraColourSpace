package komposten.tcs.rendering;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;

import komposten.tcs.backend.Style;
import komposten.tcs.backend.Style.Colour;
import komposten.tcs.backend.data.Point;
import komposten.tcs.util.ModelInstanceFactory;
import komposten.tcs.util.ShapeFactory;
import komposten.tcs.util.TCSUtils;
import komposten.tcs.util.Tetrahedron;

public class GraphSpace implements Disposable
{
	private enum MetricLineVisibility
	{
		HIDDEN,
		OUTLINE,
		FILL
	}
	private Camera camera;
	
	private List<Disposable> disposables;
	private List<ModelInstance> cornerModels;
	private TetrahedronSide[] tetrahedronSides;
	private Renderable tetrahedronLines;
	private Renderable axisLines;
	private MetricLineRenderable metricLines;

	private boolean showTetrahedronSides = false;
	private boolean showAxes = false;
	private MetricLineVisibility pointMetricVariant = MetricLineVisibility.HIDDEN;
	
	
	public GraphSpace(Style style, Camera camera, Environment environment)
	{
		this.camera = camera;
		
		cornerModels = new ArrayList<>();
		disposables = new ArrayList<>();
		
		Tetrahedron tetrahedron = new Tetrahedron(1f);

		ModelBuilder modelBuilder = new ModelBuilder();
		createTetrahedronCorners(tetrahedron, modelBuilder, style);
		
		MeshBuilder meshBuilder = new MeshBuilder();
		createTetrahedron(meshBuilder, style);
		createAxisLines(meshBuilder, style);
		
		metricLines = new MetricLineRenderable(style, environment);
	}
	
	
	public void toggleTetrahedronSides()
	{
		showTetrahedronSides = !showTetrahedronSides;
	}
	
	
	public void toggleAxisLines()
	{
		showAxes = !showAxes;
	}

	
	public void togglePointMetrics()
	{
		int index = pointMetricVariant.ordinal();
		int next = (index + 1) % MetricLineVisibility.values().length;
		pointMetricVariant = MetricLineVisibility.values()[next];
	}
	
	
	public void setPointMetricTarget(Point point)
	{
		metricLines.setTarget(point);
	}
	
	
	private void createTetrahedron(MeshBuilder meshBuilder, Style style)
	{
		Mesh[] tetrahedronMesh = ShapeFactory.createTetrahedronSideMeshes(1f, GL20.GL_TRIANGLES, meshBuilder, style);
		
		tetrahedronSides = new TetrahedronSide[4];
		
		Material materialWhite = TCSUtils.getMaterialForColour(Color.WHITE);
		for (int i = 0; i < tetrahedronMesh.length; i++)
		{
			tetrahedronSides[i] = new TetrahedronSide();
			tetrahedronSides[i].renderable = new Renderable();
			tetrahedronSides[i].renderable.material = new Material(materialWhite);
			tetrahedronSides[i].renderable.material.set(new BlendingAttribute(0.5f));
			tetrahedronSides[i].renderable.meshPart.set("tetrahedron_side_" + i, tetrahedronMesh[i], 0, tetrahedronMesh[i].getNumVertices(), GL20.GL_TRIANGLES);
			disposables.add(tetrahedronMesh[i]);

			tetrahedronSides[i].centre = getMeshCentre(tetrahedronMesh[i]);
		}

		Mesh tetrahedronMeshSingle = ShapeFactory.createTetrahedronMesh(1f, GL20.GL_LINES, meshBuilder, style);
		
		tetrahedronLines = new Renderable();
		tetrahedronLines.material = new Material(materialWhite);
		tetrahedronLines.meshPart.set("tetrahedron_lines", tetrahedronMeshSingle, 0, tetrahedronMeshSingle.getNumVertices(), GL20.GL_LINES);
		disposables.add(tetrahedronMeshSingle);
	}
	
	
	private Vector3 getMeshCentre(Mesh mesh)
	{
		int vertexSize = mesh.getVertexSize() / 4;
		int vertexCount = mesh.getNumVertices();
		float[] vertices = mesh.getVertices(new float[vertexCount * vertexSize]);
		
		float avgX = 0;
		float avgY = 0;
		float avgZ = 0;
		
		for (int i = 0; i < vertexCount; i++)
		{
			avgX += vertices[i*vertexSize];
			avgY += vertices[i*vertexSize+1];
			avgZ += vertices[i*vertexSize+2];
		}
		
		return new Vector3(avgX / vertexCount, avgY / vertexCount, avgZ / vertexCount);
	}


	private void createTetrahedronCorners(Tetrahedron tetrahedron, ModelBuilder modelBuilder, Style style)
	{
		float diameter = 0.03f;
		Model sphereModel = ShapeFactory.createSphere(modelBuilder, diameter, GL20.GL_TRIANGLES, World.SPHERE_SEGMENTS);
		
		ModelInstance redSphere = ModelInstanceFactory.create(sphereModel, tetrahedron.longPos, style.get(Colour.WL_LONG));
		ModelInstance greenSphere = ModelInstanceFactory.create(sphereModel, tetrahedron.mediumPos, style.get(Colour.WL_MEDIUM));
		ModelInstance blueSphere = ModelInstanceFactory.create(sphereModel, tetrahedron.shortPos, style.get(Colour.WL_SHORT));
		ModelInstance uvSphere = ModelInstanceFactory.create(sphereModel, tetrahedron.uvPos, style.get(Colour.WL_UV));
		ModelInstance achroSphere = ModelInstanceFactory.create(sphereModel, tetrahedron.achroPos, style.get(Colour.ACHRO));
		
		cornerModels.add(redSphere);
		cornerModels.add(greenSphere);
		cornerModels.add(blueSphere);
		cornerModels.add(uvSphere);
		cornerModels.add(achroSphere);

		disposables.add(sphereModel);
	}


	private void createAxisLines(MeshBuilder meshBuilder, Style style)
	{
		meshBuilder.begin(Usage.Position | Usage.Normal | Usage.ColorUnpacked, GL20.GL_LINES);

		float length = 0.2f;
		Vector3 start = new Vector3();
		Vector3 end = new Vector3();
		Color colourShort = style.get(Colour.WL_SHORT);
		Color colourMedium = style.get(Colour.WL_MEDIUM);
		Color colourLong = style.get(Colour.WL_LONG);
		Color colourUv = style.get(Colour.WL_UV);
		Color colourSL = colourLong.cpy().lerp(colourShort, 0.5f);
		Color colourSML = colourLong.cpy().lerp(colourMedium, 0.5f).lerp(colourShort, 1/3f);
		
		colourSL = maximiseBrightness(colourSL);
		colourSML = maximiseBrightness(colourSML);
		
		meshBuilder.line(start.set(-length, 0, 0), colourShort, end.set(length, 0, 0), colourLong);
		meshBuilder.line(start.set(0, -length, 0), colourSML, end.set(0, length, 0), colourUv);
		meshBuilder.line(start.set(0, 0, -length), colourMedium, end.set(0, 0, length), colourSL);

		Mesh mesh = meshBuilder.end();
		
		axisLines = new Renderable();
		axisLines.material = TCSUtils.getMaterialForColour(Color.WHITE);
		axisLines.meshPart.set("lines", mesh, 0, mesh.getNumVertices(), GL20.GL_LINES);
	}


	private Color maximiseBrightness(Color colour)
	{
		float largest = Math.max(colour.r, Math.max(colour.g, colour.b));
		float ratio = 1 / largest;
		
		return colour.mul(ratio);
	}
	
	
	void cameraUpdated()
	{
		Arrays.sort(tetrahedronSides, tetrahedronSideComparator);
	}
	
	
	void render(ModelBatch batch, Environment environment)
	{
		renderTetrahedron(batch, environment);
		renderAxes(batch);
		renderPointMetrics(batch);
	}


	private void renderTetrahedron(ModelBatch batch, Environment environment)
	{
		if (showTetrahedronSides)
		{
			for (TetrahedronSide side : tetrahedronSides)
			{
				batch.render(side.renderable);
			}
		}
		else
		{
			batch.render(tetrahedronLines);
		}

		batch.render(cornerModels, environment);
	}


	private void renderAxes(ModelBatch batch)
	{
		if (showAxes || pointMetricVariant != MetricLineVisibility.HIDDEN)
			batch.render(axisLines);
	}


	private void renderPointMetrics(ModelBatch batch)
	{
		if (pointMetricVariant != MetricLineVisibility.HIDDEN)
			metricLines.render(batch, pointMetricVariant == MetricLineVisibility.FILL);
	}
	
	
	@Override
	public void dispose()
	{
		for (Disposable disposable : disposables)
		{
			disposable.dispose();
		}
	}
	
	
	private Vector3 calcVector = new Vector3();
	private Comparator<TetrahedronSide> tetrahedronSideComparator = (side1, side2) -> 
	{
			float length1 = calcVector.set(camera.position).sub(side1.centre).len2();
			float length2 = calcVector.set(camera.position).sub(side2.centre).len2();
			return -Float.compare(length1, length2);
	};
	
	
	private static class TetrahedronSide
	{
		Renderable renderable;
		Vector3 centre;
	}
}
