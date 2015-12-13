package headmade.ld34;

public class ScoreVo {
	private static final String	TAG			= ScoreVo.class.getName();

	public float				symetry		= 0f;
	public float				balance		= 0f;
	public float				variety		= 0f;
	public float				itchiness	= 0f;
	public float				overall		= 0f;

	@Override
	public String toString() {
		return "ScoreVo [symetry=" + symetry + ", balance=" + balance + ", variety=" + variety + ", itchiness=" + itchiness + ", overall="
				+ overall + "]";
	}
}
