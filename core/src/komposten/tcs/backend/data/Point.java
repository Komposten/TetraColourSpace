package komposten.tcs.backend.data;

import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.math.Vector3;

/**
 * Stores information about a data point.
 */
public class Point
{
	private PointGroup group;
	private String name;
	private Vector3 coordinates;
	private Vector3 metrics;
	private Material material; //TODO Don't store materials in the backend?
	private String colour;		
	
	/**
	 * @param name The name of the point.
	 * @param coordinates The position of the point in the colour space.
	 * @param metrics The tetrachromatic colour metrics for the point. <code>x, y,</code> and
	 * <code>z</code> represent theta, phi and r respectively.
	 * @param material The material to use when rendering the point.
	 * @param colourHex The hexadecimal value for the colour of the point.
	 * @param group The {@link PointGroup group} the point belongs to.
	 */
	public Point(String name, Vector3 coordinates, Vector3 metrics, Material material, String colourHex, PointGroup group)
	{
		this.group = group;
		this.name = name;
		this.coordinates = coordinates;
		this.metrics = metrics;
		this.material = material;
		this.colour = colourHex;
	}
	
	
	public PointGroup getGroup()
	{
		return group;
	}


	public String getName()
	{
		return name;
	}


	public Vector3 getCoordinates()
	{
		return coordinates;
	}


	/**
	 * @return The tetrachromatic colour metrics for the point. <code>x, y,</code> and
	 * <code>z</code> represent theta, phi and r respectively.
	 */
	public Vector3 getMetrics()
	{
		return metrics;
	}


	public Material getMaterial()
	{
		return material;
	}

	
	/**
	 * @return The hexadecimal value for the colour of this point.
	 */
	public String getColour()
	{
		return colour;
	}
	
	
	@Override
	public String toString()
	{
		ColorAttribute colour = (ColorAttribute) material.get(ColorAttribute.Diffuse);
		return String.format("%s|(%.03f, %.03f, %.03f)[%s]", name, metrics.x, metrics.y, metrics.z, colour.color);
	}
}