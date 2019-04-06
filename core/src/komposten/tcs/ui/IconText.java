package komposten.tcs.ui;

import java.util.EnumMap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;

import komposten.tcs.backend.data.Shape;

public class IconText
{
	private static EnumMap<Shape, Texture> shapeSprites;


	public static void createIcons()
	{
		shapeSprites = new EnumMap<>(Shape.class);
		shapeSprites.put(Shape.SPHERE, createLinearTexture("circle.png"));
		shapeSprites.put(Shape.PYRAMID, createLinearTexture("triangle.png"));
		shapeSprites.put(Shape.BOX, createLinearTexture("square.png"));
	}


	private static Texture createLinearTexture(String path)
	{
		Texture texture = new Texture(Gdx.files.internal(path));
		texture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
		return texture;
	}
	
	
	public static void dispose()
	{
		for (Texture texture : shapeSprites.values())
			texture.dispose();
	}
	
	
	private String text;
	private float x;
	private float y;
	private float iconSize;
	private Shape icon;
	private Color colour;
	
	
	public IconText()
	{
		this("", 0, 0, 0, null, Color.WHITE);
	}
	
	
	public IconText(String text, float x, float y, float iconSize, Shape icon, Color colour)
	{
		this.text = text;
		this.x = x;
		this.y = y;
		this.iconSize = iconSize;
		this.icon = icon;
		this.colour = colour;
	}
	
	
	public void setText(String text)
	{
		this.text = text;
	}


	public void setX(float x)
	{
		this.x = x;
	}


	public void setY(float y)
	{
		this.y = y;
	}


	public void setIconSize(float iconSize)
	{
		this.iconSize = iconSize;
	}


	public void setIcon(Shape icon)
	{
		this.icon = icon;
	}


	public void setColour(Color colour)
	{
		this.colour = colour;
	}


	public void render(SpriteBatch batch, BitmapFont font)
	{
		float textX = x;
		
		if (icon != null)
		{
			Texture iconTexture = shapeSprites.get(icon);
			int padding = 5;
			
			batch.setColor(colour);
			batch.draw(iconTexture, x, y - iconSize/2 , iconSize, iconSize);
			batch.setColor(Color.WHITE);
			
			textX += iconSize + padding;
		}
		
		String line = String.format("[#%s]%s", colour, text);
		font.draw(batch, line, textX, y + font.getCapHeight()/2, 0, Align.left, false);
	}
}
