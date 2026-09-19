package contractor.data.scripts.weapons.shots;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.combat.entities.terrain.Asteroid;
import contractor.data.scripts.plugins.PlayerMissileCommand.PlayerMissileCommandData;
import contractor.data.scripts.util.ContractorUtils;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicTargeting;

import java.util.Iterator;
import java.util.List;

import static contractor.data.scripts.util.ContractorStaticVars.MISSILE_COMMAND_DATAID;

public class SepVLSMissileAI implements MissileAIPlugin, GuidedMissileAI {

	//////////////////////
	//     SETTINGS     //
	/// ///////////////////

	//Time to complete a wave in seconds.
	private final float WAVE_TIME = 3f;
	//Max angle of the waving in degree (divided by 3 with ECCM). Set to a negative value to avoid all waving.
	private final float WAVE_AMPLITUDE = 20f;
	//Angle with the target beyond which the missile turn around without accelerating. Avoid endless circling.
	//  Set to a negative value to disable
	private final float OVERSHOT_ANGLE = 20f;

	//Damping of the turn speed when closing on the desired aim. The smaller the snappier.
	private final float DAMPING = 0.05f;

	private final MagicTargeting.targetSeeking seeking = MagicTargeting.targetSeeking.NO_RANDOM;
	//range in which the missile seek a target in game units.
	private final int MAX_SEARCH_RANGE;

	//range under which the missile start to get progressively more precise in game units.
	private float PRECISION_RANGE = 1000f;

	//Leading loss without ECCM hullmod. The higher, the less accurate the leading calculation will be.
	//   1: perfect leading with and without ECCM
	//   2: half precision without ECCM
	//   3: a third as precise without ECCM. Default
	//   4, 5, 6 etc : 1/4th, 1/5th, 1/6th etc precision.
	private float ECCM = 1.5f;   //A VALUE BELOW 1 WILL PREVENT THE MISSILE FROM EVER HITTING ITS TARGET!

	//////////////////////
	//    VARIABLES     //
	/// ///////////////////

	//max speed of the missile after modifiers.
	private final float MAX_SPEED, OFFSET, flank, guidanceRange, guidanceRangeSquared, maxLifetime, shutdownDistSQ, missileMaxRangeSQ;
	private CombatEngineAPI engine;
	private final MissileAPI MISSILE;
	private final ShipAPI launchingShip;
	private final int maxMissiles;
	private CombatEntityAPI target;
	private Vector2f lead = new Vector2f();
	private boolean launch = true, goTerminal = false, shieldOn = false, shieldTurnedOff = false;
	private float timer = 0f, check = 0f, initial = 0f, lifetime = 0f, sWasOnTime = 0f, shieldArc = -1f, shieldFacing = 0, targetFacing = 0, targetFlux = 0;
	private int systemMode = -1, asteroidDir = 0;
	private Vector2f targetLoc, missileLoc;
	ShieldType shieldType;

	//////////////////////
	//  DATA COLLECTING //

	//////////////////////

	public SepVLSMissileAI(MissileAPI missile, ShipAPI launchingShip) {
		this.MISSILE = missile;
		this.launchingShip = launchingShip;
		MAX_SPEED = missile.getMaxSpeed();
		if (missile.getSource().getVariant().getHullMods().contains("eccm"))
			ECCM = 1;
		if (Math.random() > 0.5f)
			flank = 80f;
		else
			flank = -80f;

		OFFSET = (float) (Math.random() * MathUtils.FPI * 2);
		maxMissiles = ContractorUtils.getCommandMissileMax(launchingShip);
		guidanceRange = PRECISION_RANGE * 0.62f;
		guidanceRangeSquared = guidanceRange * guidanceRange;
		maxLifetime = missile.getMaxFlightTime();
		MAX_SEARCH_RANGE = (int) (MISSILE.getMaxRange() * 1.125f);
		missileMaxRangeSQ = MISSILE.getMaxRange() * MISSILE.getMaxRange() * 0.97f;
		shutdownDistSQ = (MISSILE.getMaxRange() * 1.125f) * (MISSILE.getMaxRange() * 1.125f);
		PRECISION_RANGE = (PRECISION_RANGE * PRECISION_RANGE);
	}

	//////////////////////
	//   MAIN AI LOOP   //

	//////////////////////

	@Override
	public void advance(float amount) {
		if (engine != Global.getCombatEngine())
			this.engine = Global.getCombatEngine();
		if (Global.getCombatEngine().isPaused() || MISSILE.isFading() || MISSILE.isFizzling())
			return;
		if (MathUtils.getDistanceSquared(MISSILE.getLocation(), launchingShip.getLocation()) > shutdownDistSQ && !launchingShip.isFighter()) {
			MISSILE.setMaxRange(1f);
			MISSILE.setMaxFlightTime(0.1f);
		}
		if (ContractorUtils.countActiveCommandMissiles(launchingShip) > maxMissiles)
			return;

		lifetime += amount;
		timer += amount;
		systemMode = getSystemMode();
		//engine.maintainStatusForPlayerShip("dev_cmd_missile", Global.getSettings().getSpriteName("systems", "contractor_commandmissile"), "Command Mode", systemMode + "", false);

		//assigning a target if there is none or it got destroyed
		if (systemMode != 6) {
			if (target == null || (target instanceof ShipAPI && !((ShipAPI) target).isAlive()) || !engine.isEntityInPlay(target)) {
				goTerminal = false;
				setTarget(MagicTargeting.pickTarget(MISSILE, seeking, MAX_SEARCH_RANGE, 360, 1, 2, 4, 4, 4, false));
				initial = 0f;
				shieldArc = -1f;

				if (target == null) {
					if (checkObstacles()) {
						MISSILE.giveCommand(ShipCommand.DECELERATE);
						float correctAngle = modeEvade(MISSILE.getFacing());
						correctAngle = MathUtils.getShortestRotation(MISSILE.getFacing(), correctAngle);
						if (correctAngle < 0)
							MISSILE.giveCommand(ShipCommand.TURN_RIGHT);
						else
							MISSILE.giveCommand(ShipCommand.TURN_LEFT);
					} else
						MISSILE.giveCommand(ShipCommand.ACCELERATE);
					return;
				}
			}
		}
		missileLoc = MISSILE.getLocation();
		float targetDistanceSQ, ourDistanceSQ, standoffDistanceSQ;
		standoffDistanceSQ = MathUtils.getDistanceSquared(launchingShip.getLocation(), target.getLocation());
		ourDistanceSQ = MathUtils.getDistanceSquared(missileLoc, launchingShip.getLocation());

		if (systemMode != 6) {
			targetLoc = target.getLocation();
			targetDistanceSQ = MathUtils.getDistanceSquared(missileLoc, targetLoc);
		} else {
			lead = launchingShip.getMouseTarget();
			targetDistanceSQ = MathUtils.getDistanceSquared(missileLoc, lead);
		}
		//finding lead point to aim to
		if ((launch || timer >= check) && systemMode != 6) {
			launch = false;
			timer -= check;
			//set the next check time
			check = Math.min(0.12f, Math.max(0.04f, targetDistanceSQ / PRECISION_RANGE));

			//best intercepting point
			lead = AIUtils.getBestInterceptPoint(
					missileLoc,
					MAX_SPEED * ECCM, //if eccm is intalled the point is accurate, otherwise it's placed closer to the target (almost tailchasing)
					targetLoc,
					target.getVelocity()
			);
			//null pointer protection
			if (lead == null)
				lead = targetLoc;

			if (!goTerminal) {
				gatherData(standoffDistanceSQ, targetDistanceSQ);
				if (target == null)
					lead = MathUtils.getPointOnCircumference(launchingShip.getLocation(), 1000f, launchingShip.getFacing());
				boolean danger = checkObstacles();
				if (shieldTurnedOff) {
					systemMode = 7;
					if (sWasOnTime + 0.5f > lifetime)
						shieldTurnedOff = false;
				} else if (systemMode < 0) {
					if (ourDistanceSQ > missileMaxRangeSQ) { //todo: figure out why this isnt triggering and missiles just void themselves
						systemMode = 7; //direct override
					} else if (standoffDistanceSQ > missileMaxRangeSQ) {
						systemMode = 2; //direct
					} else if (shieldType != null && shieldType == ShieldType.PHASE) {
						systemMode = 2; //direct
					} else if (targetFlux > 0.9f) {
						systemMode = 4; //vulture
					} else if (shieldArc >= 140f && shieldType != null && shieldType == ShieldType.FRONT) {
						systemMode = 3; //tail
					} else if (shieldArc > 30f) {
						systemMode = 0; //flank
					} else if (shieldType == null && ((ShipAPI) target).getMaxSpeed() > 79f) {
						systemMode = 3; //tail
					} else {
						systemMode = 1; //side
					}
				}
				if (danger)
					systemMode = 5; //evade
				//engine.maintainStatusForPlayerShip("dev_cmd_missile1", Global.getSettings().getSpriteName("systems", "contractor_commandmissile"), "Command Mode1:", systemMode + "", false);
			}
		}

		//best velocity vector angle for interception
		float correctAngle = VectorUtils.getAngle(missileLoc, lead);

		if (!goTerminal) {
			//engine.maintainStatusForPlayerShip("dev_cmd_missile22", Global.getSettings().getSpriteName("systems", "contractor_commandmissile"), "Command Mode22", systemMode + "", false);
			correctAngle = switch (systemMode) {
				case 5 -> modeEvade(correctAngle);
				case 4 -> modeVulture(correctAngle, targetDistanceSQ);
				case -1, 0 -> modeFlank(correctAngle, targetDistanceSQ);
				case 1 -> modeSideAttack(amount, correctAngle, targetDistanceSQ);
				case 3 -> modeTailStrike(correctAngle, targetDistanceSQ);
				case 6, 7 -> correctAngle;
				default -> modeDirect(correctAngle); //case 2
			};
		} //else
		//engine.maintainStatusForPlayerShip("dev_cmd_missile2", Global.getSettings().getSpriteName("systems", "contractor_commandmissile"), "Command Mode2", "Gone Terminal", false);
		//target angle for interception
		float aimAngle = MathUtils.getShortestRotation(MISSILE.getFacing(), correctAngle);

		if (Math.abs(aimAngle) < OVERSHOT_ANGLE) { //overshot turning
			MISSILE.giveCommand(ShipCommand.ACCELERATE);
		} else if (Math.abs(aimAngle) > OVERSHOT_ANGLE * 2.5f) {
			MISSILE.giveCommand(ShipCommand.DECELERATE);
		} else {
			if (MISSILE.getVelocity().length() < MISSILE.getMaxSpeed() * 0.5f)
				MISSILE.giveCommand(ShipCommand.ACCELERATE);
			if (aimAngle < 0)
				MISSILE.giveCommand(ShipCommand.STRAFE_RIGHT);
			else
				MISSILE.giveCommand(ShipCommand.STRAFE_LEFT);
		}

		if (aimAngle < 0)
			MISSILE.giveCommand(ShipCommand.TURN_RIGHT);
		else
			MISSILE.giveCommand(ShipCommand.TURN_LEFT);

		// Damp angular velocity if the missile aim is getting close to the targeted angle
		if (Math.abs(aimAngle) < Math.abs(MISSILE.getAngularVelocity()) * DAMPING)
			MISSILE.setAngularVelocity(aimAngle / DAMPING);
	}

	//////////////////////
	//    TARGETING     //

	//////////////////////

	private int getSystemMode() {
		if (launchingShip != engine.getPlayerShip())
			return -1;
		if (engine.getCustomData().get(MISSILE_COMMAND_DATAID) != null) {
			int mode = (((PlayerMissileCommandData) engine.getCustomData().get(MISSILE_COMMAND_DATAID)).systemMode);
			if (mode == 6)
				goTerminal = false;
			return mode;
		}
		return -1;
	}

	private void gatherData(float standoffDistanceSQ, float targetDistanceSQ) {
		if (MathUtils.isWithinRange(MISSILE, target, guidanceRange * 0.33f)) {
			goTerminal = true;
			return;
		} else if (lifetime > (maxLifetime * 0.7f)) {
			goTerminal = true;
			return;
		} else if (standoffDistanceSQ - (target.getCollisionRadius() * target.getCollisionRadius()) > shutdownDistSQ) {
			goTerminal = false;
			target = null;
			return;
		} else if (target instanceof ShipAPI && !((ShipAPI) target).isFighter()) {
			targetFlux = ((ShipAPI) target).getFluxLevel();
			goTerminal = ((ShipAPI) target).getFluxTracker().isOverloaded();
			if (goTerminal)
				return;
		}

		targetFacing = target.getFacing();
		ShieldAPI targetShield = target.getShield();
		if (targetShield != null) {
			shieldType = targetShield.getType();
			shieldArc = -1f;
			if (shieldType != ShieldType.PHASE) {
				if (shieldType == ShieldType.FRONT) {
					shieldFacing = targetFacing;
					shieldArc = targetShield.getActiveArc();
					if (shieldArc < 62f)
						shieldArc = 62f;
				} else {
					if (targetShield.isOff()) {
						if (shieldOn) {
							shieldOn = false;
							if (targetDistanceSQ < guidanceRangeSquared * 0.7f) {
								shieldTurnedOff = true;
								sWasOnTime = lifetime;
							}
						}
						shieldFacing = targetFacing;
						shieldArc = targetShield.getArc() * 0.5f;
					} else {
						shieldOn = true;
						shieldFacing = targetShield.getFacing();
						shieldArc = targetShield.getActiveArc();
					}
				}
				if (shieldArc > 300f)
					shieldArc = 300f;
				shieldArc *= 0.5f;
			} else {
				if (((ShipAPI) target).isPhased()) {
					shieldOn = true;
				} else {
					if (shieldOn)
						if (targetDistanceSQ < guidanceRangeSquared * 0.9f) {
							shieldTurnedOff = true;
							sWasOnTime = lifetime;
						}
					shieldOn = false;
				}
			}
		}
	}

	private boolean checkObstacles() {
		int collRadius = (int) MISSILE.getCollisionRadius();
		float baseRange = collRadius * 15f;
		for (int i = -2; i < 3; i++) {
			Iterator<Object> obstacles = engine.getAsteroidGrid().getCheckIterator(MathUtils.getPointOnCircumference(MISSILE.getLocation(), baseRange, MISSILE.getFacing()), baseRange * 0.9f, baseRange * 0.9f);
			Vector2f end = MathUtils.getPointOnCircumference(MISSILE.getLocation(), baseRange * 1.2f, MISSILE.getFacing() + (7f * i));
			List<Vector2f> raySegment = ContractorUtils.buildRaySegment(MISSILE.getLocation(), end, (int) ((baseRange * 1.2f) / collRadius));
			while (obstacles.hasNext()) {
				Asteroid asteroid = (Asteroid) obstacles.next();
				for (Vector2f point : raySegment) {
					if (asteroid.isPointInBounds(point)) {
						asteroidDir = i;
						return true;
					}
				}
			}
			obstacles = engine.getShipGrid().getCheckIterator(MathUtils.getPointOnCircumference(MISSILE.getLocation(), baseRange, MISSILE.getFacing()), baseRange * 0.8f, baseRange * 0.8f);
			while (obstacles.hasNext()) {
				ShipAPI ship = (ShipAPI) obstacles.next();
				if (!ship.isHulk() || ship.getOwner() == launchingShip.getOwner())
					continue;
				for (Vector2f point : raySegment) {
					if (MathUtils.isWithinRange(point, ship.getLocation(), 10f + ship.getCollisionRadius())) {
						asteroidDir = i;
						return true;
					}
				}
			}
		}
		return false;
	}

	private float modeFlank(float correctAngle, float distanceSquared) {
		float correctedAngle = correctAngle;
		float ourAngle = Misc.getAngleDiff(targetFacing, Misc.getAngleInDegrees(targetLoc, missileLoc)); //angles use local offset from target facing which is 0
		float opposedAngle;
		if (shieldType == null || shieldType == ShieldType.PHASE || shieldType == ShieldType.FRONT || shieldArc <= 0f)
			opposedAngle = 180f;
		else {
			opposedAngle = MathUtils.getShortestRotation(targetFacing, shieldFacing) + 180f;
		}
		float arcAngle = opposedAngle + Math.min(90f, (180f - shieldArc));
		float goalRange = Misc.getAngleDiff(opposedAngle, arcAngle);

		if (Misc.getAngleDiff(opposedAngle, ourAngle) < goalRange)
			return correctAngle;
		else {
			correctedAngle += flank * Math.min(1f, Math.max(0.2f, guidanceRangeSquared / distanceSquared));
			correctedAngle = Misc.normalizeAngle(correctedAngle);
		}

		//engine.maintainStatusForPlayerShip("dev_cmd_missileF", Global.getSettings().getSpriteName("systems", "contractor_commandmissile"), "Command Missile", "Flank", false);
		return correctedAngle;
	}

	private float modeSideAttack(float amount, float correctAngle, float distanceSquared) {
		float correctedAngle = correctAngle;

		if (MathUtils.isWithinRange(MISSILE, target, guidanceRange)) {
			correctedAngle += (flank * 0.75f) * Math.min(1f, Math.max(0.2f, guidanceRangeSquared / distanceSquared));
			correctedAngle = Misc.normalizeAngle(correctedAngle);
			initial += amount;
		} else
			correctedAngle += ((float) (0.8f * WAVE_AMPLITUDE * FastTrig.cos(OFFSET + MISSILE.getElapsed() * (2 * MathUtils.FPI / WAVE_TIME)))) //add wave
					* Math.min(1f, Math.max(0.3f, MathUtils.getDistanceSquared(MISSILE.getLocation(), targetLoc) / guidanceRange)); //range damper

		if (initial > maxLifetime * 0.2f)
			goTerminal = true;
		//engine.maintainStatusForPlayerShip("dev_cmd_missileS", Global.getSettings().getSpriteName("systems", "contractor_commandmissile"), "Command Missile", "Side", false);
		return correctedAngle;
	}

	private float modeDirect(float correctAngle) {
		float correctedAngle = correctAngle;

		correctedAngle += ((float) (WAVE_AMPLITUDE * FastTrig.cos(OFFSET + MISSILE.getElapsed() * (2 * MathUtils.FPI / WAVE_TIME)))) //add wave
				* Math.min(1f, Math.max(0.3f, MathUtils.getDistanceSquared(MISSILE.getLocation(), targetLoc) / guidanceRange)); //range damper

		correctedAngle = Misc.normalizeAngle(correctedAngle);

		//engine.maintainStatusForPlayerShip("dev_cmd_missileD", Global.getSettings().getSpriteName("systems", "contractor_commandmissile"), "Command Missile ", "Direct", false);
		return correctedAngle;
	}

	private float modeVulture(float correctAngle, float distanceSquared) {
		float correctedAngle = correctAngle;
		correctedAngle += flank * Math.min(1f, Math.max(0.25f, guidanceRangeSquared / distanceSquared));
		correctedAngle = Misc.normalizeAngle(correctedAngle);

		//engine.maintainStatusForPlayerShip("dev_cmd_missileV", Global.getSettings().getSpriteName("systems", "contractor_commandmissile"), "Command Missile", "Vulture", false);
		return correctedAngle;
	}

	private float modeTailStrike(float correctAngle, float distanceSquared) {
		float correctedAngle = correctAngle;
		float ourAngle = Misc.getAngleDiff(targetFacing, Misc.getAngleInDegrees(targetLoc, missileLoc)); //angles use local offset from target facing which is 0
		float opposedAngle = 180f;
		float goalRange = 45;

		if (Misc.getAngleDiff(opposedAngle, ourAngle) < goalRange)
			goTerminal = true;
		else {
			correctedAngle += flank * Math.min(1f, Math.max(0.2f, guidanceRangeSquared / distanceSquared));
			correctedAngle = Misc.normalizeAngle(correctedAngle);
		}

		//engine.maintainStatusForPlayerShip("dev_cmd_missileT", Global.getSettings().getSpriteName("systems", "contractor_commandmissile"), "Command Missile", "Tail Strike", false);
		return correctedAngle;
	}

	private float modeEvade(float correctAngle) {
		float correctedAngle = correctAngle;
		float flankModify = flank;
		if (Math.abs(MathUtils.getShortestRotation(MISSILE.getFacing(), correctedAngle)) > 60f)
			return correctedAngle;

		if ((asteroidDir < 0 && flankModify < 0) || (asteroidDir > 0 && flankModify > 0))
			flankModify *= -1;
		correctedAngle += flankModify;
		correctedAngle = Misc.normalizeAngle(correctedAngle);
		if (flankModify < 0)
			MISSILE.giveCommand(ShipCommand.STRAFE_RIGHT);
		else
			MISSILE.giveCommand(ShipCommand.STRAFE_LEFT);

		//engine.maintainStatusForPlayerShip("dev_cmd_missileE", Global.getSettings().getSpriteName("systems", "contractor_commandmissile"), "Command Missile", "Evade" + asteroidDir + " " + (int) flank, false);
		return correctedAngle;
	}

	private float modeMouse() {
		return VectorUtils.getAngle(missileLoc, lead);
	}

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

	@SuppressWarnings("EmptyMethod")
	public void init(CombatEngineAPI engine) {
	}
}
