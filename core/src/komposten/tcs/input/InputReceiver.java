package komposten.tcs.input;

import com.badlogic.gdx.InputMultiplexer;

public interface InputReceiver
{
	public void register(InputMultiplexer multiplexer);
	public void unregister(InputMultiplexer multiplexer);
}
