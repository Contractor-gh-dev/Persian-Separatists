package contractor.data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import contractor.data.scripts.util.ContractorStaticVars;
import org.magiclib.util.MagicIncompatibleHullmods;

import java.awt.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static contractor.data.scripts.util.ContractorStaticVars.CONTRACTOR_NO_NANITE_REGEN;

public class NaniteForges extends BaseHullMod {
	public static final String DATAKEY = "NaniteForgesKey_";
	public static final float REPAIR_PENALTY_MULT = 1.33f;
	public static final int AMMO_RATIO = 80, TIME_PENALTY = 10;
	public static final int SMALL_COST = 1, MED_COST = 2, LARGE_COST = 4;
	public static final Map<ShipAPI.HullSize, Integer> PENALTY_MAP = new HashMap<>(); static {
		PENALTY_MAP.put(ShipAPI.HullSize.FRIGATE, 2);
		PENALTY_MAP.put(ShipAPI.HullSize.DESTROYER, 3);
		PENALTY_MAP.put(ShipAPI.HullSize.CRUISER, 4);
		PENALTY_MAP.put(ShipAPI.HullSize.CAPITAL_SHIP, 6);
	}

	public static class NaniteForgesData {
		private final List<WeaponAPI> weps = new ArrayList<>();
		private final List<Float> wepTimers = new ArrayList<>();
		private final int timePenalty;

		NaniteForgesData(ShipAPI ship) {
			int penaltyWeight = 0;
			for (WeaponAPI wep : ship.getAllWeapons()) {
				if (wep.usesAmmo() && wep.getAmmoTracker().getAmmoPerSecond() <= 0f && !wep.getSpec().hasTag(CONTRACTOR_NO_NANITE_REGEN)) {
					weps.add(wep);
					wepTimers.add(0f);

					switch (wep.getSize()) {
						case SMALL -> penaltyWeight += SMALL_COST;
						case MEDIUM -> penaltyWeight += MED_COST;
						case LARGE -> penaltyWeight += LARGE_COST;
					}
				}
			}
			int penaltyMod = Math.max(0, penaltyWeight - PENALTY_MAP.get(ship.getHullSize()));
			timePenalty = penaltyMod * TIME_PENALTY;
		}

		public void advance(ShipAPI ship, float amount) {
			for (int i = 0; i < weps.size(); i++) {
				WeaponAPI wep = weps.get(i);
				int maxAmmo = wep.getMaxAmmo();
				int currAmmo = wep.getAmmo();
				if (currAmmo == maxAmmo || wep.isDisabled())
					continue;

				float elapsed = wepTimers.get(i);
				wepTimers.set(i, elapsed + amount);
				elapsed = wepTimers.get(i);

				if (elapsed >= 60f + timePenalty) {
					wepTimers.set(i, 0f);

					int toRegen = (int) Math.max(1, Math.floor(maxAmmo * ((double) AMMO_RATIO / (AMMO_RATIO + maxAmmo))));

					wep.setAmmo(Math.min(toRegen + currAmmo, maxAmmo));
					Global.getSoundPlayer().playSound("missile_weapon_reloaded", 1f, 0.7f, ship.getLocation(), ship.getVelocity());
				}
			}
		}
	}

	public static class NaniteForgesNpcData {
		private final List<WeaponAPI> weps = new ArrayList<>();
		private final List<Float> wepTimers = new ArrayList<>();

		NaniteForgesNpcData(ShipAPI ship) {
			for (WeaponAPI wep : ship.getAllWeapons()) {
				if (wep.usesAmmo() && wep.getAmmoTracker().getAmmoPerSecond() <= 0f && !wep.getSpec().hasTag(CONTRACTOR_NO_NANITE_REGEN)) {
					weps.add(wep);
					wepTimers.add(0f);
				}
			}
		}

		public void advance(ShipAPI ship, float amount) {
			for (int i = 0; i < weps.size(); i++) {
				WeaponAPI wep = weps.get(i);
				int maxAmmo = wep.getMaxAmmo();
				int currAmmo = wep.getAmmo();
				if (currAmmo == maxAmmo || wep.isDisabled())
					continue;

				float elapsed = wepTimers.get(i);
				wepTimers.set(i, elapsed + amount);
				elapsed = wepTimers.get(i);

				if (elapsed >= 60f) {
					wepTimers.set(i, 0f);

					int toRegen = (int) Math.max(1, Math.floor(maxAmmo * ((double) AMMO_RATIO / (AMMO_RATIO + maxAmmo))));

					wep.setAmmo(Math.min(toRegen + currAmmo, maxAmmo));
					Global.getSoundPlayer().playSound("missile_weapon_reloaded", 1f, 0.8f, ship.getLocation(), ship.getVelocity());
				}
			}
		}
	}

	@Override
	public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getCombatWeaponRepairTimeMult().modifyMult(id, REPAIR_PENALTY_MULT);
	}

	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		if (ship.getVariant().hasHullMod(HullMods.MISSILE_AUTOLOADER))
			MagicIncompatibleHullmods.removeHullmodWithWarning(ship.getVariant(), HullMods.MISSILE_AUTOLOADER, "contractor_naniteforges");
	}

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		if (!ship.isAlive())
			return;
		if (ship.getPeakTimeRemaining() <= 0f)
			return;
		if (ship.getHullSpec().hasTag(CONTRACTOR_NO_NANITE_REGEN))
			return;

		CombatEngineAPI engine = Global.getCombatEngine();
		String key = DATAKEY + ship.getId();

		NaniteForgesData data = (NaniteForgesData) engine.getCustomData().get(key);
		if (data == null) {
			data = new NaniteForgesData(ship);
			engine.getCustomData().put(key, data);
		}

		data.advance(ship, amount);
	}

	public boolean isApplicableToShip(ShipAPI ship) {
		if (ship.getVariant().hasHullMod("automated")) return false;
		if (ship.getVariant().hasHullMod("contractor_bioarmor")) return false;
		if (ship.getVariant().hasHullMod("contractor_repairnanites")) return false;
		if (ship.getVariant().hasHullMod(HullMods.MISSILE_AUTOLOADER)) return false;
		if (ContractorStaticVars.NANITE_PROTECTED_TARGETS.contains(ship.getHullSpec().getBaseHullId())) return false;
		return true;
	}

	public String getUnapplicableReason(ShipAPI ship) {
		if (ship.getVariant().hasHullMod("automated")) return "Incompatible with automated ships.";
		if (ship.getVariant().hasHullMod("contractor_bioarmor")) return "Incompatible with Bio-armor.";
		if (ship.getVariant().hasHullMod("contractor_repairnanites")) return "Incompatible with Repair Nanites.";
		if (ship.getVariant().hasHullMod(HullMods.MISSILE_AUTOLOADER)) return "Incompatible with Missile Autoloader.";
		if (ContractorStaticVars.NANITE_PROTECTED_TARGETS.contains(ship.getHullSpec().getBaseHullId())) return "Incompatible with hull.";
		return null;
	}

	public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
		return false;
	}

	public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		float opad = 10f;
		Color h = Misc.getHighlightColor();
		String time = "60";

		if (ship != null) {
			int penaltyWeight = 0;
			for (WeaponAPI wep : ship.getAllWeapons()) {
				if (wep.usesAmmo() && wep.getAmmoTracker().getAmmoPerSecond() <= 0f && !wep.getSpec().hasTag(CONTRACTOR_NO_NANITE_REGEN)) {
					switch (wep.getSize()) {
						case SMALL -> penaltyWeight += SMALL_COST;
						case MEDIUM -> penaltyWeight += MED_COST;
						case LARGE -> penaltyWeight += LARGE_COST;
					}
				}

			}
			int timeVal = 60 + (Math.max(0, penaltyWeight - PENALTY_MAP.get(ship.getHullSize())) * TIME_PENALTY);
			time = Integer.toString(timeVal);
		}

		tooltip.addPara("Augments ammo forges and loaders with nanites, allowing for relatively rapid reloads in combat conditions. " +
						"All weapons that use and do not regain ammo will now regenerate some ammo every %s seconds. " +
						"The amount regenerated is inversely proportional to the weapons max ammo, with high ammo weapons gaining less.", opad, h,
				time);
		tooltip.addPara("Ships have %s/%s/%s/%s of available throughput per hull size, and weapons consume %s/%s/%s per weapon size. " +
						"For every point above the threshold, regeneration speed will suffer a %s second penalty.", opad, h,
				Integer.toString(PENALTY_MAP.get(ShipAPI.HullSize.FRIGATE)),
				Integer.toString(PENALTY_MAP.get(ShipAPI.HullSize.DESTROYER)),
				Integer.toString(PENALTY_MAP.get(ShipAPI.HullSize.CRUISER)),
				Integer.toString(PENALTY_MAP.get(ShipAPI.HullSize.CAPITAL_SHIP)),
				Integer.toString(SMALL_COST),
				Integer.toString(MED_COST),
				Integer.toString(LARGE_COST),
				Integer.toString(TIME_PENALTY));

		tooltip.addPara("Additionally, %s weapons take %s longer to repair, and weapons that have been %s will not progress their reload timer.", opad, h,
				"All",
				Math.round(REPAIR_PENALTY_MULT * 100f - 100f) + "%",
				"disabled");

		Color b = Misc.getNegativeHighlightColor();
		tooltip.addPara("Ship is 100% weaker to Nanite corruption and cannot be uncorrupted in combat.", b, opad);
		tooltip.addPara("Precludes installation of Missile Autoloader.", b, opad);
	}

	@Override
	public CargoStackAPI getRequiredItem() {
		return Global.getSettings().createCargoStack(CargoAPI.CargoItemType.SPECIAL, new SpecialItemData(ContractorStaticVars.CONTRACTOR_BIOMETAL_ID, null), null);
	}
}
