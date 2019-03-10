package komposten.tcs;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Filter;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.PixmapIO.PNG;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
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
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.github.quickhull3d.QuickHull3D;

import komposten.utilities.logging.Level;
import komposten.utilities.logging.Logger;
import komposten.utilities.tools.FileOperations;
import komposten.utilities.tools.Geometry;
import komposten.utilities.tools.MathOps;
import komposten.utilities.tools.Regex;


public class TetraColourSpace extends ApplicationAdapter
{
	private enum FollowMode
	{
		SELECTED,
		CENTRE,
		OFF
	}
	
	private enum Shape
	{
		SPHERE(16),
		PYRAMID(17),
		BOX(15);
		
		private int value;

		private Shape(int value)
		{
			this.value = value;
		}
		
		static Shape fromString(String string)
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

	private static final float MAX_ZOOM = 0.025f;
	private static final float SENSITIVITY = -0.2f;
	private static final float SLOW_MODIFIER = .33f;
	private static final int SPHERE_SEGMENTS = 25;
	private static final int LINEAR_VELOCITY = 1;
	private static final int SCROLL_VELOCITY = 5;
	private static final int ANGULAR_VELOCITY = 50;
	private static final int ANGULAR_AUTO_VELOCITY = 20;
	private static final int MAX_DISTANCE = 5;
	private static final int SCREENSHOT_SIZE = 1080;
	private static final int SCREENSHOT_SUPERSAMPLE = 10;

	private static final int POINT_METRIC_HIDE = 0;
	private static final int POINT_METRIC_FILLED = 2;
	private static final int POINT_METRIC_OPTIONS = 3;
	
	private static final int LEGEND_HIDE = 0;
	private static final int LEGEND_GROUPS = 1;
	private static final int LEGEND_POINTS = 2;
	private static final int LEGEND_OPTIONS = 3;
	
	private Logger logger;
	private List<Point> selectionLog;
	
	private File dataFile;
	private File outputPath;
	private PerspectiveCamera camera;
	private OrthographicCamera spriteCamera;
	private ModelBatch batch;
	private Environment environment;
	private SpriteBatch spriteBatch;
	
	private Sprite crosshair;
	private BitmapFont font;
	
	private EnumMap<Shape, Texture> shapeSprites;
	
	private FrameBuffer screenshotBuffer;
	
	private Color colourBackground = new Color(.12f, .12f, .12f, 1f);
	private Color colourCrosshair = null;
	private Color colourCong = Color.RED;
	private Color colourMedium = Color.GREEN;
	private Color colourShort = Color.BLUE;
	private Color colourUv = Color.VIOLET;
	private Color colourAchro = Color.GRAY;
	private Color colourMetricLine = new Color(.89f, .89f, .89f, 1f);
	private Color colourMetricFill = colourMetricLine.cpy().mul(.5f);
	private Color colourHighlight = Color.CORAL;
	private Color colourSelection = Color.DARK_GRAY;
	
	private List<Disposable> disposables;
	private List<Disposable> volumeMeshes;
	private List<PointGroup> dataGroups;
	private List<Volume> dataVolumes;
	private List<ModelInstance> staticModels;
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
	private FollowMode followMode = FollowMode.OFF;
	private int scrollDelta = 0;
	
	private boolean takeScreenshot = false;
	private boolean showPyramidSides = false;
	private boolean showAxes = false;
	private boolean showPoints = true;
	private boolean showVolumes = true;
	private boolean showHighlight = true;
	private int showPointMetrics = 0;
	private boolean showCrosshair = true;
	private int showLegend = 0;
	private boolean hasSelection = false;
	private boolean hasHighlight = false;
	private boolean cameraDirty = true;
	private boolean autoRotate = false;
	private int autoRotation = 1;
	
	public TetraColourSpace(File dataFile, File outputPath)
	{
		if (outputPath == null)
			outputPath = new File("output/");
		else if (outputPath.exists() && !outputPath.isDirectory())
			throw new IllegalArgumentException("outputPath must be a directory: \"" + outputPath.getPath() + "\"");
		
		this.dataFile = dataFile;
		this.outputPath = outputPath;
		this.logger = new Logger("log.txt");
		this.selectionLog = new LinkedList<>();
	}


	@Override
	public void create()
	{
		Gdx.graphics.setTitle("TetraColourSpace - " + dataFile.getName());
		Gdx.input.setInputProcessor(inputProcessor);
		
		disposables = new ArrayList<>();
		volumeMeshes = new ArrayList<>();
		dataGroups = new ArrayList<>();
		dataVolumes = new ArrayList<>();
		staticModels = new ArrayList<>();
		pointModelInstances = new ArrayList<>();
		volumeRenderables = new ArrayList<>();
		materials = new HashMap<>();
		pointToModelMap = new HashMap<>();
		camera = new PerspectiveCamera(67, 1, 1);
		spriteCamera = new OrthographicCamera();
		batch = new ModelBatch();
		spriteBatch = new SpriteBatch();
		font = new BitmapFont();
		font.getData().markupEnabled = true;
		
		shapeSprites = new EnumMap<>(Shape.class);
		shapeSprites.put(Shape.SPHERE, createLinearTexture("circle.png"));
		shapeSprites.put(Shape.PYRAMID, createLinearTexture("triangle.png"));
		shapeSprites.put(Shape.BOX, createLinearTexture("square.png"));
		
		int distance = 1;
		camera.translate(distance, distance, -0.3f*distance);
		camera.near = 0.01f;
		camera.far = 300;
		lookAt(Vector3.Zero);
		
		DirectionalLight light = new DirectionalLight();
		light.setDirection(-1f, -1f, 0);
		light.setColor(Color.WHITE);
		environment = new Environment();
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.6f, 0.6f, 0.6f, 1.0f));
		environment.add(light);
		
		pointMetricsLines = new Renderable();
		pointMetricsArcs = new Renderable();
		pointMetricsLines.material = getMaterialForColour(colourMetricLine);
		pointMetricsArcs.material = getMaterialForColour(colourMetricFill);
		pointMetricsArcs.environment = environment;
		
		createScreenshotBuffer();
		loadData();
		updateViewport();
	}


	private Texture createLinearTexture(String path)
	{
		Texture texture = new Texture(Gdx.files.internal(path));
		texture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
		return texture;
	}


	private void createCrosshair()
	{
		Texture crosshairTexture = new Texture(Gdx.files.internal("crosshair.png"));
		crosshair = new Sprite(crosshairTexture);
		
		if (colourCrosshair == null)
		{
			colourCrosshair = Color.WHITE.cpy().sub(colourBackground);
			colourCrosshair.a = 1f;
		}
		
		crosshair.setColor(colourCrosshair);
		crosshair.getColor().a = 1f;
		disposables.add(crosshairTexture);
	}


	private void createScreenshotBuffer()
	{
		if (screenshotBuffer != null)
			screenshotBuffer.dispose();
		
		float ratio = Gdx.graphics.getHeight() / (float)Gdx.graphics.getWidth();
		int width = SCREENSHOT_SIZE*SCREENSHOT_SUPERSAMPLE;
		int height = (int)(width * ratio);
		screenshotBuffer = new FrameBuffer(Format.RGBA8888, width, height, true);
	}


	private void createStaticModels()
	{
		Tetrahedron tetrahedron = new Tetrahedron(1f);

		ModelBuilder modelBuilder = new ModelBuilder();
		createPyramidCorners(tetrahedron, modelBuilder);

		Model model = createSphere(modelBuilder, 0.025f, GL20.GL_LINES, 10);
		selectedModel = createModelInstance(model, Vector3.Zero, colourSelection);
		highlightModel = createModelInstance(model, Vector3.Zero, colourHighlight);
		
		MeshBuilder meshBuilder = new MeshBuilder();
		createTCSPyramid(meshBuilder);
		createAxisLines(meshBuilder);
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
		
		Color longColourActive = (applyColours ? this.colourCong : Color.WHITE);
		Color mediumColourActive = (applyColours ? this.colourMedium : Color.WHITE);
		Color shortColourActive = (applyColours ? this.colourShort : Color.WHITE);
		Color uvColourActive = (applyColours ? this.colourUv : Color.WHITE);

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
		
		ModelInstance redSphere = createModelInstance(sphereModel, tetrahedron.longPos, colourCong);
		ModelInstance greenSphere = createModelInstance(sphereModel, tetrahedron.mediumPos, colourMedium);
		ModelInstance blueSphere = createModelInstance(sphereModel, tetrahedron.shortPos, colourShort);
		ModelInstance uvSphere = createModelInstance(sphereModel, tetrahedron.uvPos, colourUv);
		ModelInstance achroSphere = createModelInstance(sphereModel, tetrahedron.achroPos, colourAchro);
		
		staticModels.add(redSphere);
		staticModels.add(greenSphere);
		staticModels.add(blueSphere);
		staticModels.add(uvSphere);
		staticModels.add(achroSphere);

		disposables.add(sphereModel);
	}


	private void createAxisLines(MeshBuilder meshBuilder)
	{
		meshBuilder.begin(Usage.Position | Usage.Normal | Usage.ColorUnpacked, GL20.GL_LINES);
		
		float length = 0.2f;
		Vector3 start = new Vector3();
		Vector3 end = new Vector3();
		meshBuilder.line(start.set(-length, 0, 0), colourShort, end.set(length, 0, 0), colourCong);
		meshBuilder.line(start.set(0, -length, 0), Color.WHITE, end.set(0, length, 0), colourUv);
		meshBuilder.line(start.set(0, 0, -length), colourMedium, end.set(0, 0, length), colourCong.cpy().lerp(colourShort, 0.5f));
		
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
		Vector3 line = point.coordinates;
		Vector3 lineInZX = new Vector3(line.x, 0, line.z);
		
		float arcRadius = lineInZX.len()*0.8f;
		int segments = 200;
		float segmentsPerRadian = segments / MathUtils.PI2;
		float theta = point.metrics.x;
		float phi = point.metrics.y;
		
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


	private void loadData()
	{
		try
		{
			Material activeMaterial = getMaterialForColour(Color.BLACK);
			
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			docBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			Document document = docBuilder.parse(dataFile);
			Element root = document.getDocumentElement();
			
			loadConfig(root);
			createStaticModels();
			createCrosshair();
			
			NodeList groups = root.getElementsByTagName("group");
			for (int g = 0; g < groups.getLength(); g++)
			{
				Element group = (Element) groups.item(g);
				
				Node groupNameAttr = group.getAttributeNode("name");
				Node groupShapeAttr = group.getAttributeNode("shape");
				
				String groupName = (groupNameAttr != null ? groupNameAttr.getNodeValue().trim() : Integer.toString(g));
				Shape shape = Shape.SPHERE;
				if (groupShapeAttr != null)
				{
					Shape attrShape = Shape.fromString(groupShapeAttr.getNodeValue().trim());
					shape = (attrShape != null ? attrShape : shape);
				}
				
				List<Point> pointList = new LinkedList<>();
				
				NodeList points = group.getElementsByTagName("point");
				for (int i = 0; i < points.getLength(); i++)
				{
					Node pointNode = points.item(i);
					
					NamedNodeMap attributes = pointNode.getAttributes();
					Node colourAttr = attributes.getNamedItem("colour");
					Node nameAttr = attributes.getNamedItem("name");
					Node positionAttr = attributes.getNamedItem("position");
					String colourHex = "";
					
					String name = "Point " + (i+1);
					if (colourAttr != null)
					{
						colourHex = colourAttr.getNodeValue().trim();
						Color colour = getColourFromHex(colourHex);
						activeMaterial = getMaterialForColour(colour);
					}
					
					if (nameAttr != null)
					{
						name = nameAttr.getNodeValue().trim();
					}
					
					String position = positionAttr.getNodeValue();
					Vector3 metrics = getColourSpaceMetricsFromLine(position);
					Vector3 coords = createVectorFromAngles(metrics.x, metrics.y, metrics.z);
					Point point = new Point(name, coords, metrics, activeMaterial, colourHex);
					pointList.add(point);
				}
				
				dataGroups.add(new PointGroup(groupName, pointList, shape));
			}
			
			NodeList volumes = root.getElementsByTagName("volume");
			for (int i = 0; i < volumes.getLength(); i++)
			{
				Node volumesNode = volumes.item(i);
				
				NamedNodeMap attributes = volumesNode.getAttributes();
				Node colourAttr = attributes.getNamedItem("colour");
				
				if (colourAttr != null)
				{
					String colourHex = colourAttr.getNodeValue().trim();
					Color colour = getColourFromHex(colourHex);
					activeMaterial = getMaterialForColour(colour);
				}

				String[] pointStrings = volumesNode.getFirstChild().getTextContent().trim().split("[\n\r]+");
				List<String> list = new LinkedList<>(Arrays.asList(pointStrings));
				for(Iterator<String> itr = list.iterator(); itr.hasNext();)
				{
					if (itr.next().trim().isEmpty())
						itr.remove();
				}
				pointStrings = list.toArray(new String[list.size()]);
				
				int[][] faces = null;
				double[] coords = new double[pointStrings.length*3];
				for (int j = 0; j < pointStrings.length; j++)
				{
					String pointString = pointStrings[j].trim();
					Vector3 vector = getCoordinatesFromLine(pointString);
					coords[j*3+0] = vector.x;
					coords[j*3+1] = vector.y;
					coords[j*3+2] = vector.z;
				}
				
				if (coords.length >= 12)
				{
					QuickHull3D quickHull = new QuickHull3D();
					quickHull.build(coords);
					
					coords = new double[quickHull.getNumVertices()*3];
					quickHull.getVertices(coords);
					
					faces = quickHull.getFaces();
				}
				else if (coords.length == 9)
				{
					faces = new int[1][];
					faces[0] = new int[] { 0, 1, 2 };
				}
				else
				{
					faces = new int[0][];
				}
				
				Volume volume = new Volume(coords, faces, activeMaterial);
				dataVolumes.add(volume);
			}
		}
		catch (IOException e)
		{
			String msg = "Error reading data file: " + dataFile.getPath();
			logger.log(Level.FATAL, getClass().getSimpleName(), msg, e, false);
		}
		catch (ParserConfigurationException e)
		{
			String msg = "Error creating the XML parser!";
			logger.log(Level.FATAL, getClass().getSimpleName(), msg, e, false);
		}
		catch (SAXException e)
		{
			String msg = "Error parsing data file: " + dataFile.getPath();
			logger.log(Level.FATAL, getClass().getSimpleName(), msg, e, false);
		}
		
		ModelBuilder builder = new ModelBuilder();
		float diameter = 0.02f;
		Model modelSphere = createSphere(builder, diameter, GL20.GL_TRIANGLES);
		Model modelBox = createBox(builder, diameter, GL20.GL_TRIANGLES);
		Model modelPyramid = createPyramid(builder, diameter, GL20.GL_TRIANGLES);
		disposables.add(modelSphere);
		disposables.add(modelBox);
		disposables.add(modelPyramid);
		
		for (PointGroup group : dataGroups)
		{
			Model model;
			
			switch (group.shape)
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
			
			for (Point point : group.points)
			{
				
				ModelInstance instance = new ModelInstance(model);
				instance.transform.translate(point.coordinates);
				setMaterial(point.material, instance);
				pointModelInstances.add(instance);
				pointToModelMap.put(point, instance);
			}
		}
		
		MeshBuilder meshBuilder = new MeshBuilder();
		Vector3 vector1 = new Vector3();
		Vector3 vector2 = new Vector3();
		Vector3 vector3 = new Vector3();
		Vector3 vector4 = new Vector3();
		Vector3 edge1 = new Vector3();
		Vector3 edge2 = new Vector3();
		Vector3 normal = new Vector3();
		
		for (Volume volume : dataVolumes)
		{
			int vertexCount = volume.coordinates.length / 3;
			if (vertexCount >= 3)
			{
				meshBuilder.begin(Usage.Position | Usage.Normal | Usage.ColorUnpacked, GL20.GL_TRIANGLES);
				for (int[] face : volume.faces)
				{
					int vertex1Index = face[0]*3;
					int vertex2Index = face[1]*3;
					int vertex3Index = face[2]*3;
					vector1.set((float) volume.coordinates[vertex1Index],
							(float) volume.coordinates[vertex1Index + 1],
							(float) volume.coordinates[vertex1Index + 2]);
					vector2.set((float) volume.coordinates[vertex2Index],
							(float) volume.coordinates[vertex2Index + 1],
							(float) volume.coordinates[vertex2Index + 2]);
					vector3.set((float) volume.coordinates[vertex3Index],
							(float) volume.coordinates[vertex3Index + 1],
							(float) volume.coordinates[vertex3Index + 2]);
					
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
						
						int vertex4Index = (vertex1Index + 3) % volume.coordinates.length;
						while (vertex4Index == vertex2Index || vertex4Index == vertex3Index)
							vertex4Index = (vertex4Index + 3) % volume.coordinates.length;
						
						vector4.set((float) volume.coordinates[vertex4Index],
							(float) volume.coordinates[vertex4Index + 1],
							(float) volume.coordinates[vertex4Index + 2]);
						
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
				renderable.material = volume.material.copy();
				renderable.material.set(new BlendingAttribute(0.5f));
				renderable.environment = environment;
				
				volumeRenderables.add(renderable);
				volumeMeshes.add(mesh);
			}
			else if (volume.coordinates.length == 6)
			{
				vector1.set((float) volume.coordinates[0],
						(float) volume.coordinates[1],
						(float) volume.coordinates[2]);
				vector2.set((float) volume.coordinates[3],
						(float) volume.coordinates[4],
						(float) volume.coordinates[5]);
				
				meshBuilder.begin(Usage.Position | Usage.Normal | Usage.ColorUnpacked, GL20.GL_LINES);
				meshBuilder.line(vector1, vector2);
				Mesh mesh = meshBuilder.end();
				
				Renderable renderable = new Renderable();
				renderable.meshPart.set("volume_line", mesh, 0, mesh.getNumVertices(), GL20.GL_LINES);
				renderable.material = volume.material.copy();
				renderable.environment = environment;
				
				volumeRenderables.add(renderable);
				volumeMeshes.add(mesh);
			}
		}
	}
	
	
	private void loadConfig(Element root)
	{
		NodeList styleNodes = root.getElementsByTagName("style");
		
		for (int i = 0; i < styleNodes.getLength(); i++)
		{
			Node styleNode = styleNodes.item(i);
			
			NodeList childNodes = styleNode.getChildNodes();
			
			for (int j = 0; j < childNodes.getLength(); j++)
			{
				Node child = childNodes.item(j);

				String name = child.getNodeName();
				String value = child.getTextContent();

				if (name.equalsIgnoreCase("background"))
					colourBackground = getColourFromHex(value);
				else if (name.equalsIgnoreCase("colour_long"))
					colourCong = getColourFromHex(value);
				else if (name.equalsIgnoreCase("colour_medium"))
					colourMedium = getColourFromHex(value);
				else if (name.equalsIgnoreCase("colour_short"))
					colourShort = getColourFromHex(value);
				else if (name.equalsIgnoreCase("colour_uv"))
					colourUv = getColourFromHex(value);
				else if (name.equalsIgnoreCase("colour_achro"))
					colourAchro = getColourFromHex(value);
				else if (name.equalsIgnoreCase("colour_arc_lines"))
					colourMetricLine = getColourFromHex(value);
				else if (name.equalsIgnoreCase("colour_arc_fill"))
					colourMetricFill = getColourFromHex(value);
				else if (name.equalsIgnoreCase("colour_highlight"))
					colourHighlight = getColourFromHex(value);
				else if (name.equalsIgnoreCase("colour_selection"))
					colourSelection = getColourFromHex(value);
			}
		}
	}


	private Vector3 getCoordinatesFromLine(String line)
	{
		Vector3 metrics = getColourSpaceMetricsFromLine(line);

		return createVectorFromAngles(metrics.x, metrics.y, metrics.z);
	}
	
	
	private Vector3 getColourSpaceMetricsFromLine(String line)
	{
		String[] values = Regex.getMatches("-?\\d+(\\.\\d+)?", line);
		float[] floats = new float[values.length];
		
		for (int i = 0; i < floats.length; i++)
			floats[i] = Float.parseFloat(values[i]);

		float theta = floats[0];
		float phi = floats[1];
		float magnitude = floats[2];
		
		return new Vector3(theta, phi, magnitude);
	}


	private static Vector3 createVectorFromAngles(float theta, float phi,
			float magnitude)
	{
		Vector3 coords = new Vector3(1, 0, 0);
		coords.rotateRad(Vector3.Y, theta);
		coords.rotateRad(new Vector3(-coords.z, 0, coords.x), phi);
		coords.setLength(magnitude);
		return coords;
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


	private Color getColourFromHex(String hexColour)
	{
		if (hexColour.startsWith("#")) hexColour = hexColour.substring(1);
		
		String r;
		String g;
		String b;
		String a = "FF";
		
		if (hexColour.length() == 3 || hexColour.length() == 4)
		{
			String twoCharFormat = "%1$s%1$s";
			r = String.format(twoCharFormat, hexColour.charAt(0));
			g = String.format(twoCharFormat, hexColour.charAt(1));
			b = String.format(twoCharFormat, hexColour.charAt(2));
			
			if (hexColour.length() == 4)
				a = String.format(twoCharFormat, hexColour.charAt(3));
		}
		else if (hexColour.length() == 6 || hexColour.length() == 8)
		{
			r = hexColour.substring(0, 2);
			g = hexColour.substring(2, 4);
			b = hexColour.substring(4, 6);
			
			if (hexColour.length() == 8)
				a = hexColour.substring(6, 8);
		}
		else
		{
			throw new IllegalArgumentException(hexColour + " is not a valid hex colour!");
		}

		return new Color(
				getColourComponentFloat(r), getColourComponentFloat(g),
				getColourComponentFloat(b), getColourComponentFloat(a));
	}


	private float getColourComponentFloat(String hex)
	{
		return Integer.parseInt(hex, 16) / 255f;
	}


	@Override
	public void render()
	{
		if (cameraDirty)
		{
			cameraDirty = false;
			camera.update();
			Arrays.sort(pyramidSides, pyramidSideComparator);
		}
		
		if (takeScreenshot)
		{
			Gdx.gl.glLineWidth(SCREENSHOT_SUPERSAMPLE);
			screenshotBuffer.begin();
		}
		
		Gdx.gl.glClearColor(colourBackground.r, colourBackground.g, colourBackground.b, colourBackground.a);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		batch.begin(camera);
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
		batch.render(staticModels, environment);
		if (showAxes || showPointMetrics != POINT_METRIC_HIDE)
			batch.render(axisLines);
		if (showPoints)
		{
			batch.render(pointModelInstances, environment);
			
			if (hasSelection)
			{
				batch.render(selectedModel, environment);
				if (showPointMetrics != POINT_METRIC_HIDE)
				{
					if (showPointMetrics == POINT_METRIC_FILLED)
						batch.render(pointMetricsArcs);
					batch.render(pointMetricsLines);
				}
			}
			if (showHighlight && hasHighlight && (!hasSelection || selectedPoint != highlightPoint))
			{
					batch.render(highlightModel, environment);
			}
		}
		if (showVolumes)
		{
			for (Renderable mesh : volumeRenderables)
				batch.render(mesh);
		}
		batch.end();
		
		if (takeScreenshot)
		{
			saveBufferToFile();
			screenshotBuffer.end();
			takeScreenshot = false;
			Gdx.gl.glLineWidth(1);
		}
		
		spriteBatch.begin();
		if (showCrosshair)
		{
			crosshair.draw(spriteBatch);
		}
		if (hasSelection)
		{
			String metrics = String.format(
					"%s%n  Theta: %.02f%n  Phi: %.02f%n  r: %.02f",
					selectedPoint.name,
					selectedPoint.metrics.x,
					selectedPoint.metrics.y,
					selectedPoint.metrics.z);
			
			font.draw(spriteBatch, metrics, 5, Gdx.graphics.getHeight()-10f);
		}
		renderLegend();
		spriteBatch.end();
		
		readInput(Gdx.graphics.getDeltaTime());
		
		if (showHighlight)
			updateHighlight();
		
		if (followMode != FollowMode.OFF)
		{
			updateFollow();
		}
	}


	private void renderLegend()
	{
		if (showLegend != LEGEND_HIDE)
		{
			float shapeSize = 12;
			float padding = 5;
			float lineHeight = Math.max(font.getLineHeight(), shapeSize);
			float x = padding;
			float xText = x + shapeSize + 5;
			float y = Gdx.graphics.getHeight() - (padding + lineHeight/2);
			
			for (PointGroup group : dataGroups)
			{
				Texture shapeTexture = shapeSprites.get(group.shape);
				
				if (showLegend == LEGEND_POINTS)
				{
					for (Point point : group.points)
					{
						spriteBatch.setColor(getColourFromHex(point.colour));
						spriteBatch.draw(shapeTexture, x, y - shapeSize/2 , shapeSize, shapeSize);
						spriteBatch.setColor(Color.WHITE);
						String line = String.format("[%s]%s", point.colour, point.name);
						font.draw(spriteBatch, line, xText, y + font.getCapHeight()/2, 0, Align.left, false);
						y -= lineHeight;
					}
				}
				else if (showLegend == LEGEND_GROUPS)
				{
					spriteBatch.setColor(getColourFromHex(group.points.get(0).colour));
					spriteBatch.draw(shapeTexture, x, y - shapeSize/2 , shapeSize, shapeSize);
					spriteBatch.setColor(Color.WHITE);
					String line = String.format("[%s]%s", group.points.get(0).colour, group.name);
					font.draw(spriteBatch, line, xText, y + font.getCapHeight()/2, 0, Align.left, false);
					y -= lineHeight;
				}
			}
		}
	}


	private void saveBufferToFile()
	{
		int width = screenshotBuffer.getWidth();
		int height = screenshotBuffer.getHeight();
		byte[] pixels = ScreenUtils.getFrameBufferPixels(0, 0, width, height, false);
		
		Pixmap pixmapSS = new Pixmap(width, height, Format.RGBA8888);
		BufferUtils.copy(pixels, 0, pixmapSS.getPixels(), pixels.length);
		
		Pixmap pixmap = new Pixmap(SCREENSHOT_SIZE, height / SCREENSHOT_SUPERSAMPLE, Format.RGBA8888);
		pixmap.setFilter(Filter.BiLinear);
		pixmapSS.setFilter(Filter.BiLinear);
		pixmap.drawPixmap(pixmapSS, 0, 0, width, height, 0, 0, pixmap.getWidth(), pixmap.getHeight());
		
		DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG);
		String dateString = dateFormat.format(new Date()).replaceAll("[^\\s0-9]+", "").trim().replace(' ', '_');
		String fileName = String.format("%s_%s.png", FileOperations.getNameWithoutExtension(dataFile), dateString);
		FileHandle file = new FileHandle(new File(outputPath, fileName));
		PNG png = new PNG((int)(width*height*1.5f));
		
		try
		{
			png.write(file, pixmap);
		}
		catch (IOException e)
		{
			String msg = "Error saving screenshot to " + file.path();
			logger.log(Level.ERROR, getClass().getSimpleName(), msg, e, false);
		}
		
		pixmap.dispose();
	}


	private void updateFollow()
	{
		switch (followMode)
		{
			case CENTRE :
				lookAt(Vector3.Zero);
				cameraDirty = true;
				break;
			case SELECTED :
				lookAt(selectedPoint.coordinates);
				cameraDirty = true;
				break;
			case OFF :
			default :
				break;
		}
	}


	private void readInput(float deltaTime)
	{
		if (readCameraInput(deltaTime))
			cameraDirty = true;
	}


	private Vector3 calcVector = new Vector3();
	private boolean readCameraInput(float deltaTime)
	{
		if (followMode != FollowMode.OFF)
			return rotationMovement(deltaTime);
		else 
			return translationMovement(deltaTime);
	}


	private boolean translationMovement(float deltaTime)
	{
		Vector3 movement = new Vector3();
		
		if (Gdx.input.isKeyPressed(Keys.W) || Gdx.input.isKeyPressed(Keys.S))
		{
			calcVector.set(camera.direction.x, 0, camera.direction.z).setLength(LINEAR_VELOCITY * deltaTime);
			
			if (Gdx.input.isKeyPressed(Keys.W))
				movement.add(calcVector);
			else
				movement.sub(calcVector);
		}
		
		if (Gdx.input.isKeyPressed(Keys.A) || Gdx.input.isKeyPressed(Keys.D))
		{
			calcVector.set(camera.direction.z, 0, -camera.direction.x).setLength(LINEAR_VELOCITY * deltaTime);
			
			if (Gdx.input.isKeyPressed(Keys.A))
				movement.add(calcVector);
			else
				movement.sub(calcVector);
		}
		
		if (Gdx.input.isKeyPressed(Keys.E) || Gdx.input.isKeyPressed(Keys.Q))
		{
			calcVector.set(camera.direction).setLength(LINEAR_VELOCITY * deltaTime);
			
			if (Gdx.input.isKeyPressed(Keys.E))
				movement.add(calcVector);
			else
				movement.sub(calcVector);
		}
		
		if (Gdx.input.isKeyPressed(Keys.SPACE))
		{
			movement.y += LINEAR_VELOCITY * deltaTime;
		}
		else if (Gdx.input.isKeyPressed(Keys.CONTROL_LEFT))
		{
			movement.y -= LINEAR_VELOCITY * deltaTime;
		}
		
		boolean needsUpdate = false;
		if (!movement.epsilonEquals(Vector3.Zero))
		{
			if (Gdx.input.isKeyPressed(Keys.SHIFT_LEFT))
				movement.scl(SLOW_MODIFIER);
			
			camera.position.x = MathOps.clamp(-MAX_DISTANCE, MAX_DISTANCE, camera.position.x + movement.x);
			camera.position.y = MathOps.clamp(-MAX_DISTANCE, MAX_DISTANCE, camera.position.y + movement.y);
			camera.position.z = MathOps.clamp(-MAX_DISTANCE, MAX_DISTANCE, camera.position.z + movement.z);
			needsUpdate = true;
		}
		
		if (Gdx.input.isButtonPressed(Buttons.RIGHT))
		{
			int mouseDX = Gdx.input.getDeltaX();
			int mouseDY = Gdx.input.getDeltaY();
			
			if (mouseDX != 0 || mouseDY != 0)
			{
				float sensitivity = SENSITIVITY;
				float rotationX = mouseDX * sensitivity;
				float rotationY = -mouseDY * sensitivity;
				
				camera.rotate(Vector3.Y, rotationX);
				
				calcVector.set(camera.direction.x, 0, camera.direction.z);
				
				rotationY = clampYRotation(rotationY);
				
				calcVector.set(camera.direction.z, 0, -camera.direction.x);
				camera.rotate(calcVector, rotationY);
				
				needsUpdate = true;
			}
		}
		
		return needsUpdate;
	}


	private boolean rotationMovement(float deltaTime)
	{
		Vector3 movement = new Vector3();
		Vector3 focalPoint = (followMode == FollowMode.CENTRE ? Vector3.Zero : selectedPoint.coordinates);
		
		float rotX = 0;
		float rotY = 0;
		float zoom = 0;

		if (Gdx.input.isKeyPressed(Keys.W) || Gdx.input.isKeyPressed(Keys.E))
			zoom += LINEAR_VELOCITY * deltaTime;
		if (Gdx.input.isKeyPressed(Keys.S) || Gdx.input.isKeyPressed(Keys.Q))
			zoom -= LINEAR_VELOCITY * deltaTime;
		if (Gdx.input.isKeyPressed(Keys.A))
			rotX -= ANGULAR_VELOCITY * deltaTime;
		if (Gdx.input.isKeyPressed(Keys.D))
			rotX += ANGULAR_VELOCITY * deltaTime;
		if (Gdx.input.isKeyPressed(Keys.SPACE))
			rotY -= ANGULAR_VELOCITY * deltaTime;
		if (Gdx.input.isKeyPressed(Keys.CONTROL_LEFT))
			rotY += ANGULAR_VELOCITY * deltaTime;
		
		if (scrollDelta != 0)
		{
			zoom -= scrollDelta * SCROLL_VELOCITY * deltaTime;
			scrollDelta = 0;
		}
		
		if (autoRotate)
		{
			rotX += ANGULAR_AUTO_VELOCITY * deltaTime * autoRotation;
		}
		
		if (Gdx.input.isButtonPressed(Buttons.RIGHT))
		{
			int mouseDX = Gdx.input.getDeltaX();
			int mouseDY = Gdx.input.getDeltaY();
			
			if (mouseDX != 0)
			{
				rotX += mouseDX * SENSITIVITY;
			}
			if (mouseDY != 0)
			{
				rotY += mouseDY * SENSITIVITY;
			}
		}
		
		if (!MathOps.equals(zoom, 0, 0.0001f))
		{
			float distanceToPoint = calcVector.set(camera.position).sub(focalPoint).len();
			float velocity = zoom;
			if (distanceToPoint - velocity < MAX_ZOOM)
				velocity = distanceToPoint - MAX_ZOOM;
				
			calcVector.set(camera.direction).setLength(velocity);
			if (velocity < 0)
				calcVector.scl(-1);
			
			movement.add(calcVector);
		}
		
		if (!MathOps.equals(rotX, 0, 0.0001f))
		{
			Vector3 vectorFromCentre = camera.position.cpy().sub(focalPoint);
			vectorFromCentre.rotate(Vector3.Y, rotX);
			vectorFromCentre.add(focalPoint);

			movement.add(vectorFromCentre.sub(camera.position));
		}
		
		if (!MathOps.equals(rotY, 0, 0.0001f))
		{
			Vector3 vectorFromCentre = camera.position.cpy().sub(focalPoint);
			
			float rotationY = -clampYRotation(-rotY);

			calcVector.set(vectorFromCentre.z, 0, -vectorFromCentre.x);
			vectorFromCentre.rotate(calcVector, rotationY);
			vectorFromCentre.add(focalPoint);

			movement.add(vectorFromCentre.sub(camera.position));
		}
		
		boolean needsUpdate = false;
		if (!movement.epsilonEquals(Vector3.Zero))
		{
			camera.position.x = MathOps.clamp(-MAX_DISTANCE, MAX_DISTANCE, camera.position.x + movement.x);
			camera.position.y = MathOps.clamp(-MAX_DISTANCE, MAX_DISTANCE, camera.position.y + movement.y);
			camera.position.z = MathOps.clamp(-MAX_DISTANCE, MAX_DISTANCE, camera.position.z + movement.z);
			needsUpdate = true;
		}
		
		return needsUpdate;
	}


	private float clampYRotation(float rotationY)
	{
		calcVector.set(camera.direction.x, 0, camera.direction.z);
		double currentAngle = Math.toDegrees(Math.atan2(calcVector.y - camera.direction.y, calcVector.len()));
		double maxAngle = 89;
		
		if (currentAngle + rotationY > maxAngle)
			rotationY = (float) (maxAngle - currentAngle);
		else if (currentAngle + rotationY < -maxAngle)
			rotationY = (float) -(maxAngle + currentAngle);
		return rotationY;
	}


	private void lookAt(Vector3 target)
	{
		camera.lookAt(target);
		camera.up.set(Vector3.Y); //Resetting the up vector since camera.lookAt() changes it.
	}
	
	
	private void updateViewport()
	{
		float ratio = Gdx.graphics.getHeight() / (float)Gdx.graphics.getWidth();
		camera.viewportHeight = ratio;
		cameraDirty = true;
		
		spriteCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		spriteBatch.setProjectionMatrix(spriteCamera.combined);
		
		if (crosshair != null)
			crosshair.setCenter(Gdx.graphics.getWidth()/2f, Gdx.graphics.getHeight()/2f);
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
	
	
	private Point getPointNearCrosshair()
	{
		Vector3 point1 = camera.position.cpy();
		Vector3 point2 = point1.cpy().add(camera.direction);
		
		Point closest = null;
		float closestDist = Float.MAX_VALUE;
		
		Vector3 calc1 = new Vector3();
		Vector3 calc2 = new Vector3();
		Vector3 calc3 = new Vector3();
		
		for (PointGroup group : dataGroups)
		{
			for (Point point : group.points)
			{
				calc1.set(point.coordinates).sub(point1);
				calc2.set(point.coordinates).sub(point2);
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
	
	
	@Override
	public void resize(int width, int height)
	{
		updateViewport();
	}


	@Override
	public void dispose()
	{
		screenshotBuffer.dispose();
		batch.dispose();
		disposeObjects();
		
		for (Texture texture : shapeSprites.values())
		{
			texture.dispose();
		}
		
		if (!selectionLog.isEmpty())
		{
			StringBuilder builder = new StringBuilder();
			builder.append("Point selections:");
			for (Point point : selectionLog)
				builder.append('\n').append(point);
			
			logger.log(Level.INFO, builder.toString());
		}
	}


	private void disposeObjects()
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
	
	
	private InputProcessor inputProcessor = new InputAdapter()
	{
		boolean nonRPressed = false;
		
		@Override
		public boolean keyDown(int keycode)
		{
			if (keycode == Keys.HOME)
			{
				followMode = FollowMode.CENTRE;
				return true;
			}
			else if (keycode == Keys.R)
			{
				nonRPressed = false;
			}
			
			return false;
		}
		
		
		@Override
		public boolean keyUp(int keycode)
		{
			if (keycode == Keys.HOME && followMode == FollowMode.CENTRE)
			{
				followMode = FollowMode.OFF;
				return true;
			}
			else if (keycode == Keys.G)
			{
				if (followMode != FollowMode.CENTRE)
					followMode = FollowMode.CENTRE;
				else
					followMode = FollowMode.OFF;
				return true;
			}
			else if (keycode == Keys.C)
			{
				showCrosshair = !showCrosshair;
				return true;
			}
			else if (keycode == Keys.L)
			{
				showLegend = (showLegend + 1) % LEGEND_OPTIONS;
			}
			else if (keycode == Keys.T)
			{
				showPyramidSides = !showPyramidSides;
				return true;
			}
			else if (keycode == Keys.Y)
			{
				showAxes = !showAxes;
				return true;
			}
			else if (keycode == Keys.H)
			{
				showHighlight = !showHighlight;
				return true;
			}
			else if (keycode == Keys.M)
			{
				showPointMetrics = (showPointMetrics + 1) % POINT_METRIC_OPTIONS;
				return true;
			}
			else if (keycode == Keys.NUM_1)
			{
				showPoints = !showPoints;
				return true;
			}
			else if (keycode == Keys.NUM_2)
			{
				showVolumes = !showVolumes;
				return true;
			}
			else if (keycode == Keys.F && hasSelection)
			{
				if (followMode != FollowMode.SELECTED)
					followMode = FollowMode.SELECTED;
				else
					followMode = FollowMode.OFF;
				return true;
			}
			else if (keycode == Keys.F12)
			{
				takeScreenshot = true;
			}
			else if (keycode == Keys.R)
			{
				if (!nonRPressed)
					autoRotate = !autoRotate;
			}
			else if (Gdx.input.isKeyPressed(Keys.R))
			{
				if (keycode >= Keys.NUMPAD_1 && keycode <= Keys.NUMPAD_9)
				{
					int sign = (autoRotation > 0 ? 1 : -1);
					autoRotation = sign * (keycode - Keys.NUMPAD_0);
					nonRPressed = true;
				}
				else if (keycode == Keys.STAR)
				{
					autoRotation = -autoRotation;
					nonRPressed = true;
				}
				else if (keycode == Keys.MINUS)
				{
					if (autoRotation < -1)
						autoRotation += 1;
					else if (autoRotation > 1)
						autoRotation -= 1;
					
					nonRPressed = true;
				}
				else if (keycode == Keys.PLUS)
				{
					autoRotation += (autoRotation < 0 ? -1 : 1);
					nonRPressed = true;
				}
				
				return true;
			}
			else if (keycode == Keys.NUMPAD_1)
			{
				camera.position.set(1, 1, -0.3f);
				lookAt(Vector3.Zero);
				cameraDirty = true;
			}
			else if (keycode == Keys.NUMPAD_2)
			{
				camera.position.set(0, -1.4f, 0.0001f);
				lookAt(Vector3.Zero);
				cameraDirty = true;
			}
			else if (keycode == Keys.NUMPAD_3)
			{
				camera.position.set(0, 1f, 0.0001f);
				lookAt(Vector3.Zero);
				cameraDirty = true;
			}
			
			return false;
		}
		
		
		@Override
		public boolean touchDown(int screenX, int screenY, int pointer, int button)
		{
			if (button == Buttons.RIGHT)
			{
				Gdx.input.setCursorCatched(true);
				return true;
			}
			else if (button == Buttons.LEFT && showPoints)
			{
				selectedPoint = getPointNearCrosshair();
				ModelInstance model = pointToModelMap.get(selectedPoint);
				if (model != null)
				{
					hasSelection = true;
					selectedModel.transform.setTranslation(model.transform.getTranslation(calcVector));
					createPointMetricMesh(selectedPoint);
					selectionLog.add(selectedPoint);
				}
				else
				{
					hasSelection = false;
				}
			}
			
			return false;
		}
		
		
		@Override
		public boolean touchUp(int screenX, int screenY, int pointer, int button)
		{
			if (button == Buttons.RIGHT)
			{
				Gdx.input.setCursorCatched(false);
				return true;
			}
			
			return false;
		}
		
		
		@Override
		public boolean scrolled(int amount)
		{
			if (Gdx.input.isButtonPressed(Buttons.RIGHT) && followMode != FollowMode.OFF)
			{
				scrollDelta += amount;
				return true;
			}

			return false;
		}
	};
	
	
	private Comparator<TetrahedronSide> pyramidSideComparator = (side1, side2) -> 
	{
			float length1 = calcVector.set(camera.position).sub(side1.centre).len2();
			float length2 = calcVector.set(camera.position).sub(side2.centre).len2();
			return -Float.compare(length1, length2);
	};
	
	
	private static class PointGroup
	{
		String name;
		List<Point> points;
		Shape shape;
		
		public PointGroup(String name, List<Point> points, Shape shape)
		{
			this.name = name;
			this.points = points;
			this.shape = shape;
		}
	}

	
	private static class Point
	{
		String name;
		Vector3 coordinates;
		Vector3 metrics;
		Material material;
		String colour;		
		
		public Point(String name, Vector3 coordinates, Vector3 metrics, Material material, String colourHex)
		{
			this.name = name;
			this.coordinates = coordinates;
			this.metrics = metrics;
			this.material = material;
			this.colour = colourHex;
		}
		
		
		@Override
		public String toString()
		{
			ColorAttribute colour = (ColorAttribute) material.get(ColorAttribute.Diffuse);
			return String.format("%s|(%.03f, %.03f, %.03f)[%s]", name, metrics.x, metrics.y, metrics.z, colour.color);
		}
	}
	
	
	private static class Volume
	{
		double[] coordinates;
		private int[][] faces;
		Material material;
		
		public Volume(double[] coordinates, int[][] faces, Material material)
		{
			this.coordinates = coordinates;
			this.faces = faces;
			this.material = material;
		}
	}
	
	
	private static class TetrahedronSide
	{
		Renderable renderable;
		Vector3 centre;
	}
	
	
	private static class Tetrahedron
	{
		Vector3 mediumPos;
		Vector3 longPos;
		Vector3 shortPos;
		Vector3 uvPos;
		Vector3 achroPos;

		public Tetrahedron(float size)
		{
			float pi = MathUtils.PI;
			float circleThird = MathUtils.PI2/3;
			float deg110 = (float) Math.toRadians(109.5);
			float magnitude = 0.75f * size;
			mediumPos = createVectorFromAngles(pi/2, pi/2 - deg110, magnitude);
			longPos = createVectorFromAngles(pi/2 - circleThird, pi/2 - deg110, magnitude);
			shortPos = createVectorFromAngles(pi/2 + circleThird, pi/2 - deg110, magnitude);
			uvPos = createVectorFromAngles(0, pi/2, magnitude);
			achroPos = Vector3.Zero.cpy();
		}
	}
}
