package contractor.data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import contractor.data.scripts.util.ContractorUtils;
import contractor.data.scripts.util.CommandMissileInterface;

import java.util.ArrayList;

public class SepVLSEveryFrame implements EveryFrameWeaponEffectPluginWithAdvanceAfter, CommandMissileInterface {
	private final ArrayList<MissileAPI> activeMissiles = new ArrayList<>();
	private int maxMissiles;
	private boolean doOnceTest = true;
	private ShipAPI ship;

	public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
	}

	public void appendMissile(MissileAPI missile) {
		activeMissiles.add(missile);
	}

	public int getActive() {
		return activeMissiles.size();
	}

	private void doOnce(WeaponAPI weapon) {
		this.ship = weapon.getShip();
		maxMissiles = ContractorUtils.getCommandMissileMax(ship);
		doOnceTest = false;
	}

	@Override
	public void advanceAfter(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
		if (engine.isPaused())
			return;

		if (doOnceTest)
			doOnce(weapon);

		if (!activeMissiles.isEmpty()) {
			int j = activeMissiles.size() - 1;
			for (; j >= 0; j--) {
				MissileAPI active = activeMissiles.get(j);
				if (active == null || !engine.isMissileAlive(active) || active.isFizzling())
					activeMissiles.remove(j);
			}
		}

		if (ContractorUtils.countActiveCommandMissiles(ship) >= maxMissiles)
			weapon.setRemainingCooldownTo(2f);
		else if (weapon.getCooldownRemaining() > weapon.getCooldown())
			weapon.setRemainingCooldownTo(weapon.getCooldown());
	}
}
