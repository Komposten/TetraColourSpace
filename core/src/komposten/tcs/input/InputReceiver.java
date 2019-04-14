package komposten.tcs.input;

public interface InputReceiver
{
	public void register(InputHandler handler);
	public void unregister(InputHandler handler);
}
