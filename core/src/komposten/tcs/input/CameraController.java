package komposten.tcs.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector3;

import komposten.tcs.input.InputHandler.InputListener;
import komposten.tcs.rendering.World;
import komposten.utilities.tools.MathOps;

public class CameraController implements InputReceiver
{
	private static final float MAX_ZOOM = 0.025f;
	private static final float SENSITIVITY = -0.2f;
	private static final float SLOW_MODIFIER = .33f;
	private static final int LINEAR_VELOCITY = 1;
	private static final int SCROLL_VELOCITY = 5;
	private static final int ANGULAR_VELOCITY = 50;
	private static final int ANGULAR_AUTO_VELOCITY = 20;
	private static final int MAX_DISTANCE = 5;
	
	private enum FollowMode
	{
		SELECTED,
		CENTRE,
		OFF
	}

	private Vector3 calcVector = new Vector3();
	
	private Camera camera;
	private World world;
	
	private FollowMode followMode = FollowMode.OFF;
	private int scrollDelta = 0;
	private boolean autoRotate = false;
	private int autoRotation = 1;
	private boolean cameraDirty = true;
	
	
	public CameraController(Camera camera, World world)
	{
		this.camera = camera;
		this.world = world;
	}
	
	
	@Override
	public void register(InputHandler handler)
	{
		handler.addListener(inputListener);
	}
	
	
	@Override
	public void unregister(InputHandler handler)
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
	
	
	public void readInput(float deltaTime)
	{
		if (followMode != FollowMode.OFF)
		{
			if (rotationMovement(deltaTime))
				cameraDirty = true;
		}
		else 
		{
			if (translationMovement(deltaTime))
				cameraDirty = true;
		}
	}


	private boolean translationMovement(float deltaTime)
	{
		Vector3 movement = new Vector3();
		
		if (Gdx.input.isKeyPressed(Keys.W) || Gdx.input.isKeyPressed(Keys.S))
		{
			calcVector.set(camera.direction.x, 0, camera.direction.z).setLength(LINEAR_VELOCITY * deltaTime);
			
			if (Gdx.input.isKeyPressed(Keys.W))
				movement.add(calcVector);
			else
				movement.sub(calcVector);
		}
		
		if (Gdx.input.isKeyPressed(Keys.A) || Gdx.input.isKeyPressed(Keys.D))
		{
			calcVector.set(camera.direction.z, 0, -camera.direction.x).setLength(LINEAR_VELOCITY * deltaTime);
			
			if (Gdx.input.isKeyPressed(Keys.A))
				movement.add(calcVector);
			else
				movement.sub(calcVector);
		}
		
		if (Gdx.input.isKeyPressed(Keys.E) || Gdx.input.isKeyPressed(Keys.Q))
		{
			calcVector.set(camera.direction).setLength(LINEAR_VELOCITY * deltaTime);
			
			if (Gdx.input.isKeyPressed(Keys.E))
				movement.add(calcVector);
			else
				movement.sub(calcVector);
		}
		
		if (Gdx.input.isKeyPressed(Keys.SPACE))
		{
			movement.y += LINEAR_VELOCITY * deltaTime;
		}
		else if (Gdx.input.isKeyPressed(Keys.CONTROL_LEFT))
		{
			movement.y -= LINEAR_VELOCITY * deltaTime;
		}
		
		boolean needsUpdate = false;
		if (!movement.epsilonEquals(Vector3.Zero))
		{
			if (Gdx.input.isKeyPressed(Keys.SHIFT_LEFT))
				movement.scl(SLOW_MODIFIER);
			
			camera.position.x = MathOps.clamp(-MAX_DISTANCE, MAX_DISTANCE, camera.position.x + movement.x);
			camera.position.y = MathOps.clamp(-MAX_DISTANCE, MAX_DISTANCE, camera.position.y + movement.y);
			camera.position.z = MathOps.clamp(-MAX_DISTANCE, MAX_DISTANCE, camera.position.z + movement.z);
			needsUpdate = true;
		}
		
		if (Gdx.input.isButtonPressed(Buttons.RIGHT))
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


	private boolean rotationMovement(float deltaTime)
	{
		Vector3 movement = new Vector3();
		Vector3 focalPoint = (followMode == FollowMode.CENTRE ? Vector3.Zero : world.getSelectedPoint().getCoordinates());
		
		float rotX = 0;
		float rotY = 0;
		float zoom = 0;

		if (Gdx.input.isKeyPressed(Keys.W) || Gdx.input.isKeyPressed(Keys.E))
			zoom += LINEAR_VELOCITY * deltaTime;
		if (Gdx.input.isKeyPressed(Keys.S) || Gdx.input.isKeyPressed(Keys.Q))
			zoom -= LINEAR_VELOCITY * deltaTime;
		if (Gdx.input.isKeyPressed(Keys.A))
			rotX -= ANGULAR_VELOCITY * deltaTime;
		if (Gdx.input.isKeyPressed(Keys.D))
			rotX += ANGULAR_VELOCITY * deltaTime;
		if (Gdx.input.isKeyPressed(Keys.SPACE))
			rotY -= ANGULAR_VELOCITY * deltaTime;
		if (Gdx.input.isKeyPressed(Keys.CONTROL_LEFT))
			rotY += ANGULAR_VELOCITY * deltaTime;
		
		if (scrollDelta != 0)
		{
			zoom -= scrollDelta * SCROLL_VELOCITY * deltaTime;
			scrollDelta = 0;
		}
		
		if (autoRotate)
		{
			rotX += ANGULAR_AUTO_VELOCITY * deltaTime * autoRotation;
		}
		
		if (Gdx.input.isButtonPressed(Buttons.RIGHT))
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

			if (Gdx.input.isKeyPressed(Keys.SHIFT_LEFT))
				calcVector.scl(SLOW_MODIFIER);
			
			movement.add(calcVector);
		}
		
		if (!MathOps.equals(rotX, 0, 0.0001f))
		{
			if (Gdx.input.isKeyPressed(Keys.SHIFT_LEFT))
				rotX /= 2;
			
			Vector3 vectorFromCentre = camera.position.cpy().sub(focalPoint);
			vectorFromCentre.rotate(Vector3.Y, rotX);
			vectorFromCentre.add(focalPoint);

			movement.add(vectorFromCentre.sub(camera.position));
		}
		
		if (!MathOps.equals(rotY, 0, 0.0001f))
		{
			if (Gdx.input.isKeyPressed(Keys.SHIFT_LEFT))
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
			camera.position.x = MathOps.clamp(-MAX_DISTANCE, MAX_DISTANCE, camera.position.x + movement.x);
			camera.position.y = MathOps.clamp(-MAX_DISTANCE, MAX_DISTANCE, camera.position.y + movement.y);
			camera.position.z = MathOps.clamp(-MAX_DISTANCE, MAX_DISTANCE, camera.position.z + movement.z);
			
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
				camera.position.set(1, 1, -0.3f);
				lookAt(Vector3.Zero);
				cameraDirty = true;
			}
			else if (action == Action.CAMERA_PRESET_2)
			{
				camera.position.set(0, -1.4f, 0.0001f);
				lookAt(Vector3.Zero);
				cameraDirty = true;
			}
			else if (action == Action.CAMERA_PRESET_3)
			{
				camera.position.set(0, 1f, 0.0001f);
				lookAt(Vector3.Zero);
				cameraDirty = true;
			}
			else if (action == Action.CATCH_MOUSE)
			{
				Gdx.input.setCursorCatched(false);
				return true;
			}
			else if (action == Action.ZOOM && followMode != FollowMode.OFF)
			{
				scrollDelta += (Integer) parameters[0];
				return true;
			}

			return false;
		}
	};
}
