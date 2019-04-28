package komposten.tcs.util;

import static komposten.tcs.util.TestUtils.assertVectorEquals;

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
		
		assertVectorEquals(expectedLong, tetrahedron.longPos, "longPos[size=1]");
		assertVectorEquals(expectedMedium, tetrahedron.mediumPos, "mediumPoslongPos[size=1]");
		assertVectorEquals(expectedShort, tetrahedron.shortPos, "shortPoslongPos[size=1]");
		assertVectorEquals(expectedUv, tetrahedron.uvPos, "uvPoslongPos[size=1]");
		assertVectorEquals(expectedAchro, tetrahedron.achroPos, "achroPoslongPos[size=1]");
		
		tetrahedron = new Tetrahedron(0.5f);
		expectedLong.scl(0.5f);
		expectedMedium.scl(0.5f);
		expectedShort.scl(0.5f);
		expectedUv.scl(0.5f);
		expectedAchro.scl(0.5f);
		
		assertVectorEquals(expectedLong, tetrahedron.longPos, "longPos[size=0.5]");
		assertVectorEquals(expectedMedium, tetrahedron.mediumPos, "mediumPos[size=0.5]");
		assertVectorEquals(expectedShort, tetrahedron.shortPos, "shortPos[size=0.5]");
		assertVectorEquals(expectedUv, tetrahedron.uvPos, "uvPos[size=0.5]");
		assertVectorEquals(expectedAchro, tetrahedron.achroPos, "achroPos[size=0.5]");
	}
}
