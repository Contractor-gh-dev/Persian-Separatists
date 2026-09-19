package contractor.data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;

public class AndrossGlowEffect implements EveryFrameWeaponEffectPlugin {

	@Override
	public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
		ShipAPI ship = null;
		if (!engine.isPaused()) {
			ship = weapon.getShip();
		}
		if (ship == null || ship.isHulk()) {
			return;
		}

		if (ship.getHullLevel() < 0.5f) {
			weapon.setForceFireOneFrame(true);
		}
	}
}
