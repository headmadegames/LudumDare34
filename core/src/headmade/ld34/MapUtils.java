package headmade.ld34;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.joints.WeldJointDef;
import com.badlogic.gdx.physics.box2d.joints.WheelJointDef;

import net.dermetfan.gdx.graphics.g2d.Box2DSprite;
import net.dermetfan.gdx.physics.box2d.Box2DMapObjectParser;

public class MapUtils {
	private static final String TAG = MapUtils.class.getName();

	public static void loadMap(final Ld34 ld34, String mapName) {
		final TiledMap map = Assets.assetsManager.get(mapName, TiledMap.class);

		final Box2DMapObjectParser parser = new Box2DMapObjectParser(Ld34.UNIT_SCALE);

		final Box2DMapObjectParser.Listener.Adapter listener = new Box2DMapObjectParser.Listener.Adapter() {

			@Override
			public void created(Fixture fixture, MapObject mapObject) {
				// Gdx.app.log(TAG, "mapObject.getProperties()" + fixture.getFilterData().maskBits);
				// if (mapObject.getName().startsWith("head") || mapObject.getName().startsWith("beard")) {
				fixture.setSensor(true);
				// }
				if (fixture.getBody().getUserData() != null) {
					for (final Body facial : ld34.facials) {
						fixture.setUserData(fixture.getBody().getUserData());
						if (facial.equals(fixture.getBody())) {
							fixture.getFilterData().categoryBits = Ld34.CATEGORYBITS_FACIALS;
							fixture.getFilterData().maskBits = Ld34.MASKBITS_FACIALS;
						}
					}
				}
				super.created(fixture, mapObject);
			}

			@Override
			public void created(Body body, MapObject mapObject) {
				if ("head".equals(mapObject.getName())) {
					ld34.head = body;
				} else if (mapObject.getName().startsWith("beard")) {
					ld34.beard = body;
				} else if (mapObject.getName().startsWith("facial")) {
					final String texName = mapObject.getName().replaceAll("facial_", "");
					body.setUserData(new Box2DSprite(Assets.instance.skin.get(texName, TextureRegion.class)));
					ld34.facials.add(body);
				} else if (mapObject.getName().startsWith("hair")) {
					ld34.hair = body;
				} else if ("ground".equals(mapObject.getName())) {
					ld34.ground = body;
				} else if ("body".equals(mapObject.getName())) {
					body.setUserData(new Box2DSprite(Assets.instance.skin.get("body", TextureRegion.class)));
					ld34.body = body;
				}
			}

		};
		parser.setListener(listener);

		parser.load(ld34.world, map);

		if (ld34.head != null) {
			final BodyDef bd = new BodyDef();
			bd.position.x = 0;
			bd.position.y = -2;
			bd.type = BodyType.DynamicBody;
			final Body wheel = ld34.world.createBody(bd);
			final CircleShape shape = new CircleShape();
			shape.setRadius(0.5f);
			final Fixture wheelFix = wheel.createFixture(shape, 1f);
			wheelFix.setSensor(true);

			final WheelJointDef mjd = new WheelJointDef();
			mjd.initialize(ld34.ground, wheel, wheel.getWorldCenter(), new Vector2(0, 1));
			mjd.motorSpeed = 3f;
			mjd.enableMotor = true;
			mjd.collideConnected = false;
			mjd.maxMotorTorque = 200f;
			ld34.world.createJoint(mjd);

			final WeldJointDef wjd = new WeldJointDef();
			wjd.initialize(ld34.head, ld34.beard, ld34.beard.getWorldCenter());
			ld34.world.createJoint(wjd);

			wjd.initialize(ld34.head, ld34.hair, ld34.hair.getWorldCenter());
			ld34.world.createJoint(wjd);

			wjd.initialize(ld34.head, ld34.body, ld34.hair.getWorldCenter());
			ld34.world.createJoint(wjd);

			wjd.frequencyHz = 2f;
			wjd.dampingRatio = 0.5f;
			wjd.referenceAngle = 0f;

			for (final Body facial : ld34.facials) {
				wjd.initialize(ld34.head, facial, facial.getWorldCenter());
				ld34.world.createJoint(wjd);
			}

			wjd.initialize(ld34.head, ld34.ground, ld34.head.getWorldCenter());
			wjd.referenceAngle = 10f;
			wjd.dampingRatio = 0.5f;
			wjd.frequencyHz = 1.5f;
			ld34.world.createJoint(wjd);
			// pulley Joint
			// final PulleyJointDef jd = new PulleyJointDef();
			// jd.initialize(wheel, ld34.head, ld34.ground.getWorldCenter(), ld34.ground.getWorldCenter().cpy().add(1, 0),
			// wheel.getWorldCenter().cpy().add(0.5f, 0), ld34.head.getWorldCenter(), 1f);
			// ld34.world.createJoint(jd);
		}
	}
}
