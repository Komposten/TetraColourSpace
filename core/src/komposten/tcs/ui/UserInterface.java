package komposten.tcs.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;

import komposten.tcs.backend.Backend;
import komposten.tcs.backend.Style.Colour;
import komposten.tcs.backend.data.Point;
import komposten.tcs.input.InputReceiver;
import komposten.tcs.rendering.World;

public class UserInterface implements Disposable, InputReceiver
{
	private static final int LEGEND_HIDE = 0;
	private static final int LEGEND_POINTS = 2;
	private static final int LEGEND_OPTIONS = 3;
	
	private Backend backend;
	private World world;
	private LegendRenderable legend;
	
	private Sprite crosshair;
	private BitmapFont font;

	private IconText metricHeader;
	private IconText metricBody;
	
	private boolean showCrosshair = true;
	private int showLegend = 0;

	
	public UserInterface(Backend backend, World world)
	{
		IconText.createIcons();
		
		this.backend = backend;
		this.world = world;
		
		font = new BitmapFont();
		font.getData().markupEnabled = true;
		
		legend = new LegendRenderable(backend, font);
		
		createMetricLabels();
		createCrosshair();
	}
	
	
	@Override
	public void register(InputMultiplexer multiplexer)
	{
		multiplexer.addProcessor(inputProcessor);
	}
	
	
	@Override
	public void unregister(InputMultiplexer multiplexer)
	{
		multiplexer.removeProcessor(inputProcessor);
	}

	private void createMetricLabels()
	{
		metricHeader = new IconText();
		metricHeader.setX(5);
		metricHeader.setIconSize(12);
		
		metricBody = new IconText();
		metricBody.setX(5);
		metricBody.setIconSize(12);
		metricBody.setColour(backend.getStyle().get(Colour.TEXT));
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
		if (world.hasSelection() && world.pointsVisible())
		{
			Point selectedPoint = world.getSelectedPoint();
			String metrics = String.format("  Theta: %.02f%n  Phi: %.02f%n  r: %.02f",
					selectedPoint.getMetrics().x,
					selectedPoint.getMetrics().y,
					selectedPoint.getMetrics().z);
			
			metricHeader.setText(selectedPoint.getName());
			metricHeader.setY(Gdx.graphics.getHeight() - 15f);
			metricHeader.setIcon(selectedPoint.getGroup().getShape());
			metricHeader.setColour(selectedPoint.getColour());
			metricHeader.render(batch, font);
			
			metricBody.setText(metrics);
			metricBody.setY(Gdx.graphics.getHeight() - 15f - font.getLineHeight());
			metricBody.render(batch, font);
		}
	}


	private void renderLegend(SpriteBatch batch)
	{
		if (showLegend != LEGEND_HIDE)
		{
			legend.render(batch, font, showLegend == LEGEND_POINTS);
		}
	}
	
	
	@Override
	public void dispose()
	{
		IconText.dispose();
		crosshair.getTexture().dispose();
	}
	
	
	private InputProcessor inputProcessor = new InputAdapter()
	{
		@Override
		public boolean keyUp(int keycode)
		{
			if (keycode == Keys.C)
			{
				showCrosshair = !showCrosshair;
				return true;
			}
			else if (keycode == Keys.L)
			{
				showLegend = (showLegend + 1) % LEGEND_OPTIONS;
				return true;
			}
			
			return false;
		}
	};
}
