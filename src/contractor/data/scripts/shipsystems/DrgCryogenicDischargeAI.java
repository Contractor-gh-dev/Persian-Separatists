package contractor.data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.WeaponUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DrgCryogenicDischargeAI implements ShipSystemAIScript {
	private ShipAPI ship;
	private ShipSystemAPI system;
	private final List<WeaponAPI> systemWeapons = new ArrayList<>();
	private static final HashMap<ShipAPI.HullSize, Float> sizeScale = new HashMap<>(); static {
		sizeScale.put(ShipAPI.HullSize.FIGHTER, 0.4f);
		sizeScale.put(ShipAPI.HullSize.FRIGATE, 0.75f);
		sizeScale.put(ShipAPI.HullSize.DESTROYER, 0.5f);
		sizeScale.put(ShipAPI.HullSize.CRUISER, 0.4f);
		sizeScale.put(ShipAPI.HullSize.CAPITAL_SHIP, 0.3f);
	}

	public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
		this.ship = ship;
		this.system = system;
		for (WeaponAPI shipWeps : ship.getAllWeapons()) {
			if (shipWeps.getId().equals("drg_cryogenicemitter"))
				systemWeapons.add(shipWeps);
		}
	}

	public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
		if (Global.getCombatEngine().isPaused() || !AIUtils.canUseSystemThisFrame(ship)) return;

		List<ShipAPI> posTargets = AIUtils.getNearbyEnemies(ship, 550f);
		float desire = 0f;
		int ammo = ship.getSystem().getAmmo();

		if (!posTargets.isEmpty()) {
			for (WeaponAPI wep : systemWeapons) {
				for (ShipAPI sysTarget : posTargets) {
					if (WeaponUtils.isWithinArc(sysTarget, wep)) {
						desire += 1f * sizeScale.get(sysTarget.getHullSize());
					}
				}
			}
		}

		List<MissileAPI> missileThreats = AIUtils.getNearbyEnemyMissiles(ship, 700f);
		float missileDamage = 0f;
		if (!missileThreats.isEmpty()) {
			for (MissileAPI missile : missileThreats) {
				missileDamage += missile.getDamage().getDamage();
			}
		}

		if (missileDamage >= ship.getFluxTracker().getMaxFlux() * 0.5f)
			desire += 1.25f;
		else if (missileDamage >= ship.getFluxTracker().getMaxFlux() * 0.3f)
			desire += 1f;
		else if (missileDamage >= ship.getFluxTracker().getMaxFlux() * 0.1f)
			desire += 0.75f;

		float maxFlux = ship.getMaxFlux();
		if (maxFlux * 0.1f > system.getFluxPerUse())
			desire = desire * (ammo / 2f);
		else
			desire = desire * (ammo / 2f) * Math.max((maxFlux + 1f - ship.getCurrFlux()) / maxFlux, 0.1f);

		if (desire >= 1f)
			ship.giveCommand(ShipCommand.USE_SYSTEM, null, 0);
	}
}
