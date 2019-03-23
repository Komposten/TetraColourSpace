package komposten.tcs.util;

import com.badlogic.gdx.graphics.Color;
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


	public static Color getColourFromHex(String hexColour)
	{
		//TODO Rewrite this to use Color(int) instead.
		if (hexColour.startsWith("#")) hexColour = hexColour.substring(1);
		
		String r;
		String g;
		String b;
		String a = "FF";
		
		if (hexColour.length() == 3 || hexColour.length() == 4)
		{
			String twoCharFormat = "%1$s%1$s";
			r = String.format(twoCharFormat, hexColour.charAt(0));
			g = String.format(twoCharFormat, hexColour.charAt(1));
			b = String.format(twoCharFormat, hexColour.charAt(2));
			
			if (hexColour.length() == 4)
				a = String.format(twoCharFormat, hexColour.charAt(3));
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


	private static float getColourComponentFloat(String hex)
	{
		return Integer.parseInt(hex, 16) / 255f;
	}
}
