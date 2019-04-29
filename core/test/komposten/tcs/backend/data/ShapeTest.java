package komposten.tcs.backend.data;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ShapeTest
{
	@Test
	void fromString_enumNamesOrValues_returnConstants()
	{
		assertThat(Shape.fromString("sphere")).isEqualTo(Shape.SPHERE);
		assertThat(Shape.fromString("16")).isEqualTo(Shape.SPHERE);
		assertThat(Shape.fromString("pyramid")).isEqualTo(Shape.PYRAMID);
		assertThat(Shape.fromString("17")).isEqualTo(Shape.PYRAMID);
		assertThat(Shape.fromString("box")).isEqualTo(Shape.BOX);
		assertThat(Shape.fromString("15")).isEqualTo(Shape.BOX);
	}
	
	
	@Test
	void fromString_invalidName_returnNull()
	{
		assertThat(Shape.fromString("notashape")).isNull();
		assertThat(Shape.fromString(null)).isNull();
	}
}
