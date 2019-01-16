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
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;

import komposten.utilities.tools.MathOps;
import komposten.utilities.tools.Regex;


public class TetraColourSpace extends ApplicationAdapter
{
	private static final int VELOCITY = 1;
	private static final int MAX_DISTANCE = 10;
	private File dataFile;
	private PerspectiveCamera camera;
	private ModelBatch batch;
	private Environment environment;
	
	private List<Point> points;
	private List<ModelInstance> models;
	private Map<Color, Material> materials;

	public TetraColourSpace(File dataFile)
	{
		this.dataFile = dataFile;
	}


	@Override
	public void create()
	{
		points = new ArrayList<>();
		models = new ArrayList<>();
		materials = new HashMap<>();
		camera = new PerspectiveCamera(67, 100, 100);
		batch = new ModelBatch();
		
		int distance = 3;
		camera.translate(distance, distance, 0);
		camera.lookAt(Vector3.Zero);
		camera.update();
		
		DirectionalLight light = new DirectionalLight();
		light.setDirection(0, 0, -1);
		light.setColor(Color.BLUE);
		environment = new Environment();
		environment.add(light);
		
		loadData();
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
					
					Vector3 coords = new Vector3(1, 0, 0);
					coords.rotateRad(Vector3.Z, theta);
					coords.rotateRad(new Vector3(coords.y, -coords.x, 0), phi);
					coords.setLength(magnitude);
					
					Point point = new Point(coords, activeMaterial);
					points.add(point);
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
		float diameter = 0.25f;
		Model model = builder.createSphere(diameter, diameter, diameter, 5, 5, new Material(),
				VertexAttributes.Usage.Position |
				VertexAttributes.Usage.ColorUnpacked |
				VertexAttributes.Usage.Normal);
		
		for (Point point : points)
		{
			ModelInstance instance = new ModelInstance(model);
			instance.transform.translate(point.coordinates);
			instance.nodes.forEach(node -> node.parts.forEach(part -> part.material = point.material));
			models.add(instance);
		}
		
		ModelInstance instance = new ModelInstance(model);
		instance.transform.setTranslation(0, 0, 0);
		instance.materials.set(0, new Material(ColorAttribute.createDiffuse(Color.BLACK)));
		models.add(instance);
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
		batch.render(models);
		batch.end();
		
		readInput(Gdx.graphics.getDeltaTime());
	}


	private void readInput(float deltaTime)
	{
		readCameraInput(deltaTime);
		readOtherInput();
	}


	private void readCameraInput(float deltaTime)
	{
		Vector3 movement = new Vector3();
		if (Gdx.input.isKeyPressed(Keys.W))
		{
			movement.x += VELOCITY * deltaTime;
		}
		else if (Gdx.input.isKeyPressed(Keys.S))
		{
			movement.x -= VELOCITY * deltaTime;
		}
		
		if (Gdx.input.isKeyPressed(Keys.A))
		{
			movement.y += VELOCITY * deltaTime;
		}
		else if (Gdx.input.isKeyPressed(Keys.D))
		{
			movement.y -= VELOCITY * deltaTime;
		}
		
		if (Gdx.input.isKeyPressed(Keys.SPACE))
		{
			movement.z += VELOCITY * deltaTime;
		}
		else if (Gdx.input.isKeyPressed(Keys.CONTROL_LEFT))
		{
			movement.z -= VELOCITY * deltaTime;
		}
		
		if (!movement.epsilonEquals(Vector3.Zero))
		{
			camera.position.x = MathOps.clamp(-MAX_DISTANCE, MAX_DISTANCE, camera.position.x + movement.x);
			camera.position.y = MathOps.clamp(-MAX_DISTANCE, MAX_DISTANCE, camera.position.y + movement.y);
			camera.position.z = MathOps.clamp(-MAX_DISTANCE, MAX_DISTANCE, camera.position.z + movement.z);
			camera.update();
		}
		
		//TODO Handle camera input.
		float mouseDX = Gdx.input.getDeltaX();
		float mouseDY = Gdx.input.getDeltaY();
	}


	private void readOtherInput()
	{
		//TODO If f just pressed -> set camera to follow the currently selected point (or centre if no point is selected).
		//TODO If r just pressed -> start automatic rotation around the vertical axis.
		//TODO If home just pressed -> look towards the pyramid centre.
	}


	@Override
	public void dispose()
	{
		batch.dispose();
	}
	
	
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
