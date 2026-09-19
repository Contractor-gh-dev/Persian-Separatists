package contractor.data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import contractor.data.scripts.util.ContractorUtils;
import contractor.data.scripts.util.CommandMissileInterface;

import java.util.ArrayList;

public class SepBoweEveryFrame implements EveryFrameWeaponEffectPluginWithAdvanceAfter, CommandMissileInterface {
	private final ArrayList<MissileAPI> activeMissiles = new ArrayList<>();
	private int maxMissiles;
	private boolean doOnceTest = true, isTorpLaunch = false;
	private ShipAPI ship;

	public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
	}

	public void appendMissile(MissileAPI missile) {
		activeMissiles.add(missile);
	}

	public int getActive() {
		return activeMissiles.size() * 2;
	}

	private void doOnce(WeaponAPI weapon) {
		this.ship = weapon.getShip();
		maxMissiles = ContractorUtils.getCommandMissileMax(ship);
		if (weapon.getSpec().hasTag("sepcmdtorp"))
			isTorpLaunch = true;
		doOnceTest = false;
	}

	@Override
	public void advanceAfter(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
		if (engine.isPaused())
			return;

		if (doOnceTest)
			doOnce(weapon);

		if (!activeMissiles.isEmpty()) {
			int j = activeMissiles.size();
			for (; j > 0; j--) {
				MissileAPI active = activeMissiles.get(j - 1);
				if (active == null || !engine.isMissileAlive(active) || active.isFizzling())
					activeMissiles.remove(j - 1);
			}
		}

		if (ContractorUtils.countActiveCommandMissiles(ship) >= maxMissiles
				|| isTorpLaunch && ContractorUtils.countActiveCommandMissiles(ship) + 2 > maxMissiles)
			weapon.setRemainingCooldownTo(6f);
		else if (weapon.getCooldownRemaining() > 5.5f)
			weapon.setRemainingCooldownTo(4.9f);
	}
}
