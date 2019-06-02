package komposten.tcs.util;

import static komposten.tcs.util.test.Vector3Assert.assertThat;
import static org.assertj.core.api.Assertions.assertThatObject;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;


class TCSUtilsTest
{
	private static final float rad45 = 45*MathUtils.degRad;


	@Test
	void getCoordinatesForMetrics_varyingTheta()
	{
		assertThat(TCSUtils.getCoordinatesForMetrics(rad45*1, 0, 1)).as("45 deg").isEqualTo(new Vector3(MathUtils.cosDeg(45), 0, -MathUtils.cosDeg(45)), within(0.001f));
		assertThat(TCSUtils.getCoordinatesForMetrics(rad45*3, 0, 1)).as("45*3 deg").isEqualTo(new Vector3(-MathUtils.cosDeg(45), 0, -MathUtils.cosDeg(45)), within(0.001f));
		assertThat(TCSUtils.getCoordinatesForMetrics(rad45*5, 0, 1)).as("45*5 deg").isEqualTo(new Vector3(-MathUtils.cosDeg(45), 0, MathUtils.cosDeg(45)), within(0.001f));
		assertThat(TCSUtils.getCoordinatesForMetrics(rad45*7, 0, 1)).as("45*7 deg").isEqualTo(new Vector3(MathUtils.cosDeg(45), 0, MathUtils.cosDeg(45)), within(0.001f));

		assertThat(TCSUtils.getCoordinatesForMetrics(-rad45*1, 0, 1)).as("-45 deg").isEqualTo(new Vector3(MathUtils.cosDeg(45), 0, MathUtils.cosDeg(45)), within(0.001f));
		assertThat(TCSUtils.getCoordinatesForMetrics(-rad45*3, 0, 1)).as("-45*3 deg").isEqualTo(new Vector3(-MathUtils.cosDeg(45), 0, MathUtils.cosDeg(45)), within(0.001f));
		assertThat(TCSUtils.getCoordinatesForMetrics(-rad45*5, 0, 1)).as("-45*5 deg").isEqualTo(new Vector3(-MathUtils.cosDeg(45), 0, -MathUtils.cosDeg(45)), within(0.001f));
		assertThat(TCSUtils.getCoordinatesForMetrics(-rad45*7, 0, 1)).as("-45*7 deg").isEqualTo(new Vector3(MathUtils.cosDeg(45), 0, -MathUtils.cosDeg(45)), within(0.001f));
	}
	
	
	@Test
	void getCoordinatesForMetrics_varyingPhi()
	{
		assertThat(TCSUtils.getCoordinatesForMetrics(0, rad45*1, 1)).as("45 deg").isEqualTo(new Vector3(MathUtils.cosDeg(45), MathUtils.cosDeg(45), 0), within(0.001f));
		assertThat(TCSUtils.getCoordinatesForMetrics(0, rad45*3, 1)).as("45*3 deg").isEqualTo(new Vector3(-MathUtils.cosDeg(45), MathUtils.cosDeg(45), 0), within(0.001f));
		assertThat(TCSUtils.getCoordinatesForMetrics(0, rad45*5, 1)).as("45*5 deg").isEqualTo(new Vector3(-MathUtils.cosDeg(45), -MathUtils.cosDeg(45), 0), within(0.001f));
		assertThat(TCSUtils.getCoordinatesForMetrics(0, rad45*7, 1)).as("45*7 deg").isEqualTo(new Vector3(MathUtils.cosDeg(45), -MathUtils.cosDeg(45), 0), within(0.001f));

		assertThat(TCSUtils.getCoordinatesForMetrics(0, -rad45*1, 1)).as("-45 deg").isEqualTo(new Vector3(MathUtils.cosDeg(45), -MathUtils.cosDeg(45), 0), within(0.001f));
		assertThat(TCSUtils.getCoordinatesForMetrics(0, -rad45*3, 1)).as("-45*3 deg").isEqualTo(new Vector3(-MathUtils.cosDeg(45), -MathUtils.cosDeg(45), 0), within(0.001f));
		assertThat(TCSUtils.getCoordinatesForMetrics(0, -rad45*5, 1)).as("-45*5 deg").isEqualTo(new Vector3(-MathUtils.cosDeg(45), MathUtils.cosDeg(45), 0), within(0.001f));
		assertThat(TCSUtils.getCoordinatesForMetrics(0, -rad45*7, 1)).as("-45*7 deg").isEqualTo(new Vector3(MathUtils.cosDeg(45), MathUtils.cosDeg(45), 0), within(0.001f));
	}
	
	
	@Test
	void getCoordinatesForMetrics_varyingR()
	{
		assertThat(TCSUtils.getCoordinatesForMetrics(0, 0, 0.5f)).as("len=0.5").isEqualTo(new Vector3(0.5f, 0, 0));
		assertThat(TCSUtils.getCoordinatesForMetrics(0, 0, 1.0f)).as("len=1.0").isEqualTo(new Vector3(1.0f, 0, 0));
		assertThatThrownBy(() -> TCSUtils.getCoordinatesForMetrics(0, 0, -1.0f), "negative magnitude").isInstanceOf(IllegalArgumentException.class);
	}
	
	
	@Test
	void getMaterialForColour()
	{
		Material material = TCSUtils.getMaterialForColour(Color.RED);
		
		assertThatObject(material).isNotNull();
		assertThatObject(TCSUtils.getMaterialForColour(Color.RED)).as("same colour instance and value").isSameAs(material);
		assertThatObject(TCSUtils.getMaterialForColour(new Color(0xff0000ff))).as("diff colour instance and same value").isSameAs(material);
		assertThatObject(TCSUtils.getMaterialForColour(Color.BLUE)).as("diff colour instance and value").isNotSameAs(material);
	}
	
	
	@Test
	void getColourFromHex_validHexCodes_ColorReturned()
	{
		assertThatObject(TCSUtils.getColourFromHex("#fedcba98")).isEqualTo(new Color(0xfedcba98));
		assertThatObject(TCSUtils.getColourFromHex("fedcba98")).isEqualTo(new Color(0xfedcba98));
		assertThatObject(TCSUtils.getColourFromHex("#fedcba")).isEqualTo(new Color(0xfedcbaff));
		assertThatObject(TCSUtils.getColourFromHex("fedcba")).isEqualTo(new Color(0xfedcbaff));
		assertThatObject(TCSUtils.getColourFromHex("#fedc")).isEqualTo(new Color(0xffeeddcc));
		assertThatObject(TCSUtils.getColourFromHex("fedc")).isEqualTo(new Color(0xffeeddcc));
		assertThatObject(TCSUtils.getColourFromHex("#fed")).isEqualTo(new Color(0xffeeddff));
		assertThatObject(TCSUtils.getColourFromHex("fed")).isEqualTo(new Color(0xffeeddff));
	}
	
	
	@Test
	void getColourFromHex_invalidHexCodes_IllegalArgumentException()
	{
		assertThatThrownBy(() -> TCSUtils.getColourFromHex("#f"), "on invalid hex colour").isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> TCSUtils.getColourFromHex("f"), "on invalid hex colour").isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> TCSUtils.getColourFromHex("#fffff"), "on invalid hex colour").isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> TCSUtils.getColourFromHex("fffff"), "on invalid hex colour").isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> TCSUtils.getColourFromHex("#gggggg"), "on invalid hex colour").isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> TCSUtils.getColourFromHex("gggggg"), "on invalid hex colour").isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> TCSUtils.getColourFromHex("-2"), "on invalid hex colour").isInstanceOf(IllegalArgumentException.class);
	}
}
