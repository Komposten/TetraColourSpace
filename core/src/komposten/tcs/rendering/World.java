package komposten.tcs.rendering;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder.VertexInfo;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;

import komposten.tcs.backend.Backend;
import komposten.tcs.backend.Style;
import komposten.tcs.backend.Style.Colour;
import komposten.tcs.backend.data.Point;
import komposten.tcs.backend.data.PointGroup;
import komposten.tcs.backend.data.Volume;
import komposten.tcs.util.Tetrahedron;
import komposten.utilities.tools.Geometry;

/*
 * TODO Move the metric lines and arcs to the UI?
 */
/*
 * TODO Move the data points and volumes into separate classes (Dataset?).
 * TODO Move the static models into a separate class?
 */
public class World implements Disposable
{
	private static final int SPHERE_SEGMENTS = 25;
	private static final int POINT_METRIC_HIDE = 0;
	private static final int POINT_METRIC_FILLED = 2;
	private static final int POINT_METRIC_OPTIONS = 3;
	
	private Backend backend;
	private Camera camera;
	private Environment environment;

	private Vector3 calcVector = new Vector3();

	private List<Disposable> disposables;
	private List<Disposable> volumeMeshes;
	private List<ModelInstance> pyramidCornerModels;
	private List<ModelInstance> pointModelInstances;
	private List<Renderable> volumeRenderables;
	private Map<Point, ModelInstance> pointToModelMap;
	private TetrahedronSide[] pyramidSides;
	private Renderable pyramidLines;
	private Renderable axisLines;
	private Renderable pointMetricsLines;
	private Renderable pointMetricsArcs;
	private ModelInstance selectedModel;
	private ModelInstance highlightModel;
	private Map<Color, Material> materials;
	
	private Point highlightPoint;
	private Point selectedPoint;
	
	private boolean showPyramidSides = false;
	private boolean showAxes = false;
	private boolean showPoints = true;
	private boolean showVolumes = true;
	private boolean showHighlight = true;
	private int showPointMetrics = 0;
	private boolean hasSelection = false;
	private boolean hasHighlight = false;


	public World(Backend backend, Camera camera)
	{
		this.backend = backend;
		this.camera = camera;
		
		disposables = new ArrayList<>();
		volumeMeshes = new ArrayList<>();
		pyramidCornerModels = new ArrayList<>();
		pointModelInstances = new ArrayList<>();
		volumeRenderables = new ArrayList<>();
		materials = new HashMap<>();
		pointToModelMap = new HashMap<>();

		createEnvironment();
		
		generatePointObjects();
		generateVolumeObjects();
		createStaticModels();
	}
	
	
	public boolean hasSelection()
	{
		return hasSelection;
	}
	
	
	public boolean hasHighlight()
	{
		return hasHighlight;
	}
	
	
	public boolean pointsVisible()
	{
		return showPoints;
	}
	
	
	public Point getSelectedPoint()
	{
		return selectedPoint;
	}
	
	
	public void togglePyramidSides()
	{
		showPyramidSides = !showPyramidSides;
	}
	
	
	public void toggleAxisLines()
	{
		showAxes = !showAxes;
	}
	
	
	public void toggleHighlight()
	{
		showHighlight = !showHighlight;
	}
	
	
	public void togglePointMetrics()
	{
		showPointMetrics = (showPointMetrics + 1) % POINT_METRIC_OPTIONS;
	}
	
	
	public void togglePoints()
	{
		showPoints = !showPoints;
	}
	
	
	public void toggleVolumes()
	{
		showVolumes = !showVolumes;
	}
	
	
	private void createEnvironment()
	{
		DirectionalLight light = new DirectionalLight();
		light.setDirection(-1f, -1f, 0);
		light.setColor(Color.WHITE);
		environment = new Environment();
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.6f, 0.6f, 0.6f, 1.0f));
		environment.add(light);
	}
	
	
	private void createStaticModels()
	{
		Tetrahedron tetrahedron = new Tetrahedron(1f);

		ModelBuilder modelBuilder = new ModelBuilder();
		createPyramidCorners(tetrahedron, modelBuilder);

		Style style = backend.getStyle();
		Model model = createSphere(modelBuilder, 0.025f, GL20.GL_LINES, 10);
		selectedModel = createModelInstance(model, Vector3.Zero, style.get(Colour.SELECTION));
		highlightModel = createModelInstance(model, Vector3.Zero, style.get(Colour.HIGHLIGHT));
		
		MeshBuilder meshBuilder = new MeshBuilder();
		createTCSPyramid(meshBuilder);
		createAxisLines(meshBuilder);
		
		pointMetricsLines = new Renderable();
		pointMetricsArcs = new Renderable();
		pointMetricsLines.material = getMaterialForColour(style.get(Colour.METRIC_LINE));
		pointMetricsArcs.material = getMaterialForColour(style.get(Colour.METRIC_FILL));
		pointMetricsArcs.environment = environment;
	}
	
	
	private void createTCSPyramid(MeshBuilder meshBuilder)
	{
		Mesh[] pyramidMesh = createPyramidMeshSeparate(1f, GL20.GL_TRIANGLES, meshBuilder, true);
		
		pyramidSides = new TetrahedronSide[4];
		
		for (int i = 0; i < pyramidMesh.length; i++)
		{
			pyramidSides[i] = new TetrahedronSide();
			pyramidSides[i].renderable = new Renderable();
			pyramidSides[i].renderable.material = new Material(getMaterialForColour(Color.WHITE));
			pyramidSides[i].renderable.material.set(new BlendingAttribute(0.5f));
			pyramidSides[i].renderable.meshPart.set("pyramid_side_" + i, pyramidMesh[i], 0, pyramidMesh[i].getNumVertices(), GL20.GL_TRIANGLES);
			disposables.add(pyramidMesh[i]);

			pyramidSides[i].centre = getMeshCentre(pyramidMesh[i]);
		}

		Mesh pyramidMeshSingle = createPyramidMesh(1f, GL20.GL_LINES, meshBuilder, true);
		
		pyramidLines = new Renderable();
		pyramidLines.material = new Material(getMaterialForColour(Color.WHITE));
		pyramidLines.meshPart.set("pyramid_lines", pyramidMeshSingle, 0, pyramidMeshSingle.getNumVertices(), GL20.GL_LINES);
		disposables.add(pyramidMeshSingle);
	}


	private Mesh createPyramidMesh(float size, int primitiveType, MeshBuilder meshBuilder, boolean applyColours)
	{
		Mesh[] sides = createPyramidMeshSeparate(size, primitiveType, meshBuilder, applyColours);
		
		meshBuilder.begin(Usage.Position | Usage.Normal | Usage.ColorUnpacked, GL20.GL_TRIANGLES);
		for (Mesh side : sides)
		{
			meshBuilder.addMesh(side);
			side.dispose();
		}
		
		return meshBuilder.end();
	}
	
	
	private Mesh[] createPyramidMeshSeparate(float size, int primitiveType, MeshBuilder meshBuilder, boolean applyColours)
	{
		Tetrahedron tetrahedron = new Tetrahedron(size);
		
		boolean triangleType = (primitiveType == GL20.GL_TRIANGLES);
		return createPyramidSeparate(tetrahedron, meshBuilder, triangleType, applyColours);
	}


	private Mesh[] createPyramidSeparate(Tetrahedron tetrahedron, MeshBuilder meshBuilder, boolean doubleFaced, boolean applyColours)
	{
		Mesh[] meshes = new Mesh[4];
		int meshAttributes = Usage.Position | Usage.Normal | Usage.ColorUnpacked;
		meshBuilder.begin(meshAttributes, GL20.GL_TRIANGLES);
		
		Vector3 longPos = tetrahedron.longPos;
		Vector3 mediumPos = tetrahedron.mediumPos;
		Vector3 shortPos = tetrahedron.shortPos;
		Vector3 uvPos = tetrahedron.uvPos;
		
		Style style = backend.getStyle();
		Color longColourActive = (applyColours ? style.get(Colour.WL_LONG) : Color.WHITE);
		Color mediumColourActive = (applyColours ? style.get(Colour.WL_MEDIUM) : Color.WHITE);
		Color shortColourActive = (applyColours ? style.get(Colour.WL_SHORT) : Color.WHITE);
		Color uvColourActive = (applyColours ? style.get(Colour.WL_UV) : Color.WHITE);

		Vector3 normal = mediumPos.cpy().add(longPos).add(shortPos);
		short corner1 = meshBuilder.vertex(mediumPos, normal, mediumColourActive, Vector2.Zero);
		short corner2 = meshBuilder.vertex(longPos, normal, longColourActive, Vector2.Zero);
		short corner3 = meshBuilder.vertex(shortPos, normal, shortColourActive, Vector2.Zero);
		meshBuilder.triangle(corner1, corner2, corner3);

		if (doubleFaced)
		{
			normal = normal.scl(-1);
			corner1 = meshBuilder.vertex(mediumPos, normal, mediumColourActive, Vector2.Zero);
			corner2 = meshBuilder.vertex(longPos, normal, longColourActive, Vector2.Zero);
			corner3 = meshBuilder.vertex(shortPos, normal, shortColourActive, Vector2.Zero);
			meshBuilder.triangle(corner3, corner2, corner1);
		}
		
		meshes[0] = meshBuilder.end();
		meshBuilder.begin(meshAttributes, GL20.GL_TRIANGLES);
		
		normal = longPos.cpy().add(mediumPos).add(uvPos);
		corner1 = meshBuilder.vertex(longPos, normal, longColourActive, Vector2.Zero);
		corner2 = meshBuilder.vertex(mediumPos, normal, mediumColourActive, Vector2.Zero);
		corner3 = meshBuilder.vertex(uvPos, normal, uvColourActive, Vector2.Zero);
		meshBuilder.triangle(corner1, corner2, corner3);

		if (doubleFaced)
		{
			normal = normal.scl(-1);
			corner1 = meshBuilder.vertex(longPos, normal, longColourActive, Vector2.Zero);
			corner2 = meshBuilder.vertex(mediumPos, normal, mediumColourActive, Vector2.Zero);
			corner3 = meshBuilder.vertex(uvPos, normal, uvColourActive, Vector2.Zero);
			meshBuilder.triangle(corner3, corner2, corner1);
		}
		
		meshes[1] = meshBuilder.end();
		meshBuilder.begin(meshAttributes, GL20.GL_TRIANGLES);

		normal = longPos.cpy().add(uvPos).add(shortPos);
		corner1 = meshBuilder.vertex(longPos, normal, longColourActive, Vector2.Zero);
		corner2 = meshBuilder.vertex(uvPos, normal, uvColourActive, Vector2.Zero);
		corner3 = meshBuilder.vertex(shortPos, normal, shortColourActive, Vector2.Zero);
		meshBuilder.triangle(corner1, corner2, corner3);

		if (doubleFaced)
		{
			normal = normal.scl(-1);
			corner1 = meshBuilder.vertex(longPos, normal, longColourActive, Vector2.Zero);
			corner2 = meshBuilder.vertex(uvPos, normal, uvColourActive, Vector2.Zero);
			corner3 = meshBuilder.vertex(shortPos, normal, shortColourActive, Vector2.Zero);
			meshBuilder.triangle(corner3, corner2, corner1);
		}
		
		meshes[2] = meshBuilder.end();
		meshBuilder.begin(meshAttributes, GL20.GL_TRIANGLES);

		normal = uvPos.cpy().add(mediumPos).add(shortPos);
		corner1 = meshBuilder.vertex(uvPos, normal, uvColourActive, Vector2.Zero);
		corner2 = meshBuilder.vertex(mediumPos, normal, mediumColourActive, Vector2.Zero);
		corner3 = meshBuilder.vertex(shortPos, normal, shortColourActive, Vector2.Zero);
		meshBuilder.triangle(corner1, corner2, corner3);

		if (doubleFaced)
		{
			normal = normal.scl(-1);
			corner1 = meshBuilder.vertex(uvPos, normal, uvColourActive, Vector2.Zero);
			corner2 = meshBuilder.vertex(mediumPos, normal, mediumColourActive, Vector2.Zero);
			corner3 = meshBuilder.vertex(shortPos, normal, shortColourActive, Vector2.Zero);
			meshBuilder.triangle(corner3, corner2, corner1);
		}
		
		meshes[3] = meshBuilder.end();

		return meshes;
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


	private void createPyramidCorners(Tetrahedron tetrahedron, ModelBuilder modelBuilder)
	{
		float diameter = 0.03f;
		Model sphereModel = createSphere(modelBuilder, diameter, GL20.GL_TRIANGLES);
		
		Style style = backend.getStyle();
		ModelInstance redSphere = createModelInstance(sphereModel, tetrahedron.longPos, style.get(Colour.WL_LONG));
		ModelInstance greenSphere = createModelInstance(sphereModel, tetrahedron.mediumPos, style.get(Colour.WL_MEDIUM));
		ModelInstance blueSphere = createModelInstance(sphereModel, tetrahedron.shortPos, style.get(Colour.WL_SHORT));
		ModelInstance uvSphere = createModelInstance(sphereModel, tetrahedron.uvPos, style.get(Colour.WL_UV));
		ModelInstance achroSphere = createModelInstance(sphereModel, tetrahedron.achroPos, style.get(Colour.ACHRO));
		
		pyramidCornerModels.add(redSphere);
		pyramidCornerModels.add(greenSphere);
		pyramidCornerModels.add(blueSphere);
		pyramidCornerModels.add(uvSphere);
		pyramidCornerModels.add(achroSphere);

		disposables.add(sphereModel);
	}


	private void createAxisLines(MeshBuilder meshBuilder)
	{
		meshBuilder.begin(Usage.Position | Usage.Normal | Usage.ColorUnpacked, GL20.GL_LINES);

		Style style = backend.getStyle();
		float length = 0.2f;
		Vector3 start = new Vector3();
		Vector3 end = new Vector3();
		Color colourShort = style.get(Colour.WL_SHORT);
		Color colourMedium = style.get(Colour.WL_MEDIUM);
		Color colourLong = style.get(Colour.WL_LONG);
		Color colourUv = style.get(Colour.WL_UV);
		Color colourShortLong = colourLong.cpy().lerp(colourShort, 0.5f);
		
		meshBuilder.line(start.set(-length, 0, 0), colourShort, end.set(length, 0, 0), colourLong);
		meshBuilder.line(start.set(0, -length, 0), Color.WHITE, end.set(0, length, 0), colourUv);
		meshBuilder.line(start.set(0, 0, -length), colourMedium, end.set(0, 0, length), colourShortLong);

		Mesh mesh = meshBuilder.end();
		
		axisLines = new Renderable();
		axisLines.material = getMaterialForColour(Color.WHITE);
		axisLines.meshPart.set("lines", mesh, 0, mesh.getNumVertices(), GL20.GL_LINES);
	}


	private Model createSphere(ModelBuilder modelBuilder, float diameter, int primitiveType)
	{
		return createSphere(modelBuilder, diameter, primitiveType, SPHERE_SEGMENTS);
	}


	private Model createSphere(ModelBuilder modelBuilder, float diameter, int primitiveType, int segments)
	{
		return modelBuilder.createSphere(
				diameter, diameter, diameter, segments, segments,
				primitiveType, new Material(), Usage.Position | Usage.Normal);
	}
	
	
	private Model createBox(ModelBuilder modelBuilder, float size, int primitiveType)
	{
		return modelBuilder.createBox(size, size, size, primitiveType,
				new Material(), Usage.Position | Usage.Normal);
	}
	
	
	private Model createPyramid(ModelBuilder modelBuilder, float size, int primitiveType)
	{
		Mesh mesh = createPyramidMesh(size, primitiveType, new MeshBuilder(), false);
		
		modelBuilder.begin();
		modelBuilder.part("pyramid", mesh, primitiveType, new Material());
		return modelBuilder.end();
	}
	
	
	private ModelInstance createModelInstance(Model model, Vector3 position, Color colour)
	{
		Material material = materials.get(colour);
		
		if (material == null)
		{
			material = new Material(ColorAttribute.createDiffuse(colour));
			materials.put(colour, material);
		}
		
		ModelInstance instance = new ModelInstance(model);
		instance.transform.setTranslation(position);
		setMaterial(material, instance);
		return instance;
	}
	
	
	private void createPointMetricMesh(Point point)
	{
		Vector3 line = point.getCoordinates();
		Vector3 lineInZX = new Vector3(line.x, 0, line.z);
		
		float arcRadius = lineInZX.len()*0.8f;
		int segments = 200;
		float segmentsPerRadian = segments / MathUtils.PI2;
		float theta = point.getMetrics().x;
		float phi = point.getMetrics().y;
		
		while (theta > MathUtils.PI) theta -= MathUtils.PI2;
		while (theta < -MathUtils.PI) theta += MathUtils.PI2;

		float thetaDeg = theta*MathUtils.radiansToDegrees;
		
		int thetaSegments = MathUtils.ceil(Math.abs(theta)*segmentsPerRadian);
		int phiSegments = MathUtils.ceil(Math.abs(phi)*segmentsPerRadian);
		float[] thetaArc = Geometry.createArc(0, -theta, arcRadius, thetaSegments);
		float[] phiArc = Geometry.createArc(0, phi, arcRadius, phiSegments);
		
		MeshBuilder builder = new MeshBuilder();
		builder.begin(Usage.Position | Usage.Normal | Usage.ColorUnpacked, GL20.GL_LINES);
		
		//Create the lines from origin
		MeshPart lines = builder.part("lines", GL20.GL_LINES);
		builder.line(Vector3.Zero, line);
		builder.line(Vector3.Zero, lineInZX);
		
		//Create the arc outlines
		Vector3 start = new Vector3();
		Vector3 end = new Vector3();
		for (int i = 2; i < thetaArc.length; i+=2)
		{
			start.set(thetaArc[i-2], 0, thetaArc[i-1]);
			end.set(thetaArc[i], 0, thetaArc[i+1]);
			
			if (i == 2)
				builder.line(Vector3.Zero, start);
			else if (i/2 == (thetaArc.length-1)/2)
				builder.line(end, Vector3.Zero);
			builder.line(start, end);
		}
		
		for (int i = 2; i < phiArc.length; i+=2)
		{
			start.set(phiArc[i-2], phiArc[i-1], 0);
			end.set(phiArc[i], phiArc[i+1], 0);
			
			start.rotate(Vector3.Y, thetaDeg);
			end.rotate(Vector3.Y, thetaDeg);

			if (i == 2)
				builder.line(Vector3.Zero, start);
			else if (i/2 == (phiArc.length-1)/2)
				builder.line(end, Vector3.Zero);
			builder.line(start, end);
		}
		
		//Create the filled arcs
		MeshPart arcFill = builder.part("arc_fill", GL20.GL_TRIANGLES);
		Vector3 normal = new Vector3();
		Vector3 normalInv = new Vector3();
		VertexInfo startVertex = new VertexInfo();
		VertexInfo endVertex = new VertexInfo();
		VertexInfo zeroVertex = new VertexInfo().setPos(Vector3.Zero);

		normal.set(0, -1, 0);
		normalInv.set(normal).scl(-1);
		for (int i = 2; i < thetaArc.length; i+=2)
		{
			startVertex.setPos(thetaArc[i-2], 0, thetaArc[i-1]);
			endVertex.setPos(thetaArc[i], 0, thetaArc[i+1]);
			
			startVertex.setNor(normal);
			endVertex.setNor(normal);
			zeroVertex.setNor(normal);
			builder.triangle(startVertex, endVertex, zeroVertex);
			
			startVertex.setNor(normalInv);
			endVertex.setNor(normalInv);
			zeroVertex.setNor(normalInv);
			builder.triangle(startVertex, zeroVertex, endVertex);
		}
		
		normal.set(0, 0, -1).rotate(Vector3.Y, thetaDeg);
		normalInv.set(normal).scl(-1);
		for (int i = 2; i < phiArc.length; i+=2)
		{
			startVertex.setPos(phiArc[i-2], phiArc[i-1], 0);
			endVertex.setPos(phiArc[i], phiArc[i+1], 0);
			
			startVertex.position.rotate(Vector3.Y, thetaDeg);
			endVertex.position.rotate(Vector3.Y, thetaDeg);
			
			startVertex.setNor(normal);
			endVertex.setNor(normal);
			zeroVertex.setNor(normal);
			builder.triangle(startVertex, endVertex, zeroVertex);

			startVertex.setNor(normalInv);
			endVertex.setNor(normalInv);
			zeroVertex.setNor(normalInv);
			builder.triangle(startVertex, zeroVertex, endVertex);
		}
		
		Mesh mesh = builder.end();
		pointMetricsLines.meshPart.set(lines);
		pointMetricsArcs.meshPart.set(arcFill);
		
		disposables.add(mesh);
	}


	private void generatePointObjects()
	{
		ModelBuilder builder = new ModelBuilder();
		float diameter = 0.02f;
		Model modelSphere = createSphere(builder, diameter, GL20.GL_TRIANGLES);
		Model modelBox = createBox(builder, diameter, GL20.GL_TRIANGLES);
		Model modelPyramid = createPyramid(builder, diameter, GL20.GL_TRIANGLES);
		disposables.add(modelSphere);
		disposables.add(modelBox);
		disposables.add(modelPyramid);
		
		for (PointGroup group : backend.getDataGroups())
		{
			Model model;
			
			switch (group.getShape())
			{
				case BOX :
					model = modelBox;
					break;
				case PYRAMID :
					model = modelPyramid;
					break;
				case SPHERE :
				default :
					model = modelSphere;
					break;
			}
			
			for (Point point : group.getPoints())
			{
				
				ModelInstance instance = new ModelInstance(model);
				instance.transform.translate(point.getCoordinates());
				setMaterial(getMaterialForColour(point.getColour()).copy(), instance);
				pointModelInstances.add(instance);
				pointToModelMap.put(point, instance);
			}
		}
	}


	private void generateVolumeObjects()
	{
		MeshBuilder meshBuilder = new MeshBuilder();
		Vector3 vector1 = new Vector3();
		Vector3 vector2 = new Vector3();
		Vector3 vector3 = new Vector3();
		Vector3 vector4 = new Vector3();
		Vector3 edge1 = new Vector3();
		Vector3 edge2 = new Vector3();
		Vector3 normal = new Vector3();
		
		for (Volume volume : backend.getDataVolumes())
		{
			double[] volumeCoords = volume.getCoordinates();
			
			int vertexCount = volumeCoords.length / 3;
			if (vertexCount >= 3)
			{
				meshBuilder.begin(Usage.Position | Usage.Normal | Usage.ColorUnpacked, GL20.GL_TRIANGLES);
				for (int[] face : volume.getFaces())
				{
					int vertex1Index = face[0]*3;
					int vertex2Index = face[1]*3;
					int vertex3Index = face[2]*3;
					vector1.set((float) volumeCoords[vertex1Index],
							(float) volumeCoords[vertex1Index + 1],
							(float) volumeCoords[vertex1Index + 2]);
					vector2.set((float) volumeCoords[vertex2Index],
							(float) volumeCoords[vertex2Index + 1],
							(float) volumeCoords[vertex2Index + 2]);
					vector3.set((float) volumeCoords[vertex3Index],
							(float) volumeCoords[vertex3Index + 1],
							(float) volumeCoords[vertex3Index + 2]);
					
					edge1.set(vector2).sub(vector1);
					edge2.set(vector3).sub(vector1);
					normal.x = edge1.y*edge2.z - edge1.z*edge2.y;
					normal.y = edge1.z*edge2.x - edge1.x*edge2.z;
					normal.z = edge1.x*edge2.y - edge1.y*edge2.x;
					normal.nor();
					
					if (vertexCount > 3)
					{
						// This code ensures that the normal is pointing outwards.
						// Basic idea: 
						// 1) Find any point not in the current face.
						// 2) Check if adding or subtracting the normal takes us away from said point.
						
						int vertex4Index = (vertex1Index + 3) % volumeCoords.length;
						while (vertex4Index == vertex2Index || vertex4Index == vertex3Index)
							vertex4Index = (vertex4Index + 3) % volumeCoords.length;
						
						vector4.set((float) volumeCoords[vertex4Index],
							(float) volumeCoords[vertex4Index + 1],
							(float) volumeCoords[vertex4Index + 2]);
						
						float length1 = calcVector.set(vector1).add(normal).sub(vector4).len2();
						float length2 = calcVector.set(vector1).sub(normal).sub(vector4).len2();
						
						if (length1 < length2)
							normal.scl(-1);
					}
					
					short vertex1 = meshBuilder.vertex(vector1, normal, Color.WHITE, null);
					short vertex2 = meshBuilder.vertex(vector2, normal, Color.WHITE, null);
					short vertex3 = meshBuilder.vertex(vector3, normal, Color.WHITE, null);
					meshBuilder.triangle(vertex1, vertex2, vertex3);
				}
				Mesh mesh = meshBuilder.end();
				
				Renderable renderable = new Renderable();
				renderable.meshPart.set("volume_polygon", mesh, 0, mesh.getNumVertices(), GL20.GL_TRIANGLES);
				renderable.material = getMaterialForColour(volume.getColour()).copy();
				renderable.material.set(new BlendingAttribute(0.5f));
				renderable.environment = environment;
				
				volumeRenderables.add(renderable);
				volumeMeshes.add(mesh);
			}
			else if (volumeCoords.length == 6)
			{
				vector1.set((float) volumeCoords[0],
						(float) volumeCoords[1],
						(float) volumeCoords[2]);
				vector2.set((float) volumeCoords[3],
						(float) volumeCoords[4],
						(float) volumeCoords[5]);
				
				meshBuilder.begin(Usage.Position | Usage.Normal | Usage.ColorUnpacked, GL20.GL_LINES);
				meshBuilder.line(vector1, vector2);
				Mesh mesh = meshBuilder.end();
				
				Renderable renderable = new Renderable();
				renderable.meshPart.set("volume_line", mesh, 0, mesh.getNumVertices(), GL20.GL_LINES);
				renderable.material = getMaterialForColour(volume.getColour()).copy();
				renderable.environment = environment;
				
				volumeRenderables.add(renderable);
				volumeMeshes.add(mesh);
			}
		}
	}


	private void setMaterial(Material material, ModelInstance instance)
	{
		instance.nodes.forEach(node -> node.parts.forEach(part -> part.material = material));
	}


	private Material getMaterialForColour(Color colour)
	{
		Material activeMaterial = null;
		
		if (!materials.containsKey(colour))
		{
			activeMaterial = new Material(ColorAttribute.createDiffuse(colour));
			materials.put(colour, activeMaterial);
		}
		else
		{
			activeMaterial = materials.get(colour);
		}
		
		return activeMaterial;
	}
	
	
	public void update()
	{
		if (showHighlight)
			updateHighlight();
	}
	
	
	private void updateHighlight()
	{
		highlightPoint = getPointNearCrosshair();
		ModelInstance model = pointToModelMap.get(highlightPoint);
		if (model != null)
		{
			hasHighlight = true;
			highlightModel.transform.setTranslation(model.transform.getTranslation(calcVector));
		}
		else
		{
			hasHighlight = false;
		}
	}
	
	
	public Point updateSelection()
	{
		if (showPoints)
		{
			selectedPoint = highlightPoint;
			ModelInstance model = pointToModelMap.get(selectedPoint);
			if (model != null)
			{
				hasSelection = true;
				selectedModel.transform.setTranslation(model.transform.getTranslation(calcVector));
				createPointMetricMesh(selectedPoint);
				return selectedPoint;
			}
			else
			{
				hasSelection = false;
			}
		}

		return null;
	}
	
	
	private Point getPointNearCrosshair()
	{
		Vector3 point1 = camera.position.cpy();
		Vector3 point2 = point1.cpy().add(camera.direction);
		
		Point closest = null;
		float closestDist = Float.MAX_VALUE;
		
		Vector3 calc1 = new Vector3();
		Vector3 calc2 = new Vector3();
		Vector3 calc3 = new Vector3();
		
		for (PointGroup group : backend.getDataGroups())
		{
			for (Point point : group.getPoints())
			{
				calc1.set(point.getCoordinates()).sub(point1);
				calc2.set(point.getCoordinates()).sub(point2);
				calc1.crs(calc2);
				
				calc3.set(point2).sub(point1);
				
				float dist = calc1.len() / calc3.len();
				
				if (dist < closestDist)
				{
					closest = point;
					closestDist = dist;
				}
			}
		}
		
		if (closestDist < 0.2)
		{
			return closest;
		}
		
		return null;
	}
	
	
	public void render(ModelBatch batch, boolean cameraUpdated)
	{
		if (cameraUpdated)
		{
			Arrays.sort(pyramidSides, pyramidSideComparator);
		}
		
		renderPyramid(batch);
		renderAxes(batch);
		renderPoints(batch);
		renderVolumes(batch);
		renderPointMetrics(batch);
	}


	private void renderPyramid(ModelBatch batch)
	{
		if (showPyramidSides)
		{
			for (TetrahedronSide side : pyramidSides)
			{
				batch.render(side.renderable);
			}
		}
		else
		{
			batch.render(pyramidLines);
		}

		batch.render(pyramidCornerModels, environment);
	}


	private void renderAxes(ModelBatch batch)
	{
		if (showAxes || showPointMetrics != POINT_METRIC_HIDE)
			batch.render(axisLines);
	}


	private void renderPoints(ModelBatch batch)
	{
		if (showPoints)
		{
			batch.render(pointModelInstances, environment);

			if (hasSelection)
			{
				batch.render(selectedModel, environment);
			}
			if (showHighlight && hasHighlight && (!hasSelection || selectedPoint != highlightPoint))
			{
				batch.render(highlightModel, environment);
			}
		}
	}


	private void renderVolumes(ModelBatch batch)
	{
		if (showVolumes)
		{
			for (Renderable mesh : volumeRenderables)
				batch.render(mesh);
		}
	}


	private void renderPointMetrics(ModelBatch batch)
	{
		if (showPoints && hasSelection && showPointMetrics != POINT_METRIC_HIDE)
		{
			if (showPointMetrics == POINT_METRIC_FILLED)
				batch.render(pointMetricsArcs);
			batch.render(pointMetricsLines);
		}
	}
	
	
	@Override
	public void dispose()
	{
		for (Disposable disposable : disposables)
		{
			disposable.dispose();
		}
		for (Disposable mesh : volumeMeshes)
		{
			mesh.dispose();
		}
	}
	
	
	private Comparator<TetrahedronSide> pyramidSideComparator = (side1, side2) -> 
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
