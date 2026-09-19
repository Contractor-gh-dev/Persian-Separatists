package contractor.data.scripts.shipsystems;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;

import java.util.ArrayList;
import java.util.List;

import static contractor.data.scripts.shipsystems.SepInterdictor.BASE_RANGE;

public class SepInterdictorAI implements ShipSystemAIScript {
	private ShipSystemAPI system;
	private ShipAPI ship;
	private CombatEngineAPI engine;
	private float timer = 0f, allyTimer = 0f;

	public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
		this.system = system;
		this.ship = ship;
		this.engine = engine;
	}

	@Override
	public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
		if (target == null || target.getCollisionClass().equals(CollisionClass.NONE)) {
			deactivate();
			return;
		}
		if (target.isFighter())
			return;
		float range = ship.getMutableStats().getSystemRangeBonus().computeEffective(BASE_RANGE);
		if (MathUtils.getDistanceSquared(ship.getLocation(), target.getLocation()) > (range * range) - target.getCollisionRadius()) {
			deactivate();
			return;
		}
		if (!Misc.isInArc(ship.getFacing(), 100f, ship.getLocation(), target.getLocation())) {
			deactivate();
			return;
		}
		timer += amount;
		if (timer < 0.1f)
			return;
		timer = 0f;

		float desire = evaluate(target);

		if (system.isActive())
			desire += 0.1f;
		if (desire > 0.5f)
			activate();
		else {
			deactivate();
			return;
		}
		ShipAPI ally = null;

		if (system.isActive()) {
			allyTimer += amount;
			if (allyTimer > 1f) {
				allyTimer = 0f;
				ally = findAlly();
			}
			if (ally != null)
				ship.getAIFlags().setFlag(AIFlags.MANEUVER_TARGET, 1.1f, ally);
		}
	}

	private ShipAPI findAlly() {
		List<ShipAPI> allShips = engine.getShips();
		List<ShipAPI> allies = new ArrayList<>();
		for (ShipAPI ship : allShips) {
			if (ship == this.ship || ship.getOwner() != this.ship.getOwner() || ship.isNonCombat(false) || ship.isFighter())
				continue;
			if (MathUtils.getDistanceSquared(this.ship.getLocation(), ship.getLocation()) > 1500f * 1500f)
				continue;

			allies.add(ship);
		}

		if (allies.isEmpty())
			return null;
		else if (allies.size() == 1)
			return allies.get(0);

		ShipAPI ally = null;

		for (ShipAPI ship : allies) {
			if (ally == null) {
				ally = ship;
				continue;
			}

			if (ally.getHullSize().ordinal() < ship.getHullSize().ordinal()) {
				ally = ship;
			} else if (ally.getHullSize().ordinal() == ship.getHullSize().ordinal()) {
				float allySTR = 0f, shipSTR = 0f;
				for (WeaponAPI wep : ally.getAllWeapons()) {
					allySTR += wep.getDerivedStats().getSustainedDps();
				}
				for (WeaponAPI wep : ship.getAllWeapons()) {
					shipSTR += wep.getDerivedStats().getSustainedDps();
				}
				allySTR *= ally.getHullLevel();
				shipSTR *= ship.getHullLevel();

				if (shipSTR > allySTR)
					ally = ship;
			}
		}
		return ally;
	}

	private float evaluate(ShipAPI target) {
		float eval = 1f;
		if (ship.getCaptain().getPersonalityAPI().getId().equals(Personalities.AGGRESSIVE) || ship.getCaptain().getPersonalityAPI().getId().equals(Personalities.RECKLESS))
			eval += 0.5f;

		List<ShipAPI> list = AIUtils.getNearbyAllies(ship, 1250f);
		eval += list.size() * 0.1f;
		eval *= 1.5f - ship.getFluxLevel();
		eval *= ship.getHullLevel();
		ShipAPI.HullSize size = target.getHullSize();

		switch (size) {
			case CAPITAL_SHIP -> eval *= 0.5f;
			case CRUISER -> eval *= 0.75f;
			case DESTROYER -> eval *= 1.1f;
			case FRIGATE -> eval *= 1.25f;
			case FIGHTER -> eval *= 0.4f;
		}

		if (ship.getAIFlags().hasFlag(AIFlags.RUN_QUICKLY)
				|| ship.getAIFlags().hasFlag(AIFlags.BACKING_OFF)
				|| ship.getAIFlags().hasFlag(AIFlags.DO_NOT_USE_FLUX)
				|| ship.getAIFlags().hasFlag(AIFlags.HAS_INCOMING_DAMAGE))
			eval *= 0.5f;

		if (target.getShield() != null && target.getShield().isOn())
			eval -= 0.15f;

		float missileThreat = 0f;

		for (MissileAPI missile : CombatUtils.getMissilesWithinRange(ship.getLocation(), 600f)) {
			switch (missile.getDamageType()) {
				case FRAGMENTATION -> missileThreat += (missile.getDamageAmount() / ship.getHitpoints()) * 0.1f;
				case KINETIC -> missileThreat += (missile.getDamageAmount() / ship.getHitpoints()) * 0.125f;
				case HIGH_EXPLOSIVE -> missileThreat += (missile.getDamageAmount() / ship.getHitpoints()) * 0.25f;
				default -> missileThreat += (missile.getDamageAmount() / ship.getHitpoints()) * 0.15f;
			}
		}

		eval -= missileThreat;
		missileThreat = 0f;

		for (DamagingProjectileAPI proj : CombatUtils.getProjectilesWithinRange(ship.getLocation(), 400f)) {
			switch (proj.getDamageType()) {
				case FRAGMENTATION -> missileThreat += (proj.getDamageAmount() / ship.getHitpoints()) * 0.075f;
				case KINETIC -> missileThreat += (proj.getDamageAmount() / ship.getHitpoints()) * 0.1f;
				case HIGH_EXPLOSIVE -> missileThreat += (proj.getDamageAmount() / ship.getHitpoints()) * 0.2f;
				default -> missileThreat += (proj.getDamageAmount() / ship.getHitpoints()) * 0.15f;
			}
		}

		eval -= missileThreat * 0.5f;

		return eval;
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
