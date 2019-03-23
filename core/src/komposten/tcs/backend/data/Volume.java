package komposten.tcs.backend.data;

import com.badlogic.gdx.graphics.Color;

/**
 * Stores information about a volume consisting of several data points.
 */
public class Volume
{
	private double[] coordinates;
	private int[][] faces;
	private Color colour;
	

	/**
	 * @param coordinates The coordinates for the points forming the volume,
	 *          stored as <code>x1, y1, z1, x2, y2, z2, ...</code>
	 * @param faces An array listing all the faces in the volume polygon. Each
	 *          face should consist of indices for the three vertices forming the
	 *          face.<br />
	 *          E.g. the face [1, 2, 3] corresponds to the first three vertices in
	 *          <code>coordinates</code>.
	 * @param colour The colour to use for the volume.
	 */
	public Volume(double[] coordinates, int[][] faces, Color colour)
	{
		this.coordinates = coordinates;
		this.faces = faces;
		this.colour = colour;
	}

	/**
	 * @return The coordinates for the points forming the volume,
	 *          stored as <code>x1, y1, z1, x2, y2, z2, ...</code>
	 */
	public double[] getCoordinates()
	{
		return coordinates;
	}

	/**
	 * @return An array listing all the faces in the volume polygon. Each
	 *          face should consist of indices for the three vertices forming the
	 *          face.<br />
	 *          E.g. the face [1, 2, 3] corresponds to the first three vertices in
	 *          <code>coordinates</code>.
	 */
	public int[][] getFaces()
	{
		return faces;
	}

	public Color getColour()
	{
		return colour;
	}
}