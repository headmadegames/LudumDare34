package headmade.ld34;

import java.util.HashMap;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.PolygonRegion;
import com.badlogic.gdx.graphics.g2d.PolygonSprite;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.Shape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJointDef;
import com.badlogic.gdx.physics.box2d.joints.WeldJointDef;
import com.badlogic.gdx.utils.Array;

import net.dermetfan.gdx.graphics.g2d.Box2DSprite;
import net.dermetfan.gdx.physics.box2d.Box2DUtils;
import net.dermetfan.gdx.physics.box2d.Chain;
import net.dermetfan.gdx.physics.box2d.Chain.Builder;
import net.dermetfan.gdx.physics.box2d.Chain.Connection;

public class Ld34 extends Game {
	private static final String				TAG						= Ld34.class.getName();

	public final static int					VELOCITY_ITERS			= 3;
	public final static int					POSITION_ITERS			= 2;
	public final static int					MAX_FPS					= 60;
	public final static int					MIN_FPS					= 15;
	public final static float				MAX_STEPS				= 1f + MAX_FPS / MIN_FPS;
	public final static float				TIME_STEP				= 1f / MAX_FPS;

	public static final int					STATE_POS_SELECT_X		= 0;
	public static final int					STATE_POS_SELECT_Y		= 1;
	public static final int					STATE_DIRECTION_SELECT	= 2;
	public static final int					STATE_GROWING			= 3;
	public final static int[]				STATES					= { STATE_POS_SELECT_X, STATE_POS_SELECT_Y, STATE_DIRECTION_SELECT,
			STATE_GROWING };

	public static float						UNIT_SCALE;

	SpriteBatch								batch;
	PolygonSpriteBatch						polyBatch;
	ShapeRenderer							shapeRenderer;
	Box2DDebugRenderer						box2dRenderer;
	OrthographicCamera						camFace;
	OrthographicCamera						camBeard;
	World									world;

	float									zoom					= 1f;

	protected boolean						debugEnabled			= true;

	protected Body							ground;
	protected Body							head;
	protected Body							hair;
	protected Body							beard;
	protected Array<Body>					facials					= new Array<Body>();
	protected Array<Chain>					chains					= new Array<Chain>();

	private Chain							currentChain;
	private int								currentState			= 0;
	private long							startTime;
	private long							chainStartTime;
	private long							chainSegmentTime;
	private Color							colorBeard				= new Color(0x222222FF);
	private Color							colorBeard2				= new Color(0x888888FF);
	private Color							colorSkin				= new Color(0xBBAAAAFF);
	private Color							colorBlush				= new Color(0xDDAAAAFF);
	private float							selectionMoveSpeed		= 0.01f;
	private float							rotateSpeed				= 7f;
	private Vector2							selectionPos			= new Vector2(0, 0);
	private Vector2							selectionPosWorld		= new Vector2(0, 0);
	private Vector2							selectionMove			= new Vector2(selectionMoveSpeed, 0);
	private PolygonShape					chainShape;
	private Polygon							beardPolygon;
	private Vector2							beardCenter;

	private Pixmap							pix;
	private Texture							textureSolid;
	private Box2DSprite						beardBox2dSprite;
	private EarClippingTriangulator			triangulator;
	private HashMap<Fixture, PolygonSprite>	polySprites				= new HashMap<Fixture, PolygonSprite>();

	@Override
	public void create() {
		UNIT_SCALE = 48f / new Float(Gdx.graphics.getWidth());
		selectionMoveSpeed = 1 * UNIT_SCALE;
		selectionMove = new Vector2(selectionMoveSpeed, 0);

		batch = new SpriteBatch();
		shapeRenderer = new ShapeRenderer();
		box2dRenderer = new Box2DDebugRenderer();
		polyBatch = new PolygonSpriteBatch();

		world = new World(new Vector2(0, -10f), true);
		world.setContactListener(new Ld34Contactlistener(this));

		triangulator = new EarClippingTriangulator();
		pix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
		pix.setColor(Color.WHITE);
		pix.fill();
		textureSolid = new Texture(pix);

		chainShape = new PolygonShape();
		chainShape.setAsBox(0.5f, 0.3f);
		beardCenter = new Vector2();
		beardPolygon = new Polygon();

		// Load all assets
		Assets.instance.init();
		Assets.instance.loadAll();
		Assets.assetsManager.finishLoading();
		Assets.instance.onFinishLoading();

		beardBox2dSprite = new Box2DSprite(Assets.instance.skin.get("beard", TextureRegion.class));

		camFace = new OrthographicCamera(Gdx.graphics.getWidth() / 4f * UNIT_SCALE, Gdx.graphics.getHeight() / 4f * UNIT_SCALE);
		camFace.translate(36 * UNIT_SCALE, 24 * UNIT_SCALE);
		// cam.zoom = 0.5f;
		camFace.update();

		Gdx.input.setInputProcessor(new Ld34InputProcessor(this));

		MapUtils.loadMap(this, Assets.mapHead);
	}

	@Override
	public void render() {
		update();
		Gdx.gl.glClearColor(0.4f, 0.4f, 0.4f, 1.0f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		final Camera cam = camFace;

		shapeRenderer.setProjectionMatrix(cam.combined);
		polyBatch.setProjectionMatrix(cam.combined);

		{ // fill head
			shapeRenderer.begin(ShapeType.Filled);
			shapeRenderer.setColor(colorSkin);

			drawCircles(head.getFixtureList());

			shapeRenderer.end();
		}

		// { // outline head
		// shapeRenderer.begin(ShapeType.Line);
		// shapeRenderer.setColor(colorSkin);
		//
		// outlinePolys(head.getFixtureList());
		// drawCircles(head.getFixtureList());
		//
		// shapeRenderer.end();
		// }

		{ // fill polys
			polyBatch.setColor(Color.WHITE);

			// polyBatch.begin();
			// fillPolys(hair.getFixtureList(), colorBeard);
			// polyBatch.end();

			polyBatch.begin();
			fillPolys(head.getFixtureList(), colorSkin);
			polyBatch.end();
		}

		{
			batch.begin();

			batch.setColor(Color.WHITE);
			batch.setProjectionMatrix(cam.combined);

			Box2DSprite.draw(batch, world);

			batch.end();
		}

		{
			// shapeRenderer.begin(ShapeType.Filled);
			shapeRenderer.begin(ShapeType.Line);
			shapeRenderer.setColor(Color.RED);

			final Vector2 v1 = new Vector2();
			Box2DUtils.aabb(beard).getCenter(v1).add(selectionPos);
			final Vector2 v2 = v1.cpy();
			if (currentState == STATE_POS_SELECT_X) {
				v1.y = -100;
				v2.y = 100;
				shapeRenderer.line(v1, v2);
			} else if (currentState == STATE_POS_SELECT_Y) {
				v1.x = -100;
				v2.x = 100;
				shapeRenderer.line(v1, v2);
			} else if (currentState == STATE_DIRECTION_SELECT) {
				final Vector2 triAddVec = selectionMove.cpy().scl(4);
				final Vector2 v3 = v1.cpy().add(triAddVec);
				v1.add(triAddVec.rotate(90));
				v2.add(triAddVec.rotate(180));
				shapeRenderer.triangle(v1.x, v1.y, v2.x, v2.y, v3.x, v3.y);
			}

			shapeRenderer.end();
		}

		if (debugEnabled) {
			box2dRenderer.render(world, cam.combined);
		}
	}

	private void fillPolys(Array<Fixture> array, Color color) {
		for (final Fixture fix : array) {
			final Shape shape = fix.getShape();
			if (shape instanceof PolygonShape) {
				final Polygon polygon = new Polygon();
				Box2DUtils.as((PolygonShape) shape, polygon);
				polygon.setOrigin(fix.getBody().getWorldCenter().x, fix.getBody().getWorldCenter().y);

				PolygonSprite polySprite = polySprites.get(fix);
				if (polySprite == null) {
					polySprite = createPolySprite(polygon.getTransformedVertices());
					polySprites.put(fix, polySprite);
				} else {
					polySprite.setOrigin(polygon.getOriginX(), polygon.getOriginY());
					// polySprite.setPosition(polygon.getX(), polygon.getY());
				}
				fillPoly(polySprite, color);
			}
		}
	}

	private void outlinePolys(final Array<Fixture> fixtures) {
		for (final Fixture fix : fixtures) {
			final Shape shape = fix.getShape();
			if (shape instanceof PolygonShape) {
				final Polygon poly = new Polygon();
				Box2DUtils.as((PolygonShape) shape, poly);
				poly.translate(fix.getBody().getWorldCenter().x, fix.getBody().getWorldCenter().y);
				shapeRenderer.polygon(poly.getTransformedVertices());
			}
		}
	}

	private void drawCircles(final Array<Fixture> fixtures) {
		for (final Fixture fix : fixtures) {
			final Shape shape = fix.getShape();
			if (shape instanceof CircleShape) {
				final Circle circle = new Circle();
				Box2DUtils.as((CircleShape) shape, circle);
				circle.setPosition(fix.getBody().getPosition().cpy().add(((CircleShape) shape).getPosition()));
				shapeRenderer.circle(circle.x, circle.y, circle.radius, 32);
			}
		}
	}

	private void fillPoly(PolygonSprite poly, Color color) {
		polyBatch.setColor(color);
		// TODO fix HACK. Why -3.2f?
		polyBatch.draw(poly.getRegion(), poly.getOriginX() - 3.2f, poly.getOriginY());
		// textureSolid2.dispose();
	}

	private PolygonSprite createPolySprite(float[] transformedVertices) {
		final short[] tris = triangulator.computeTriangles(transformedVertices).items;
		final PolygonRegion polyReg = new PolygonRegion(new TextureRegion(textureSolid), transformedVertices, tris);
		final PolygonSprite poly = new PolygonSprite(polyReg);
		return poly;
	}

	private void update() {
		// camBeard.update();

		world.step(TIME_STEP, VELOCITY_ITERS, POSITION_ITERS);

		Box2DUtils.aabb(beard).getCenter(beardCenter);
		selectionPosWorld.set(beardCenter).add(selectionPos);
		if (currentState == STATE_POS_SELECT_X || currentState == STATE_POS_SELECT_Y) {
			if (Box2DUtils.as(beard.getFixtureList().first(), beardPolygon).contains(selectionPosWorld.cpy().add(selectionMove))) {
				selectionPos.add(selectionMove);
			} else {
				selectionMove.scl(-1f);
				selectionPos.add(selectionMove);
			}
		} else if (currentState == STATE_DIRECTION_SELECT) {
			selectionMove.rotate(rotateSpeed);
		} else if (currentState == STATE_GROWING) {
			if (1000 < System.currentTimeMillis() - chainStartTime) {
				incCurrentState();
			} else if (100 < System.currentTimeMillis() - chainSegmentTime) {
				Gdx.app.log(TAG, "Extending chain");
				chainSegmentTime = System.currentTimeMillis();
				currentChain.extend();
			}
		}
	}

	public void incCurrentState() {
		currentState++;
		if (currentState > STATE_GROWING) {
			currentState = STATE_POS_SELECT_X;
		}
		Gdx.app.log(TAG, "New State is " + currentState);

		if (currentState == STATE_POS_SELECT_X) {
			selectionPos.x = 0;
			selectionPos.y = 0;
			selectionMove.x = selectionMoveSpeed;
			selectionMove.y = 0;
		} else if (currentState == STATE_POS_SELECT_Y) {
			selectionPos.y = 0;
			selectionMove.x = 0;
			selectionMove.y = selectionMoveSpeed;
		} else if (currentState == STATE_GROWING) {

			final Builder builder = new Builder() {

				BodyDef				bodyDef		= new BodyDef();
				FixtureDef			fixtureDef	= new FixtureDef();
				RevoluteJointDef	jointDef	= new RevoluteJointDef();
				// WeldJointDef jointDef = new WeldJointDef();

				{ // constructor
					bodyDef.type = BodyType.DynamicBody;
					fixtureDef.shape = chainShape;
					fixtureDef.density = 0.2f;
					fixtureDef.filter.categoryBits = 0x0100;
					jointDef.localAnchorA.y = -Box2DUtils.height(chainShape) / 2;
					jointDef.localAnchorB.y = Box2DUtils.height(chainShape) / 2;
					// jointDef.frequencyHz = 0;
					// jointDef.referenceAngle = selectionMove.angleRad(new Vector2(1, 0));
					// jointDef.lowerAngle = -0.1f;
					// jointDef.upperAngle = 0.1f;
				}

				@Override
				public Body createSegment(int index, int length, Chain chain) {
					bodyDef.position.x = selectionPosWorld.x;
					bodyDef.position.y = selectionPosWorld.y;
					final float angle = selectionMove.cpy().rotate(90).angleRad();
					Gdx.app.log(TAG, "Angle " + angle);
					bodyDef.angle = angle;
					final Body segment = world.createBody(bodyDef);
					segment.setUserData(beardBox2dSprite);
					final Fixture segFix = segment.createFixture(fixtureDef);
					segFix.setUserData(beardBox2dSprite);
					if (index == 0) {
						final WeldJointDef jd = new WeldJointDef();
						jd.initialize(head, segment, selectionPosWorld);
						// jd.referenceAngle = angle;
						jd.frequencyHz = 0;
						// jd.bodyA = head;
						// jd.bodyB = segment;
						// jd.localAnchorA.x = head.getLocalPoint(selectionPosWorld).x;
						// jd.localAnchorA.y = head.getLocalPoint(selectionPosWorld).y;
						// jd.localAnchorB.x = 0f;
						// jd.localAnchorB.y = 0f;
						world.createJoint(jd);
					}
					selectionPosWorld.add(selectionMove.cpy().nor().scl(0.25f));
					return segment;
				}

				@Override
				public Connection createConnection(Body seg1, int seg1index, Body seg2, int seg2index) {
					jointDef.bodyA = seg1;
					jointDef.bodyB = seg2;
					return new Connection(world.createJoint(jointDef));
				}
			};

			currentChain = new Chain(builder);
			chains.add(currentChain);
			chainStartTime = System.currentTimeMillis();
			chainSegmentTime = System.currentTimeMillis();
			currentChain.extend();
		} else {
			// selectionPos.x = 0;
			// selectionPos.y = 0;
			// selectionMove.y = 0;
			// selectionMove.x = 0;
		}
	}

	public int getCurrentState() {
		return currentState;
	}

	@Override
	public void dispose() {
		super.dispose();
		shapeRenderer.dispose();
		batch.dispose();
		box2dRenderer.dispose();
		world.dispose();
		pix.dispose();
		Assets.instance.dispose();
	}
}