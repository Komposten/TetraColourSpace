package komposten.tcs.ui;

import java.util.EnumMap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;

import komposten.tcs.backend.Backend;
import komposten.tcs.backend.Style.Colour;
import komposten.tcs.backend.data.Point;
import komposten.tcs.backend.data.PointGroup;
import komposten.tcs.backend.data.Shape;
import komposten.tcs.rendering.World;

public class UserInterface implements Disposable
{
	private static final int LEGEND_HIDE = 0;
	private static final int LEGEND_GROUPS = 1;
	private static final int LEGEND_POINTS = 2;
	private static final int LEGEND_OPTIONS = 3;
	
	private Backend backend;
	private World world;
	
	private EnumMap<Shape, Texture> shapeSprites;
	private Sprite crosshair;
	private BitmapFont font;
	
	private boolean showCrosshair = true;
	private int showLegend = 0;

	
	public UserInterface(Backend backend, World world)
	{
		this.backend = backend;
		this.world = world;
		
		font = new BitmapFont();
		font.getData().markupEnabled = true;
		
		createShapes();
		createCrosshair();
	}
	
	
	public void toggleCrosshair()
	{
		showCrosshair = !showCrosshair;
	}
	
	
	public void toggleLegend()
	{
		showLegend = (showLegend + 1) % LEGEND_OPTIONS;
	}


	private void createShapes()
	{
		shapeSprites = new EnumMap<>(Shape.class);
		shapeSprites.put(Shape.SPHERE, createLinearTexture("circle.png"));
		shapeSprites.put(Shape.PYRAMID, createLinearTexture("triangle.png"));
		shapeSprites.put(Shape.BOX, createLinearTexture("square.png"));
	}


	private Texture createLinearTexture(String path)
	{
		Texture texture = new Texture(Gdx.files.internal(path));
		texture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
		return texture;
	}


	private void createCrosshair()
	{
		Texture crosshairTexture = new Texture(Gdx.files.internal("crosshair.png"));
		crosshair = new Sprite(crosshairTexture);
		
		Color colourCrosshair = backend.getStyle().get(Colour.CROSSHAIR);
		Color colourBackground = backend.getStyle().get(Colour.BACKGROUND);
		if (colourCrosshair == null)
		{
			colourCrosshair = Color.WHITE.cpy().sub(colourBackground);
			colourCrosshair.a = 1f;
		}
		
		crosshair.setColor(colourCrosshair);
		crosshair.getColor().a = 1f;
	}
	
	
	public void resize(int width, int height)
	{
		if (crosshair != null)
		{
			crosshair.setCenter(width/2f, height/2f);
		}
	}
	
	
	public void render(SpriteBatch batch)
	{
		if (showCrosshair)
		{
			crosshair.draw(batch);
		}
		
		renderMetricText(batch);
		renderLegend(batch);
	}


	private void renderMetricText(SpriteBatch batch)
	{
		if (world.hasSelection())
		{
			Point selectedPoint = world.getSelectedPoint();
			renderTextWithSymbol(selectedPoint.getName(),
					shapeSprites.get(selectedPoint.getGroup().getShape()), 5, Gdx.graphics.getHeight() - 15f,
					5, 12, selectedPoint.getColour(), batch);
			String metrics = String.format(
					"%n  Theta: %.02f%n  Phi: %.02f%n  r: %.02f",
					selectedPoint.getMetrics().x,
					selectedPoint.getMetrics().y,
					selectedPoint.getMetrics().z);
			
			batch.setColor(backend.getStyle().get(Colour.TEXT));
			font.draw(batch, metrics, 5, Gdx.graphics.getHeight()-10f);
			batch.setColor(Color.WHITE);
		}
	}


	private void renderLegend(SpriteBatch batch)
	{
		if (showLegend != LEGEND_HIDE)
		{
			float shapeSize = 12;
			float padding = 5;
			float lineHeight = Math.max(font.getLineHeight(), shapeSize);
			float x = padding;
			float y = Gdx.graphics.getHeight() - (padding + lineHeight/2);
			
			for (PointGroup group : backend.getDataGroups())
			{
				Texture shapeTexture = shapeSprites.get(group.getShape());
				
				if (showLegend == LEGEND_POINTS)
				{
					for (Point point : group.getPoints())
					{
						renderTextWithSymbol(point.getName(), shapeTexture, x, y, padding, shapeSize, point.getColour(), batch);
						y -= lineHeight;
					}
				}
				else if (showLegend == LEGEND_GROUPS)
				{
					renderTextWithSymbol(group.getName(), shapeTexture, x, y, padding, shapeSize, group.getColour(), batch);
					y -= lineHeight;
				}
			}
		}
	}


	private void renderTextWithSymbol(String text, Texture shapeTexture, float x, float y,
			float padding, float shapeSize, Color colour, SpriteBatch batch)
	{
		batch.setColor(colour);
		batch.draw(shapeTexture, x, y - shapeSize/2 , shapeSize, shapeSize);
		batch.setColor(Color.WHITE);
		String line = String.format("[#%s]%s", colour, text);
		font.draw(batch, line, x + shapeSize + padding, y + font.getCapHeight()/2, 0, Align.left, false);
	}
	
	
	@Override
	public void dispose()
	{
		for (Texture texture : shapeSprites.values())
			texture.dispose();
		crosshair.getTexture().dispose();
	}
}
