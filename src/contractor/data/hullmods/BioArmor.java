package contractor.data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import contractor.data.scripts.util.ContractorStaticVars;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static contractor.data.scripts.util.ContractorStaticVars.CONTRACTOR_NO_NANITE_REGEN;
import static contractor.data.scripts.util.ContractorUtils.isNaniteHull;

public class BioArmor extends BaseHullMod {
	public static class BioArmorData {
		private final IntervalUtil tracker = new IntervalUtil(4.5f, 5.5f);
	}

	public static class BioArmorDataNpc {
		private final IntervalUtil tracker = new IntervalUtil(1.5f, 2f);
	}

	public static final String DATAKEY = "BioRegenKey_";
	public static final float ARMORMOD = 0.6f;
	public static final float ARMORSMOD = 0.8f;
	public static final float CRMOD = 0.8f;
	public static final float CRNPCMOD = 0.9f;
	private static final Map<Object, Object> armor = new HashMap<>(); static {
		armor.put(ShipAPI.HullSize.FIGHTER, 0.05f);
		armor.put(ShipAPI.HullSize.FRIGATE, 0.04f);
		armor.put(ShipAPI.HullSize.DESTROYER, 0.03f);
		armor.put(ShipAPI.HullSize.CRUISER, 0.02f);
		armor.put(ShipAPI.HullSize.CAPITAL_SHIP, 0.01f);
	}

	public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
		boolean sMod = isSMod(stats);
		if (sMod)
			stats.getArmorBonus().modifyMult(id, ARMORSMOD);
		else
			stats.getArmorBonus().modifyMult(id, ARMORMOD);
		if (isNaniteHull(stats))
			stats.getPeakCRDuration().modifyMult(id, CRNPCMOD);
		else
			stats.getPeakCRDuration().modifyMult(id, CRMOD);
	}

	public void advanceInCombat(ShipAPI ship, float amount) {
		if (!ship.isAlive())
			return;
		if (ship.getPeakTimeRemaining() <= 0f)
			return;
		if (ship.getHullSpec().hasTag(CONTRACTOR_NO_NANITE_REGEN))
			return;

		String key = DATAKEY + ship.getId();
		if (isNaniteHull(ship))
			performActionNpc(ship, amount, key);
		else
			performAction(ship, amount, key);
	}

	private void performAction(ShipAPI ship, float amount, String key) {
		CombatEngineAPI engine = Global.getCombatEngine();
		BioArmorData data = (BioArmorData) engine.getCustomData().get(key);
		if (data == null) {
			data = new BioArmorData();
			engine.getCustomData().put(key, data);
		}

		data.tracker.advance(amount);
		if (data.tracker.intervalElapsed()) {
			float[][] armorGrid = ship.getArmorGrid().getGrid();
			float armorFract = ship.getArmorGrid().getMaxArmorInCell() * (float) armor.get(ship.getHullSize());

			for (int i = 0; i < armorGrid.length; i++)
				for (int j = 0; j < armorGrid[i].length; j++) {
					if (armorGrid[i][j] < ship.getArmorGrid().getMaxArmorInCell()) {
						float modVal = ship.getArmorGrid().getArmorValue(i, j) + armorFract;
						if (modVal > ship.getArmorGrid().getMaxArmorInCell())
							modVal = ship.getArmorGrid().getMaxArmorInCell();
						ship.getArmorGrid().setArmorValue(i, j, modVal);
					}
				}
			ship.syncWithArmorGridState();
		}
	}

	private void performActionNpc(ShipAPI ship, float amount, String key) {
		CombatEngineAPI engine = Global.getCombatEngine();
		BioArmorDataNpc data = (BioArmorDataNpc) engine.getCustomData().get(key);
		if (data == null) {
			data = new BioArmorDataNpc();
			engine.getCustomData().put(key, data);
		}

		data.tracker.advance(amount);
		if (data.tracker.intervalElapsed()) {
			float[][] armorGrid = ship.getArmorGrid().getGrid();
			float armorFract = ship.getArmorGrid().getMaxArmorInCell() * (float) armor.get(ship.getHullSize()) * 1.5f;

			for (int i = 0; i < armorGrid.length; i++)
				for (int j = 0; j < armorGrid[i].length; j++) {
					if (armorGrid[i][j] < ship.getArmorGrid().getMaxArmorInCell()) {
						float modVal = ship.getArmorGrid().getArmorValue(i, j) + armorFract;
						if (modVal > ship.getArmorGrid().getMaxArmorInCell())
							modVal = ship.getArmorGrid().getMaxArmorInCell();
						ship.getArmorGrid().setArmorValue(i, j, modVal);
					}
				}
			ship.syncWithArmorGridState();
		}
	}

	public boolean isApplicableToShip(ShipAPI ship) {
		if (ship.getVariant().hasHullMod("automated")) return false;
		if (ship.getVariant().hasHullMod("contractor_repairnanites")) return false;
		if (ship.getVariant().hasHullMod("contractor_naniteforges")) return false;
		if (ship.getVariant().hasHullMod("ablative_armor")) return false;
		if (ship.getVariant().hasHullMod("contractor_compositearmor")) return false;
		if (ship.getVariant().hasHullMod("contractor_reactivearmor")) return false;
		if (ship.getVariant().hasHullMod("contractor_hardenedarmor")) return false;
		if (ship.getVariant().hasHullMod("contractor_reflectivearmor")) return false;
		if (ContractorStaticVars.NANITE_PROTECTED_TARGETS.contains(ship.getHullSpec().getBaseHullId())) return false;
		return true;
	}

	public String getUnapplicableReason(ShipAPI ship) {
		if (ship.getVariant().hasHullMod("automated")) return "Incompatible with automated ships.";
		if (ship.getVariant().hasHullMod("contractor_repairnanites")) return "Incompatible with Repair Nanites.";
		if (ship.getVariant().hasHullMod("contractor_naniteforges")) return "Incompatible with Nanite Forges.";
		if (ship.getVariant().hasHullMod("ablative_armor")) return "Incompatible with Ablative Armor.";
		if (ship.getVariant().hasHullMod("contractor_compositearmor")) return "Incompatible with Composite Armor.";
		if (ship.getVariant().hasHullMod("contractor_reactivearmor")) return "Incompatible with Reactive Armor.";
		if (ship.getVariant().hasHullMod("contractor_hardenedarmor")) return "Incompatible with Hardened armor.";
		if (ship.getVariant().hasHullMod("contractor_reflectivearmor")) return "Incompatible with Reflective armor.";
		if (ContractorStaticVars.NANITE_PROTECTED_TARGETS.contains(ship.getHullSpec().getBaseHullId())) return "Incompatible with this hull.";
		return null;
	}

	public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
		return false;
	}

	public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		float opad = 10f;

		Color h = Misc.getHighlightColor();
		Color b = Misc.getNegativeHighlightColor();
		String time = "five";

		if (isNaniteHull(ship))
			time = "two";

		tooltip.addPara("Replaces inner armor layers with self-repairing bio-metal nanites. Armor regenerates by %s/%s/%s/%s every " + time +
						" seconds on average, depending on hull size, while ship has peak performance time remaining.", opad, h,
				(int) Math.round((float) armor.get(ShipAPI.HullSize.FRIGATE) * 100f) + "%",
				(int) Math.round((float) armor.get(ShipAPI.HullSize.DESTROYER) * 100f) + "%",
				(int) Math.round((float) armor.get(ShipAPI.HullSize.CRUISER) * 100f) + "%",
				(int) Math.round((float) armor.get(ShipAPI.HullSize.CAPITAL_SHIP) * 100f) + "%");
		tooltip.addPara("Reduces max armor by %s and peak readiness time by %s.", opad, b,
				(int) Math.round(ARMORMOD * 100f) - 100 + "%",
				(int) 100 - Math.round(CRMOD * 100f) + "%");

		tooltip.addPara("Ship is 100% weaker to Nanite corruption and cannot be uncorrupted in combat.", b, opad);
	}

	public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
		if (index == 0) return "" + (int) Math.round((float) armor.get(ShipAPI.HullSize.FRIGATE) * 100f) + "%";
		if (index == 1) return "" + (int) Math.round((float) armor.get(ShipAPI.HullSize.DESTROYER) * 100f) + "%";
		if (index == 2) return "" + (int) Math.round((float) armor.get(ShipAPI.HullSize.CRUISER) * 100f) + "%";
		if (index == 3) return "" + (int) Math.round((float) armor.get(ShipAPI.HullSize.CAPITAL_SHIP) * 100f) + "%";
		if (index == 4) return "" + (int) Math.round(ARMORMOD * 100f) + "%";
		if (index == 5) return "" + (int) Math.round(CRMOD * 100f) + "%";
		return null;
	}

	public String getSModDescriptionParam(int index, ShipAPI.HullSize hullSize) {
		if (index == 0) return 100 - Math.round(ARMORSMOD * 100f) + "%";
		return null;
	}

	@Override
	public CargoStackAPI getRequiredItem() {
		return Global.getSettings().createCargoStack(CargoAPI.CargoItemType.SPECIAL, new SpecialItemData(ContractorStaticVars.CONTRACTOR_BIOMETAL_ID, null), null);
	}
}
