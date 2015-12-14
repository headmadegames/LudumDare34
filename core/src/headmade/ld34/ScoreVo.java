package headmade.ld34;

import com.badlogic.gdx.math.MathUtils;

public class ScoreVo {
	private static final String	TAG				= ScoreVo.class.getName();

	public float				symetry			= 0f;
	public float				balance			= 0f;
	public float				variety			= 0f;
	public float				itchiness		= 0f;
	public float				impressiveness	= 0f;
	public float				overall			= 0f;

	@Override
	public String toString() {
		return "ScoreVo [symetry=" + symetry + ", balance=" + balance + ", variety=" + variety + ", itchiness=" + itchiness
				+ ", impressiveness=" + impressiveness + ", overall=" + overall + "]";
	}

	public String getSymetryRating() {
		return Math.round(symetry) + "/10";
		// return Math.max(10, Math.round(symetry)) + "/10";
	}

	public String getBalanceRating() {
		final float rating = 11 - Math.abs(balance * 10f);
		// return Math.round(rating) + "/10";
		return MathUtils.clamp(Math.round(rating), 1, 10) + "/10";
	}

	public String getVarietyRating() {
		// return Math.round(variety) + "/10";
		return MathUtils.clamp(Math.round(variety), 1, 10) + "/10";
	}

	public String getImpressivenessRating() {
		final float rating = (impressiveness - 5) / 1.3f;
		// return Math.round(rating) + "/10";
		return MathUtils.clamp(Math.round(rating), 1, 10) + "/10";
	}

}
