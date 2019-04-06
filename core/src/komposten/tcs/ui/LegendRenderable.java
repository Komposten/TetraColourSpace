package komposten.tcs.ui;

import java.util.LinkedList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import komposten.tcs.backend.Backend;
import komposten.tcs.backend.data.Point;
import komposten.tcs.backend.data.PointGroup;

public class LegendRenderable
{
	private Backend backend;
	private List<IconText> pointTextElements;
	private List<IconText> groupTextElements;


	public LegendRenderable(Backend backend, BitmapFont font)
	{
		this.backend = backend;
		this.pointTextElements = new LinkedList<>();
		this.groupTextElements = new LinkedList<>();
		
		initialise(font);
	}
	
	
	private void initialise(BitmapFont font)
	{
		float shapeSize = 12;
		float padding = 5;
		float lineHeight = Math.max(font.getLineHeight(), shapeSize);
		float x = padding;
		float yP = Gdx.graphics.getHeight() - (padding + lineHeight/2);
		float yG = Gdx.graphics.getHeight() - (padding + lineHeight/2);
		
		for (PointGroup group : backend.getDataGroups())
		{
			for (Point point : group.getPoints())
			{
				pointTextElements.add(new IconText(point.getName(), x, yP, shapeSize, group.getShape(), point.getColour()));
				yP -= lineHeight;
			}
			
			groupTextElements.add(new IconText(group.getName(), x, yG, shapeSize, group.getShape(), group.getColour()));
			yG -= lineHeight;
		}
	
	}
	
	
	public void render(SpriteBatch batch, BitmapFont font, boolean perPoint)
	{
		List<IconText> toRender = (perPoint ? pointTextElements : groupTextElements);
		
		for (IconText iconText : toRender)
			iconText.render(batch, font);
	}
}
