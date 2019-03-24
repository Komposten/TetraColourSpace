package komposten.tcs.util;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.math.Vector3;


public class TCSUtils
{
	private static Map<Color, Material> materials = new HashMap<>();
	
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
		if (hexColour.startsWith("#"))
		{
			hexColour = hexColour.substring(1);
		}
		
		if (hexColour.matches("[0-9A-Fa-f]+"))
		{
			if (hexColour.length() == 3 || hexColour.length() == 4)
			{
				char[] chars = new char[hexColour.length()*2];
				
				for (int i = 0; i < hexColour.length(); i++)
					chars[i*2] = chars[i*2+1] = hexColour.charAt(i);
				
				hexColour = new String(chars);
			}
			
			if (hexColour.length() == 6)
			{
				hexColour += "FF";
			}
			
			if (hexColour.length() == 8)
			{
				int rgb = Integer.parseInt(hexColour.substring(0, 6), 16);
				int alpha = Integer.parseInt(hexColour.substring(6), 16);
				return new Color(rgb << 8 | alpha);
			}
		}
		
		throw new IllegalArgumentException(hexColour + " is not a valid hex colour!");
	}


	public static Material getMaterialForColour(Color colour)
	{
		return materials.computeIfAbsent(colour, c -> new Material(ColorAttribute.createDiffuse(colour)));
	}
}
