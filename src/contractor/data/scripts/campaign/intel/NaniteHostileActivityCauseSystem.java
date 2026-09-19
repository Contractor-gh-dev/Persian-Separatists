package contractor.data.scripts.campaign.intel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.events.*;
import com.fs.starfarer.api.ui.MapParams;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;
import contractor.data.scripts.util.ContractorStaticVars;
import contractor.data.scripts.util.ContractorUtils;

public class NaniteHostileActivityCauseSystem extends BaseHostileActivityCause2 {
	public NaniteHostileActivityCauseSystem(HostileActivityEventIntel intel) {
		super(intel);
	}

	@Override
	public TooltipMakerAPI.TooltipCreator getTooltip() {
		int numColonies = Misc.getPlayerMarkets(false).size();
		MarketAPI largestMarket = ContractorUtils.getLargestPlayerMarket();
		StarSystemAPI largestSys = largestMarket.getStarSystem();
		final String colonies = numColonies != 1 ? "colonies" : "colony";
		final String isOrAre = numColonies != 1 ? "are" : "is";

		return new BaseFactorTooltip() {
			@Override
			public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
				float opad = 10f;

				MapParams params = new MapParams();
				params.showSystem(largestSys);

				float w = tooltip.getWidthSoFar();
				float h = Math.round(w / 1.6f);
				params.positionToShowAllMarkersAndSystems(true, Math.min(w, h));

				//UIPanelAPI map = tooltip.createSectorMap(w, h, params, aStr + " " + Misc.ucFirst(systems));
				UIPanelAPI map = tooltip.createSectorMap(w, h, params, largestSys.getNameWithLowercaseType());

				tooltip.addPara("Your " + colonies + " " + isOrAre + " located near what appears to be the Nanite home cluster, the largest " +
								"of which is located in the " + largestSys.getNameWithLowercaseType() + ". " +
								"This results in a greater risk of being detected by integration fleets, and eventual "
								+ "existential danger for your " + colonies + ".", 0f, Misc.getNegativeHighlightColor(),
						"existential danger for your " + colonies);

				tooltip.addPara("Even when relatively sparse, the Nanite fleets are a constant thorn in your side, " +
						"wasting defensive resources and exposing your " + colonies + " to other dangers.", opad);

				tooltip.addCustom(map, opad);
			}
		};
	}

	public String getDesc() {
		return "Colony location";
	}

	@Override
	public boolean shouldShow() {
		return getProgress() != 0;
	}

	public int getProgress() {
		int total = 0;
		if (!Global.getSector().getPlayerMemoryWithoutUpdate().getBoolean("$encounteredNanites"))
			return total;
		else {
			for (final StarSystemAPI system : Misc.getSystemsWithPlayerColonies(false)) {
				if (system.hasTag(ContractorStaticVars.CONTRACTOR_NANITE_NO_SPAWN)
						|| system.hasTag(Tags.SYSTEM_ALREADY_USED_FOR_STORY)
						|| system.hasTag(Tags.THEME_CORE)
						|| system.hasTag(Tags.THEME_REMNANT)
						|| system.hasTag(Tags.THREAT))
					continue;

				if (system.hasTag(ContractorStaticVars.CONTRACTOR_NANITE_ID))
					total += getProgressForSystem(system);
			}
		}
		return total;
	}

	protected int getProgressForSystem(StarSystemAPI system) {
		if (system == null) return 0;
		int total = 0;

		int factor = ContractorUtils.computeNaniteDistFactor(system) + 1;
		if (factor > 0)
			total += ContractorStaticVars.NANITE_PROGRESS_SCALE / factor;

		return total;
	}

	public float getMagnitudeContribution(StarSystemAPI system) {
		if (getProgress() <= 0) return 0f;

		return (0.4f + 0.6f * intel.getMarketPresenceFactor(system)) * ContractorStaticVars.NANITE_MAX_MAG;
	}
}
