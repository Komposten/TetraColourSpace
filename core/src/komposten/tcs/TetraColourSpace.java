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
package komposten.tcs;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Filter;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.PixmapIO.PNG;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ScreenUtils;

import komposten.tcs.backend.Backend;
import komposten.tcs.backend.Style.Colour;
import komposten.tcs.backend.data.Point;
import komposten.tcs.input.Action;
import komposten.tcs.input.CameraController;
import komposten.tcs.input.InputHandler;
import komposten.tcs.input.InputHandler.InputListener;
import komposten.tcs.rendering.World;
import komposten.tcs.ui.UserInterface;
import komposten.utilities.logging.Level;
import komposten.utilities.logging.Logger;
import komposten.utilities.tools.FileOperations;


public class TetraColourSpace extends ApplicationAdapter
{
	private static final int SCREENSHOT_SIZE = 1080;
	private static final int SCREENSHOT_SUPERSAMPLE = 10;
	
	private Logger logger;
	private List<Point> selectionLog;
	
	private Backend backend;
	private World world;
	private UserInterface userInterface;
	private CameraController cameraController;
	private InputHandler inputHandler;
	
	private File dataFile;
	private File outputPath;
	private PerspectiveCamera camera;
	private OrthographicCamera spriteCamera;
	private ModelBatch batch;
	private SpriteBatch spriteBatch;
	
	private FrameBuffer screenshotBuffer;
	
	private List<Disposable> disposables;
	
	private boolean takeScreenshot = false;
	private boolean cameraDirty = true;
	
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
		
		inputHandler = new InputHandler("config.ini", logger);
		Gdx.input.setInputProcessor(inputHandler);
		
		disposables = new ArrayList<>();
		camera = new PerspectiveCamera(67, 1, 1);
		spriteCamera = new OrthographicCamera();
		batch = new ModelBatch();
		spriteBatch = new SpriteBatch();
		
		inputHandler.addListener(inputListener);
		
		createScreenshotBuffer();
		loadData();
		setupPerspectiveCamera();
		updateViewport();
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
		userInterface = new UserInterface(backend, world);
		
		userInterface.attachToInputHandler(inputHandler);
		world.attachToInputHandler(inputHandler);
	}


	private void setupPerspectiveCamera()
	{
		int distance = 1;
		camera.translate(distance, distance, -0.3f*distance);
		camera.near = 0.01f;
		camera.far = 300;
		
		cameraController = new CameraController(camera, world);
		cameraController.lookAt(Vector3.Zero);
		cameraController.attachToInputHandler(inputHandler);
	}


	@Override
	public void render()
	{
		boolean cameraUpdated = cameraDirty || cameraController.isCameraDirty();
		if (cameraUpdated)
		{
			cameraDirty = false;
			camera.update();
			cameraController.clearCameraDirty();
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
		userInterface.render(spriteBatch);
		spriteBatch.end();
		
		//Handle input
		cameraController.readInput(Gdx.graphics.getDeltaTime(), inputHandler);
		
		//Update stuff
		world.update();
		updateSelectionLog();
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


	private void updateSelectionLog()
	{
		Point lastSelection = null;
		Point currentSelection = world.getSelectedPoint();
		
		if (!selectionLog.isEmpty())
			lastSelection = selectionLog.get(selectionLog.size()-1);
		
		if (currentSelection != null && currentSelection != lastSelection)
			selectionLog.add(currentSelection);
	}


	private void updateViewport()
	{
		float ratio = Gdx.graphics.getHeight() / (float)Gdx.graphics.getWidth();
		camera.viewportHeight = ratio;
		cameraDirty = true;
		
		spriteCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		spriteBatch.setProjectionMatrix(spriteCamera.combined);
		
		userInterface.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
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
		userInterface.dispose();
		disposeObjects();
		
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
	

	private InputListener inputListener = new InputListener()
	{
		@Override
		public boolean onActionStopped(Action action, Object... parameters)
		{
			if (action == Action.SCREENSHOT)
			{
				takeScreenshot = true;
				return true;
			}
			
			return false;
		}

		
		@Override
		public boolean onActionStarted(Action action, Object... parameters)
		{
			return false;
		}
	};
}
