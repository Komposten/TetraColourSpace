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
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;

import komposten.utilities.tools.MathOps;
import komposten.utilities.tools.Regex;


public class TetraColourSpace extends ApplicationAdapter
{
	private static final int SPHERE_SEGMENTS = 25;
	private static final int VELOCITY = 1;
	private static final int MAX_DISTANCE = 10;
	private File dataFile;
	private PerspectiveCamera camera;
	private ModelBatch batch;
	private Environment environment;
	
	private List<Disposable> disposables;
	private List<Point> dataPoints;
	private List<ModelInstance> staticModels;
	private List<ModelInstance> dataModels;
	private Mesh pyramidMesh;
	private Renderable pyramidRenderable;
	private Map<Color, Material> materials;

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
		staticModels = new ArrayList<>();
		dataModels = new ArrayList<>();
		materials = new HashMap<>();
		camera = new PerspectiveCamera(67, 1, Gdx.graphics.getHeight() / (float)Gdx.graphics.getWidth());
		batch = new ModelBatch();
		
		int distance = 2;
		camera.translate(distance, distance, 0);
		camera.near = 0.1f;
		camera.far = 300;
		lookAt(Vector3.Zero);
		camera.update();
		
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
		ModelBuilder modelBuilder = new ModelBuilder();
		float diameter = 0.05f;
		Model sphereModel = modelBuilder.createSphere(
				diameter, diameter, diameter, SPHERE_SEGMENTS, SPHERE_SEGMENTS,
				new Material(), Usage.Position | Usage.Normal);

		float pi = MathUtils.PI;
		float circleThird = MathUtils.PI2/3;
		Vector3 redPos = createVectorFromAngles(circleThird - pi, -pi/2 + circleThird, 1);
		Vector3 greenPos = createVectorFromAngles(-circleThird + pi, -pi/2 + circleThird, 1);
		Vector3 bluePos = createVectorFromAngles(-pi, -pi/2 + circleThird, 1);
		Vector3 uvPos = createVectorFromAngles(0, -pi/2, 1);
		Vector3 achroPos = Vector3.Zero.cpy();
		
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
		

		MeshBuilder meshBuilder = new MeshBuilder();
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
		
		pyramidMesh = meshBuilder.end();
		
		pyramidRenderable = new Renderable();
		pyramidRenderable.material = new Material(getMaterialForColour(Color.WHITE));
		pyramidRenderable.environment = environment;
		pyramidRenderable.meshPart.set("pyramid", pyramidMesh, 0, pyramidMesh.getNumVertices(), GL20.GL_LINES);
		
		disposables.add(sphereModel);
		disposables.add(pyramidMesh);
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
			
			while ((line = reader.readLine()) != null)
			{
				if (line.startsWith("[color="))
				{
					String inputHex = Regex.getMatches("#[A-Fa-f0-9]+", line)[0];
					Color colour = getColourFromHex(inputHex);
					activeMaterial = getMaterialForColour(colour);
				}
				else if (line.startsWith("[point="))
				{
					String[] values = Regex.getMatches("-?\\d+\\.\\d+", line);
					float[] floats = new float[values.length];
					
					for (int i = 0; i < floats.length; i++)
						floats[i] = Float.parseFloat(values[i]);

					float theta = floats[0];
					float phi = floats[1];
					float magnitude = floats[2];
					
					Vector3 coords = createVectorFromAngles(theta, phi, magnitude);
					
					Point point = new Point(coords, activeMaterial);
					dataPoints.add(point);
				}
				else if (line.startsWith("[volume]"))
				{
					//TODO Handle volumes.
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
	}


	private Vector3 createVectorFromAngles(float theta, float phi,
			float magnitude)
	{
		Vector3 coords = new Vector3(1, 0, 0);
		coords.rotateRad(Vector3.Y, theta);
		coords.rotateRad(new Vector3(coords.z, 0, -coords.x), phi);
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
		Gdx.gl.glClearColor(1, 1, 1, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		batch.begin(camera);
		batch.render(pyramidRenderable);
		batch.render(dataModels, environment);
		batch.render(staticModels, environment);
		batch.end();
		
		readInput(Gdx.graphics.getDeltaTime());
	}


	private void readInput(float deltaTime)
	{
		if (readCameraInput(deltaTime) || readOtherInput())
			camera.update();
	}


	private boolean readCameraInput(float deltaTime)
	{
		Vector3 movement = new Vector3();
		
		if (Gdx.input.isKeyPressed(Keys.W) || Gdx.input.isKeyPressed(Keys.S))
		{
			Vector3 velocity = camera.direction.cpy();
			velocity.y = 0;
			velocity.setLength(VELOCITY * deltaTime);
			
			if (Gdx.input.isKeyPressed(Keys.W))
				movement.add(velocity);
			else
				movement.sub(velocity);
		}
		
		if (Gdx.input.isKeyPressed(Keys.A) || Gdx.input.isKeyPressed(Keys.D))
		{
			Vector3 velocity = new Vector3(camera.direction.z, 0, -camera.direction.x);
			velocity.setLength(VELOCITY * deltaTime);
			
			if (Gdx.input.isKeyPressed(Keys.A))
				movement.add(velocity);
			else
				movement.sub(velocity);
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
		
		//TODO Handle camera input.
		if (Gdx.input.isButtonPressed(Buttons.RIGHT))
		{
			int mouseDX = Gdx.input.getDeltaX();
			int mouseDY = Gdx.input.getDeltaY();
			
			if (mouseDX != 0 || mouseDY != 0)
			{
				float sensitivity = -0.2f;
				
				camera.rotate(Vector3.Y, mouseDX * sensitivity);
				Vector3 axis = new Vector3(camera.direction.z, 0, -camera.direction.x);
				camera.rotate(axis, -mouseDY * sensitivity);
				//FIXME Prevent rotation beyond straight up/down.
				needsUpdate = true;
			}
		}
		
		return needsUpdate;
	}


	private boolean readOtherInput()
	{
		boolean needUpdate = false;
		if (Gdx.input.isKeyJustPressed(Keys.HOME))
		{
			lookAt(Vector3.Zero);
			needUpdate = true;
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
}
