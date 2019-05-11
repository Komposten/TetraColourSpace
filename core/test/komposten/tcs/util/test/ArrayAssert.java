package komposten.tcs.util.test;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

import com.badlogic.gdx.utils.Array;

public class ArrayAssert<T> extends AbstractAssert<ArrayAssert<T>, Array<T>>
{
	public ArrayAssert(Array<T> actual)
	{
		super(actual, ArrayAssert.class);
	}
	
	
	public static <T> ArrayAssert<T> assertThat(Array<T> actual)
	{
		return new ArrayAssert<>(actual);
	}
	
	
	public ArrayAssert<T> hasSize(int size)
	{
		isNotNull();
		Assertions.assertThat(actual.size).isEqualTo(size);
		
		return this;
	}
}
