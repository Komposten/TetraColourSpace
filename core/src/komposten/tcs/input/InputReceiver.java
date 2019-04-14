package komposten.tcs.input;

public interface InputReceiver
{
	public void attachToInputHandler(InputHandler handler);
	public void detachFromInputHandler(InputHandler handler);
}
