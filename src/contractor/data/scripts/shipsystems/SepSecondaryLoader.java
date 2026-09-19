package contractor.data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

import java.util.List;

public class SepSecondaryLoader extends BaseShipSystemScript {
	private boolean done = false;
	public static final float FLUX_VENT_RATIO = 0.75f;
	public static final float COOLDOWN_LIMIT = 0.4f;

	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		CombatEngineAPI engine = Global.getCombatEngine();
		if (engine.isPaused())
			return;
		ShipAPI ship = (ShipAPI) stats.getEntity();

		if (!done && (state == State.IN || state == State.ACTIVE)) {
			done = true;

			List<WeaponAPI> weapons = ship.getAllWeapons();
			float fluxCost = 0f;

			for (WeaponAPI wep : weapons) {
				if ((wep.getType() == WeaponAPI.WeaponType.BALLISTIC || wep.getType() == WeaponAPI.WeaponType.HYBRID || wep.getType() == WeaponAPI.WeaponType.COMPOSITE) && !wep.getSpec().isBeam()) {
					wep.ensureClonedSpec();
					if (wep.usesAmmo() && wep.getSpec().getReloadSize() > 0) {
						int reload = (int) Math.max(wep.getSpec().getBurstSize(), wep.getSpec().getReloadSize());
						if (reload >= 1)
							wep.setAmmo(Math.min(wep.getAmmo() + reload, wep.getMaxAmmo()));
					}

					if (!wep.isDisabled())
						wep.setRemainingCooldownTo(COOLDOWN_LIMIT);

					fluxCost += wep.getFluxCostToFire();
				}
			}

			fluxCost *= FLUX_VENT_RATIO;
			float softFlux = Math.max(0f, ship.getFluxLevel() - ship.getHardFluxLevel());
			softFlux *= ship.getCurrFlux();

			if (softFlux < fluxCost)
				ship.getFluxTracker().setCurrFlux(ship.getCurrFlux() - softFlux);
			else
				ship.getFluxTracker().setCurrFlux(ship.getCurrFlux() - fluxCost);
		}
		if (state == State.OUT)
			done = false;
	}

	public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
		ShipSystemAPI.SystemState state = system.getState();

		if (state == ShipSystemAPI.SystemState.IN)
			return "LOADING";
		if (state == ShipSystemAPI.SystemState.ACTIVE || state == ShipSystemAPI.SystemState.OUT)
			return "LOAD COMPLETE";
		if (state == ShipSystemAPI.SystemState.COOLDOWN || system.isOutOfAmmo())
			return "REARMING";
		return null;
	}

}
