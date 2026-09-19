package contractor.data.hullmods;

import java.awt.*;
import java.util.List;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.AIHints;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.magiclib.util.MagicIncompatibleHullmods;

public class AdvancedPointDefense extends BaseHullMod {
	public static final float DAMAGE_BONUS = 50f;
	public static final float TURN_BONUS = 25f;

	@Override
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getDynamic().getMod(Stats.PD_IGNORES_FLARES).modifyFlat(id, 1f);
		stats.getDynamic().getMod(Stats.PD_BEST_TARGET_LEADING).modifyFlat(id, 1f);
		stats.getDamageToMissiles().modifyPercent(id, DAMAGE_BONUS);
		stats.getBeamWeaponTurnRateBonus().modifyPercent(id, TURN_BONUS);
	}

	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		if (ship.getVariant().hasHullMod(HullMods.POINTDEFENSEAI)) {
			MagicIncompatibleHullmods.removeHullmodWithWarning(ship.getVariant(), HullMods.POINTDEFENSEAI, "contractor_advancedpd");
			return;
		}

		List<WeaponAPI> weapons = ship.getAllWeapons();
		for (WeaponAPI wep : weapons) {
			boolean sizeMatches = wep.getSize() == WeaponSize.SMALL;
			if (sizeMatches && wep.getType() != WeaponType.MISSILE && !wep.hasAIHint(AIHints.STRIKE)) {
				wep.setPD(true);
			}
		}
	}

	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "" + Math.round(DAMAGE_BONUS) + "%";
		if (index == 1) return "" + Math.round(TURN_BONUS) + "%";
		return null;
	}

	public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		float opad = 10f;
		Color badcol = Misc.getNegativeHighlightColor();
		tooltip.addPara("Precludes installation of Integrated Point Defense AI.", badcol, opad);
	}
}





