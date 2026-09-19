package contractor.data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import contractor.data.scripts.util.ContractorStaticVars;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static contractor.data.scripts.util.ContractorStaticVars.CONTRACTOR_NO_NANITE_REGEN;

public class RepairNanites extends BaseHullMod {
	public static final String DATAKEY = "RepairNanitesKey_";
	public static final float EMP_DAMAGE_TAKEN_MULT = 1.66f;
	public static final float BASE_REPAIR_RATE = 1f;
	public static final Map<ShipAPI.HullSize, Float> SIZE_SCALE = new HashMap<>(); static {
		SIZE_SCALE.put(ShipAPI.HullSize.FIGHTER, 2f);
		SIZE_SCALE.put(ShipAPI.HullSize.FRIGATE, 3f);
		SIZE_SCALE.put(ShipAPI.HullSize.DESTROYER, 2f);
		SIZE_SCALE.put(ShipAPI.HullSize.CRUISER, 1.5f);
		SIZE_SCALE.put(ShipAPI.HullSize.CAPITAL_SHIP, 1f);
	}

	public static class RepairNanitesData {
		private float elapsed = 0f;
		public float fatigue = 1f;
		public static final float fatigueRatio = 8.5171f;

		public void advance(ShipAPI ship, float amount) {
			elapsed += amount;
			if (elapsed >= 0.1f) {
				elapsed -= 0.1f;
				doRepairs(ship, amount);
			}
		}

		protected void doRepairs(ShipAPI ship, float amount) {
			float curr = ship.getHitpoints();
			if (curr >= ship.getMaxHitpoints())
				return;

			float fatigueScale = (float) Math.pow(Math.min(1f, (fatigueRatio / Math.log(fatigue))), 4);
			float sizeScale = SIZE_SCALE.get(ship.getHullSize());
			float toRepair = (ship.getMaxHitpoints() / 200f) * BASE_REPAIR_RATE * sizeScale * fatigueScale * amount;
			if (ship.getMaxHitpoints() - curr < toRepair)
				toRepair = ship.getMaxHitpoints() - curr;

			ship.setHitpoints(curr + toRepair);
			fatigue += toRepair;
		}
	}

	public void advanceInCombat(ShipAPI ship, float amount) {
		if (!ship.isAlive())
			return;
		if (ship.getPeakTimeRemaining() <= 0f)
			return;
		if (ship.getHullSpec().hasTag(CONTRACTOR_NO_NANITE_REGEN))
			return;

		CombatEngineAPI engine = Global.getCombatEngine();
		String key = DATAKEY + ship.getId();

		RepairNanitesData data = (RepairNanitesData) engine.getCustomData().get(key);
		if (data == null) {
			data = new RepairNanitesData();
			engine.getCustomData().put(key, data);
		}

		data.advance(ship, amount);
	}

	public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getEmpDamageTakenMult().modifyMult(id, EMP_DAMAGE_TAKEN_MULT);
	}

	public boolean isApplicableToShip(ShipAPI ship) {
		if (ship.getVariant().hasHullMod("automated")) return false;
		if (ship.getVariant().hasHullMod("contractor_bioarmor")) return false;
		if (ship.getVariant().hasHullMod("contractor_naniteforges")) return false;
		if (ship.getVariant().hasHullMod("contractor_dclockers")) return false;
		if (ContractorStaticVars.NANITE_PROTECTED_TARGETS.contains(ship.getHullSpec().getBaseHullId())) return false;
		return true;
	}

	public String getUnapplicableReason(ShipAPI ship) {
		if (ship.getVariant().hasHullMod("automated")) return "Incompatible with automated ships.";
		if (ship.getVariant().hasHullMod("contractor_bioarmor")) return "Incompatible with Bio-armor.";
		if (ship.getVariant().hasHullMod("contractor_naniteforges")) return "Incompatible with Nanite Forges.";
		if (ship.getVariant().hasHullMod("contractor_dclockers")) return "Incompatible with DC Lockers.";
		if (ContractorStaticVars.NANITE_PROTECTED_TARGETS.contains(ship.getHullSpec().getBaseHullId())) return "Incompatible with hull.";
		return null;
	}

	public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
		return false;
	}

	public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		float opad = 10f;

		Color h = Misc.getHighlightColor();
		String frigate = Float.toString(SIZE_SCALE.get(ShipAPI.HullSize.FRIGATE) * BASE_REPAIR_RATE * 0.5f);
		String destroyer = Float.toString(SIZE_SCALE.get(ShipAPI.HullSize.DESTROYER) * BASE_REPAIR_RATE * 0.5f);
		String cruiser = Float.toString(SIZE_SCALE.get(ShipAPI.HullSize.CRUISER) * BASE_REPAIR_RATE * 0.5f);
		String capital = Float.toString(SIZE_SCALE.get(ShipAPI.HullSize.CAPITAL_SHIP) * BASE_REPAIR_RATE * 0.5f);
		frigate = frigate.substring(0, frigate.lastIndexOf(".") + 2);
		destroyer = destroyer.substring(0, destroyer.lastIndexOf(".") + 2);
		cruiser = cruiser.substring(0, cruiser.lastIndexOf(".") + 2);
		capital = capital.substring(0, capital.lastIndexOf(".") + 2);

		tooltip.addPara("Distributes nanite nodes throughout the hull that allow near limitless repairs to be made during combat. " +
						"When damaged,the hull is repaired at %s/%s/%s/%s of total hitpoints per second, depending on hull size, while the ship has peak performance time remaining. ", opad, h,
				frigate + "%",
				destroyer + "%",
				cruiser + "%",
				capital + "%");
		tooltip.addPara("The nanites suffer fatigue as repairs are made, and will gradually incur a repair speed penalty after %s hitpoints have been repaired.", opad, h,
				"5000");
		tooltip.addPara("Ship takes %s more %s damage.", opad, h,
				Math.round(EMP_DAMAGE_TAKEN_MULT * 100 - 100f) + "%",
				"Emp");

		Color b = Misc.getNegativeHighlightColor();
		tooltip.addPara("Ship is 100% weaker to Nanite corruption and cannot be uncorrupted in combat.", b, opad);
	}

	@Override
	public CargoStackAPI getRequiredItem() {
		return Global.getSettings().createCargoStack(CargoAPI.CargoItemType.SPECIAL, new SpecialItemData(ContractorStaticVars.CONTRACTOR_BIOMETAL_ID, null), null);
	}
}
