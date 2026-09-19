package contractor.data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;

public class ReflectiveArmor extends BaseHullMod {
	private static final float ENERGYDAMAGEMOD = 0.8f;

	public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getEnergyDamageTakenMult().modifyMult(id, ENERGYDAMAGEMOD);
	}

	public boolean isApplicableToShip(ShipAPI ship) {
		if (ship.getVariant().hasHullMod("ablative_armor")) return false;
		if (ship.getVariant().hasHullMod("contractor_hardenedarmor")) return false;
		if (ship.getVariant().hasHullMod("contractor_compositearmor")) return false;
		if (ship.getVariant().hasHullMod("contractor_reactivearmor")) return false;
		if (ship.getVariant().hasHullMod("contractor_bioarmor")) return false;
		return true;
	}

	public String getUnapplicableReason(ShipAPI ship) {
		if (ship.getVariant().hasHullMod("ablative_armor")) return "Incompatible with Ablative Armor.";
		if (ship.getVariant().hasHullMod("contractor_hardenedarmor")) return "Incompatible with Hardened Armor.";
		if (ship.getVariant().hasHullMod("contractor_compositearmor")) return "Incompatible with Composite Armor.";
		if (ship.getVariant().hasHullMod("contractor_reactivearmor")) return "Incompatible with Reactive Armor.";
		if (ship.getVariant().hasHullMod("contractor_bioarmor")) return "Incompatible with Bio-armor.";
		return null;
	}

	public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
		if (index == 0) return "Energy";
		if (index == 1) return (int) (100 - Math.round(ENERGYDAMAGEMOD * 100f)) + "%";
		return null;
	}

}
