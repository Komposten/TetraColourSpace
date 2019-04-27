package komposten.tcs.desktop;

import java.io.File;

import com.badlogic.gdx.Files.FileType;
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
			File outputDir = (arg.length > 1 ? new File(arg[1]) : null);
			LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
			config.useGL30 = true;
			config.samples = 8;
			config.addIcon("icon128.png", FileType.Internal);
			config.addIcon("icon32.png", FileType.Internal);
			new LwjglApplication(new TetraColourSpace(file, outputDir), config);
		}
		else
		{
			System.out.println("Usage: <filePath> [outputPath]");
			System.out.println("\t<filePath> Path to an XML graph file.");
			System.out.println("\t[outputPath] Path to a folder to save screenshots in.");
		}
	}
}
