package contractor.data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.magiclib.util.MagicIncompatibleHullmods;

import java.awt.*;

public class ExactingSpecifications extends BaseHullMod {
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		if (ship.getVariant().hasHullMod(HullMods.SAFETYOVERRIDES)) {
			MagicIncompatibleHullmods.removeHullmodWithWarning(ship.getVariant(), HullMods.SAFETYOVERRIDES, "contractor_exacting_specifications");
			return;
		}
		if (ship.getVariant().hasHullMod(HullMods.UNSTABLE_INJECTOR)) {
			MagicIncompatibleHullmods.removeHullmodWithWarning(ship.getVariant(), HullMods.UNSTABLE_INJECTOR, "contractor_exacting_specifications");
			return;
		}
	}

	public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		float opad = 10f;
		Color badcol = Misc.getNegativeHighlightColor();
		tooltip.addPara("Safety Overrides and Unstable Injector prohibited.", badcol, opad);
	}

	public boolean showInRefitScreenModPickerFor(ShipAPI ship) {
		return false;
	}
}



