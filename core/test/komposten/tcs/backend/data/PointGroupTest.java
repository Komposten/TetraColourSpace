package komposten.tcs.backend.data;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.badlogic.gdx.graphics.Color;

class PointGroupTest
{
	private PointGroup group;


	@BeforeEach
	void init()
	{
		group = new PointGroup("Group", Shape.SPHERE, 1);
	}
	

	@Test
	void getColour_noPoints_returnWhite()
	{
		assertThat(group.getColour()).isEqualTo(Color.WHITE);
	}
	

	@Test
	void getColour_hasPoints_returnFirstPointColour()
	{
		group.getPoints().add(new Point("", null, null, Color.RED, null));
		
		assertThat(group.getColour()).isEqualTo(Color.RED);
	}
}
