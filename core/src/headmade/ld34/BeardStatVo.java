package headmade.ld34;

import com.badlogic.gdx.math.Vector2;

import net.dermetfan.gdx.physics.box2d.Chain;

public class BeardStatVo {
	private static final String	TAG					= BeardStatVo.class.getName();

	public Chain				chain;
	public Vector2				startPoint;
	public int					segmentCount		= 0;
	public float				totalMass			= 0f;
	public float				balance				= 0f;
	public float				balanceMass			= 0f;
	public float				angleSum			= 0f;
	public float				angleDiffSum		= 0f;
	public float				refAngleDiffSum		= 0f;
	public float				refAngleChange		= 0f;
	public float				directionChanges	= 0f;
	public float				minX				= 0f;
	public float				minY				= 0f;
	public float				maxX				= 0f;
	public float				maxY				= 0f;

	public void reset() {
		startPoint = null;
		segmentCount = 0;
		totalMass = 0f;
		balance = 0f;
		balanceMass = 0f;
		angleSum = 0f;
		angleDiffSum = 0f;
		refAngleDiffSum = 0f;
		directionChanges = 0f;
		minX = 0f;
		minY = 0f;
		maxX = 0f;
		maxY = 0f;
	}

	@Override
	public String toString() {
		return "BeardStatVo [chain=" + chain + ", startPoint=" + startPoint + ", segmentCount=" + segmentCount + ", totalMass=" + totalMass
				+ ", balance=" + balance + ", balanceMass=" + balanceMass + ", angleSum=" + angleSum + ", angleDiffSum=" + angleDiffSum
				+ ", refAngleDiffSum=" + refAngleDiffSum + ", directionChanges=" + directionChanges + ", minX=" + minX + ", minY=" + minY
				+ ", maxX=" + maxX + ", maxY=" + maxY + "]";
	}

}
