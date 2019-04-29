package komposten.tcs.util;

import static komposten.tcs.util.test.Vector3Assert.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

import com.badlogic.gdx.math.Vector3;

class TetrahedronTest
{
	@Test
	void Tetrahedron_cornerPositions()
	{
		Tetrahedron tetrahedron = new Tetrahedron(1);
		Vector3 expectedLong = new Vector3(0.6123f, -0.2504f, 0.3535f);
		Vector3 expectedMedium = new Vector3(0, -0.2503f, -0.7070f);
		Vector3 expectedShort = new Vector3(-0.6123f ,-0.2504f ,0.3535f);
		Vector3 expectedUv = new Vector3(0, 0.75f, 0);
		Vector3 expectedAchro = new Vector3(0, 0, 0);
		
		assertThat(tetrahedron.longPos).as("longPos[size=1]").isEqualTo(expectedLong, within(0.001f));
		assertThat(tetrahedron.mediumPos).as("mediumPos[size=1]").isEqualTo(expectedMedium, within(0.001f));
		assertThat(tetrahedron.shortPos).as("shortPos[size=1]").isEqualTo(expectedShort, within(0.001f));
		assertThat(tetrahedron.uvPos).as("uvPos[size=1]").isEqualTo(expectedUv, within(0.001f));
		assertThat(tetrahedron.achroPos).as("achroPos[size=1]").isEqualTo(expectedAchro, within(0.001f));
		
		tetrahedron = new Tetrahedron(0.5f);
		expectedLong.scl(0.5f);
		expectedMedium.scl(0.5f);
		expectedShort.scl(0.5f);
		expectedUv.scl(0.5f);
		expectedAchro.scl(0.5f);
		
		assertThat(tetrahedron.longPos).as("longPos[size=0.5]").isEqualTo(expectedLong, within(0.001f));
		assertThat(tetrahedron.mediumPos).as("mediumPos[size=0.5]").isEqualTo(expectedMedium, within(0.001f));
		assertThat(tetrahedron.shortPos).as("shortPos[size=0.5]").isEqualTo(expectedShort, within(0.001f));
		assertThat(tetrahedron.uvPos).as("uvPos[size=0.5]").isEqualTo(expectedUv, within(0.001f));
		assertThat(tetrahedron.achroPos).as("achroPos[size=0.5]").isEqualTo(expectedAchro, within(0.001f));
	}
}
