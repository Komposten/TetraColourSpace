package komposten.tcs.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.badlogic.gdx.graphics.Color;

import komposten.tcs.backend.Style.Colour;
import komposten.tcs.backend.Style.Setting;

class StyleTest
{
	private Node createStyleNode(Object[] styles, String[] values, boolean allowInvalidStyle)
	{
		if (styles.length != values.length)
		{
			throw new IllegalArgumentException("styles and values must have the same length!");
		}
		
		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			Document document = factory.newDocumentBuilder().newDocument();
			
			Element style = document.createElement("style");
			
			for (int i = 0; i < styles.length; i++)
			{
				Object object = styles[i];
				
				Element child;
				String id;
				if (object instanceof Colour)
				{
					child = document.createElement("colour");
					id = ((Colour)object).name();
				}
				else if (object instanceof Setting)
				{
					child = document.createElement("setting");
					id = ((Setting)object).name();
				}
				else if (allowInvalidStyle)
				{
					child = document.createElement(object.toString());
					id = object.toString();
				}
				else
				{
					throw new IllegalArgumentException(object + " is not a valid style property!");
				}
				
				child.setAttribute("id", id);
				child.setTextContent(values[i]);
				
				style.appendChild(child);
			}
			
			return style;
		}
		catch (ParserConfigurationException e)
		{
			throw new RuntimeException(e);
		}
	}


	@Test
	void constructor_defaultsLoaded()
	{
		Style style = new Style();
		
		for (Colour colour : Colour.values())
			assertThat(style.has(colour)).as(colour.name()).isTrue();
		
		for (Setting setting : Setting.values())
			assertThat(style.has(setting)).as(setting.name()).isTrue();
	}
	
	
	@Test
	void loadStyle_validColours_stylesAdded()
	{
		Object[] styles = {
				Colour.WL_SHORT,
				Colour.WL_MEDIUM,
				Colour.WL_LONG,
				Colour.WL_UV,
				Colour.ACHRO
		};
		String[] values = {
				"#1289abef",
				"1289abef",
				"1289ab",
				"19ae",
				"19a"
		};
		Node node = createStyleNode(styles, values, false);
		Style style = new Style(node);
		
		assertThat(style.get(Colour.WL_SHORT)).isEqualTo(new Color(0x1289abef));
		assertThat(style.get(Colour.WL_MEDIUM)).isEqualTo(new Color(0x1289abef));
		assertThat(style.get(Colour.WL_LONG)).isEqualTo(new Color(0x1289abff));
		assertThat(style.get(Colour.WL_UV)).isEqualTo(new Color(0x1199aaee));
		assertThat(style.get(Colour.ACHRO)).isEqualTo(new Color(0x1199aaff));
	}
	
	
	@Test
	void loadStyle_settings_stylesAdded()
	{
		Object[] styles = { Setting.CORNER_SIZE, Setting.POINT_SIZE };
		String[] values = { "0.01", "0.10" };
		
		Node node = createStyleNode(styles, values, false);
		Style style = new Style(node);

		assertThat(style.get(Setting.CORNER_SIZE).floatValue()).isEqualTo(0.01f, within(0.00001f));
		assertThat(style.get(Setting.POINT_SIZE).floatValue()).isEqualTo(0.10f, within(0.00001f));
	}
	
	
	@Test
	void loadStyle_zeroOrNegativeSize_exception()
	{
		Object[] styles = { Setting.CORNER_SIZE };
		String[] values = { "0" };
		assertThatIllegalArgumentException().as("zero corner size")
				.isThrownBy(() -> new Style(createStyleNode(styles, values, false)));

		values[0] = "-1";
		assertThatIllegalArgumentException().as("negative corner size")
				.isThrownBy(() -> new Style(createStyleNode(styles, values, false)));
		
		styles[0] = Setting.POINT_SIZE;
		assertThatIllegalArgumentException().as("negative point size")
				.isThrownBy(() -> new Style(createStyleNode(styles, values, false)));

		values[0] = "0";
		assertThatIllegalArgumentException().as("zero point size")
				.isThrownBy(() -> new Style(createStyleNode(styles, values, false)));
	}
	
	
	@Test
	void loadStyle_sphereQualityLessThan5_clampTo5()
	{
		Object[] styles = { Setting.SPHERE_QUALITY };
		String[] values = { "4" };
		assertThat(new Style(createStyleNode(styles, values, false))
				.get(Setting.SPHERE_QUALITY).intValue()).as("4 sphere quality")
						.isEqualTo(5);

		values[0] = "-1";
		assertThat(new Style(createStyleNode(styles, values, false))
				.get(Setting.SPHERE_QUALITY).intValue()).as("negative sphere quality")
						.isEqualTo(5);

		values[0] = "6";
		assertThat(new Style(createStyleNode(styles, values, false))
				.get(Setting.SPHERE_QUALITY).intValue()).as("positive sphere quality")
						.isEqualTo(6);
	}
	
	
	@Test
	void loadStyle_renderModes()
	{
		Object[] styles = { Setting.RENDER_MODE };
		String[] values = { "" + Style.RENDER_MODE_FAST };
		assertThat(new Style(createStyleNode(styles, values, false))
				.get(Setting.RENDER_MODE).intValue()).as("render mode fast number").isEqualTo(Style.RENDER_MODE_FAST);

		values[0] = "" + Style.RENDER_MODE_SLOW;
		assertThat(new Style(createStyleNode(styles, values, false))
				.get(Setting.RENDER_MODE).intValue()).as("render mode slow number").isEqualTo(Style.RENDER_MODE_SLOW);

		values[0] = "fast";
		assertThat(new Style(createStyleNode(styles, values, false))
				.get(Setting.RENDER_MODE).intValue()).as("render mode fast text").isEqualTo(Style.RENDER_MODE_FAST);

		values[0] = "slow";
		assertThat(new Style(createStyleNode(styles, values, false))
				.get(Setting.RENDER_MODE).intValue()).as("render mode slow text").isEqualTo(Style.RENDER_MODE_SLOW);

		values[0] = "gibberish";
		assertThatIllegalArgumentException().as("render mode gibberish")
				.isThrownBy(() -> new Style(createStyleNode(styles, values, false)));

		values[0] = "-1";
		assertThatIllegalArgumentException().as("render mode negative")
				.isThrownBy(() -> new Style(createStyleNode(styles, values, false)));
	}
	
	
	@Test
	void loadStyle_nonElementNode_ignore() throws ParserConfigurationException
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		Document document = factory.newDocumentBuilder().newDocument();
		Element style = document.createElement("style");
		style.appendChild(document.createComment("Comment!"));
		
		assertThatCode(() -> new Style(style)).as("comment in style XML").doesNotThrowAnyException();
	}
	
	
	@Test
	void loadStyle_invalidElementType_ignore()
	{
		Object[] styles = { "Cookie" };
		String[] values = { "Chocolate chip" };
		
		Node styleNode = createStyleNode(styles, values, true);
		assertThatCode(() -> new Style(styleNode))
				.as("cookie element in style XML").doesNotThrowAnyException();
		
		Style style = new Style(styleNode);
		assertThat(style.getChangedColours()).as("invalid element; should not change the style").isEmpty();
		assertThat(style.getChangedSettings()).as("invalid element; should not change the style").isEmpty();
	}
	
	
	@Test
	void loadStyle_missingId_ignore()
	{
		Object[] styles = { Colour.BACKGROUND };
		String[] values = { "#135" };
		
		Node styleNode = createStyleNode(styles, values, false);
		styleNode.getFirstChild().getAttributes().removeNamedItem("id");
		
		assertThatCode(() -> new Style(styleNode)).as("no ID attribute").doesNotThrowAnyException();
		
		Style style = new Style(styleNode);
		assertThat(style.getChangedColours()).as("missing id; should not change the style").isEmpty();
		assertThat(style.getChangedSettings()).as("missing id; should not change the style").isEmpty();
	}
	
	
	@Test
	void loadStyle_invalidId_ignore()
	{
		Object[] styles = { Colour.BACKGROUND, Setting.CORNER_SIZE };
		String[] values = { "#135", "0.1" };
		
		Node styleNode = createStyleNode(styles, values, false);
		styleNode.getFirstChild().getAttributes().getNamedItem("id").setTextContent("cookie");
		styleNode.getLastChild().getAttributes().getNamedItem("id").setTextContent("cake");
		
		assertThatCode(() -> new Style(styleNode)).as("invalid ID attributes").doesNotThrowAnyException();
		
		Style style = new Style(styleNode);
		assertThat(style.getChangedColours()).as("invalid id; should not change the style").isEmpty();
		assertThat(style.getChangedSettings()).as("invalid id; should not change the style").isEmpty();
	}
	
	
	@Test
	void getChangedColours()
	{
		Object[] styles = { Colour.TEXT, Colour.HIGHLIGHT };
		String[] values = { "#123", "#321"};
		
		Style style = new Style(createStyleNode(styles, values, false));
		Map<Colour, Color> changes = style.getChangedColours();
		
		assertThat(changes).hasSize(2).containsKeys(Colour.TEXT, Colour.HIGHLIGHT);
		assertThat(changes.get(Colour.TEXT).toString()).isEqualTo("112233ff");
		assertThat(changes.get(Colour.HIGHLIGHT).toString()).isEqualTo("332211ff");
	}
	
	
	@Test
	void getChangedColours_changedToPrevious_dontCountAsChanged()
	{
		Object[] styles = { Colour.WL_LONG };
		String[] values = { "#f00" };
		
		assertThat(new Style(createStyleNode(styles, values, false)).getChangedColours())
			.isEmpty();;
	}
	
	
	@Test
	void getChangedSettings()
	{
		Object[] styles = { Setting.CORNER_SIZE, Setting.RENDER_MODE };
		String[] values = { "1f", "slow"};
		
		Style style = new Style(createStyleNode(styles, values, false));
		Map<Setting, Number> changes = style.getChangedSettings();
		
		assertThat(changes).hasSize(2).containsKeys(Setting.CORNER_SIZE, Setting.RENDER_MODE);
		assertThat(changes.get(Setting.CORNER_SIZE).floatValue()).isCloseTo(1f, within(0.01f));
		assertThat(changes.get(Setting.RENDER_MODE).intValue()).isEqualTo(Style.RENDER_MODE_SLOW);
	}
	
	
	@Test
	void getChangedSettings_changedToPrevious_dontCountAsChanged()
	{
		Object[] styles = { Setting.RENDER_MODE };
		String[] values = { "fast" };
		
		assertThat(new Style(createStyleNode(styles, values, false)).getChangedSettings())
			.isEmpty();;
	}
	
	
	@Test
	void getNumberFromString_notNumberNotRenderMode_exception()
	{
		Object[] styles = { Setting.CORNER_SIZE };
		String[] values = { "gibberish" };

		assertThatIllegalArgumentException()
				.isThrownBy(() -> new Style(createStyleNode(styles, values, false)));
	}
}
