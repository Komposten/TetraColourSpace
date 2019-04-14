package komposten.tcs.input;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputProcessor;

import komposten.utilities.data.InputMapper;


public class InputHandler implements InputProcessor
{
	public interface InputListener
	{
		public boolean onActionStarted(Action action, Object... parameters);
		public boolean onActionStopped(Action action, Object... parameters);
	}
	
	private InputMapper<Action> mapper;
	private List<InputListener> listeners;


	public InputHandler()
	{
		mapper = new InputMapper<>();
		listeners = new ArrayList<>();
		
		defaultMappings();
	}


	private void defaultMappings()
	{
		mapper.registerKey(Keys.W, Action.MOVE_FORWARD);
		mapper.registerKey(Keys.S, Action.MOVE_BACKWARD);
		mapper.registerKey(Keys.A, Action.MOVE_LEFT);
		mapper.registerKey(Keys.D, Action.MOVE_RIGHT);
		mapper.registerKey(Keys.SPACE, Action.MOVE_UP);
		mapper.registerKey(Keys.CONTROL_LEFT, Action.MOVE_DOWN);
		mapper.registerKey(Keys.CONTROL_RIGHT, Action.MOVE_DOWN);
		mapper.registerKey(Keys.E, Action.MOVE_IN);
		mapper.registerKey(Keys.Q, Action.MOVE_OUT);
		mapper.registerMouseWheel(-1, Action.ZOOM);
		mapper.registerMouseWheel(1, Action.ZOOM);
		mapper.registerKey(Keys.SHIFT_LEFT, Action.REDUCE_SPEED);
		mapper.registerKey(Keys.SHIFT_RIGHT, Action.REDUCE_SPEED);
		mapper.registerKey(getMouseButtonCode(Buttons.RIGHT), Action.CATCH_MOUSE);
		mapper.registerKey(getMouseButtonCode(Buttons.LEFT), Action.SELECT_POINT);
		mapper.registerKey(Keys.G, Action.FOLLOW_ORIGIN);
		mapper.registerKey(Keys.HOME, Action.FOLLOW_ORIGIN_HOLD);
		mapper.registerKey(Keys.F, Action.FOLLOW_SELECTION);
		mapper.registerKey(Keys.R, Action.TOGGLE_ROTATION);
		mapper.registerKey(Keys.STAR, Action.ROTATION_DIRECTION);
		mapper.registerKey(Keys.PLUS, Action.ROTATION_SPEED_INCREMENT);
		mapper.registerKey(Keys.MINUS, Action.ROTATION_SPEED_DECREMENT);
		mapper.registerKey(Keys.NUMPAD_1, Action.ROTATION_SPEED_1);
		mapper.registerKey(Keys.NUMPAD_2, Action.ROTATION_SPEED_2);
		mapper.registerKey(Keys.NUMPAD_3, Action.ROTATION_SPEED_3);
		mapper.registerKey(Keys.NUMPAD_4, Action.ROTATION_SPEED_4);
		mapper.registerKey(Keys.NUMPAD_5, Action.ROTATION_SPEED_5);
		mapper.registerKey(Keys.NUMPAD_6, Action.ROTATION_SPEED_6);
		mapper.registerKey(Keys.NUMPAD_7, Action.ROTATION_SPEED_7);
		mapper.registerKey(Keys.NUMPAD_8, Action.ROTATION_SPEED_8);
		mapper.registerKey(Keys.NUMPAD_9, Action.ROTATION_SPEED_9);
		mapper.registerKey(Keys.NUMPAD_1, Action.CAMERA_PRESET_1);
		mapper.registerKey(Keys.NUMPAD_2, Action.CAMERA_PRESET_2);
		mapper.registerKey(Keys.NUMPAD_3, Action.CAMERA_PRESET_3);
		mapper.registerKey(Keys.H, Action.TOGGLE_HIGHLIGHT);
		mapper.registerKey(Keys.NUM_1, Action.TOGGLE_POINTS);
		mapper.registerKey(Keys.NUM_2, Action.TOGGLE_VOLUMES);
		mapper.registerKey(Keys.T, Action.TOGGLE_TETRAHEDRON_SIDES);
		mapper.registerKey(Keys.Y, Action.TOGGLE_AXES);
		mapper.registerKey(Keys.M, Action.TOGGLE_METRICS);
		mapper.registerKey(Keys.C, Action.TOGGLE_CROSSHAIR);
		mapper.registerKey(Keys.L, Action.TOGGLE_LEGEND);
		mapper.registerKey(Keys.F12, Action.SCREENSHOT);
	}


	public void addListener(InputListener listener)
	{
		listeners.add(listener);
	}


	public void removeListener(InputListener listener)
	{
		listeners.remove(listener);
	}


	@Override
	public boolean keyDown(int keycode)
	{
		List<Action> mappings = mapper.getKeyMappings(keycode);

		return sendActionStarted(mappings);
	}


	@Override
	public boolean keyUp(int keycode)
	{
		List<Action> mappings = mapper.getKeyMappings(keycode);

		return sendActionStopped(mappings);
	}


	@Override
	public boolean keyTyped(char character)
	{
		return false;
	}


	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button)
	{
		button = getMouseButtonCode(button);

		List<Action> mappings = mapper.getKeyMappings(button);

		return sendActionStarted(mappings);
	}


	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button)
	{
		button = getMouseButtonCode(button);

		List<Action> mappings = mapper.getKeyMappings(button);

		return sendActionStopped(mappings);
	}


	private int getMouseButtonCode(int button)
	{
		if (button == Buttons.LEFT)
			button = InputMapper.MOUSE1;
		else if (button == Buttons.RIGHT)
			button = InputMapper.MOUSE2;
		else if (button == Buttons.MIDDLE)
			button = InputMapper.MOUSE3;
		return button;
	}


	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer)
	{
		return false;
	}


	@Override
	public boolean mouseMoved(int screenX, int screenY)
	{
		return false;//sendMoveAction(screenX, screenY);
	}


	@Override
	public boolean scrolled(int amount)
	{
		int direction = InputMapper.getMouseWheelCode(amount);

		List<Action> mappings = mapper.getKeyMappings(direction);

		return sendActionStopped(mappings, amount);
	}


	private boolean sendActionStarted(List<Action> actions, Object... parameters)
	{
		for (Action action : actions)
		{
			for (InputListener listener : listeners)
			{
				if (listener.onActionStarted(action, parameters))
					return true;
			}
		}

		return false;
	}


	private boolean sendActionStopped(List<Action> actions, Object... parameters)
	{
		for (Action action : actions)
		{
			for (InputListener listener : listeners)
			{
				if (listener.onActionStopped(action, parameters))
					return true;
			}
		}

		return false;
	}
	
	
//	private boolean sendMoveAction(int deltaX, int deltaY)
//	{
//		for (InputListener listener : listeners)
//		{
//			if (listener.onMouseMoved(deltaX, deltaY))
//				return true;
//		}
//		
//		return false;
//	}
}
