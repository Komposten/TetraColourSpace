package komposten.tcs.backend.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ShapeTest
{
	@Test
	void fromString_enumNamesOrValues_returnConstants()
	{
		assertEquals(Shape.SPHERE, Shape.fromString("sphere"));
		assertEquals(Shape.SPHERE, Shape.fromString("16"));
		assertEquals(Shape.PYRAMID, Shape.fromString("pyramid"));
		assertEquals(Shape.PYRAMID, Shape.fromString("17"));
		assertEquals(Shape.BOX, Shape.fromString("box"));
		assertEquals(Shape.BOX, Shape.fromString("15"));
	}
	
	
	@Test
	void fromString_invalidName_returnNull()
	{
		assertNull(Shape.fromString("notashape"));
		assertNull(Shape.fromString(null));
	}
}
