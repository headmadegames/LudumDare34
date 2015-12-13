package headmade.ld34;

import java.util.HashMap;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
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

	public static final int					CATEGORYBITS_HAIR		= 0x0002;
	public static final int					CATEGORYBITS_FACIALS	= 0x0001;
	public static final int					MASKBITS_HAIR			= CATEGORYBITS_FACIALS;
	public static final int					MASKBITS_FACIALS		= CATEGORYBITS_HAIR;

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
	OrthographicCamera						camPix;
	World									world;
	ScoreService							scoreService;

	float									zoom					= 1f;

	protected boolean						debugEnabled			= false;

	protected Body							ground;
	protected Body							head;
	protected Body							hair;
	protected Body							beard;
	protected Body							body;
	protected Array<Body>					facials					= new Array<Body>();
	protected Array<Chain>					chains					= new Array<Chain>();
	protected Fixture						lastChainFixture;

	protected boolean						showLogo				= true;
	protected boolean						showInstructions		= true;

	private Chain							currentChain;
	private int								currentState			= 0;
	private long							startTime;
	private long							chainStartTime;
	private long							chainSegmentTime;
	private Color							colorBeard				= new Color(0x404040FF);
	private Color							colorBeard2				= new Color(0x888888FF);
	private Color							colorSkin				= new Color(0xFADACAFF);
	private Color							colorBlush				= new Color(0xFFAA99FF);
	private float							selectionMoveSpeed;
	private float							growSpeed;
	private float							rotateSpeed				= 7f;
	private float							chainRotSpeedRad		= 0.04f;
	private Vector2							selectionPos			= new Vector2(0, 0);
	private Vector2							selectionPosWorld		= new Vector2(0, 0);
	private Vector2							selectionMove			= new Vector2(selectionMoveSpeed, 0);
	private float							chainReferenceAngle		= 0f;
	private PolygonShape					chainShape;
	private Polygon							beardPolygon;
	private Vector2							beardCenter;

	private Pixmap							pix;
	private Texture							textureSolid;
	private TextureRegion					logoTex;
	private TextureRegion					instructionsTex;
	private Box2DSprite						beardBox2dSprite;
	private EarClippingTriangulator			triangulator;
	private HashMap<Fixture, PolygonSprite>	polySprites				= new HashMap<Fixture, PolygonSprite>();
	private Sound							sndGrow;
	private Sound							sndReleave;

	@Override
	public void create() {
		UNIT_SCALE = 48f / new Float(Gdx.graphics.getWidth());
		selectionMoveSpeed = 1 * UNIT_SCALE;
		growSpeed = 4;
		selectionMove = new Vector2(selectionMoveSpeed, 0);

		batch = new SpriteBatch();
		shapeRenderer = new ShapeRenderer();
		polyBatch = new PolygonSpriteBatch();
		box2dRenderer = new Box2DDebugRenderer();

		world = new World(new Vector2(0, -10f), true);
		world.setContactListener(new Ld34Contactlistener(this));

		triangulator = new EarClippingTriangulator();
		pix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
		pix.setColor(Color.WHITE);
		pix.fill();
		textureSolid = new Texture(pix);

		chainShape = new PolygonShape();
		chainShape.setAsBox(0.3f, 0.3f);
		beardCenter = new Vector2();
		beardPolygon = new Polygon();

		// Load all assets
		Assets.instance.init();
		Assets.instance.loadAll();
		Assets.assetsManager.finishLoading();
		Assets.instance.onFinishLoading();

		final Music music = Assets.assetsManager.get(Assets.music, Music.class);
		music.setVolume(0.2f);
		music.setLooping(true);
		music.play();

		sndGrow = Assets.assetsManager.get(Assets.sndGrow, Sound.class);
		sndReleave = Assets.assetsManager.get(Assets.sndReleave, Sound.class);

		beardBox2dSprite = new Box2DSprite(Assets.instance.skin.get("beard", TextureRegion.class));
		beardBox2dSprite.setZIndex(1000);

		camFace = new OrthographicCamera(Gdx.graphics.getWidth() / 4f * UNIT_SCALE, Gdx.graphics.getHeight() / 4f * UNIT_SCALE);
		camFace.translate(34 * UNIT_SCALE, 24 * UNIT_SCALE);
		// cam.zoom = 0.5f;
		camFace.update();

		camPix = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		logoTex = Assets.instance.skin.get("logo", TextureRegion.class);
		instructionsTex = Assets.instance.skin.get("Instructions", TextureRegion.class);

		scoreService = new ScoreService(this);

		Gdx.input.setInputProcessor(new Ld34InputProcessor(this));

		MapUtils.loadMap(this, Assets.mapHead);
	}

	@Override
	public void render() {
		Gdx.gl.glClearColor(0.8f, 0.9f, 1f, 1.0f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		if (showLogo || showInstructions) {
			batch.begin();

			batch.setColor(Color.WHITE);
			batch.setProjectionMatrix(camPix.combined);

			if (showLogo) {
				batch.draw(logoTex, -camPix.viewportWidth / 2, -camPix.viewportHeight / 2);
			} else {
				batch.draw(instructionsTex, -camPix.viewportWidth / 2, -camPix.viewportHeight / 2);
			}

			batch.end();
			return;
		}

		update();
		final Camera cam = camFace;

		shapeRenderer.setProjectionMatrix(cam.combined);
		polyBatch.setProjectionMatrix(cam.combined);

		{ // fill hair
			shapeRenderer.begin(ShapeType.Filled);

			shapeRenderer.setColor(colorBeard);
			drawCircles(hair.getFixtureList());

			shapeRenderer.end();
		}

		{ // fill polys
			polyBatch.setColor(Color.WHITE);

			// polyBatch.begin();
			// fillPolys(hair.getFixtureList(), colorBeard);
			// polyBatch.end();

			polyBatch.begin();
			fillPolys(head.getFixtureList(), colorSkin);
			polyBatch.end();
		}

		{ // outline head
			shapeRenderer.begin(ShapeType.Line);
			shapeRenderer.setColor(Color.BLACK);

			outlinePolys(head.getFixtureList());
			// drawCircles(head.getFixtureList());

			shapeRenderer.end();
		}

		{ // fill head
			shapeRenderer.begin(ShapeType.Filled);

			shapeRenderer.setColor(colorSkin);
			drawCircles(head.getFixtureList());

			shapeRenderer.end();
		}

		{
			batch.begin();

			batch.setColor(Color.WHITE);
			batch.setProjectionMatrix(cam.combined);

			Box2DSprite.draw(batch, world);

			batch.end();
		}

		{ // draw UI
			Gdx.gl.glEnable(GL20.GL_BLEND);
			Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
			shapeRenderer.begin(ShapeType.Filled);
			// shapeRenderer.begin(ShapeType.Line);
			shapeRenderer.setColor(new Color(0xFF000088));
			// shapeRenderer.getColor().a = 0.7f;

			final Vector2 v1 = selectionPosWorld.cpy();
			// Box2DUtils.aabb(beard).getCenter(v1).add(selectionPos);
			final Vector2 v2 = v1.cpy();
			if (currentState == STATE_POS_SELECT_X) {
				v1.y = -100;
				v2.y = 100;
				shapeRenderer.line(v1, v2);
			} else if (currentState == STATE_POS_SELECT_Y) {
				v1.x = -100;
				v2.x = 100;
				shapeRenderer.line(v1, v2);
			} else if (currentState == STATE_GROWING || currentState == STATE_DIRECTION_SELECT) {
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
		polyBatch.draw(poly.getRegion(), poly.getOriginX(), poly.getOriginY());
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
			// would the next move be out of bounds?
			if (Box2DUtils.as(beard.getFixtureList().first(), beardPolygon).contains(selectionPosWorld.cpy().add(selectionMove))) {
				selectionPos.add(selectionMove);
			} else {
				selectionMove.scl(-1f);
				selectionPos.add(selectionMove);
			}
		} else if (currentState == STATE_DIRECTION_SELECT) {
			selectionMove.rotate(rotateSpeed);
		} else if (currentState == STATE_GROWING) {
			if (Gdx.app.getType().equals(ApplicationType.Android) || Gdx.app.getType().equals(ApplicationType.iOS)) {
				if (Gdx.input.isTouched(0)) {

				}
			} else {
				if (Gdx.input.isButtonPressed(Buttons.LEFT) && Gdx.input.isButtonPressed(Buttons.RIGHT)) {
					// abort
					incCurrentState();
					return;
				} else {
					if (Gdx.input.isButtonPressed(Buttons.LEFT)) {
						chainReferenceAngle += chainRotSpeedRad;
						selectionMove.rotateRad(chainRotSpeedRad);
					}
					if (Gdx.input.isButtonPressed(Buttons.RIGHT)) {
						chainReferenceAngle += -chainRotSpeedRad;
						selectionMove.rotateRad(-chainRotSpeedRad);
					}
				}
				if (3000 < System.currentTimeMillis() - chainStartTime) {
					incCurrentState();
				} else if (300 < System.currentTimeMillis() - chainSegmentTime) {
					chainSegmentTime = System.currentTimeMillis();
					currentChain.extend();
				}
			}
		}
	}

	public void incCurrentState() {
		currentState++;
		if (currentState > STATE_GROWING) {
			currentState = STATE_POS_SELECT_X;
			sndGrow.stop();
			sndReleave.play(0.5f);
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

			sndGrow.play(0.5f);

			final Builder builder = new Builder() {

				BodyDef				bodyDef				= new BodyDef();
				FixtureDef			fixtureDef			= new FixtureDef();
				RevoluteJointDef	jointDef			= new RevoluteJointDef();
				// WeldJointDef jointDef = new WeldJointDef();
				private float		chainBodyDefAngle	= 0f;

				{ // constructor
					bodyDef.type = BodyType.DynamicBody;
					fixtureDef.shape = chainShape;
					fixtureDef.density = 0.01f;
					fixtureDef.filter.categoryBits = CATEGORYBITS_HAIR;
					fixtureDef.filter.maskBits = MASKBITS_HAIR;
					// jointDef.localAnchorA.y = -Box2DUtils.height(chainShape) / 2;
					// jointDef.localAnchorB.y = Box2DUtils.height(chainShape) / 2;
					jointDef.localAnchorB.y = selectionMove.cpy().scl(growSpeed).len();
					// jointDef.localAnchorB.y = selectionMove.cpy().len();
					jointDef.enableLimit = true;
					// jointDef.frequencyHz = 0;
					// jointDef.lowerAngle = -0.1f;
					// jointDef.upperAngle = 0.1f;
				}

				@Override
				public Body createSegment(int index, int length, Chain chain) {
					bodyDef.position.x = selectionPosWorld.x;
					bodyDef.position.y = selectionPosWorld.y;
					if (index == 0) {
						chainBodyDefAngle = selectionMove.cpy().rotate(90).angleRad();
					} else {
						chainBodyDefAngle += chainReferenceAngle;
					}
					bodyDef.angle = chainBodyDefAngle;// + chainReferenceAngle;
					// Gdx.app.log(TAG, "Creating segment with bodydef angle " + bodyDef.angle);
					final Body segment = world.createBody(bodyDef);
					segment.setUserData(beardBox2dSprite);
					lastChainFixture = segment.createFixture(fixtureDef);
					lastChainFixture.setUserData(beardBox2dSprite);
					if (index == 0) {
						final WeldJointDef jd = new WeldJointDef();
						jd.initialize(head, segment, selectionPosWorld);
						jd.referenceAngle = chainReferenceAngle;
						// jd.frequencyHz = 0;
						world.createJoint(jd);
					}
					selectionPos.add(selectionMove.cpy().scl(growSpeed));
					selectionPosWorld.add(selectionMove.cpy().scl(growSpeed));
					return segment;
				}

				@Override
				public Connection createConnection(Body seg1, int seg1index, Body seg2, int seg2index) {
					jointDef.referenceAngle = chainReferenceAngle;
					chainReferenceAngle = 0f;
					// Gdx.app.log(TAG, "Reference Angle is " + jointDef.referenceAngle);
					// jointDef.frequencyHz = 0;
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

			scoreService.addBeardChain(currentChain);
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

	@Override
	public void resize(int width, int height) {
		camFace.viewportWidth = width * UNIT_SCALE / 4f;
		camFace.viewportHeight = height * UNIT_SCALE / 4f;
		// camFace.position.set(width * UNIT_SCALE / 2, height * UNIT_SCALE / 2, 0);
		camFace.update();

		// camPix.viewportWidth = width;
		// camPix.viewportHeight = height;
		// camPix.update();

		super.resize(width, height);
	}
}
