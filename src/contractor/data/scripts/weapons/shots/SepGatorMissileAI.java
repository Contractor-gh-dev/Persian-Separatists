package contractor.data.scripts.weapons.shots;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicTargeting;

public class SepGatorMissileAI implements MissileAIPlugin, GuidedMissileAI {

	//////////////////////
	//     SETTINGS     //
	//////////////////////

	//Time to complete a wave in seconds.
	private final float WAVE_TIME = 3;

	//Max angle of the waving in degree (divided by 3 with ECCM). Set to a negative value to avoid all waving.
	private final float WAVE_AMPLITUDE = 9;

	//Damping of the turn speed when closing on the desired aim. The smaller the snappier.
	private final float DAMPING = 0.05f;

	//Does the missile switch its target if it has been destroyed?
	private final boolean TARGET_SWITCH = true;

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
	private final MagicTargeting.targetSeeking seeking = MagicTargeting.targetSeeking.IGNORE_SOURCE;

	//Target class priorities
	//set to 0 to ignore that class
	private final int fighters = 8;
	private final int frigates = 5;
	private final int destroyers = 3;
	private final int cruisers = 2;
	private final int capitals = 1;

	//Arc to look for targets into
	//set to 360 or more to ignore
	private final int SEARCH_CONE = 120;

	//range in which the missile seek a target in game units.
	private final int MAX_SEARCH_RANGE = 750;

	//should the missile fall back to the closest enemy when no target is found within the search parameters
	//only used with limited search cones
	private final boolean FAILSAFE = false;

	//range under which the missile start to get progressively more precise in game units.
	private float PRECISION_RANGE = 300f;

	//Leading loss without ECCM hullmod. The higher, the less accurate the leading calculation will be.
	//   1: perfect leading with and without ECCM
	//   2: half precision without ECCM
	//   3: a third as precise without ECCM. Default
	//   4, 5, 6 etc : 1/4th, 1/5th, 1/6th etc precision.
	private final float ECCM = 1.75f;   //A VALUE BELOW 1 WILL PREVENT THE MISSILE FROM EVER HITTING ITS TARGET!


	//////////////////////
	//    VARIABLES     //
	//////////////////////

	//max speed of the missile after modifiers.
	//Random starting offset for the waving.
	private final float OFFSET;
	private CombatEngineAPI engine;
	private final MissileAPI MISSILE;
	private final float maxSpeed;
	private CombatEntityAPI target;
	private Vector2f lead = new Vector2f();
	private boolean launch = true;
	private float timer = 0, check = 0f;
	private float lastAngle;

	//////////////////////
	//  DATA COLLECTING //

	//////////////////////

	public SepGatorMissileAI(MissileAPI missile, ShipAPI launchingShip) {
		this.MISSILE = missile;
		maxSpeed = missile.getMaxSpeed();
		//calculate the precision range factor
		PRECISION_RANGE = (float) Math.pow((2 * PRECISION_RANGE), 2);
		OFFSET = (float) (Math.random() * MathUtils.FPI * 2);
		lastAngle = missile.getFacing();
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
		if (Global.getCombatEngine().isPaused() || MISSILE.isFading() || MISSILE.isFizzling()) {
			return;
		}

		//assigning a target if there is none or it got destroyed
		if (target == null || (TARGET_SWITCH && ((target instanceof ShipAPI && !((ShipAPI) target).isAlive()) || !engine.isEntityInPlay(target)))) {
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
			//forced acceleration by default
			MISSILE.giveCommand(ShipCommand.ACCELERATE);
			if (target == null) {
				float cAngle = lastAngle;
				cAngle += (float) (WAVE_AMPLITUDE * 2 * FastTrig.cos(OFFSET + MISSILE.getElapsed() * (2 * MathUtils.FPI / WAVE_TIME)));
				float aimAngle = MathUtils.getShortestRotation(MISSILE.getFacing(), cAngle);
				if (aimAngle < 0) {
					MISSILE.giveCommand(ShipCommand.TURN_RIGHT);
				} else {
					MISSILE.giveCommand(ShipCommand.TURN_LEFT);
				}
			}
			return;
		}

		if (target.getCollisionClass() == CollisionClass.NONE) {
			target = null;
			return;
		}

		if (Math.abs(MathUtils.getShortestRotation(MISSILE.getFacing(), VectorUtils.getAngle(MISSILE.getLocation(), target.getLocation()))) > SEARCH_CONE / 2f) {
			target = null;
			MISSILE.giveCommand(ShipCommand.ACCELERATE);
			lastAngle = MISSILE.getFacing();
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
			lead = AIUtils.getBestInterceptPoint(
					MISSILE.getLocation(),
					maxSpeed * ECCM, //if eccm is intalled the point is accurate, otherwise it's placed closer to the target (almost tailchasing)
					target.getLocation(),
					target.getVelocity()
			);
			//null pointer protection
			if (lead == null) {
				lead = target.getLocation();
			}
		}

		//best velocity vector angle for interception
		float correctAngle = VectorUtils.getAngle(
				MISSILE.getLocation(),
				lead
		);


		//velocity angle correction
		float offCourseAngle = MathUtils.getShortestRotation(
				VectorUtils.getFacing(MISSILE.getVelocity()),
				correctAngle
		);

		float correction = MathUtils.getShortestRotation(
				correctAngle,
				VectorUtils.getFacing(MISSILE.getVelocity()) + 180
		)
				* 0.5f * //oversteer
				(float) ((FastTrig.sin(MathUtils.FPI / 90 * (Math.min(Math.abs(offCourseAngle), 45))))); //damping when the correction isn't important

		//modified optimal facing to correct the velocity vector angle as soon as possible
		correctAngle = correctAngle + correction;


		//waving
		correctAngle += (float) (WAVE_AMPLITUDE * check * FastTrig.cos(OFFSET + MISSILE.getElapsed() * (2 * MathUtils.FPI / WAVE_TIME)));


		//target angle for interception
		float aimAngle = MathUtils.getShortestRotation(MISSILE.getFacing(), correctAngle);

		MISSILE.giveCommand(ShipCommand.ACCELERATE);

		if (aimAngle < 0) {
			MISSILE.giveCommand(ShipCommand.TURN_RIGHT);
		} else {
			MISSILE.giveCommand(ShipCommand.TURN_LEFT);
		}

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
		this.target = target;
	}

	@SuppressWarnings("EmptyMethod")
	public void init(CombatEngineAPI engine) {
	}
}