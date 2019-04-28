package komposten.tcs.util;

import static komposten.tcs.util.TestUtils.assertVectorEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

class TCSUtilsTest
{
	private static final float rad45 = 45*MathUtils.degRad;


	@Test
	void createVectorFromAngles_varyingTheta()
	{
		assertVectorEquals(new Vector3(MathUtils.cosDeg(45), 0, -MathUtils.cosDeg(45)), TCSUtils.createVectorFromAngles(rad45*1, 0, 1), "45 deg");
		assertVectorEquals(new Vector3(-MathUtils.cosDeg(45), 0, -MathUtils.cosDeg(45)), TCSUtils.createVectorFromAngles(rad45*3, 0, 1), "45*3 deg");
		assertVectorEquals(new Vector3(-MathUtils.cosDeg(45), 0, MathUtils.cosDeg(45)), TCSUtils.createVectorFromAngles(rad45*5, 0, 1), "45*5 deg");
		assertVectorEquals(new Vector3(MathUtils.cosDeg(45), 0, MathUtils.cosDeg(45)), TCSUtils.createVectorFromAngles(rad45*7, 0, 1), "45*7 deg");

		assertVectorEquals(new Vector3(MathUtils.cosDeg(45), 0, MathUtils.cosDeg(45)), TCSUtils.createVectorFromAngles(-rad45*1, 0, 1), "-45 deg");
		assertVectorEquals(new Vector3(-MathUtils.cosDeg(45), 0, MathUtils.cosDeg(45)), TCSUtils.createVectorFromAngles(-rad45*3, 0, 1), "-45*3 deg");
		assertVectorEquals(new Vector3(-MathUtils.cosDeg(45), 0, -MathUtils.cosDeg(45)), TCSUtils.createVectorFromAngles(-rad45*5, 0, 1), "-45*5 deg");
		assertVectorEquals(new Vector3(MathUtils.cosDeg(45), 0, -MathUtils.cosDeg(45)), TCSUtils.createVectorFromAngles(-rad45*7, 0, 1), "-45*7 deg");
	}
	
	
	@Test
	void createVectorFromAngles_varyingPhi()
	{
		assertVectorEquals(new Vector3(MathUtils.cosDeg(45), MathUtils.cosDeg(45), 0), TCSUtils.createVectorFromAngles(0, rad45*1, 1), "45 deg");
		assertVectorEquals(new Vector3(-MathUtils.cosDeg(45), MathUtils.cosDeg(45), 0), TCSUtils.createVectorFromAngles(0, rad45*3, 1), "45*3 deg");
		assertVectorEquals(new Vector3(-MathUtils.cosDeg(45), -MathUtils.cosDeg(45), 0), TCSUtils.createVectorFromAngles(0, rad45*5, 1), "45*5 deg");
		assertVectorEquals(new Vector3(MathUtils.cosDeg(45), -MathUtils.cosDeg(45), 0), TCSUtils.createVectorFromAngles(0, rad45*7, 1), "45*7 deg");

		assertVectorEquals(new Vector3(MathUtils.cosDeg(45), -MathUtils.cosDeg(45), 0), TCSUtils.createVectorFromAngles(0, -rad45*1, 1), "-45 deg");
		assertVectorEquals(new Vector3(-MathUtils.cosDeg(45), -MathUtils.cosDeg(45), 0), TCSUtils.createVectorFromAngles(0, -rad45*3, 1), "-45*3 deg");
		assertVectorEquals(new Vector3(-MathUtils.cosDeg(45), MathUtils.cosDeg(45), 0), TCSUtils.createVectorFromAngles(0, -rad45*5, 1), "-45*5 deg");
		assertVectorEquals(new Vector3(MathUtils.cosDeg(45), MathUtils.cosDeg(45), 0), TCSUtils.createVectorFromAngles(0, -rad45*7, 1), "-45*7 deg");
	}
	
	
	@Test
	void createVectorFromAngles_varyingR()
	{
		assertVectorEquals(new Vector3(0.5f, 0, 0), TCSUtils.createVectorFromAngles(0, 0, 0.5f), "len=0.5");
		assertVectorEquals(new Vector3(1.0f, 0, 0), TCSUtils.createVectorFromAngles(0, 0, 1.0f), "len=1.0");
		assertThrows(IllegalArgumentException.class, () -> TCSUtils.createVectorFromAngles(0, 0, -0.5f), "len=-0.5");
		assertThrows(IllegalArgumentException.class, () -> TCSUtils.createVectorFromAngles(0, 0, -1.0f), "len=-1.0");
	}
	
	
	@Test
	void getMaterialForColour()
	{
		Material material = TCSUtils.getMaterialForColour(Color.RED);
		
		assertNotNull(material);
		assertTrue(material == TCSUtils.getMaterialForColour(Color.RED), "same colour instance and value");
		assertTrue(material == TCSUtils.getMaterialForColour(new Color(0xff0000ff)), "diff colour instance and same value");
		assertNotEquals(material, TCSUtils.getMaterialForColour(Color.BLUE), "diff colour instance and value");
	}
	
	
	@Test
	void getColourFromHex_validHexCodes_ColorReturned()
	{
		assertEquals(new Color(0xfedcba98), TCSUtils.getColourFromHex("#fedcba98"));
		assertEquals(new Color(0xfedcba98), TCSUtils.getColourFromHex("fedcba98"));
		assertEquals(new Color(0xfedcbaff), TCSUtils.getColourFromHex("#fedcba"));
		assertEquals(new Color(0xfedcbaff), TCSUtils.getColourFromHex("fedcba"));
		assertEquals(new Color(0xffeeddcc), TCSUtils.getColourFromHex("#fedc"));
		assertEquals(new Color(0xffeeddcc), TCSUtils.getColourFromHex("fedc"));
		assertEquals(new Color(0xffeeddff), TCSUtils.getColourFromHex("#fed"));
		assertEquals(new Color(0xffeeddff), TCSUtils.getColourFromHex("fed"));
	}
	
	
	@Test
	void getColourFromHex_invalidHexCodes_IllegalArgumentException()
	{
		assertThrows(IllegalArgumentException.class, () -> TCSUtils.getColourFromHex("#f"));
		assertThrows(IllegalArgumentException.class, () -> TCSUtils.getColourFromHex("f"));
		assertThrows(IllegalArgumentException.class, () -> TCSUtils.getColourFromHex("#fffff"));
		assertThrows(IllegalArgumentException.class, () -> TCSUtils.getColourFromHex("fffff"));
		assertThrows(IllegalArgumentException.class, () -> TCSUtils.getColourFromHex("#gggggg"));
		assertThrows(IllegalArgumentException.class, () -> TCSUtils.getColourFromHex("gggggg"));
		assertThrows(IllegalArgumentException.class, () -> TCSUtils.getColourFromHex("-2"));
	}
}
