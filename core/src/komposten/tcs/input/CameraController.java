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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;

import komposten.tcs.input.InputHandler.InputListener;
import komposten.tcs.rendering.World;
import komposten.tcs.util.TCSUtils;
import komposten.utilities.tools.MathOps;

public class CameraController implements InputReceiver
{
	private static final float MAX_ZOOM = 0.025f;
	private static final float SENSITIVITY = -0.2f;
	private static final float SLOW_MODIFIER = .33f;
	private static final float LINEAR_VELOCITY = 1;
	private static final float ZOOM_VELOCITY = 1;
	private static final float SCROLL_VELOCITY = 5;
	private static final int ANGULAR_VELOCITY = 50;
	private static final int ANGULAR_AUTO_VELOCITY = 20;
	private static final int MAX_DISTANCE = 5;
	
	private enum FollowMode
	{
		SELECTED,
		CENTRE,
		OFF
	}

	private final float distanceModifier;
	private final float zoomModifier;
	
	private final float maxDistance;
	private final float linearVelocity;
	private final float zoomVelocity;
	private final float scrollVelocity;

	private Vector3 calcVector = new Vector3();
	
	private Camera camera;
	private World world;
	
	private FollowMode followMode = FollowMode.OFF;
	private float scrollDelta = 0;
	private boolean autoRotate = false;
	private int autoRotation = 1;
	private boolean cameraDirty = true;
	
	
	public CameraController(Camera camera, World world)
	{
		this.camera = camera;
		this.world = world;
		
		if (camera instanceof PerspectiveCamera)
		{
			PerspectiveCamera pCamera = (PerspectiveCamera) camera;
			distanceModifier = TCSUtils.getDistanceModifier(pCamera.fieldOfView);
			zoomModifier = TCSUtils.getZoomModifier(pCamera.fieldOfView);
		}
		else
		{
			distanceModifier = 1;
			zoomModifier = 1;
		}
		
		this.maxDistance = MAX_DISTANCE * distanceModifier;
		this.linearVelocity = LINEAR_VELOCITY;
		this.zoomVelocity = ZOOM_VELOCITY * zoomModifier;
		this.scrollVelocity = SCROLL_VELOCITY * zoomModifier;
	}
	
	
	@Override
	public void attachToInputHandler(InputHandler handler)
	{
		handler.addListener(inputListener);
	}
	
	
	@Override
	public void detachFromInputHandler(InputHandler handler)
	{
		handler.removeListener(inputListener);
	}
	
	
	public boolean isCameraDirty()
	{
		return cameraDirty;
	}
	
	
	public void clearCameraDirty()
	{
		this.cameraDirty = false;
	}
	
	
	public void readInput(float deltaTime, InputHandler handler)
	{
		if (followMode != FollowMode.OFF)
		{
			if (rotationMovement(deltaTime, handler))
				cameraDirty = true;
		}
		else 
		{
			if (translationMovement(deltaTime, handler))
				cameraDirty = true;
		}
	}


	private boolean translationMovement(float deltaTime, InputHandler handler)
	{
		Vector3 movement = new Vector3();
		
		if (handler.isActionActive(Action.MOVE_FORWARD) ||
				handler.isActionActive(Action.MOVE_BACKWARD))
		{
			calcVector
					.set(camera.direction.x, 0, camera.direction.z)
					.setLength(linearVelocity * deltaTime);
			
			if (handler.isActionActive(Action.MOVE_FORWARD))
				movement.add(calcVector);
			else
				movement.sub(calcVector);
		}
		
		if (handler.isActionActive(Action.MOVE_LEFT) ||
				handler.isActionActive(Action.MOVE_RIGHT))
		{
			calcVector
					.set(camera.direction.z, 0, -camera.direction.x)
					.setLength(linearVelocity * deltaTime);
			
			if (handler.isActionActive(Action.MOVE_LEFT))
				movement.add(calcVector);
			else
				movement.sub(calcVector);
		}
		
		if (handler.isActionActive(Action.MOVE_IN) ||
				handler.isActionActive(Action.MOVE_OUT))
		{
			calcVector.set(camera.direction).setLength(zoomVelocity * deltaTime);
			
			if (handler.isActionActive(Action.MOVE_IN))
				movement.add(calcVector);
			else
				movement.sub(calcVector);
		}
		
		if (handler.isActionActive(Action.MOVE_UP))
		{
			movement.y += linearVelocity * deltaTime;
		}
		else if (handler.isActionActive(Action.MOVE_DOWN))
		{
			movement.y -= linearVelocity * deltaTime;
		}
		
		boolean needsUpdate = false;
		if (!movement.epsilonEquals(Vector3.Zero))
		{
			if (handler.isActionActive(Action.REDUCE_SPEED))
				movement.scl(SLOW_MODIFIER);
			
			camera.position.x = MathOps.clamp(-maxDistance, maxDistance, camera.position.x + movement.x);
			camera.position.y = MathOps.clamp(-maxDistance, maxDistance, camera.position.y + movement.y);
			camera.position.z = MathOps.clamp(-maxDistance, maxDistance, camera.position.z + movement.z);
			needsUpdate = true;
		}
		
		if (handler.isActionActive(Action.CATCH_MOUSE))
		{
			int mouseDX = Gdx.input.getDeltaX();
			int mouseDY = Gdx.input.getDeltaY();
			
			if (mouseDX != 0 || mouseDY != 0)
			{
				float sensitivity = SENSITIVITY;
				float rotationX = mouseDX * sensitivity;
				float rotationY = -mouseDY * sensitivity;
				
				camera.rotate(Vector3.Y, rotationX);
				
				calcVector.set(camera.direction.x, 0, camera.direction.z);
				
				rotationY = clampYRotation(rotationY);
				
				calcVector.set(camera.direction.z, 0, -camera.direction.x);
				camera.rotate(calcVector, rotationY);
				
				needsUpdate = true;
			}
		}
		
		return needsUpdate;
	}


	private boolean rotationMovement(float deltaTime, InputHandler handler)
	{
		Vector3 movement = new Vector3();
		Vector3 focalPoint = (followMode == FollowMode.CENTRE ? Vector3.Zero : world.getSelectedPoint().getCoordinates());
		
		float rotX = 0;
		float rotY = 0;
		float zoom = 0;

		if (handler.isActionActive(Action.MOVE_FORWARD) || handler.isActionActive(Action.MOVE_IN))
			zoom += zoomVelocity * deltaTime;
		if (handler.isActionActive(Action.MOVE_BACKWARD) || handler.isActionActive(Action.MOVE_OUT))
			zoom -= zoomVelocity * deltaTime;
		if (handler.isActionActive(Action.MOVE_LEFT))
			rotX -= ANGULAR_VELOCITY * deltaTime;
		if (handler.isActionActive(Action.MOVE_RIGHT))
			rotX += ANGULAR_VELOCITY * deltaTime;
		if (handler.isActionActive(Action.MOVE_UP))
			rotY -= ANGULAR_VELOCITY * deltaTime;
		if (handler.isActionActive(Action.MOVE_DOWN))
			rotY += ANGULAR_VELOCITY * deltaTime;
		
		if (scrollDelta != 0)
		{
			zoom -= scrollDelta * scrollVelocity * deltaTime;
			scrollDelta = 0;
		}
		
		if (autoRotate)
		{
			rotX += ANGULAR_AUTO_VELOCITY * deltaTime * autoRotation;
		}
		
		if (handler.isActionActive(Action.CATCH_MOUSE))
		{
			int mouseDX = Gdx.input.getDeltaX();
			int mouseDY = Gdx.input.getDeltaY();
			
			if (mouseDX != 0)
			{
				rotX += mouseDX * SENSITIVITY;
			}
			if (mouseDY != 0)
			{
				rotY += mouseDY * SENSITIVITY;
			}
		}
		
		if (!MathOps.equals(zoom, 0, 0.0001f))
		{
			float distanceToPoint = calcVector.set(camera.position).sub(focalPoint).len();
			float velocity = zoom;
			if (distanceToPoint - velocity < MAX_ZOOM)
				velocity = distanceToPoint - MAX_ZOOM;
				
			calcVector.set(camera.direction).setLength(velocity);
			if (velocity < 0)
				calcVector.scl(-1);

			if (handler.isActionActive(Action.REDUCE_SPEED))
				calcVector.scl(SLOW_MODIFIER);
			
			movement.add(calcVector);
		}
		
		if (!MathOps.equals(rotX, 0, 0.0001f))
		{
			if (handler.isActionActive(Action.REDUCE_SPEED))
				rotX /= 2;
			
			Vector3 vectorFromCentre = camera.position.cpy().sub(focalPoint);
			vectorFromCentre.rotate(Vector3.Y, rotX);
			vectorFromCentre.add(focalPoint);

			movement.add(vectorFromCentre.sub(camera.position));
		}
		
		if (!MathOps.equals(rotY, 0, 0.0001f))
		{
			if (handler.isActionActive(Action.REDUCE_SPEED))
				rotY /= 2;
			
			Vector3 vectorFromCentre = camera.position.cpy().sub(focalPoint);
			
			float rotationY = -clampYRotation(-rotY);

			calcVector.set(vectorFromCentre.z, 0, -vectorFromCentre.x);
			vectorFromCentre.rotate(calcVector, rotationY);
			vectorFromCentre.add(focalPoint);

			movement.add(vectorFromCentre.sub(camera.position));
		}
		
		boolean needsUpdate = false;
		if (!movement.epsilonEquals(Vector3.Zero))
		{
			camera.position.x = MathOps.clamp(-maxDistance, maxDistance, camera.position.x + movement.x);
			camera.position.y = MathOps.clamp(-maxDistance, maxDistance, camera.position.y + movement.y);
			camera.position.z = MathOps.clamp(-maxDistance, maxDistance, camera.position.z + movement.z);
			
			lookAt(focalPoint);
			
			needsUpdate = true;
		}
		
		return needsUpdate;
	}


	private float clampYRotation(float rotationY)
	{
		calcVector.set(camera.direction.x, 0, camera.direction.z);
		double currentAngle = Math.toDegrees(Math.atan2(calcVector.y - camera.direction.y, calcVector.len()));
		double maxAngle = 89;
		
		if (currentAngle + rotationY > maxAngle)
			rotationY = (float) (maxAngle - currentAngle);
		else if (currentAngle + rotationY < -maxAngle)
			rotationY = (float) -(maxAngle + currentAngle);
		return rotationY;
	}


	public void lookAt(Vector3 target)
	{
		camera.lookAt(target);
		camera.up.set(Vector3.Y); //Resetting the up vector since camera.lookAt() changes it.
	}
	

	private InputListener inputListener = new InputListener()
	{
		boolean nonRPressed = false;
		boolean rDown = false;


		private void setFollowMode(FollowMode mode)
		{
			followMode = mode;

			switch (followMode)
			{
				case CENTRE :
					lookAt(Vector3.Zero);
					cameraDirty = true;
					break;
				case SELECTED :
					lookAt(world.getSelectedPoint().getCoordinates());
					cameraDirty = true;
					break;
				case OFF :
				default :
					break;
			}
		}


		@Override
		public boolean onActionStarted(Action action, Object... parameters)
		{
			if (action == Action.FOLLOW_ORIGIN_HOLD)
			{
				setFollowMode(FollowMode.CENTRE);
				return true;
			}
			else if (action == Action.TOGGLE_ROTATION)
			{
				nonRPressed = false;
				rDown = true;
			}
			else if (action == Action.CATCH_MOUSE)
			{
				Gdx.input.setCursorCatched(true);
				return true;
			}

			return false;
		}
		
		
		@Override
		public boolean onActionStopped(Action action, Object... parameters)
		{
			if (action == Action.FOLLOW_ORIGIN_HOLD && followMode == FollowMode.CENTRE)
			{
				setFollowMode(FollowMode.OFF);
				return true;
			}
			else if (action == Action.FOLLOW_ORIGIN)
			{
				if (followMode != FollowMode.CENTRE)
					setFollowMode(FollowMode.CENTRE);
				else
					setFollowMode(FollowMode.OFF);
				return true;
			}
			else if (action == Action.FOLLOW_SELECTION && world.hasSelection())
			{
				if (followMode != FollowMode.SELECTED)
					setFollowMode(FollowMode.SELECTED);
				else
					setFollowMode(FollowMode.OFF);
				return true;
			}
			else if (action == Action.TOGGLE_ROTATION)
			{
				if (!nonRPressed)
					autoRotate = !autoRotate;

				rDown = false;
			}
			else if (rDown)
			{
				if (action.name().matches("ROTATION_SPEED_\\d+"))
				{
					int sign = (autoRotation > 0 ? 1 : -1);
					int speed = Integer.parseInt(action.name().substring(15));
					autoRotation = sign * speed;
					nonRPressed = true;
				}
				else if (action == Action.ROTATION_DIRECTION)
				{
					autoRotation = -autoRotation;
					nonRPressed = true;
				}
				else if (action == Action.ROTATION_SPEED_DECREMENT)
				{
					if (autoRotation < -1)
						autoRotation += 1;
					else if (autoRotation > 1)
						autoRotation -= 1;

					nonRPressed = true;
				}
				else if (action == Action.ROTATION_SPEED_INCREMENT)
				{
					autoRotation += (autoRotation < 0 ? -1 : 1);
					nonRPressed = true;
				}

				return true;
			}
			else if (action == Action.CAMERA_PRESET_1)
			{
				camera.position.set(1, 1, -0.3f).scl(distanceModifier);
				lookAt(Vector3.Zero);
				cameraDirty = true;
			}
			else if (action == Action.CAMERA_PRESET_2)
			{
				camera.position.set(0, -1.4f, 0.0001f).scl(distanceModifier);
				lookAt(Vector3.Zero);
				cameraDirty = true;
			}
			else if (action == Action.CAMERA_PRESET_3)
			{
				camera.position.set(0, 1f, 0.0001f).scl(distanceModifier);
				lookAt(Vector3.Zero);
				cameraDirty = true;
			}
			else if (action == Action.CATCH_MOUSE)
			{
				Gdx.input.setCursorCatched(false);
				return true;
			}
			else if (action.name().startsWith("ZOOM") && followMode != FollowMode.OFF)
			{
				float amount;
				
				if (parameters.length > 0)
					amount = (Integer) parameters[0];
				else if (action == Action.ZOOM_IN)
					amount = -scrollVelocity;
				else
					amount = scrollVelocity;
				
				scrollDelta += amount;
				return true;
			}

			return false;
		}
	};
}
