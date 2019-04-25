package komposten.tcs.backend.data;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;


/**
 * A group of data points.
 */
public class PointGroup
{
	private String name;
	private List<Point> points;
	private Shape shape;
	private float size;
	
	
	public PointGroup(String name, Shape shape, float size)
	{
		this(name, new ArrayList<>(), shape, size);
	}
	
	
	/**
	 * 
	 * @param name The name of the group.
	 * @param points A list of {@link Point points}. The PointGroup will be backed
	 *          by this list, so external changes are reflected in the group.
	 * @param shape The shape to use when rendering the point.
	 * @param size The point size to use, or a negative value to use the default.
	 */
	public PointGroup(String name, List<Point> points, Shape shape, float size)
	{
		this.name = name;
		this.points = points;
		this.shape = shape;
		this.size = size;
	}

	public String getName()
	{
		return name;
	}

	public List<Point> getPoints()
	{
		return points;
	}

	public Shape getShape()
	{
		return shape;
	}
	
	public float getSize()
	{
		return size;
	}
	
	public void setPoints(List<Point> points)
	{
		this.points = points;
	}
	
	/**
	 * @return The colour of the first point in the list, to represent all points.
	 */
	public Color getColour()
	{
		return (points.isEmpty() ? Color.WHITE : points.get(0).getColour());
	}
}