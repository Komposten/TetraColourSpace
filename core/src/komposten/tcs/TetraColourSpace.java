package komposten.tcs;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Filter;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.PixmapIO.PNG;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ScreenUtils;

import komposten.tcs.backend.Backend;
import komposten.tcs.backend.Style.Colour;
import komposten.tcs.backend.data.Point;
import komposten.tcs.backend.data.PointGroup;
import komposten.tcs.backend.data.Shape;
import komposten.tcs.rendering.World;
import komposten.utilities.logging.Level;
import komposten.utilities.logging.Logger;
import komposten.utilities.tools.FileOperations;
import komposten.utilities.tools.MathOps;


public class TetraColourSpace extends ApplicationAdapter
{
	private enum FollowMode
	{
		SELECTED,
		CENTRE,
		OFF
	}
	
	private static final float MAX_ZOOM = 0.025f;
	private static final float SENSITIVITY = -0.2f;
	private static final float SLOW_MODIFIER = .33f;
	private static final int LINEAR_VELOCITY = 1;
	private static final int SCROLL_VELOCITY = 5;
	private static final int ANGULAR_VELOCITY = 50;
	private static final int ANGULAR_AUTO_VELOCITY = 20;
	private static final int MAX_DISTANCE = 5;
	private static final int SCREENSHOT_SIZE = 1080;
	private static final int SCREENSHOT_SUPERSAMPLE = 10;
	
	private static final int LEGEND_HIDE = 0;
	private static final int LEGEND_GROUPS = 1;
	private static final int LEGEND_POINTS = 2;
	private static final int LEGEND_OPTIONS = 3;
	
	private Logger logger;
	private List<Point> selectionLog;
	
	private Backend backend;
	private World world;
	
	private File dataFile;
	private File outputPath;
	private PerspectiveCamera camera;
	private OrthographicCamera spriteCamera;
	private ModelBatch batch;
	private SpriteBatch spriteBatch;
	
	private Sprite crosshair;
	private BitmapFont font;
	
	private EnumMap<Shape, Texture> shapeSprites;
	
	private FrameBuffer screenshotBuffer;
	
	private List<Disposable> disposables;
	
	private FollowMode followMode = FollowMode.OFF;
	private int scrollDelta = 0;
	
	private boolean takeScreenshot = false;
	private boolean showCrosshair = true;
	private int showLegend = 0;
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
		
		Color colourCrosshair = backend.getStyle().get(Colour.CROSSHAIR);
		Color colourBackground = backend.getStyle().get(Colour.BACKGROUND);
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


	private void loadData()
	{
		backend = new Backend(dataFile, logger);
		world = new World(backend, camera);
		
		createCrosshair();
	}


	@Override
	public void render()
	{
		boolean cameraUpdated = cameraDirty;
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
		
		Color colourBackground = backend.getStyle().get(Colour.BACKGROUND);
		Gdx.gl.glClearColor(colourBackground.r, colourBackground.g, colourBackground.b, colourBackground.a);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		batch.begin(camera);
		world.render(batch, cameraUpdated);
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
		if (world.hasSelection())
		{
			Point selectedPoint = world.getSelectedPoint();
			renderTextWithSymbol(selectedPoint.getName(),
					shapeSprites.get(selectedPoint.getGroup().getShape()), 5, Gdx.graphics.getHeight() - 15f,
					5, 12, selectedPoint.getColour());
			String metrics = String.format(
					"%n  Theta: %.02f%n  Phi: %.02f%n  r: %.02f",
					selectedPoint.getMetrics().x,
					selectedPoint.getMetrics().y,
					selectedPoint.getMetrics().z);
			
			spriteBatch.setColor(backend.getStyle().get(Colour.TEXT));
			font.draw(spriteBatch, metrics, 5, Gdx.graphics.getHeight()-10f);
			spriteBatch.setColor(Color.WHITE);
		}
		renderLegend();
		spriteBatch.end();
		
		//Handle input
		readInput(Gdx.graphics.getDeltaTime());
		
		//Update stuff
		world.update();
		
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
			float y = Gdx.graphics.getHeight() - (padding + lineHeight/2);
			
			for (PointGroup group : backend.getDataGroups())
			{
				Texture shapeTexture = shapeSprites.get(group.getShape());
				
				if (showLegend == LEGEND_POINTS)
				{
					for (Point point : group.getPoints())
					{
						renderTextWithSymbol(point.getName(), shapeTexture, x, y, padding, shapeSize, point.getColour());
						y -= lineHeight;
					}
				}
				else if (showLegend == LEGEND_GROUPS)
				{
					renderTextWithSymbol(group.getName(), shapeTexture, x, y, padding, shapeSize, group.getColour());
					y -= lineHeight;
				}
			}
		}
	}


	private void renderTextWithSymbol(String text, Texture shapeTexture, float x, float y,
			float padding, float shapeSize, Color colour)
	{
		spriteBatch.setColor(colour);
		spriteBatch.draw(shapeTexture, x, y - shapeSize/2 , shapeSize, shapeSize);
		spriteBatch.setColor(Color.WHITE);
		String line = String.format("[%s]%s", colour, text);
		font.draw(spriteBatch, line, x + shapeSize + padding, y + font.getCapHeight()/2, 0, Align.left, false);
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
				lookAt(world.getSelectedPoint().getCoordinates());
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
		Vector3 focalPoint = (followMode == FollowMode.CENTRE ? Vector3.Zero : world.getSelectedPoint().getCoordinates());
		
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
		
		world.dispose();
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
				world.togglePyramidSides();
				return true;
			}
			else if (keycode == Keys.Y)
			{
				world.toggleAxisLines();
				return true;
			}
			else if (keycode == Keys.H)
			{
				world.toggleHighlight();
				return true;
			}
			else if (keycode == Keys.M)
			{
				world.togglePointMetrics();
				return true;
			}
			else if (keycode == Keys.NUM_1)
			{
				world.togglePoints();
				return true;
			}
			else if (keycode == Keys.NUM_2)
			{
				world.toggleVolumes();
				return true;
			}
			else if (keycode == Keys.F && world.hasSelection())
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
			else if (button == Buttons.LEFT)
			{
				Point newSelection = world.updateSelection();
				
				if (newSelection != null)
					selectionLog.add(newSelection);
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
}
