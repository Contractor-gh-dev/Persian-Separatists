package contractor.data.scripts.industries;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;
import java.util.Objects;

public class DuskInternalAffairs extends BaseIndustry {
	public void apply() {
		super.apply(true);

		String desc = getNameForModifier();

		supply(Commodities.FOOD, 3);
		supply(Commodities.ORGANICS, 2);
		supply(Commodities.CREW, 6);
		supply(Commodities.FUEL, 1);
		supply(Commodities.SUPPLIES, 6);
		supply(Commodities.DOMESTIC_GOODS, 2);
		supply(Commodities.DRUGS, 1);
		supply(Commodities.HAND_WEAPONS, 1);
		supply(Commodities.MARINES, 3);
		supply(Commodities.SHIPS, 1);

		market.getAccessibilityMod().modifyFlat(getModId(0), -0.5f, desc);
		market.getStability().modifyFlat(getModId(1), -3f, desc);


		if (!isFunctional()) {
			supply.clear();
			unapply();
		}
	}

	@Override
	public void unapply() {
		super.unapply();
		market.getAccessibilityMod().unmodifyFlat(getModId(0));
		market.getStability().unmodifyFlat(getModId(1));
	}

	@Override
	public boolean isAvailableToBuild() {
		return false;
	}

	@Override
	public String getUnavailableReason() {
		if (!super.isAvailableToBuild()) return super.getUnavailableReason();

		return "The Dusk Research Group cannot support this action.";
	}

	public boolean showWhenUnavailable() {
		return Objects.equals(Global.getSector().getMemoryWithoutUpdate().get("$drgIA_Build_Allowed"), true);
	}

	public float getPatherInterest() {
		return 1f + super.getPatherInterest();
	}

	@Override
	protected void addPostDemandSection(TooltipMakerAPI tooltip, boolean hasDemand, IndustryTooltipMode mode) {
		if (mode != IndustryTooltipMode.NORMAL || isFunctional()) {
			MutableStat fake = new MutableStat(0);

			String desc = getNameForModifier();
			float a = -0.5f;
			fake.modifyFlat(getModId(0), a, desc);

			String totalStr = (int) Math.round(a * 100f) + "%";
			Color h = Misc.getNegativeHighlightColor();


			float opad = 10f;
			float pad = 3f;

			tooltip.addPara("Accessibility penalty: %s", opad, h, totalStr);

			a = -3f;
			totalStr = (int) a + "";
			fake.modifyFlat(getModId(1), a, desc);
			tooltip.addPara("Stability penalty: %s", opad, h, totalStr);

			tooltip.addPara("Operational restrictions and exclusivity requirements of an Internal Affairs command center result in significant reduction in accessibility and stability.", opad);
		}
	}
}