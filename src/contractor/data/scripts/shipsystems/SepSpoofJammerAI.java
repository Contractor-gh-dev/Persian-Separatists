package contractor.data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

import static contractor.data.scripts.shipsystems.SepSpoofJammer.BASE_RANGE;

public class SepSpoofJammerAI implements ShipSystemAIScript {
	private ShipAPI ship;
	private ShipSystemAPI system;
	private CombatEngineAPI engine;
	private float hurtScale = 0f;

	@Override
	public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
		this.ship = ship;
		this.system = system;
		this.engine = engine;
	}

	@Override
	public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
		if (ship.getCurrentCR() < 0.3f) {
			deactivate();
			return;
		}
		float range = ship.getMutableStats().getSystemRangeBonus().computeEffective(BASE_RANGE);
		List<ShipAPI> allShips = engine.getShips();
		List<ShipAPI> enemies = new ArrayList<>();
		for (ShipAPI s : allShips) {
			if (s.getOriginalOwner() == ship.getOriginalOwner())
				continue;
			if (s.isFighter() || s.isHulk())
				continue;
			float dist = MathUtils.getDistanceSquared(ship.getLocation(), s.getLocation());
			if (dist < (range * range))
				enemies.add(s);
		}
		if (enemies.isEmpty()) {
			deactivate();
			return;
		}
		for (int i = enemies.size() - 1; i >= 0; i--) {
			float dist = MathUtils.getDistanceSquared(ship.getLocation(), enemies.get(i).getLocation());
			if (dist < (1000f * 1000f))
				enemies.remove(i);
		}

		float desire = 0f;
		desire += 0.5f - ship.getFluxLevel();
		desire += -0.8f + ship.getHullLevel();
		if (ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.ESCORT_OTHER_SHIP))
			desire += 0.5f;

		if (target != null) {
			ShipAPI.HullSize targetSize = target.getHullSize();
			switch (targetSize) {
				case FRIGATE -> desire -= 0.2f;
				case DESTROYER -> desire += 0.1f;
				case CRUISER -> desire += 0.3f;
				case CAPITAL_SHIP -> desire += 0.35f;
				case FIGHTER -> desire -= 0.5f;
			}
			float halfPlus = (range * 0.5f) + 100f;
			desire *= MathUtils.getDistanceSquared(ship, target) / (halfPlus * halfPlus);
		}

		desire += enemies.size() * 0.1f;
		desire += AIUtils.getNearbyAllies(ship, 1000f).size() * 0.15f;

		if (ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.RUN_QUICKLY) || ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.BACKING_OFF))
			desire *= 0.75f;
		if (ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.DO_NOT_USE_FLUX) || ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.HAS_INCOMING_DAMAGE))
			desire *= 0.8f;
		if (ship.getSinceLastDamageTaken() < 0.2f) {
			desire *= 0.8f;
			hurtScale += amount * 0.33f;
			desire -= hurtScale;
		} else
			hurtScale = 0f;

		if (system.isActive())
			desire += 0.25f;

		if (ship == engine.getPlayerShip() && Global.getSettings().isDevMode())
			engine.maintainStatusForPlayerShip(this, system.getSpecAPI().getIconSpriteName(), system.getSpecAPI().getName(), "desire: " + desire, false);

		if (desire > 1f)
			activate();
		else
			deactivate();
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
