# TetraColourSpace XML format
TetraColourSpace uses simple XML-files to describe the graphs. The different elements that may be used are described below, and there is a complete example at the end of this file.

## Elements
### Overview
- [`<data>`](#data): The root element containing the graph's data and style information.
- [`<style>`](#style): The style element containing style information.
- [`<colour>`](#colour): Set the colour of an element of the graph.
- [`<setting>`](#setting): Set a graph setting.
- [`<group>`](#group): Indicates a group of data points.
- [`<point>`](#point): Describes a data point.
- [`<volume>`](#volume): Describes a data volume.

---

### `<data>`
The root element containing the graph's data and style information. Anything that occurs outside of this element is excluded.

May only occur once.

**Attributes**

None

**Children**
- [`<style>`](#style)
- [`<group>`](#group)
- [`<volume>`](#volume)

---

### `<style>`
The style element containing style information for the visuals of the graph. This controls e.g. background colour, text colour and the colours of the four corners in the colourspace tetrahedron.

May only occur once, and only as a child of [`<data>`](#data).

**Attributes**

None

**Children**
- [`<colour>`](#colour)
- [`<setting>`](#setting)

---

### `<colour>`
Sets the colour of an element of the graph itself. It could be e.g. background colour, text colour or the colour of a corner in the colourspace tetrahedron.

May occur several times, but only as a child of [`<style>`](#style).

**Attributes**
- `id`: One of `background, text, crosshair, wl_long, wl_medium, wl_short, wl_uv, achro, metric_line, metric_fill, highlight, or selection`

**Value**

A hexadecimal colour value in the format `#AARRGGBB` or `#RRGGBB`.

**Example**

```xml
<colour id="background">#FFF</colour>
```

---

### `<setting>`
Sets a setting for the graph. It could be e.g. sphere quality or default data point size.

May occur several times, but only as a child of [`<style>`](#style).

**Attributes**
- `id`: One of `point_size, corner_size, or sphere_quality`.

**Value**

A positive, non-zero integer or floating-point value. `sphere_quality` is floored to the nearest integer.

**Example**

```xml
<setting id="point_size">0.03</colour>
```


---

### `<group>`
Indicates a group of data points, comparable with a "data series" in e.g. excel.

May occur several times, but only as a child of [`<data>`](#data).

**Attributes**
- `name`: The name of the group. Appears in the legend.
- `shape`: The name of the shape to use for all points in the group. One of `box, sphere or pyramid` (or `15, 16 or 17`, from R's plot character types).

**Children**
- [`<point>`](#point)

---

### `<point/>`
Describes a data point.

May occur several times, but only as a child of [`<group>`](#group).

**Attributes**
- `name`: The name of the point. Appears in the legend and when selected.
- `colour`: The colour of the point in the format `#AARRGGBB` or `#RRGGBB`.
- `position`: The position of the point in the format `theta,phi,magnitude`; where `theta` is the angle in the XZ-plane from the x-axis, `phi` the angle up/down from the XZ-plane, and `magnitude` the distance from the origin. Floating-point values should use `.` as decimal separator, and may use scientific notation.

**Example**

```xml
<point name="Point1" colour="#FFFFFF" position="-0.33,-0.41,0.51"/>
```

---

### `<volume>`
Describes a data point. A quick hull algorithm is applied to the points to generate the volume polygon, discarding all points that are not at the volume's edge. Thus, it is fine to pass entire point clouds as points to a volume element.

May occur several times, but only as a child of [`<data>`](#data).

**Attributes**
- `colour`: The colour of the volume in the format `#AARRGGBB` or `#RRGGBB`.

**Value**

A list of data points formatted as `point's position` attribute, with one row per point.

**Example**

```xml
<volume colour="#FF0000">
  -0.33,-0.41,0.51
  -0.34,-0.39,0.49
  -0.38,-0.34,0.29
  -0.23,-0.60,0.22
</volume>
```

---
## Complete example
```xml
<?xml version="1.0"?>
<data>
  <style>
    <colour id="background">#FFFFFF</colour>
    <colour id="wl_medium">#77AAFF</colour>
  </style>
  <group name="Group1" shape="sphere">
    <point name="Point1" colour="#FFFFFF" position="-0.33,-0.41,0.51"/>
    <point name="Point2" colour="#FFFFFF" position="-0.31,-0.39,0.55"/>
  </group>
  <group name="Group2" shape="box">
    <point name="Point3" colour="#FF00FF" position="-0.56,-0.43,0.31"/>
    <point name="Point4" colour="#FF00FF" position="-0.59,-0.42,0.28"/>
  </group>
  <volume colour="#FF0000">
    -0.33,-0.41,0.51
    -0.34,-0.39,0.49
    -0.38,-0.34,0.29
    -0.23,-0.60,0.22
  </volume>
</data>
```