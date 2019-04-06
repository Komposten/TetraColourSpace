package komposten.tcs.rendering;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder.VertexInfo;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;

import komposten.tcs.backend.Style;
import komposten.tcs.backend.Style.Colour;
import komposten.tcs.backend.data.Point;
import komposten.tcs.util.TCSUtils;
import komposten.utilities.tools.Geometry;

public class MetricLineRenderable implements Disposable
{
	private Renderable pointMetricsLines;
	private Renderable pointMetricsArcs;
	private Mesh mesh;
	
	public MetricLineRenderable(Style style, Environment environment)
	{
		pointMetricsLines = new Renderable();
		pointMetricsArcs = new Renderable();
		pointMetricsLines.material = TCSUtils.getMaterialForColour(style.get(Colour.METRIC_LINE));
		pointMetricsArcs.material = TCSUtils.getMaterialForColour(style.get(Colour.METRIC_FILL));
		pointMetricsArcs.environment = environment;
	}
	
	
	public void setTarget(Point target)
	{
		if (mesh != null)
			dispose();
		if (target == null)
		{
			mesh = null;
			return;
		}
		
		float phi = target.getMetrics().y;
		float theta = getNormalisedTheta(target.getMetrics().x);
		float thetaDeg = theta*MathUtils.radiansToDegrees;
		
		Vector3 line = target.getCoordinates();
		Vector3 lineInZX = new Vector3(line.x, 0, line.z);
		
		float arcRadius = lineInZX.len()*0.8f;
		float segmentsPerRadian = 200 / MathUtils.PI2;
		
		float[] thetaArc = createArc(-theta, arcRadius, segmentsPerRadian);
		float[] phiArc = createArc(phi, arcRadius, segmentsPerRadian);
		
		MeshBuilder builder = new MeshBuilder();
		builder.begin(Usage.Position | Usage.Normal | Usage.ColorUnpacked, GL20.GL_LINES);
		
		MeshPart lines = createOutlines(thetaDeg, line, lineInZX, thetaArc, phiArc, builder);
		MeshPart arcFill = createFills(thetaDeg, thetaArc, phiArc, builder);
		
		mesh = builder.end();
		pointMetricsLines.meshPart.set(lines);
		pointMetricsArcs.meshPart.set(arcFill);
	}


	private float getNormalisedTheta(float theta)
	{
		while (theta > MathUtils.PI) theta -= MathUtils.PI2;
		while (theta < -MathUtils.PI) theta += MathUtils.PI2;
		
		return theta;
	}


	private float[] createArc(float angle, float arcRadius, float segmentsPerRadian)
	{
		int segments = MathUtils.ceil(Math.abs(angle)*segmentsPerRadian);
		return Geometry.createArc(0, angle, arcRadius, segments);
	}


	private MeshPart createOutlines(float thetaDeg, Vector3 line, Vector3 lineInZX,
			float[] thetaArc, float[] phiArc, MeshBuilder builder)
	{
		MeshPart lines = builder.part("lines", GL20.GL_LINES);

		createOriginLines(builder, line, lineInZX);
		createArcOutlines(builder, thetaArc, phiArc, thetaDeg);
		
		return lines;
	}


	private void createOriginLines(MeshBuilder builder, Vector3 line, Vector3 lineInZX)
	{
		builder.line(Vector3.Zero, line);
		builder.line(Vector3.Zero, lineInZX);
	}


	private void createArcOutlines(MeshBuilder builder, float[] thetaArc, float[] phiArc,
			float thetaDeg)
	{
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
	}


	private MeshPart createFills(float thetaDeg, float[] thetaArc, float[] phiArc,
			MeshBuilder builder)
	{
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
			
			arcTriangle(builder, normal, startVertex, endVertex, zeroVertex);
			arcTriangle(builder, normalInv, startVertex, zeroVertex, endVertex);
		}
		
		normal.set(0, 0, -1).rotate(Vector3.Y, thetaDeg);
		normalInv.set(normal).scl(-1);
		for (int i = 2; i < phiArc.length; i+=2)
		{
			startVertex.setPos(phiArc[i-2], phiArc[i-1], 0);
			endVertex.setPos(phiArc[i], phiArc[i+1], 0);
			
			startVertex.position.rotate(Vector3.Y, thetaDeg);
			endVertex.position.rotate(Vector3.Y, thetaDeg);
			
			arcTriangle(builder, normal, startVertex, endVertex, zeroVertex);
			arcTriangle(builder, normalInv, startVertex, zeroVertex, endVertex);
		}
		
		return arcFill;
	}


	private void arcTriangle(MeshBuilder builder, Vector3 normal, VertexInfo vertex1,
			VertexInfo vertex2, VertexInfo vertex3)
	{
		vertex1.setNor(normal);
		vertex2.setNor(normal);
		vertex3.setNor(normal);
		builder.triangle(vertex1, vertex2, vertex3);
	}


	public void render(ModelBatch batch, boolean filled)
	{
		if (mesh != null)
		{
			if (filled)
				batch.render(pointMetricsArcs);
			batch.render(pointMetricsLines);
		}
	}
	
	
	@Override
	public void dispose()
	{
		mesh.dispose();
	}
}
