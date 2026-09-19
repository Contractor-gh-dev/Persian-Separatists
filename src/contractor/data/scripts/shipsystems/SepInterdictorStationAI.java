package contractor.data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import static com.fs.starfarer.api.util.Misc.getShipWeight;
import static com.fs.starfarer.api.util.Misc.isInArc;

public class SepInterdictorStationAI implements ShipSystemAIScript {
	private ShipSystemAPI system;
	private ShipAPI ship;
	private float elapsed = 0f;
	private static final float TARGET_DISTANCE = 1600f * 1600f;

	public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
		this.system = system;
		this.ship = ship;
	}

	@Override
	public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
		elapsed += amount;
		if (elapsed < 0.1f)
			return;
		elapsed -= 0.1f;

		ShipAPI targSwap = target;
		if (targSwap == null) {
			targSwap = retryTarget();
			if (targSwap == null) {
				deactivate();
				return;
			}
		}

		if (targSwap.getCollisionClass().equals(CollisionClass.NONE)) {
			targSwap = retryTarget();
			if (targSwap == null) {
				deactivate();
				return;
			}
		}
		if (MathUtils.getDistanceSquared(ship.getLocation(), targSwap.getLocation()) > TARGET_DISTANCE - targSwap.getCollisionRadius()) {
			targSwap = retryTarget();
			if (targSwap == null) {
				deactivate();
				return;
			}
		}
		if (!isInArc(ship.getFacing(), 110f, ship.getLocation(), targSwap.getLocation())) {
			targSwap = retryTarget();
			if (targSwap == null) {
				deactivate();
				return;
			}
		}

		float desire = evaluate(targSwap);
		if (desire > 0.5f)
			activate();
		else
			deactivate();
	}

	private ShipAPI retryTarget() {
		WeightedRandomPicker<ShipAPI> picker = new WeightedRandomPicker<>();
		for (ShipAPI test : Global.getCombatEngine().getShips()) {
			float weight = countEnemyWeightInArcAroundLocation(test, ship.getOwner(), ship.getFacing(), 110f, 1600f, null, true);
			if (weight > 0f)
				picker.add(test, weight);
		}

		return picker.pick();
	}

	private float evaluate(ShipAPI target) {
		float eval = 1f;
		if (ship.getCaptain().getPersonalityAPI().getId().equals(Personalities.AGGRESSIVE) || ship.getCaptain().getPersonalityAPI().getId().equals(Personalities.RECKLESS))
			eval += 0.5f;

		eval *= 1.75f - ship.getFluxLevel();
		eval *= 0.25f + ship.getHullLevel();

		float missileThreat = 0f;

		for (MissileAPI missile : CombatUtils.getMissilesWithinRange(ship.getLocation(), 600f)) {
			switch (missile.getDamageType()) {
				case FRAGMENTATION -> missileThreat += (missile.getDamageAmount() / ship.getHitpoints()) * 0.1f;
				case KINETIC -> missileThreat += (missile.getDamageAmount() / ship.getHitpoints()) * 0.125f;
				case HIGH_EXPLOSIVE -> missileThreat += (missile.getDamageAmount() / ship.getHitpoints()) * 0.25f;
				default -> missileThreat += (missile.getDamageAmount() / ship.getHitpoints()) * 0.15f;
			}
		}
		eval -= missileThreat * 0.8f;

		missileThreat = 0f;

		for (DamagingProjectileAPI proj : CombatUtils.getProjectilesWithinRange(ship.getLocation(), 400f)) {
			switch (proj.getDamageType()) {
				case FRAGMENTATION -> missileThreat += (proj.getDamageAmount() / ship.getHitpoints()) * 0.075f;
				case KINETIC -> missileThreat += (proj.getDamageAmount() / ship.getHitpoints()) * 0.1f;
				case HIGH_EXPLOSIVE -> missileThreat += (proj.getDamageAmount() / ship.getHitpoints()) * 0.2f;
				default -> missileThreat += (proj.getDamageAmount() / ship.getHitpoints()) * 0.15f;
			}
		}
		eval -= missileThreat * 0.4f;

		return eval;
	}

	private static float countEnemyWeightInArcAroundLocation(ShipAPI ship, int owner, float dir, float arc, float maxRange,
															 ShipAPI ignore, boolean awareOnly) {
		Vector2f loc = ship.getLocation();

		float weight = 0;
		if (ship.isStationModule()) return weight;
		if (ship.isFighter()) return weight;
		if (ship.isHulk()) return weight;
		if (ship.isDrone()) return weight;
		if (ship.isShuttlePod()) return weight;
		if (ship.getOwner() == 100) return weight;
		if (owner == ship.getOwner()) return weight;
		if (ship == ignore) return weight;
		if (awareOnly && !Global.getCombatEngine().isAwareOf(owner, ship)) return weight;

		float dist = MathUtils.getDistanceSquared(loc, ship.getLocation());
		if (dist > maxRange * maxRange) return weight;

		if (arc >= 360f || isInArc(dir, arc, loc, ship.getLocation())) {
			weight += getShipWeight(ship);
			//weight += other.getHullSize().ordinal();
		}

		return weight;
	}

	private void activate() {
		if (!system.isActive())
			ship.giveCommand(ShipCommand.USE_SYSTEM, null, 0);
	}

	private void deactivate() {
		if (system.isActive())
			ship.giveCommand(ShipCommand.USE_SYSTEM, null, 0);
	}
}
