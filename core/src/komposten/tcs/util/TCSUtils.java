package komposten.tcs.util;

import com.badlogic.gdx.math.Vector3;


public class TCSUtils
{
	private TCSUtils() {}


	/**
	 * Creates a coordinate vector from the given colour metrics.
	 * 
	 * @param theta
	 * @param phi
	 * @param magnitude The absolute magnitude (i.e. not a relative value between
	 *          0 and 1).
	 * @return
	 */
	public static Vector3 createVectorFromAngles(float theta, float phi, float magnitude)
	{
		Vector3 coords = new Vector3(1, 0, 0);
		coords.rotateRad(Vector3.Y, theta);
		coords.rotateRad(new Vector3(-coords.z, 0, coords.x), phi);
		coords.setLength(magnitude);
		return coords;
	}
}
