package komposten.tcs.rendering;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;

import komposten.tcs.backend.Backend;
import komposten.tcs.backend.Style;
import komposten.tcs.backend.Style.Colour;
import komposten.tcs.backend.Style.Setting;
import komposten.tcs.backend.data.Point;
import komposten.tcs.backend.data.PointGroup;
import komposten.tcs.backend.data.Volume;
import komposten.tcs.input.Action;
import komposten.tcs.input.InputHandler;
import komposten.tcs.input.InputHandler.InputListener;
import komposten.tcs.input.InputReceiver;
import komposten.tcs.util.ModelInstanceFactory;
import komposten.tcs.util.ShapeFactory;

public class World implements Disposable, InputReceiver
{
	private Backend backend;
	private Camera camera;
	private Environment environment;
	
	private GraphSpace graphSpace;

	private List<Disposable> disposables;
	private List<PointGroupRenderable> groupRenderables;
	private List<VolumeRenderable> volumeRenderables;
	private ModelInstance selectedModel;
	private ModelInstance highlightModel;
	
	private Point highlightPoint;
	private Point selectedPoint;
	
	private boolean showPoints = true;
	private boolean showVolumes = true;
	private boolean showHighlight = true;
	private boolean hasSelection = false;
	private boolean hasHighlight = false;


	public World(Backend backend, Camera camera)
	{
		this.backend = backend;
		this.camera = camera;
		
		disposables = new ArrayList<>();

		createEnvironment();
		
		generatePointObjects();
		generateVolumeObjects();
		createStaticModels();
	}
	
	
	@Override
	public void attachToInputHandler(InputHandler handler)
	{
		handler.addListener(inputListener);
		graphSpace.attachToInputHandler(handler);
	}
	
	
	@Override
	public void detachFromInputHandler(InputHandler handler)
	{
		handler.removeListener(inputListener);
		graphSpace.detachFromInputHandler(handler);
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
		graphSpace = new GraphSpace(backend.getStyle(), camera, environment);

		Style style = backend.getStyle();
		Model model = ShapeFactory.createSphere(new ModelBuilder(), 1, GL20.GL_LINES, 10);
		selectedModel = ModelInstanceFactory.create(model, Vector3.Zero, style.get(Colour.SELECTION));
		highlightModel = ModelInstanceFactory.create(model, Vector3.Zero, style.get(Colour.HIGHLIGHT));
	}
	
	
	private void generatePointObjects()
	{
		groupRenderables = new ArrayList<>(backend.getDataGroups().size());
		int sphereSegments = backend.getStyle().get(Setting.SPHERE_QUALITY).intValue();
		
		for (PointGroup group : backend.getDataGroups())
		{
			PointGroupRenderable dataGroup = new PointGroupRenderable(group, sphereSegments);
			groupRenderables.add(dataGroup);
			disposables.add(dataGroup);
		}
	}


	private void generateVolumeObjects()
	{
		volumeRenderables = new ArrayList<>(backend.getDataVolumes().size());
		
		for (Volume volume : backend.getDataVolumes())
		{
			VolumeRenderable renderable = new VolumeRenderable(volume, environment);
			volumeRenderables.add(renderable);
			disposables.add(renderable);
		}
	}
	
	
	public void update()
	{
		if (showHighlight)
			updateHighlight();
	}
	
	
	private void updateHighlight()
	{
		highlightPoint = getPointNearCrosshair();
		if (highlightPoint != null)
		{
			hasHighlight = true;
			scaleAndTranslate(highlightPoint, highlightModel);
		}
		else
		{
			hasHighlight = false;
		}
	}
	
	
	public Point updateSelection()
	{
		if (showPoints && showHighlight)
		{
			selectedPoint = highlightPoint;
			graphSpace.setPointMetricTarget(selectedPoint);
			if (selectedPoint != null)
			{
				hasSelection = true;
				scaleAndTranslate(selectedPoint, selectedModel);
				return selectedPoint;
			}
			else
			{
				hasSelection = false;
			}
		}
		else if (!showHighlight)
		{
			selectedPoint = null;
			hasSelection = false;
		}

		return null;
	}


	private void scaleAndTranslate(Point point, ModelInstance modelInstance)
	{
		float scale = point.getGroup().getSize() * 1.25f;
		float oldScale = modelInstance.transform.getScaleX();
		modelInstance.transform.setTranslation(point.getCoordinates());
		modelInstance.transform.scl(scale/oldScale);
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
			graphSpace.cameraUpdated();
		}
		
		graphSpace.render(batch, environment);
		renderPoints(batch);
		renderVolumes(batch);
	}


	private void renderPoints(ModelBatch batch)
	{
		if (showPoints)
		{
			for (PointGroupRenderable groupRenderable : groupRenderables)
				groupRenderable.render(batch, environment);

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
			for (VolumeRenderable volumeRenderable : volumeRenderables)
				volumeRenderable.render(batch);
		}
	}
	
	
	@Override
	public void dispose()
	{
		for (Disposable disposable : disposables)
		{
			disposable.dispose();
		}
	}
	

	private InputListener inputListener = new InputListener()
	{
		@Override
		public boolean onActionStopped(Action action, Object... parameters)
		{
			if (action == Action.TOGGLE_HIGHLIGHT)
			{
				showHighlight = !showHighlight;

				if (!showHighlight)
				{
					hasHighlight = false;
					highlightPoint = null;
				}
				return true;
			}
			else if (action == Action.TOGGLE_POINTS)
			{
				showPoints = !showPoints;
				return true;
			}
			else if (action == Action.TOGGLE_VOLUMES)
			{
				showVolumes = !showVolumes;
				return true;
			}

			return false;
		}


		@Override
		public boolean onActionStarted(Action action, Object... parameters)
		{
			if (action == Action.SELECT_POINT)
			{
				updateSelection();
				return true;
			}
			
			return false;
		}
	};
}
