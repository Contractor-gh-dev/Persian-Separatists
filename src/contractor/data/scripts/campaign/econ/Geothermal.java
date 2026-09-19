package contractor.data.scripts.campaign.econ;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.util.ArrayList;
import java.util.List;

public class Geothermal extends BaseMarketConditionPlugin {

	public static final int FARMING_BONUS = 1;

	public static final List<String> MODIFIED_CONDITIONS = new ArrayList<>();

	static {
		MODIFIED_CONDITIONS.add(Conditions.COLD);
		MODIFIED_CONDITIONS.add(Conditions.VERY_COLD);
		MODIFIED_CONDITIONS.add(Conditions.POOR_LIGHT);
		MODIFIED_CONDITIONS.add(Conditions.DARK);
	}

	public void apply(String id) {
		for (String cid : MODIFIED_CONDITIONS) {
			if (market.hasCondition(cid)) {
				market.getHazard().modifyFlat(id, -0.25f);
				break;
			}
		}

		Industry industry = getIndustry();
		if (industry != null) {
			industry.getSupplyBonusFromOther().modifyFlat(id, FARMING_BONUS, Misc.ucFirst(condition.getName().toLowerCase()));
		}
	}

	public void unapply(String id) {
		market.getHazard().unmodify(id);
		Industry industry = getIndustry();
		if (industry != null) {
			industry.getSupplyBonusFromOther().unmodifyFlat(id);
		}
	}

	protected Industry getIndustry() {
		Industry industry = market.getIndustry(Industries.FARMING);
		if (industry == null) {
			industry = market.getIndustry(Industries.AQUACULTURE);
		}
		return industry;
	}

	protected void createTooltipAfterDescription(TooltipMakerAPI tooltip, boolean expanded) {
		float opad = 10f;

		tooltip.addPara("-%s hazard rating on planets with Cold, Extreme Cold, Poor Light, or Darkness.\n\nIncreases food production by %s (Farming).",
				opad, Misc.getHighlightColor(),
				"25%", "" + FARMING_BONUS);
	}
}




