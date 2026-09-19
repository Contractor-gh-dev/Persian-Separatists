package contractor.data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import contractor.data.scripts.statuseffects.StatusEffect;
import contractor.data.scripts.statuseffects.StatusEffectUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicTargeting;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static contractor.data.scripts.util.ContractorUtils.applyForce;
import static org.magiclib.util.MagicTargeting.pickShipTarget;

public class SepInterdictor extends BaseShipSystemScript {
	public static final float BASE_RANGE = 1000f;
	private static final Color CORE = new Color(10, 100, 255, 255);
	private static final Color FRINGE = new Color(25, 150, 200, 200);
	private static final Color OVERLOAD_CORE = new Color(0, 120, 180, 255);
	private static final Color OVERLOAD_FRINGE = new Color(0, 190, 180, 200);
	private final IntervalUtil interval = new IntervalUtil(0.1f, 0.25f);
	private ShipAPI heldTarget;
	private int heldTargetStrikes;
	private static final EmpArcEntityAPI.EmpArcParams PARAMS = new EmpArcEntityAPI.EmpArcParams(); static {
		PARAMS.segmentLengthMult = 7f;
		PARAMS.zigZagReductionFactor = 0.2f;
		PARAMS.fadeOutDist = 400f;
		PARAMS.minFadeOutMult = 2.5f;
		PARAMS.flickerRateMult = 0.8f;
	}

	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		float range;
		if (stats.getEntity() instanceof ShipAPI ship)
			range = stats.getSystemRangeBonus().computeEffective(BASE_RANGE);
		else return;

		for (WeaponAPI wep : ship.getAllWeapons()) {
			if (state == State.IN || state == State.ACTIVE) {
				if (wep.getId().equals("drg_boltzman_deco"))
					wep.setForceFireOneFrame(true);
				else if (wep.getId().equals("sep_interdictor_deco"))
					wep.setForceFireOneFrame(true);
			}

		}

		CombatEngineAPI engine = Global.getCombatEngine();

		if (state == State.ACTIVE) {
			float amount = engine.getElapsedInLastFrame();
			interval.advance(amount);
			if (interval.intervalElapsed()) {
				List<Vector2f> points = new ArrayList<>();
				for (WeaponSlotAPI slot : ship.getHullSpec().getAllWeaponSlotsCopy()) {
					if (slot.isSystemSlot()) {
						Vector2f slotLocation = new Vector2f(slot.computePosition(ship));
						points.add(slotLocation);
					}
				}

				ShipAPI target = pickShipTarget(ship, MagicTargeting.targetSeeking.NO_RANDOM, (int) range, 120, 1, 1, 1, 1, 1);
				if (heldTarget != target) {
					heldTarget = target;
					heldTargetStrikes = 0;
				}
				Global.getSoundPlayer().playSound("energy_lash_fire", 0.9f, 0.7f, ship.getLocation(), ship.getVelocity());

				if (target != null && !target.getCollisionClass().equals(CollisionClass.NONE) && MathUtils.getDistanceSquared(ship.getLocation(), target.getLocation()) < range * range) {
					Vector2f targetLoc = target.getLocation();
					float damage;
					if (target.getShield() == null)
						damage = 10f;
					else
						damage = 10f / target.getShield().getFluxPerPointOfDamage();

					for (Vector2f point : points) {
						EmpArcEntityAPI arc = engine.spawnEmpArc(ship, point, ship, target, DamageType.ENERGY, damage, 10f, range, "pseudoparticle_jet_hit_heavy",
								(float) (Math.random() * 8f + 4f), FRINGE, CORE, PARAMS);
						arc.setFadedOutAtStart(true);
						arc.setWarping(0.2f);
						if (!arc.isShieldHit()) {
							heldTargetStrikes++;
						}
					}

					if (heldTargetStrikes > 42) {
						heldTargetStrikes = 0;
						StatusEffectUtils.addOrMaintainEffect(target, StatusEffect.StatusType.ELECTRIC, 1.25f, 1.5f);
						Global.getSoundPlayer().playSound("voltaic_discharge_emp_impact", 0.95f, 0.6f, target.getLocation(), target.getVelocity());
						int j = (int) (4 + (Math.random() * 6));
						for (int i = 0; i < j; i++) {
							Vector2f rand = MathUtils.getRandomPointInCircle(target.getLocation(), 160f);
							engine.spawnEmpArcVisual(targetLoc, target, rand, null, (float) (Math.random() * 12f + 6f), OVERLOAD_FRINGE, OVERLOAD_CORE);
						}
					}

					Vector2f targetVel = new Vector2f(target.getVelocity());
					int scale = Math.max(Math.round((target.getMass() * 2) / ship.getMass()), 1);
					targetVel.scale(scale);
					Vector2f.add(targetVel, ship.getVelocity(), targetVel);
					targetVel.x = targetVel.x / (scale + 1);
					targetVel.y = targetVel.y / (scale + 1);
					target.getVelocity().x = targetVel.x;
					target.getVelocity().y = targetVel.y;

					float force = (MathUtils.getDistanceSquared(ship.getLocation(), targetLoc) / (range * range)) * 120f;
					applyForce(target, ship.getLocation(), force);

					float speed = Math.max(1f / scale, 0.2f);
					stats.getMaxSpeed().modifyMult(id, speed);
					stats.getAcceleration().modifyMult(id, speed);
					stats.getDeceleration().modifyMult(id, speed);
					stats.getTurnAcceleration().modifyMult(id, speed);
					stats.getMaxTurnRate().modifyMult(id, speed);
				} else {
					for (Vector2f point : points) {
						float min = ship.getFacing() - 45f;
						float max = ship.getFacing() + 45f;
						min = Misc.normalizeAngle(min);
						max = Misc.normalizeAngle(max);
						Vector2f rand = MathUtils.getRandomPointInCone(ship.getLocation(), 192f, min, max);
						engine.spawnEmpArcVisual(point, ship, rand, null, (float) (Math.random() * 8f + 4f), FRINGE, CORE);
					}
					unapply(stats, id);
				}
			}
		}
	}

	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getMaxSpeed().unmodify(id);
		stats.getAcceleration().unmodify(id);
		stats.getDeceleration().unmodify(id);
		stats.getTurnAcceleration().unmodify(id);
		stats.getMaxTurnRate().unmodify(id);
		heldTarget = null;
		heldTargetStrikes = 0;
	}

	public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
		if (system.isCoolingDown())
			return "COOLING";

		float range = ship.getMutableStats().getSystemRangeBonus().computeEffective(BASE_RANGE);
		ShipAPI target = pickShipTarget(ship, MagicTargeting.targetSeeking.NO_RANDOM, (int) range, 120, 1, 1, 1, 1, 1);
		if (target != null) {
			if (MathUtils.getDistanceSquared(ship.getLocation(), target.getLocation()) < range * range) {
				if (system.isActive())
					return "LOCKED";
				else
					return "READY";
			} else
				return "OUT OF RANGE";
		}
		return "NO TARGET";
	}
}
