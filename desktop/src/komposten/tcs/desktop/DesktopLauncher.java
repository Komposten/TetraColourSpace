/*
 * Copyright 2019 Jakob Hjelm
 * 
 * This file is part of TetraColourSpace.
 * 
 * TetraColourSpace is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
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
