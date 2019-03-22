package komposten.tcs.backend.data;

public enum Shape
{
	SPHERE(16),
	PYRAMID(17),
	BOX(15);
	
	private int value;

	private Shape(int value)
	{
		this.value = value;
	}
	
	public static Shape fromString(String string)
	{
		for (Shape shape : values())
		{
			if (string.equalsIgnoreCase(shape.name()) ||
					string.equals(Integer.toString(shape.value)))
				return shape;
		}
		
		return null;
	}
}