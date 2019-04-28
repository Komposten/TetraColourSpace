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

public enum Action
{
	MOVE_FORWARD,
	MOVE_BACKWARD,
	MOVE_LEFT,
	MOVE_RIGHT,
	MOVE_UP,
	MOVE_DOWN,
	MOVE_IN,
	MOVE_OUT,
	
	ZOOM_IN,
	ZOOM_OUT,
	
	REDUCE_SPEED,
	CATCH_MOUSE,
	SELECT_POINT,
	
	FOLLOW_ORIGIN,
	FOLLOW_ORIGIN_HOLD,
	FOLLOW_SELECTION,
	
	TOGGLE_ROTATION,
	ROTATION_DIRECTION,
	ROTATION_SPEED_INCREMENT,
	ROTATION_SPEED_DECREMENT,
	ROTATION_SPEED_1,
	ROTATION_SPEED_2,
	ROTATION_SPEED_3,
	ROTATION_SPEED_4,
	ROTATION_SPEED_5,
	ROTATION_SPEED_6,
	ROTATION_SPEED_7,
	ROTATION_SPEED_8,
	ROTATION_SPEED_9,
	
	CAMERA_PRESET_1,
	CAMERA_PRESET_2,
	CAMERA_PRESET_3,
	
	TOGGLE_HIGHLIGHT,
	TOGGLE_POINTS,
	TOGGLE_VOLUMES,
	
	TOGGLE_TETRAHEDRON_SIDES,
	TOGGLE_AXES,
	TOGGLE_METRICS,
	
	TOGGLE_CROSSHAIR,
	TOGGLE_LEGEND,
	
	SCREENSHOT
}
