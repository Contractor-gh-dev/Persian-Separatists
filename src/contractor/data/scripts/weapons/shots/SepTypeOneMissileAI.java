package contractor.data.scripts.weapons.shots;
//By Tartiflette, fast and highly customizable Missile AI.

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicTargeting;

import java.awt.*;

public class SepTypeOneMissileAI implements MissileAIPlugin, GuidedMissileAI {

	//////////////////////
	//     SETTINGS     //
	//////////////////////

	//Damping of the turn speed when closing on the desired aim. The smaller the snappier.
	private final float DAMPING = 0.05f;

	//Does the missile switch its target if it has been destroyed?
	private boolean TARGET_SWITCH = true;

	//Does the missile find a random target or aways tries to hit the ship's one?
	/*
	 *  NO_RANDOM,
	 * If the launching ship has a valid target within arc, the missile will pursue it.
	 * If there is no target, it will check for an unselected cursor target within arc.
	 * If there is none, it will pursue its closest valid threat within arc.
	 *
	 *  LOCAL_RANDOM,
	 * If the ship has a target, the missile will pick a random valid threat around that one.
	 * If the ship has none, the missile will pursue a random valid threat around the cursor, or itself.
	 * Can produce strange behavior if used with a limited search cone.
	 *
	 *  FULL_RANDOM,
	 * The missile will always seek a random valid threat within arc around itself.
	 *
	 *  IGNORE_SOURCE,
	 * The missile will pick the closest target of interest. Useful for custom MIRVs.
	 *
	 */
	private final MagicTargeting.targetSeeking seeking = MagicTargeting.targetSeeking.NO_RANDOM;

	//Target class priorities
	//set to 0 to ignore that class
	private final int fighters = 0;
	private final int frigates = 1;
	private final int destroyers = 3;
	private final int cruisers = 4;
	private final int capitals = 5;

	//Arc to look for targets into
	//set to 360 or more to ignore
	private int SEARCH_CONE = 360;

	//range in which the missile seek a target in game units.
	private final int MAX_SEARCH_RANGE = 2000;

	//should the missile fall back to the closest enemy when no target is found within the search parameters
	//only used with limited search cones
	private final boolean FAILSAFE = false;

	//range under which the missile start to get progressively more precise in game units.
	private float PRECISION_RANGE = 750f;

	//Leading loss without ECCM hullmod. The higher, the less accurate the leading calculation will be.
	//   1: perfect leading with and without ECCM
	//   2: half precision without ECCM
	//   3: a third as precise without ECCM. Default
	//   4, 5, 6 etc : 1/4th, 1/5th, 1/6th etc precision.
	private float ECCM = 1.25f;   //A VALUE BELOW 1 WILL PREVENT THE MISSILE FROM EVER HITTING ITS TARGET!


	//////////////////////
	//    VARIABLES     //
	//////////////////////

	//max speed of the missile after modifiers.
	private final float MAX_SPEED;
	private CombatEngineAPI engine;
	private final MissileAPI MISSILE;
	private CombatEntityAPI target;
	private Vector2f lead = new Vector2f();
	private boolean launch = true, initial = true;
	private float timer = 0, check = 0f, initialTimer = 0f, lockAngle;
	private final java.awt.Color eColor = new Color(150, 150, 255);

	//////////////////////
	//  DATA COLLECTING //

	//////////////////////

	public SepTypeOneMissileAI(MissileAPI missile, ShipAPI launchingShip) {
		this.MISSILE = missile;
		MAX_SPEED = missile.getMaxSpeed();
		if (missile.getSource().getVariant().getHullMods().contains("eccm")) {
			ECCM = 1;
		}
		//calculate the precision range factor
		PRECISION_RANGE = (float) Math.pow((2 * PRECISION_RANGE), 2);
	}

	//////////////////////
	//   MAIN AI LOOP   //

	//////////////////////

	@Override
	public void advance(float amount) {

		if (engine != Global.getCombatEngine()) {
			this.engine = Global.getCombatEngine();
		}

		//skip the AI if the game is paused, the missile is engineless or fading
		if (engine.isPaused() || MISSILE.isFading() || MISSILE.isFizzling()) {
			return;
		}

		//assigning a target if there is none or it got destroyed
		if (target == null
				|| (TARGET_SWITCH && ((target instanceof ShipAPI && !((ShipAPI) target).isAlive())
				|| !engine.isEntityInPlay(target))
		)
		) {
			setTarget(
					MagicTargeting.pickTarget(
							MISSILE,
							seeking,
							MAX_SEARCH_RANGE,
							SEARCH_CONE,
							fighters,
							frigates,
							destroyers,
							cruisers,
							capitals,
							FAILSAFE
					)
			);
			initialTimer = 0f;
			return;
		}

		if (target.getCollisionClass() == CollisionClass.NONE) {
			target = null;
			return;
		}

		timer += amount;
		//finding lead point to aim to
		if (launch || timer >= check) {
			launch = false;
			timer -= check;
			//set the next check time
			check = Math.min(
					0.25f,
					Math.max(
							0.05f,
							MathUtils.getDistanceSquared(MISSILE.getLocation(), target.getLocation()) / PRECISION_RANGE)
			);
			//best intercepting point
			lead = AIUtils.getBestInterceptPoint(
					MISSILE.getLocation(),
					MAX_SPEED * ECCM, //if eccm is intalled the point is accurate, otherwise it's placed closer to the target (almost tailchasing)
					target.getLocation(),
					target.getVelocity()
			);
			//null pointer protection
			if (lead == null) {
				lead = target.getLocation();
			}
		}

		//best velocity vector angle for interception
		float correctAngle = VectorUtils.getAngle(MISSILE.getLocation(), lead);

		//target angle for interception
		float aimAngle = MathUtils.getShortestRotation(MISSILE.getFacing(), correctAngle);
		if (!initial && Math.abs(MathUtils.getShortestRotation(lockAngle, correctAngle)) > 20f) {
			float angleLimit = Math.copySign(20f, aimAngle);
			correctAngle = Misc.normalizeAngle(lockAngle + angleLimit);
			aimAngle = MathUtils.getShortestRotation(MISSILE.getFacing(), correctAngle);
		}

		if (initial && Math.abs(aimAngle) < 75f) {
			initialTimer += amount;
			if (initialTimer >= 1.5f) {
				initial = false;
				TARGET_SWITCH = false;
				SEARCH_CONE = 180;
				lockAngle = correctAngle;
				Vector2f loc = MISSILE.getLocation();
				engine.spawnExplosion(loc, new Vector2f(0f, 0f), eColor, 20f, 0.5f);

				for (int i = 0; i < 20; i++) {
					float face = MISSILE.getFacing() + 180f;
					float rand = (float) ((Math.random() - 0.5f) * 15f);
					Vector2f vel;

					face += rand;
					if (face > 360f)
						face -= 360f;
					else if (face < 0f)
						face += 360f;

					vel = Misc.getUnitVectorAtDegreeAngle(face);
					VectorUtils.resize(vel, 128f + (float) ((Math.random() - 0.5f) * 30f), vel);

					engine.addHitParticle(loc, vel, 20f + (float) ((Math.random() - 0.5f) * 10f), 0.4f, 0.75f, eColor);
				}

			}
			if (MISSILE.getMoveSpeed() >= 50f)
				MISSILE.giveCommand(ShipCommand.DECELERATE);
		} else {
			if (Math.abs(aimAngle) < 45f)
				MISSILE.giveCommand(ShipCommand.ACCELERATE);
		}

		if (aimAngle < 0)
			MISSILE.giveCommand(ShipCommand.TURN_RIGHT);
		else
			MISSILE.giveCommand(ShipCommand.TURN_LEFT);

		// Damp angular velocity if the missile aim is getting close to the targeted angle
		if (Math.abs(aimAngle) < Math.abs(MISSILE.getAngularVelocity()) * DAMPING) {
			MISSILE.setAngularVelocity(aimAngle / DAMPING);
		}
	}

	//////////////////////
	//    TARGETING     //

	//////////////////////

	@Override
	public CombatEntityAPI getTarget() {
		return target;
	}

	@Override
	public void setTarget(CombatEntityAPI target) {
		if (target instanceof MissileAPI) {
			if (((MissileAPI) target).isFlare())
				return;
		}
		this.target = target;
	}
}