package komposten.tcs.desktop;

import java.io.File;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

import komposten.tcs.TetraColourSpace;


public class DesktopLauncher
{
	public static void main(String[] arg)
	{
		if (arg.length > 0)
		{
			File file = new File(arg[0]);
			LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
			config.useGL30 = true;
			config.samples = 8;
			new LwjglApplication(new TetraColourSpace(file), config);
		}
		else
		{
			System.out.println("Usage: [filePath]");
		}
	}
}
