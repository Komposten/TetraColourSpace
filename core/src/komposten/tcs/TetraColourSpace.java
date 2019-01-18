package komposten.tcs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
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
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import com.github.quickhull3d.QuickHull3D;

import komposten.utilities.tools.MathOps;
import komposten.utilities.tools.Regex;


public class TetraColourSpace extends ApplicationAdapter
{
	private static final float SENSITIVITY = -0.2f;
	private static final int SPHERE_SEGMENTS = 25;
	private static final int VELOCITY = 2;
	private static final int MAX_DISTANCE = 10;
	private File dataFile;
	private PerspectiveCamera camera;
	private ModelBatch batch;
	private Environment environment;
	
	private List<Disposable> disposables;
	private List<Point> dataPoints;
	private List<Volume> dataVolumes;
	private List<ModelInstance> staticModels;
	private List<ModelInstance> dataModels;
	private List<Renderable> dataMeshes;
	private Renderable pyramidLines;
	private Renderable pyramidSides;
	private Map<Color, Material> materials;
	
	private boolean showPyramidSides = false;
	private boolean showPoints = true;
	private boolean showVolumes = true;
	private boolean cameraDirty = true;

	public TetraColourSpace(File dataFile)
	{
		this.dataFile = dataFile;
	}


	@Override
	public void create()
	{
		Gdx.input.setInputProcessor(inputProcessor);
		
		disposables = new ArrayList<>();
		dataPoints = new ArrayList<>();
		dataVolumes = new ArrayList<>();
		staticModels = new ArrayList<>();
		dataModels = new ArrayList<>();
		dataMeshes = new ArrayList<>();
		materials = new HashMap<>();
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
		
		createStaticModels();
		loadData();
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

		createPyramidCorners(redPos, greenPos, bluePos, uvPos, achroPos);
		
		MeshBuilder meshBuilder = new MeshBuilder();
		createPyramidSides(redPos, greenPos, bluePos, uvPos, meshBuilder);
		createPyramidEdges(redPos, greenPos, bluePos, uvPos, meshBuilder);
		
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
			Vector3 bluePos, Vector3 uvPos, Vector3 achroPos)
	{
		ModelBuilder modelBuilder = new ModelBuilder();
		float diameter = 0.03f;
		Model sphereModel = modelBuilder.createSphere(
				diameter, diameter, diameter, SPHERE_SEGMENTS, SPHERE_SEGMENTS,
				new Material(), Usage.Position | Usage.Normal);
		
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
		try (BufferedReader reader = new BufferedReader(new FileReader(dataFile)))
		{
			String line;
			Material activeMaterial = getMaterialForColour(Color.BLACK);
			
			boolean isInVolume = false;
			List<String> volumeData = new ArrayList<>();
			
			while ((line = reader.readLine()) != null)
			{
				if (isInVolume)
				{
					if (!line.startsWith("[/volume]"))
					{
						volumeData.add(line);
					}
					else
					{
						isInVolume = false;

						int[][] faces = null;
						double[] coords = new double[volumeData.size()*3];
						for (int i = 0; i < volumeData.size(); i++)
						{
							Vector3 vector = getCoordinatesFromLine(volumeData.get(i));
							coords[i*3+0] = vector.x;
							coords[i*3+1] = vector.y;
							coords[i*3+2] = vector.z;
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
						
						volumeData.clear();
					}
					
				}
				else if (line.startsWith("[color="))
				{
					String inputHex = Regex.getMatches("#[A-Fa-f0-9]+", line)[0];
					Color colour = getColourFromHex(inputHex);
					activeMaterial = getMaterialForColour(colour);
				}
				else if (line.startsWith("[point="))
				{
					Vector3 coords = getCoordinatesFromLine(line);
					Point point = new Point(coords, activeMaterial);
					dataPoints.add(point);
				}
				else if (line.startsWith("[volume]"))
				{
					isInVolume = true;
				}
			}
		}
		catch (FileNotFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ModelBuilder builder = new ModelBuilder();
		float diameter = 0.02f;
		Model model = builder.createSphere(
				diameter, diameter, diameter, SPHERE_SEGMENTS, SPHERE_SEGMENTS,
				new Material(), Usage.Position | Usage.Normal);
		disposables.add(model);
		
		for (Point point : dataPoints)
		{
			ModelInstance instance = new ModelInstance(model);
			instance.transform.translate(point.coordinates);
			setMaterial(point.material, instance);
			dataModels.add(instance);
		}
		
		MeshBuilder meshBuilder = new MeshBuilder();
		Vector3 vector1 = new Vector3();
		Vector3 vector2 = new Vector3();
		Vector3 vector3 = new Vector3();
		Vector3 edge1 = new Vector3();
		Vector3 edge2 = new Vector3();
		Vector3 normal = new Vector3();
		
		for (Volume volume : dataVolumes)
		{
			if (volume.coordinates.length >= 9)
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
					//FIXME Ensure that the normals point outwards.
					
					short vertex1 = meshBuilder.vertex(vector1, normal, Color.WHITE, null);
					short vertex2 = meshBuilder.vertex(vector2, normal, Color.WHITE, null);
					short vertex3 = meshBuilder.vertex(vector3, normal, Color.WHITE, null);
					meshBuilder.triangle(vertex1, vertex2, vertex3);
				}
				Mesh mesh = meshBuilder.end();
				
				Renderable renderable = new Renderable();
				renderable.meshPart.set("volume", mesh, 0, mesh.getNumVertices(), GL20.GL_TRIANGLES);
				renderable.material = volume.material.copy();
	//			renderable.material.set(new BlendingAttribute(0.5f));
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
		String[] values = Regex.getMatches("-?\\d+\\.\\d+", line);
		float[] floats = new float[values.length];
		
		for (int i = 0; i < floats.length; i++)
			floats[i] = Float.parseFloat(values[i]);

		float theta = floats[0];
		float phi = floats[1];
		float magnitude = floats[2];
		
		return createVectorFromAngles(theta, phi, magnitude);
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
		
		Gdx.gl.glClearColor(1, 1, 1, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		batch.begin(camera);
		batch.render(showPyramidSides ? pyramidSides : pyramidLines);
		batch.render(staticModels, environment);
		if (showPoints)
		{
			batch.render(dataModels, environment);
		}
		if (showVolumes)
		{
			for (Renderable mesh : dataMeshes)
				batch.render(mesh);
		}
		batch.end();
		
		readInput(Gdx.graphics.getDeltaTime());
	}


	private void readInput(float deltaTime)
	{
		if (readCameraInput(deltaTime))
			cameraDirty = true;
		if (readOtherInput())
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


	private boolean readOtherInput()
	{
		boolean needUpdate = false;
		if (Gdx.input.isKeyPressed(Keys.HOME))
		{
			lookAt(Vector3.Zero);
			needUpdate = true;
		}
		
		if (Gdx.input.isKeyJustPressed(Keys.T))
		{
			showPyramidSides = !showPyramidSides;
		}
		
		if (Gdx.input.isKeyJustPressed(Keys.NUM_1))
		{
			showPoints = !showPoints;
		}
		
		if (Gdx.input.isKeyJustPressed(Keys.NUM_2))
		{
			showVolumes = !showVolumes;
		}
		
		//TODO If f just pressed -> set camera to follow the currently selected point (or centre if no point is selected).
		//TODO If r just pressed -> start automatic rotation around the vertical axis.
		
		return needUpdate;
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
	
	
	@Override
	public void resize(int width, int height)
	{
		updateViewport();
	}


	@Override
	public void dispose()
	{
		batch.dispose();
		for (Disposable disposable : disposables)
		{
			disposable.dispose();
		}
	}
	
	
	private InputProcessor inputProcessor = new InputAdapter()
	{
		@Override
		public boolean touchDown(int screenX, int screenY, int pointer, int button)
		{
			if (button == Buttons.RIGHT)
			{
				Gdx.input.setCursorCatched(true);
				return true;
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
		Vector3 coordinates;
		Material material;
		
		public Point(float x, float y, float z, Material material)
		{
			this(new Vector3(x, y, z), material);
		}
		
		
		public Point(Vector3 coordinates, Material material)
		{
			this.coordinates = coordinates;
			this.material = material;
		}
		
		
		@Override
		public String toString()
		{
			ColorAttribute colour = (ColorAttribute) material.get(ColorAttribute.Diffuse);
			return String.format("(%.03f, %.03f, %.03f)[%s]", coordinates.x, coordinates.y, coordinates.z, colour.color);
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
