package komposten.tcs;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Filter;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.PixmapIO.PNG;
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
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.github.quickhull3d.QuickHull3D;

import komposten.utilities.tools.FileOperations;
import komposten.utilities.tools.MathOps;
import komposten.utilities.tools.Regex;


public class TetraColourSpace extends ApplicationAdapter
{
	private enum FollowMode
	{
		Selected,
		Centre,
		Off
	}
	
	private static final float SENSITIVITY = -0.2f;
	private static final int SPHERE_SEGMENTS = 25;
	private static final int VELOCITY = 2;
	private static final int MAX_DISTANCE = 5;
	private static final int SCREENSHOT_SIZE = 1080;
	private static final int SCREENSHOT_SUPERSAMPLE = 10;
	
	private File dataFile;
	private File outputPath;
	private PerspectiveCamera camera;
	private ModelBatch batch;
	private Environment environment;
	
	private FrameBuffer screenshotBuffer;
	
	private List<Disposable> disposables;
	private List<Point> dataPoints;
	private List<Volume> dataVolumes;
	private List<ModelInstance> staticModels;
	private List<ModelInstance> dataModels;
	private List<Renderable> dataMeshes;
	private Map<Point, ModelInstance> pointToModelMap;
	private Renderable pyramidLines;
	private Renderable pyramidSides;
	private Renderable axisLines;
	private ModelInstance selectedModel;
	private ModelInstance highlightModel;
	private Map<Color, Material> materials;
	
	private Point highlightPoint;
	private Point selectedPoint;
	private FollowMode followMode = FollowMode.Off;
	
	private boolean takeScreenshot = false;
	private boolean showPyramidSides = false;
	private boolean showAxes = false;
	private boolean showPoints = true;
	private boolean showVolumes = true;
	private boolean showHighlight = true;
	private boolean hasSelection = false;
	private boolean hasHighlight = false;
	private boolean cameraDirty = true;

	public TetraColourSpace(File dataFile, File outputPath)
	{
		if (outputPath == null)
			outputPath = new File("output/");
		else if (outputPath.exists() && !outputPath.isDirectory())
			throw new IllegalArgumentException("outputPath must be a directory: \"" + outputPath.getPath() + "\"");
		
		this.dataFile = dataFile;
		this.outputPath = outputPath;
	}


	@Override
	public void create()
	{
		Gdx.graphics.setTitle("TetraColourSpace - " + dataFile.getName());
		Gdx.input.setInputProcessor(inputProcessor);
		
		disposables = new ArrayList<>();
		dataPoints = new ArrayList<>();
		dataVolumes = new ArrayList<>();
		staticModels = new ArrayList<>();
		dataModels = new ArrayList<>();
		dataMeshes = new ArrayList<>();
		materials = new HashMap<>();
		pointToModelMap = new HashMap<>();
		camera = new PerspectiveCamera(67, 1, 1);
		batch = new ModelBatch();
		
		int distance = 2;
		updateViewport();
		camera.translate(distance, distance, 0);
		camera.near = 0.1f;
		camera.far = 300;
		lookAt(Vector3.Zero);
		
		DirectionalLight light = new DirectionalLight();
		light.setDirection(-1f, -1f, 0);
		light.setColor(Color.WHITE);
		environment = new Environment();
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.6f, 0.6f, 0.6f, 1.0f));
		environment.add(light);
		
		createScreenshotBuffer();
		createStaticModels();
		loadData();
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
		float pi = MathUtils.PI;
		float circleThird = MathUtils.PI2/3;
		float deg110 = (float) Math.toRadians(109.5);
		Vector3 greenPos = createVectorFromAngles(pi/2, pi/2 - deg110, 0.75f);
		Vector3 redPos = createVectorFromAngles(pi/2 - circleThird, pi/2 - deg110, 0.75f);
		Vector3 bluePos = createVectorFromAngles(pi/2 + circleThird, pi/2 - deg110, 0.75f);
		Vector3 uvPos = createVectorFromAngles(0, pi/2, 0.75f);
		Vector3 achroPos = Vector3.Zero.cpy();

		ModelBuilder modelBuilder = new ModelBuilder();
		createPyramidCorners(redPos, greenPos, bluePos, uvPos, achroPos, modelBuilder);

		Model model = createSphere(modelBuilder, 0.025f, GL20.GL_LINES, 10);
		selectedModel = createModelInstance(model, Vector3.Zero, Color.DARK_GRAY);
		highlightModel = createModelInstance(model, Vector3.Zero, Color.CORAL);
		
		MeshBuilder meshBuilder = new MeshBuilder();
		createPyramidSides(redPos, greenPos, bluePos, uvPos, meshBuilder);
		createPyramidEdges(redPos, greenPos, bluePos, uvPos, meshBuilder);
		
		createAxisLines(meshBuilder);
	}


	private void createPyramidSides(Vector3 redPos, Vector3 greenPos,
			Vector3 bluePos, Vector3 uvPos, MeshBuilder meshBuilder)
	{
		meshBuilder.begin(Usage.Position | Usage.Normal | Usage.ColorUnpacked, GL20.GL_TRIANGLES);

		Vector3 normal = greenPos.cpy().add(redPos).add(bluePos);
		short corner1 = meshBuilder.vertex(greenPos, normal, Color.GREEN, Vector2.Zero);
		short corner2 = meshBuilder.vertex(redPos, normal, Color.RED, Vector2.Zero);
		short corner3 = meshBuilder.vertex(bluePos, normal, Color.BLUE, Vector2.Zero);
		meshBuilder.triangle(corner1, corner2, corner3);

		normal = normal.scl(-1);
		corner1 = meshBuilder.vertex(greenPos, normal, Color.GREEN, Vector2.Zero);
		corner2 = meshBuilder.vertex(redPos, normal, Color.RED, Vector2.Zero);
		corner3 = meshBuilder.vertex(bluePos, normal, Color.BLUE, Vector2.Zero);
		meshBuilder.triangle(corner3, corner2, corner1);
		
		normal = redPos.cpy().add(greenPos).add(uvPos);
		corner1 = meshBuilder.vertex(redPos, normal, Color.RED, Vector2.Zero);
		corner2 = meshBuilder.vertex(greenPos, normal, Color.GREEN, Vector2.Zero);
		corner3 = meshBuilder.vertex(uvPos, normal, Color.VIOLET, Vector2.Zero);
		meshBuilder.triangle(corner1, corner2, corner3);
		
		normal = normal.scl(-1);
		corner1 = meshBuilder.vertex(redPos, normal, Color.RED, Vector2.Zero);
		corner2 = meshBuilder.vertex(greenPos, normal, Color.GREEN, Vector2.Zero);
		corner3 = meshBuilder.vertex(uvPos, normal, Color.VIOLET, Vector2.Zero);
		meshBuilder.triangle(corner3, corner2, corner1);
		
		normal = redPos.cpy().add(uvPos).add(bluePos);
		corner1 = meshBuilder.vertex(redPos, normal, Color.RED, Vector2.Zero);
		corner2 = meshBuilder.vertex(uvPos, normal, Color.VIOLET, Vector2.Zero);
		corner3 = meshBuilder.vertex(bluePos, normal, Color.BLUE, Vector2.Zero);
		meshBuilder.triangle(corner1, corner2, corner3);

		normal = normal.scl(-1);
		corner1 = meshBuilder.vertex(redPos, normal, Color.RED, Vector2.Zero);
		corner2 = meshBuilder.vertex(uvPos, normal, Color.VIOLET, Vector2.Zero);
		corner3 = meshBuilder.vertex(bluePos, normal, Color.BLUE, Vector2.Zero);
		meshBuilder.triangle(corner3, corner2, corner1);
		
		normal = uvPos.cpy().add(greenPos).add(bluePos);
		corner1 = meshBuilder.vertex(uvPos, normal, Color.VIOLET, Vector2.Zero);
		corner2 = meshBuilder.vertex(greenPos, normal, Color.GREEN, Vector2.Zero);
		corner3 = meshBuilder.vertex(bluePos, normal, Color.BLUE, Vector2.Zero);
		meshBuilder.triangle(corner1, corner2, corner3);

		normal = normal.scl(-1);
		corner1 = meshBuilder.vertex(uvPos, normal, Color.VIOLET, Vector2.Zero);
		corner2 = meshBuilder.vertex(greenPos, normal, Color.GREEN, Vector2.Zero);
		corner3 = meshBuilder.vertex(bluePos, normal, Color.BLUE, Vector2.Zero);
		meshBuilder.triangle(corner3, corner2, corner1);

		Mesh pyramidMesh = meshBuilder.end();
		
		pyramidSides = new Renderable();
		pyramidSides.material = new Material(getMaterialForColour(Color.WHITE));
		pyramidSides.material.set(new BlendingAttribute(0.5f));
		pyramidSides.meshPart.set("pyramid", pyramidMesh, 0, pyramidMesh.getNumVertices(), GL20.GL_TRIANGLES);

		disposables.add(pyramidMesh);
	}
	
	
	private void createPyramidEdges(Vector3 redPos, Vector3 greenPos,
			Vector3 bluePos, Vector3 uvPos, MeshBuilder meshBuilder)
	{
		meshBuilder.begin(Usage.Position | Usage.Normal | Usage.ColorUnpacked, GL20.GL_TRIANGLES);
		
		Vector3 normal = greenPos.cpy().add(redPos).add(bluePos);
		short corner1 = meshBuilder.vertex(greenPos, normal, Color.GREEN, Vector2.Zero);
		short corner2 = meshBuilder.vertex(redPos, normal, Color.RED, Vector2.Zero);
		short corner3 = meshBuilder.vertex(bluePos, normal, Color.BLUE, Vector2.Zero);
		meshBuilder.triangle(corner1, corner2, corner3);
		
		normal = redPos.cpy().add(greenPos).add(uvPos);
		corner1 = meshBuilder.vertex(redPos, normal, Color.RED, Vector2.Zero);
		corner2 = meshBuilder.vertex(greenPos, normal, Color.GREEN, Vector2.Zero);
		corner3 = meshBuilder.vertex(uvPos, normal, Color.VIOLET, Vector2.Zero);
		meshBuilder.triangle(corner1, corner2, corner3);
		
		normal = redPos.cpy().add(uvPos).add(bluePos);
		corner1 = meshBuilder.vertex(redPos, normal, Color.RED, Vector2.Zero);
		corner2 = meshBuilder.vertex(uvPos, normal, Color.VIOLET, Vector2.Zero);
		corner3 = meshBuilder.vertex(bluePos, normal, Color.BLUE, Vector2.Zero);
		meshBuilder.triangle(corner1, corner2, corner3);
		
		normal = uvPos.cpy().add(greenPos).add(bluePos);
		corner1 = meshBuilder.vertex(uvPos, normal, Color.VIOLET, Vector2.Zero);
		corner2 = meshBuilder.vertex(greenPos, normal, Color.GREEN, Vector2.Zero);
		corner3 = meshBuilder.vertex(bluePos, normal, Color.BLUE, Vector2.Zero);
		meshBuilder.triangle(corner1, corner2, corner3);
		
		Mesh pyramidMesh = meshBuilder.end();

		pyramidLines = new Renderable();
		pyramidLines.material = new Material(getMaterialForColour(Color.WHITE));
//		pyramidLines.environment = environment;
		pyramidLines.meshPart.set("pyramid", pyramidMesh, 0, pyramidMesh.getNumVertices(), GL20.GL_LINES);
		
		disposables.add(pyramidMesh);
	}


	private void createPyramidCorners(Vector3 redPos, Vector3 greenPos,
			Vector3 bluePos, Vector3 uvPos, Vector3 achroPos, ModelBuilder modelBuilder)
	{
		float diameter = 0.03f;
		Model sphereModel = createSphere(modelBuilder, diameter, GL20.GL_TRIANGLES);
		
		ModelInstance redSphere = createModelInstance(sphereModel, redPos, Color.RED);
		ModelInstance greenSphere = createModelInstance(sphereModel, greenPos, Color.GREEN);
		ModelInstance blueSphere = createModelInstance(sphereModel, bluePos, Color.BLUE);
		ModelInstance uvSphere = createModelInstance(sphereModel, uvPos, Color.VIOLET);
		ModelInstance achroSphere = createModelInstance(sphereModel, achroPos, Color.GRAY);
		
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
		meshBuilder.line(start.set(-length, 0, 0), Color.BLUE, end.set(length, 0, 0), Color.RED);
		meshBuilder.line(start.set(0, -length, 0), Color.WHITE, end.set(0, length, 0), Color.VIOLET);
		meshBuilder.line(start.set(0, 0, -length), Color.GREEN, end.set(0, 0, length), Color.PURPLE);
		
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
		Model sphereModel = modelBuilder.createSphere(
				diameter, diameter, diameter, segments, segments,
				primitiveType, new Material(), Usage.Position | Usage.Normal);
		return sphereModel;
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


	private void loadData()
	{
		try
		{
			Material activeMaterial = getMaterialForColour(Color.BLACK);
			
			DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document document = docBuilder.parse(dataFile);
			Element root = document.getDocumentElement();
			
			NodeList points = root.getElementsByTagName("point");
			for (int i = 0; i < points.getLength(); i++)
			{
				Node pointNode = points.item(i);
				
				NamedNodeMap attributes = pointNode.getAttributes();
				Node colourAttr = attributes.getNamedItem("colour");
				Node nameAttr = attributes.getNamedItem("name");
				Node positionAttr = attributes.getNamedItem("position");
				
				String name = "Point " + (i+1);
				if (colourAttr != null)
				{
					String colourHex = colourAttr.getNodeValue().trim();
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
				Point point = new Point(name, coords, metrics, activeMaterial);
				dataPoints.add(point);
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
			System.err.println("Error reading file: " + dataFile.getPath());
			System.err.println("  Cause: " + e.getMessage());
		}
		catch (ParserConfigurationException e)
		{
			System.err.println("Error creating the XML parser!");
			System.err.println("  Cause: " + e.getMessage());
		}
		catch (SAXException e)
		{
			System.err.println("Error parsing file: " + dataFile.getPath());
			System.err.println("  Cause: " + e.getMessage());
		}
		
		ModelBuilder builder = new ModelBuilder();
		float diameter = 0.02f;
		Model model = createSphere(builder, diameter, GL20.GL_TRIANGLES);
		disposables.add(model);
		
		for (Point point : dataPoints)
		{
			ModelInstance instance = new ModelInstance(model);
			instance.transform.translate(point.coordinates);
			setMaterial(point.material, instance);
			dataModels.add(instance);
			pointToModelMap.put(point, instance);
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
				renderable.meshPart.set("volume", mesh, 0, mesh.getNumVertices(), GL20.GL_TRIANGLES);
				renderable.material = volume.material.copy();
				renderable.material.set(new BlendingAttribute(0.5f));
				renderable.environment = environment;
				
				dataMeshes.add(renderable);
				
	//			renderable = new Renderable();
	//			renderable.meshPart.set("volume_lines", mesh, 0, mesh.getNumVertices(), GL20.GL_LINES);
	//			renderable.material = volume.material.copy();
	//			renderable.material.set(new BlendingAttribute(0.7f));
	//			renderable.environment = environment;
	//			
	//			dataMeshes.add(renderable);
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
				renderable.meshPart.set("volume", mesh, 0, mesh.getNumVertices(), GL20.GL_LINES);
				renderable.material = volume.material.copy();
				renderable.environment = environment;
				
				dataMeshes.add(renderable);
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
		String[] values = Regex.getMatches("-?\\d+\\.\\d+", line);
		float[] floats = new float[values.length];
		
		for (int i = 0; i < floats.length; i++)
			floats[i] = Float.parseFloat(values[i]);

		float theta = floats[0];
		float phi = floats[1];
		float magnitude = floats[2];
		
		return new Vector3(theta, phi, magnitude);
	}


	private Vector3 createVectorFromAngles(float theta, float phi,
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
			r = String.format("%1$s%1$s", hexColour.charAt(0));
			g = String.format("%1$s%1$s", hexColour.charAt(1));
			b = String.format("%1$s%1$s", hexColour.charAt(2));
			
			if (hexColour.length() == 4)
				a = String.format("%1$s%1$s", hexColour.charAt(3));
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
		}
		
		if (takeScreenshot)
		{
			Gdx.gl.glLineWidth(SCREENSHOT_SUPERSAMPLE);
			screenshotBuffer.begin();
		}
		
		Gdx.gl.glClearColor(1, 1, 1, 0);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		batch.begin(camera);
		batch.render(showPyramidSides ? pyramidSides : pyramidLines);
		batch.render(staticModels, environment);
		if (showAxes)
			batch.render(axisLines);
		if (showPoints)
		{
			batch.render(dataModels, environment);
			
			if (hasSelection)
			{
				batch.render(selectedModel, environment);
			}
			if (showHighlight && hasHighlight)
			{
				if (!hasSelection || selectedPoint != highlightPoint)
					batch.render(highlightModel, environment);
			}
		}
		if (showVolumes)
		{
			for (Renderable mesh : dataMeshes)
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
		
		readInput(Gdx.graphics.getDeltaTime());
		
		if (showHighlight)
			updateHighlight();
		
		if (followMode != FollowMode.Off)
		{
			updateFollow();
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
			System.err.println("Error writing file: " + file.path());
			System.err.println("  Cause: " + e.getMessage());
		}
		
		pixmap.dispose();
	}


	private void updateFollow()
	{
		switch (followMode)
		{
			case Centre :
				lookAt(Vector3.Zero);
				cameraDirty = true;
				break;
			case Selected :
				lookAt(selectedPoint.coordinates);
				cameraDirty = true;
				break;
			case Off :
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
		Vector3 movement = new Vector3();
		
		if (Gdx.input.isKeyPressed(Keys.W) || Gdx.input.isKeyPressed(Keys.S))
		{
			calcVector.set(camera.direction.x, 0, camera.direction.z).setLength(VELOCITY * deltaTime);
			
			if (Gdx.input.isKeyPressed(Keys.W))
				movement.add(calcVector);
			else
				movement.sub(calcVector);
		}
		
		if (Gdx.input.isKeyPressed(Keys.A) || Gdx.input.isKeyPressed(Keys.D))
		{
			calcVector.set(camera.direction.z, 0, -camera.direction.x).setLength(VELOCITY * deltaTime);
			
			if (Gdx.input.isKeyPressed(Keys.A))
				movement.add(calcVector);
			else
				movement.sub(calcVector);
		}
		
		if (Gdx.input.isKeyPressed(Keys.E) || Gdx.input.isKeyPressed(Keys.Q))
		{
			calcVector.set(camera.direction).setLength(VELOCITY * deltaTime);
			
			if (Gdx.input.isKeyPressed(Keys.E))
				movement.add(calcVector);
			else
				movement.sub(calcVector);
		}
		
		if (Gdx.input.isKeyPressed(Keys.SPACE))
		{
			movement.y += VELOCITY * deltaTime;
		}
		else if (Gdx.input.isKeyPressed(Keys.CONTROL_LEFT))
		{
			movement.y -= VELOCITY * deltaTime;
		}
		
		boolean needsUpdate = false;
		if (!movement.epsilonEquals(Vector3.Zero))
		{
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
				
				double currentAngle = Math.toDegrees(Math.atan2(calcVector.y - camera.direction.y, calcVector.len()));
				double maxAngle = 89;
				
				if (currentAngle + rotationY > maxAngle)
					rotationY = (float) (maxAngle - currentAngle);
				else if (currentAngle + rotationY < -maxAngle)
					rotationY = (float) -(maxAngle + currentAngle);
				
				calcVector.set(camera.direction.z, 0, -camera.direction.x);
				camera.rotate(calcVector, rotationY);
				
				needsUpdate = true;
			}
		}
		
		return needsUpdate;
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
		
		for (Point point : dataPoints)
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
		for (Disposable disposable : disposables)
		{
			disposable.dispose();
		}
	}
	
	
	private InputProcessor inputProcessor = new InputAdapter()
	{
		@Override
		public boolean keyDown(int keycode)
		{
			if (keycode == Keys.HOME)
			{
				followMode = FollowMode.Centre;
				return true;
			}
			
			return false;
		}
		
		
		@Override
		public boolean keyUp(int keycode)
		{
			if (keycode == Keys.HOME && followMode == FollowMode.Centre)
			{
				followMode = FollowMode.Off;
				return true;
			}
			else if (keycode == Keys.C)
			{
				if (followMode != FollowMode.Centre)
					followMode = FollowMode.Centre;
				else
					followMode = FollowMode.Off;
				return true;
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
				if (followMode != FollowMode.Selected)
					followMode = FollowMode.Selected;
				else
					followMode = FollowMode.Off;
				return true;
			}
			else if (keycode == Keys.F12)
			{
				takeScreenshot = true;
			}
			
			
			return false;
		}
		
		
		@Override
		public boolean touchDown(int screenX, int screenY, int pointer, int button)
		{
			if (button == Buttons.RIGHT)
			{
				Gdx.input.setCursorCatched(true);
				followMode = FollowMode.Off;
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
					System.out.println(selectedPoint);
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
	};
	
	
	private static class Point
	{
		String name;
		Vector3 coordinates;
		Vector3 metrics;
		Material material;
		
		public Point(String name, Vector3 coordinates, Vector3 metrics, Material material)
		{
			this.name = name;
			this.coordinates = coordinates;
			this.metrics = metrics;
			this.material = material;
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
}
