package komposten.tcs.util;

import org.opentest4j.AssertionFailedError;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

public class TestUtils
{
	public static void assertVectorEquals(Vector3 expected, Vector3 actual, String message)
	{
		boolean xEqual = MathUtils.isEqual(expected.x, actual.x, 0.001f);
		boolean yEqual = MathUtils.isEqual(expected.y, actual.y, 0.001f);
		boolean zEqual = MathUtils.isEqual(expected.z, actual.z, 0.001f);
		
		if (!xEqual || !yEqual || !zEqual)
		{
			String msg;
			
			if (message != null)
				msg = String.format("%s ==> expected: <%s> but was: <%s>", message, expected, actual);
			else
				msg = String.format("expected: <%s> but was: <%s>", expected, actual);
			
			throw new AssertionFailedError(msg, expected, actual);
		}
	}
	
	
	public static void assertVectorEquals(Vector3 expected, Vector3 actual)
	{
		assertVectorEquals(expected, actual, null);
	}
}
