package headmade.ld34;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJoint;
import com.badlogic.gdx.utils.Array;

import net.dermetfan.gdx.physics.box2d.Chain;

public class ScoreService {
	private static final String	TAG					= ScoreService.class.getName();

	private Ld34				ld34;
	private Array<BeardStatVo>	beardChains			= new Array<BeardStatVo>();
	private int					facialCollisions	= 0;
	private int					facialCreations		= 0;

	public ScoreService(Ld34 ld34) {
		super();
		this.ld34 = ld34;
	}

	public ScoreVo calcScore() {
		final ScoreVo score = new ScoreVo();

		final float center = ld34.head.getWorldCenter().x;

		for (final BeardStatVo beardStatVo : beardChains) {
			beardStatVo.reset();
			Float lastAngle = null;
			float lastAngleDiff = 0f;
			for (final Body segment : beardStatVo.chain.getSegments()) {
				final Vector2 segmentPos = segment.getWorldCenter().cpy().sub(ld34.head.getWorldCenter());
				if (beardStatVo.startPoint == null) {
					beardStatVo.startPoint = segmentPos;
					beardStatVo.minX = beardStatVo.startPoint.x;
					beardStatVo.maxX = beardStatVo.startPoint.x;
					beardStatVo.minY = beardStatVo.startPoint.y;
					beardStatVo.maxY = beardStatVo.startPoint.y;
				} else {
					if (segmentPos.x < beardStatVo.minX) {
						beardStatVo.minX = segmentPos.x;
					} else if (segmentPos.x > beardStatVo.maxX) {
						beardStatVo.maxX = segmentPos.x;
					}
					if (segmentPos.y < beardStatVo.minY) {
						beardStatVo.minY = segmentPos.y;
					} else if (segmentPos.y > beardStatVo.maxY) {
						beardStatVo.maxY = segmentPos.y;
					}
				}
				beardStatVo.segmentCount++;
				beardStatVo.totalMass += segment.getMass();
				beardStatVo.balance += segmentPos.x;
				beardStatVo.balanceMass += beardStatVo.balance * segment.getMass();
				beardStatVo.angleSum += segment.getAngle();

				if (segment.getJointList().first().joint instanceof RevoluteJoint) {
					final float refAngle = ((RevoluteJoint) segment.getJointList().first().joint).getReferenceAngle();
					if (lastAngle == null) {
						lastAngle = refAngle;
					} else {
						final float angleDiff = lastAngle - refAngle;
						beardStatVo.refAngleChange = Math.abs(refAngle);
						beardStatVo.refAngleDiffSum += angleDiff;
						// beardStatVo.angleDiffSum += angleDiff;
						if (!MathUtils.isEqual(angleDiff, lastAngleDiff) // not equal
								&& ((angleDiff < 0f && lastAngle > 0f) || (angleDiff > 0f && lastAngle < 0f))) {
							beardStatVo.directionChanges++;
						}
						lastAngle = refAngle;
						lastAngleDiff = angleDiff;
					}
				}
			}
			score.balance += beardStatVo.balanceMass;
			score.variety += Math.abs(beardStatVo.refAngleDiffSum) + beardStatVo.directionChanges;

			Gdx.app.log(TAG, "" + beardStatVo);
		}

		float minX = 0;
		float minY = 0;
		float maxX = 0;
		float maxY = 0;
		for (final BeardStatVo beardStatVo : beardChains) {
			if (minX > beardStatVo.minX) {
				minX = beardStatVo.minX;
			} else if (maxX < beardStatVo.maxX) {
				maxX = beardStatVo.maxX;
			}
			if (minY > beardStatVo.minY) {
				minY = beardStatVo.minY;
			} else if (maxY < beardStatVo.maxY) {
				maxY = beardStatVo.maxY;
			}
		}
		score.impressiveness = (maxX - minX) + (maxY - minY);

		score.itchiness = facialCollisions + facialCreations * 20;

		Gdx.app.log(TAG, "" + score);
		return score;
	}

	private float balanceFactor(float center, float x) {
		return x - center;
	}

	public void addBeardChain(Chain chain) {
		final BeardStatVo beardVo = new BeardStatVo();
		beardVo.chain = chain;
		beardChains.add(beardVo);
	}

	public void incFacialCollisions() {
		facialCollisions++;
		Gdx.app.log(TAG, "Facial collision count " + facialCollisions);
	}

	public void incFacialCreations() {
		facialCreations++;
		Gdx.app.log(TAG, "Facial creation count " + facialCreations);
	}
}
