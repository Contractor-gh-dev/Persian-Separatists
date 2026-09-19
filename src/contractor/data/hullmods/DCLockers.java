package contractor.data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class DCLockers extends BaseHullMod {
	private static final String DATAKEY = "DCLockerKey_";
	private static final float WEAPON_REPAIR = 30f;
	private static final float MALF_REDUCE = 25f;
	private static final float CARGO_DEBUFF = 25f;
	private static final float REPAIR_RATE = 2f;
	private static final Map<ShipAPI.HullSize, Float> hitpoints = new HashMap<>(); static {
		hitpoints.put(ShipAPI.HullSize.FIGHTER, 250f);
		hitpoints.put(ShipAPI.HullSize.FRIGATE, 1000f);
		hitpoints.put(ShipAPI.HullSize.DESTROYER, 2000f);
		hitpoints.put(ShipAPI.HullSize.CRUISER, 3000f);
		hitpoints.put(ShipAPI.HullSize.CAPITAL_SHIP, 5000f);
	}

	private static final Map<ShipAPI.HullSize, Float> repairScale = new HashMap<>(); static {
		repairScale.put(ShipAPI.HullSize.FIGHTER, 0.5f);
		repairScale.put(ShipAPI.HullSize.FRIGATE, 1f);
		repairScale.put(ShipAPI.HullSize.DESTROYER, 1.5f);
		repairScale.put(ShipAPI.HullSize.CRUISER, 2f);
		repairScale.put(ShipAPI.HullSize.CAPITAL_SHIP, 3f);
	}

	public static class DCLockersData {
		private final IntervalUtil tracker = new IntervalUtil(0.1f, 0.1f);
		public float charge = 0f;

		DCLockersData(float initial) {
			charge = initial;
		}

		protected void doRepairs(ShipAPI ship) {
			float curr = ship.getHitpoints();
			if (curr >= ship.getMaxHitpoints() * 0.8f)
				return;

			float scale = repairScale.get(ship.getHullSize());
			ship.setHitpoints(curr + (REPAIR_RATE * scale));
			charge -= REPAIR_RATE * scale;
		}

	}

	public void advanceInCombat(ShipAPI ship, float amount) {
		if (!ship.isAlive())
			return;

		CombatEngineAPI engine = Global.getCombatEngine();
		String key = DATAKEY + ship.getId();

		DCLockersData data = (DCLockersData) engine.getCustomData().get(key);
		if (data == null) {
			data = new DCLockersData((float) hitpoints.get(ship.getHullSize()));
			engine.getCustomData().put(key, data);
		}

		if (data.charge <= 0f)
			return;

		data.tracker.advance(amount);
		if (data.tracker.intervalElapsed()) {
			data.doRepairs(ship);
		}
	}

	public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
		boolean sMod = isSMod(stats);
		stats.getCombatEngineRepairTimeMult().modifyPercent(id, -WEAPON_REPAIR);
		stats.getCombatWeaponRepairTimeMult().modifyPercent(id, -WEAPON_REPAIR);
		stats.getWeaponMalfunctionChance().modifyPercent(id, -MALF_REDUCE);
		stats.getEngineMalfunctionChance().modifyPercent(id, -MALF_REDUCE);

		if (sMod)
			stats.getCargoMod().modifyPercent(id, -CARGO_DEBUFF);
	}

	public boolean isSModEffectAPenalty() {
		return true;
	}

	public boolean isApplicableToShip(ShipAPI ship) {
		if (ship.getVariant().hasHullMod("automated")) return false;
		if (ship.getVariant().hasHullMod("contractor_repairnanites")) return false;
		return true;
	}

	public String getUnapplicableReason(ShipAPI ship) {
		if (ship.getVariant().hasHullMod("automated")) return "Incompatible with automated ships.";
		if (ship.getVariant().hasHullMod("contractor_repairnanites")) return "Incompatible with Repair Nanites.";
		return null;
	}

	public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
		return false;
	}

	public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		float opad = 10f;

		Color h = Misc.getHighlightColor();

		tooltip.addPara("Distributed damage control lockers throughout the hull allow extensive repairs to be made during combat. " +
						"Weapons and engines are repaired %s faster, and are %s less likely to malfunction. When below %s hull, ship is repaired at %s/%s/%s/%s hitpoints per second. " +
						"Maximum repairs are %s/%s/%s/%s depending on hull size.", opad, h,
				Math.round(WEAPON_REPAIR) + "%", Math.round(MALF_REDUCE) + "%", "80%",
				Math.round(repairScale.get(ShipAPI.HullSize.FRIGATE) * REPAIR_RATE * 10) + "",
				Math.round(repairScale.get(ShipAPI.HullSize.DESTROYER) * REPAIR_RATE * 10) + "",
				Math.round(repairScale.get(ShipAPI.HullSize.CRUISER) * REPAIR_RATE * 10) + "",
				Math.round(repairScale.get(ShipAPI.HullSize.CAPITAL_SHIP) * REPAIR_RATE * 10) + "",
				Math.round(hitpoints.get(ShipAPI.HullSize.FRIGATE)) + "",
				Math.round(hitpoints.get(ShipAPI.HullSize.DESTROYER)) + "",
				Math.round(hitpoints.get(ShipAPI.HullSize.CRUISER)) + "",
				Math.round(hitpoints.get(ShipAPI.HullSize.CAPITAL_SHIP)) + "");
	}

	public String getSModDescriptionParam(int index, ShipAPI.HullSize hullSize) {
		if (index == 0) return (int) Math.round(CARGO_DEBUFF) + "%";
		return null;
	}
}
