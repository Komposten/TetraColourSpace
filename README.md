# TetraColourSpace
### What is TetraColourSpace?
TetraColourSpace is a 3D graph program for visualising data points a tetrachromatic colourspace. It was originally created to be used in conjunction with the R package [pavo](https://CRAN.R-project.org/package=pavo), as pavo's built-in 3D graph had very limited functionality (and annoying controls).

Datasets containing information about data points are loaded from XML-files and displayed within the confines of a tetrahedron (representing a tetrachromatic colourspace). An [R-script](R/tcs_plot.R) with functions for generating correctly formatted XML-files from pavo's `colspace` objects is included in the R folder.

### Features
- Visualise groups of data points within a tetrachromatic colour space.
  - Colour the data points individually.
  - Use different 3D-models for different data sets.
- Visualise volumes filled by data points.
- Display simple legends (by data point or group).
- Move the camera around freely and intuitively.
- Lock the camera towards a point (or the achromatic centre) to rotate around it.
- Select points to display their names and colour metrics (theta, phi and r).
- Show or hide graph elements:
  - The points and volumes (separately)
  - The selected point's metrics
  - The legend
  - The axes
  - The crosshair
  - Change the tetrahedron between a wire-frame and semi-transparent shape.
- Customise the colours of graph elements:
  - The background
  - The colourspace tetrahedron's corners and achromatic centre
  - Text
  - The metric visualisation
  - The crosshair
  - The highlight and selection
- Configurable key bindings.
- Take screenshots from within the program.

 Screenshot 1 |Screenshot 2 |Screenshot 3 
--- | --- | ---
![Screenshot 1](../assets/screenshots/screenshot-1.png?raw=true)|![Screenshot 2](../assets/screenshots/screenshot-2?raw=true)|![Screenshot 3](../assets/screenshots/screenshot-3.png?raw=true)

### Running TetraColourSpace
**Download a pre-built version**
1) Download an existing [release](https://github.com/Komposten/TetraColourSpace/releases).
2) Extract the .zip archive.

**Build the latest version using Gradle**
1) Clone the repository.
2) Open a command prompt and navigate to the repository's root folder.
3) Run `gradlew.bat desktop:dist` or `gradlew desktop:dist`. Requires a JDK.
4) Find the .zip archive with the runnable .jar in `desktop/build/distributions`.

**Running**
1) Run the jar file using `javaw -jar TetraColourSpace-[VERSION].jar <xml-file> [screenshot-folder]` or by double-clicking it.
- `xml-file` is a path to the graph XML-file to load.
- `screenshot-folder` is a path to a folder to store screenshots in (defaults to `/output`).

### Dependencies
* [LibGDX](https://libgdx.badlogicgames.com)
* [Quickhull3D](https://github.com/Quickhull3d/quickhull3d)
* [SLF4J](https://www.slf4j.org)
* [Komposten's Utilities](https://github.com/Komposten/Utilities)

### XML-file format
The basic format is outlined below. Point positions are given as `theta, phi, magnitude`. More details can be found in [XML_FORMAT.md](XML-FORMAT.md).
```xml
<?xml version="1.0"?>
<data>
  <group name="Group1" shape="sphere">
    <point name="Point1" colour="#FFFFFF" position="-0.33,-0.41,0.51"/>
  </group>
  <volume colour="#FF0000">
  -0.33,-0.41,0.51
  -0.34,-0.39,0.49
  -0.38,-0.34,0.29
  -0.23,-0.60,0.22
  </volume>
</data>
```

### Keybindings
Key-bindings are set using a `config.ini` file, placed next to the .jar-file. See [`desktop/config.ini`](desktop/config.ini) for a list of all commands. Available key-codes can be found here: [LibGDX key strings](https://github.com/libgdx/libgdx/blob/master/gdx/src/com/badlogic/gdx/Input.java#L253).

If the config file is missing, the following defaults are used:

**Free camera control**
- `W/A/S/D`: forward, backward, left and right along the XZ-plane
- `Space/Ctrl`: up and down
- `E/Q`: move in/out (i.e. in the direction the camera is looking)
- `Right mouse (drag)`: rotate the camera
- `Shift`: reduce movement speed
- `Left mouse (click)`: select highlighted point

**Locking the camera**
- `F`: toggle lock towards the current selection
- `G`: toggle lock towards the achromatic centre/origin
- `Home`: hold to lock towards the achromatic centre/origin

**Locked camera controls**
- `A/D/Space/Ctrl`: move left, right, up and down around the focal point
- `W/E/S/Q/mouse wheel`: move towards/away from the focal point
- `Right mouse (drag)`: rotate around the focal point
- `Shift`: reduce movement speed

**Auto-rotation (only in locked camera mode)**
- `R`: toggle auto-rotation on/off
- `R + *`: reverse rotation direction
- `R + +`: increase rotation speed
- `R + -`: decrease rotation speed
- `R + numpad 1-9`: set rotation speed (1-9)

**Toggle elements**
- `H`: toggle highlight
- `1`: toggle points
- `2`: toggle volumes
- `T`: toggle tetrahedron sides
- `Y`: toggle axes
- `M`: toggle metrics (requires selected point)
- `C`: toggle crosshair
- `L`: toggle legend

**Other**
- `F12`: take screenshot

### License
This program is free software as long as the terms of the GNU GPL v3 license (or later versions, at your option) are complied with. See [LICENSE](LICENSE) for the full license text.

I reserve the exclusive right to re-license the code.