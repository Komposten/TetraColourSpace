package komposten.tcs.rendering;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder.VertexInfo;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;

import komposten.tcs.backend.Backend;
import komposten.tcs.backend.Style;
import komposten.tcs.backend.Style.Colour;
import komposten.tcs.backend.data.Point;
import komposten.tcs.backend.data.PointGroup;
import komposten.tcs.backend.data.Volume;
import komposten.tcs.util.ModelInstanceFactory;
import komposten.tcs.util.ShapeFactory;
import komposten.tcs.util.TCSUtils;
import komposten.utilities.tools.Geometry;

/*
 * TODO Move the metric lines and arcs to the UI?
 */
public class World implements Disposable
{
	public static final int SPHERE_SEGMENTS = 25;
	
	private static final int POINT_METRIC_HIDE = 0;
	private static final int POINT_METRIC_FILLED = 2;
	private static final int POINT_METRIC_OPTIONS = 3;
	
	private Backend backend;
	private Camera camera;
	private Environment environment;
	
	private GraphSpace graphSpace;

	private List<Disposable> disposables;
	private List<PointGroupRenderable> groupRenderables;
	private List<VolumeRenderable> volumeRenderables;
	private Renderable pointMetricsLines;
	private Renderable pointMetricsArcs;
	private ModelInstance selectedModel;
	private ModelInstance highlightModel;
	
	private Point highlightPoint;
	private Point selectedPoint;
	
	private boolean showPoints = true;
	private boolean showVolumes = true;
	private boolean showHighlight = true;
	private boolean showAxes = false;
	private int showPointMetrics = 0;
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
	
	
	public void toggleTetrahedronSides()
	{
		graphSpace.toggleTetrahedronSides();
	}
	
	
	public void toggleAxisLines()
	{
		showAxes = !showAxes;
		
		graphSpace.setShowAxes(showPointMetrics != POINT_METRIC_HIDE || showAxes);
	}
	
	
	public void toggleHighlight()
	{
		showHighlight = !showHighlight;
	}
	
	
	public void togglePointMetrics()
	{
		showPointMetrics = (showPointMetrics + 1) % POINT_METRIC_OPTIONS;
		
		graphSpace.setShowAxes(showPointMetrics != POINT_METRIC_HIDE || showAxes);
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
		graphSpace = new GraphSpace(backend.getStyle(), camera);

		Style style = backend.getStyle();
		Model model = ShapeFactory.createSphere(new ModelBuilder(), 0.025f, GL20.GL_LINES, 10);
		selectedModel = ModelInstanceFactory.create(model, Vector3.Zero, style.get(Colour.SELECTION));
		highlightModel = ModelInstanceFactory.create(model, Vector3.Zero, style.get(Colour.HIGHLIGHT));
		
		pointMetricsLines = new Renderable();
		pointMetricsArcs = new Renderable();
		pointMetricsLines.material = TCSUtils.getMaterialForColour(style.get(Colour.METRIC_LINE));
		pointMetricsArcs.material = TCSUtils.getMaterialForColour(style.get(Colour.METRIC_FILL));
		pointMetricsArcs.environment = environment;
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
		groupRenderables = new ArrayList<>(backend.getDataGroups().size());
		
		for (PointGroup group : backend.getDataGroups())
		{
			PointGroupRenderable dataGroup = new PointGroupRenderable(group, 0.02f);
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
			highlightModel.transform.setTranslation(highlightPoint.getCoordinates());
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
			if (selectedPoint != null)
			{
				hasSelection = true;
				selectedModel.transform.setTranslation(selectedPoint.getCoordinates());
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
			graphSpace.cameraUpdated();
		}
		
		graphSpace.render(batch, environment);
		renderPoints(batch);
		renderVolumes(batch);
		renderPointMetrics(batch);
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
	}
}
