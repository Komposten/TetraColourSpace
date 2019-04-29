package komposten.tcs.util.test;

import static org.assertj.core.api.Assertions.within;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.data.Offset;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

public class Vector3Assert extends AbstractAssert<Vector3Assert, Vector3>
{

	public Vector3Assert(Vector3 actual)
	{
		super(actual, Vector3Assert.class);
	}
	
	
	public static Vector3Assert assertThat(Vector3 actual)
	{
		return new Vector3Assert(actual);
	}
	
	
	/**
	 * Verifies that the actual vector is close to the given one within an offset
	 * of {@link MathUtils#FLOAT_ROUNDING_ERROR}. 
	 */
	public Vector3Assert isEqualTo(Vector3 expected)
	{
		return isEqualTo(expected, within(MathUtils.FLOAT_ROUNDING_ERROR));
	}
	
	
	/**
	 * Verifies that the actual vector is close to the given one within the given
	 * offset value. 
	 */
	public Vector3Assert isEqualTo(Vector3 expected, Offset<Float> offset)
	{
		isNotNull();
		if (!actual.epsilonEquals(expected, offset.value))
		{
			failWithMessage("%n" +
          "Expecting:%n" +
          "  <%s>%n" +
          "to be close to:%n" +
          "  <%s>%n" +
          "by less than or equal to <%s> but difference was:%n" + 
          "  <%s>.",
          vectorToString(actual),
          vectorToString(expected),
          offset.value,
          vectorToString(absDiff(actual, expected)));
		}
		
		return this;
	}


	private Vector3 absDiff(Vector3 actual, Vector3 expected)
	{
		Vector3 result = new Vector3(actual).sub(expected);
		
		result.x = Math.abs(result.x);
		result.y = Math.abs(result.y);
		result.z = Math.abs(result.z);
		
		return result;
	}
	
	
	private String vectorToString(Vector3 vector)
	{
		return String.format("(%f, %f, %f)", vector.x, vector.y, vector.z);
	}
}
