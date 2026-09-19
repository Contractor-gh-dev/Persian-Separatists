package contractor.data.scripts.campaign.intel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.events.*;
import com.fs.starfarer.api.ui.MapParams;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;
import contractor.data.scripts.util.ContractorStaticVars;
import contractor.data.scripts.util.ContractorUtils;

import java.util.List;

import static com.fs.starfarer.api.impl.campaign.intel.events.StandardPerseanLeagueActivityCause.*;
import static contractor.data.scripts.campaign.intel.NaniteHostileActivityFactor.CORE_DIST_LY;

public class NaniteHostileActivityCauseSize extends BaseHostileActivityCause2 {
	public NaniteHostileActivityCauseSize(HostileActivityEventIntel intel) {
		super(intel);
	}

	@Override
	public TooltipMakerAPI.TooltipCreator getTooltip() {
		MarketAPI largestMarket = ContractorUtils.getLargestPlayerMarket();
		StarSystemAPI largestSys = largestMarket.getStarSystem();

		return new BaseFactorTooltip() {
			@Override
			public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
				float opad = 10f;

				MapParams params = new MapParams();
				params.showSystem(largestSys);

				float w = tooltip.getWidthSoFar();
				float h = Math.round(w / 1.6f);
				params.positionToShowAllMarkersAndSystems(true, Math.min(w, h));

				UIPanelAPI map = tooltip.createSectorMap(w, h, params, largestSys.getNameWithLowercaseType());

				tooltip.addPara("Interstellar civilisation naturally generates all kinds of detectable " +
								"signals and activity for all to hear, simply by virtue of existing. Far from the core, these signals are also heard by " +
								"Nanites, attracting integration fleets. ", 0f, Misc.getHighlightColor(),
						"attracting integration fleets");

				tooltip.addPara("Event progress value is based on the number and size of colonies "
								+ "under your control outside the core. Requires one size %s colony, or at least %s colonies with one "
								+ "being at least size %s.", opad, Misc.getHighlightColor(),
						"" + LARGE_COLONY, "" + COUNT_IF_MEDIUM, "" + MEDIUM_COLONY);

				tooltip.addCustom(map, opad);
			}
		};
	}

	public String getDesc() {
		return "Colony size";
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

				if (Misc.getDistanceLY(system.getLocation(), Misc.getCoreCenter()) > CORE_DIST_LY * 0.75f)
					total += getProgressForSize(system);
			}
		}
		return total;
	}

	protected int getProgressForSize(StarSystemAPI system) {
		if (system == null) return 0;
		int total = 0;
		int numCol = 0;
		int maxSize = 0;

		List<MarketAPI> markets = Misc.getMarketsInLocation(system, Factions.PLAYER);
		for (MarketAPI market : markets) {
			maxSize = Math.max(maxSize, market.getSize());
			numCol++;
		}

		if (maxSize >= LARGE_COLONY) {
			maxSize -= 3;
			maxSize *= 2;
			maxSize += numCol;
		} else if (numCol >= COUNT_IF_MEDIUM && maxSize >= MEDIUM_COLONY) {
			maxSize -= 2;
			maxSize *= 2;
			maxSize += numCol;
		} else
			maxSize = 0;

		total += maxSize;

		return total;
	}

	public float getMagnitudeContribution(StarSystemAPI system) {
		if (getProgress() <= 0) return 0f;

		return (0.4f + 0.6f * intel.getMarketPresenceFactor(system)) * ContractorStaticVars.NANITE_MAX_MAG;
	}
}
