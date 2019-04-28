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
package komposten.tcs.input;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputProcessor;

import komposten.utilities.data.InputMapper;
import komposten.utilities.logging.Level;
import komposten.utilities.logging.Logger;
import komposten.utilities.tools.FileOperations;


public class InputHandler implements InputProcessor
{
	public interface InputListener
	{
		public boolean onActionStarted(Action action, Object... parameters);
		public boolean onActionStopped(Action action, Object... parameters);
	}
	
	private Logger logger;
	private InputMapper<Action> mapper;
	private List<InputListener> listeners;
	
	private Set<Action> activeActions;
	
	
	public InputHandler(Logger logger)
	{
		this((File)null, logger);
	}


	public InputHandler(String keyConfigPath, Logger logger)
	{
		this(new File(keyConfigPath), logger);
	}


	public InputHandler(File keyConfigFile, Logger logger)
	{
		this.mapper = new InputMapper<>();
		this.listeners = new ArrayList<>();
		this.activeActions = new HashSet<>();
		this.logger = logger;
		
		if (keyConfigFile != null)
			loadConfig(keyConfigFile);
		
		defaultMappings();
	}


	private void loadConfig(File keyConfigFile)
	{
		try
		{
			Map<String, String> config = FileOperations.loadConfigFile(keyConfigFile, false);
			
			for (Entry<String, String> entry : config.entrySet())
			{
				String actionName = entry.getKey().trim();
				String codeString = entry.getValue().trim();
				int[] codes = stringToCodes(codeString);
				
				mapIfNotSet(Action.valueOf(actionName.toUpperCase()), codes);
			}
		}
		catch (FileNotFoundException e)
		{
			logger.log(Level.INFO, "No config file, loading default keybindings.");
		}
	}


	private int[] stringToCodes(String codeString)
	{
		codeString = codeString.replaceAll("\\s*,\\s*", ",");
		
		String[] split = splitCodeString(codeString);
		
		return Arrays.stream(split).mapToInt(this::stringToCode).toArray();
	}


	private String[] splitCodeString(String codeString)
	{
		String[] split;
		
		if (codeString.equals(","))
		{
			split = new String[] { codeString };
		}
		else
		{
			String comma = "Comma";
			if (codeString.startsWith(",,"))
				codeString = comma + codeString.substring(1);
			if (codeString.endsWith(",,"))
				codeString = codeString.substring(0, codeString.length()-1) + comma;
			if (codeString.contains(",,,"))
				codeString = codeString.replace(",,,", ","+comma+",");
			split = codeString.split(",");
		}
		return split;
	}


	private int stringToCode(String codeString)
	{
		int code = Keys.valueOf(codeString);
		
		if (code == -1)
		{
			switch (codeString)
			{
				case "Mouse 1" :
				case "M1" :
					code = InputMapper.MOUSE1;
					break;
				case "Mouse 2" :
				case "M2" :
					code = InputMapper.MOUSE2;
					break;
				case "Mouse 3" :
				case "M3" :
					code = InputMapper.MOUSE3;
					break;
				case "MouseWheelUp" :
				case "MWheelUp" :
					code = InputMapper.MOUSE_WHEEL_UP;
					break;
				case "MouseWheelDown" :
				case "MWheelDown" :
					code = InputMapper.MOUSE_WHEEL_DOWN;
					break;
				case "Comma" :
					code = Keys.COMMA;
					break;
				default :
					code = InputMapper.INVALID_CODE;
					break;
			}
		}
		
		return code;
	}


	private void defaultMappings()
	{
		mapIfNotSet(Action.MOVE_FORWARD, Keys.W);
		mapIfNotSet(Action.MOVE_BACKWARD, Keys.S);
		mapIfNotSet(Action.MOVE_LEFT, Keys.A);
		mapIfNotSet(Action.MOVE_RIGHT, Keys.D);
		mapIfNotSet(Action.MOVE_UP, Keys.SPACE);
		mapIfNotSet(Action.MOVE_DOWN, Keys.CONTROL_LEFT, Keys.CONTROL_RIGHT);
		mapIfNotSet(Action.MOVE_IN, Keys.E);
		mapIfNotSet(Action.MOVE_OUT, Keys.Q);
		mapIfNotSet(Action.ZOOM_IN, InputMapper.getMouseWheelCode(1));
		mapIfNotSet(Action.ZOOM_OUT, InputMapper.getMouseWheelCode(-1));
		mapIfNotSet(Action.REDUCE_SPEED, Keys.SHIFT_LEFT, Keys.SHIFT_RIGHT);
		mapIfNotSet(Action.CATCH_MOUSE, getMouseButtonCode(Buttons.RIGHT));
		mapIfNotSet(Action.SELECT_POINT, getMouseButtonCode(Buttons.LEFT));
		mapIfNotSet(Action.FOLLOW_ORIGIN, Keys.G);
		mapIfNotSet(Action.FOLLOW_ORIGIN_HOLD, Keys.HOME);
		mapIfNotSet(Action.FOLLOW_SELECTION, Keys.F);
		mapIfNotSet(Action.TOGGLE_ROTATION, Keys.R);
		mapIfNotSet(Action.ROTATION_DIRECTION, Keys.STAR);
		mapIfNotSet(Action.ROTATION_SPEED_INCREMENT, Keys.PLUS);
		mapIfNotSet(Action.ROTATION_SPEED_DECREMENT, Keys.MINUS);
		mapIfNotSet(Action.ROTATION_SPEED_1, Keys.NUMPAD_1);
		mapIfNotSet(Action.ROTATION_SPEED_2, Keys.NUMPAD_2);
		mapIfNotSet(Action.ROTATION_SPEED_3, Keys.NUMPAD_3);
		mapIfNotSet(Action.ROTATION_SPEED_4, Keys.NUMPAD_4);
		mapIfNotSet(Action.ROTATION_SPEED_5, Keys.NUMPAD_5);
		mapIfNotSet(Action.ROTATION_SPEED_6, Keys.NUMPAD_6);
		mapIfNotSet(Action.ROTATION_SPEED_7, Keys.NUMPAD_7);
		mapIfNotSet(Action.ROTATION_SPEED_8, Keys.NUMPAD_8);
		mapIfNotSet(Action.ROTATION_SPEED_9, Keys.NUMPAD_9);
		mapIfNotSet(Action.CAMERA_PRESET_1, Keys.NUMPAD_1);
		mapIfNotSet(Action.CAMERA_PRESET_2, Keys.NUMPAD_2);
		mapIfNotSet(Action.CAMERA_PRESET_3, Keys.NUMPAD_3);
		mapIfNotSet(Action.TOGGLE_HIGHLIGHT, Keys.H);
		mapIfNotSet(Action.TOGGLE_POINTS, Keys.NUM_1);
		mapIfNotSet(Action.TOGGLE_VOLUMES, Keys.NUM_2);
		mapIfNotSet(Action.TOGGLE_TETRAHEDRON_SIDES, Keys.T);
		mapIfNotSet(Action.TOGGLE_AXES, Keys.Y);
		mapIfNotSet(Action.TOGGLE_METRICS, Keys.M);
		mapIfNotSet(Action.TOGGLE_CROSSHAIR, Keys.C);
		mapIfNotSet(Action.TOGGLE_LEGEND, Keys.L);
		mapIfNotSet(Action.SCREENSHOT, Keys.F12);
	}
	
	
	private void mapIfNotSet(Action action, int... codes)
	{
		if (codes == null || codes.length == 0)
			return;
		
		if (mapper.getMappingsForAction(action).isEmpty())
		{
			for (int code : codes)
			{
				mapper.registerKey(code, action);
			}
		}
	}
	
	
	public boolean isActionActive(Action action)
	{
		return activeActions.contains(action);
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
		return false;
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
		boolean usedEvent = false;
		
		for (Action action : actions)
		{
			activeActions.add(action);
			for (InputListener listener : listeners)
			{
				if (listener.onActionStarted(action, parameters))
				{
					usedEvent = true;
					break;
				}
			}
		}

		return usedEvent;
	}


	private boolean sendActionStopped(List<Action> actions, Object... parameters)
	{
		boolean usedEvent = false;
		
		for (Action action : actions)
		{
			activeActions.remove(action);
			for (InputListener listener : listeners)
			{
				if (listener.onActionStopped(action, parameters))
				{
					usedEvent = true;
					break;
				}
			}
		}

		return usedEvent;
	}
}
