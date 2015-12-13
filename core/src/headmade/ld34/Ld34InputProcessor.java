package headmade.ld34;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

public class Ld34InputProcessor implements InputProcessor {
	private static final String	TAG	= Ld34InputProcessor.class.getName();

	private Ld34				ld34;

	public Ld34InputProcessor(Ld34 ld34) {
		this.ld34 = ld34;
	}

	@Override
	public boolean keyDown(int keycode) {
		if (keycode == Keys.LEFT || keycode == Keys.A) {
			ld34.camFace.translate(-1f, 0f);
			ld34.camFace.update();
			return true;
		} else if (keycode == Keys.RIGHT || keycode == Keys.D) {
			ld34.camFace.translate(1f, 0f);
			ld34.camFace.update();
			return true;
		} else if (keycode == Keys.UP || keycode == Keys.W) {
			ld34.camFace.translate(0f, 1f);
			ld34.camFace.update();
			return true;
		} else if (keycode == Keys.DOWN || keycode == Keys.S) {
			ld34.camFace.translate(0f, -1f);
			ld34.camFace.update();
			return true;
		} else if (keycode == Keys.F12) {
			ld34.debugEnabled = !ld34.debugEnabled;
			ld34.scoreService.calcScore();
			return true;
		}
		return false;
	}

	@Override
	public boolean keyUp(int keycode) {
		return false;
	}

	@Override
	public boolean keyTyped(char character) {
		return false;
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		final Vector3 mouse = ld34.camFace.unproject(new Vector3(screenX, screenY, 0));
		Gdx.app.log(TAG, "Mouse clicked at " + mouse);

		if (ld34.showLogo) {
			ld34.showLogo = false;
			return true;
		}

		if (ld34.showInstructions) {
			ld34.showInstructions = false;
			return true;
		}

		if (ld34.getCurrentState() != ld34.STATE_GROWING) {
			ld34.incCurrentState();
		}
		return true;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		return false;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		return false;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		return false;
	}

	@Override
	public boolean scrolled(int amount) {
		ld34.camFace.zoom += amount * 0.5f;
		ld34.camFace.zoom = MathUtils.clamp(ld34.camFace.zoom, 0.5f, 50f);
		ld34.camFace.update();
		Gdx.app.log(TAG, "new zoom " + ld34.camFace.zoom);
		return false;
	}

}
