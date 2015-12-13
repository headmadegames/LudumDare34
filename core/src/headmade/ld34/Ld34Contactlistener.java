package headmade.ld34;

import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Manifold;

public class Ld34Contactlistener implements ContactListener {
	private static final String	TAG	= Ld34Contactlistener.class.getName();

	private Ld34				ld34;

	public Ld34Contactlistener(Ld34 ld34) {
		this.ld34 = ld34;
	}

	@Override
	public void beginContact(Contact contact) {
		if (contact.getFixtureA().getFilterData().categoryBits == Ld34.CATEGORYBITS_HAIR
				|| contact.getFixtureB().getFilterData().categoryBits == Ld34.CATEGORYBITS_HAIR) {
			if (contact.getFixtureA().getFilterData().categoryBits == Ld34.CATEGORYBITS_FACIALS
					|| contact.getFixtureB().getFilterData().categoryBits == Ld34.CATEGORYBITS_FACIALS) {

				ld34.scoreService.incFacialCollisions();

				if (ld34.getCurrentState() == Ld34.STATE_GROWING
						&& (contact.getFixtureA().equals(ld34.lastChainFixture) || contact.getFixtureB().equals(ld34.lastChainFixture))) {
					ld34.scoreService.incFacialCreations();
				}
			}
		}
	}

	@Override
	public void endContact(Contact contact) {
	}

	@Override
	public void preSolve(Contact contact, Manifold oldManifold) {
	}

	@Override
	public void postSolve(Contact contact, ContactImpulse impulse) {
	}

}
