package komposten.tcs.util.test;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.mockito.Mockito;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.graphics.GL20;

public class GdxTest
{
	protected static Application application;
	
	@BeforeAll
	private static void initAll()
	{
		application = new HeadlessApplication(new ApplicationListener()
		{
			@Override
			public void resume() {}
			
			@Override
			public void resize(int width, int height) {}
			
			@Override
			public void render() {}
			
			@Override
			public void pause() {}
			
			@Override
			public void dispose() {}
			
			@Override
			public void create() {}
		});
		
		Gdx.gl20 = Mockito.mock(GL20.class);
		Gdx.gl = Gdx.gl20;
	}
	
	
	@AfterAll
	private static void dispose()
	{
		application.exit();
		application = null;
	}
}
