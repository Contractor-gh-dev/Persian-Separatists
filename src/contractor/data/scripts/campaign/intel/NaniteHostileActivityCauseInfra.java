package contractor.data.scripts.campaign.intel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.events.*;
import com.fs.starfarer.api.ui.MapParams;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;
import contractor.data.scripts.util.ContractorStaticVars;
import contractor.data.scripts.util.ContractorUtils;

import static contractor.data.scripts.campaign.intel.NaniteHostileActivityFactor.CORE_DIST_LY;

public class NaniteHostileActivityCauseInfra extends BaseHostileActivityCause2 {
	public NaniteHostileActivityCauseInfra(HostileActivityEventIntel intel) {
		super(intel);
	}

	@Override
	public TooltipMakerAPI.TooltipCreator getTooltip() {
		int numColonies = 0;
		int impact = 0;
		StarSystemAPI impactfulSys = null;
		for (final StarSystemAPI system : Misc.getSystemsWithPlayerColonies(false)) {
			if (impact == 0) {
				impactfulSys = system;
				impact = getProgressForInfra(system);
			} else {
				if (getProgressForInfra(system) > impact) {
					impactfulSys = system;
					impact = getProgressForInfra(system);
				} else if (getProgressForInfra(system) == impact
						&& ContractorUtils.getPlayerMarketSizeSum(system) > ContractorUtils.getPlayerMarketSizeSum(impactfulSys)) {
					impactfulSys = system;
					impact = getProgressForInfra(system);
				}
			}
			numColonies += Misc.getMarketsInLocation(system, Factions.PLAYER).size();
		}

		final StarSystemAPI sys = impactfulSys;
		final String colonies = numColonies != 1 ? "colonies" : "colony";
		final String isOrAre = numColonies != 1 ? "are" : "is";

		return new BaseFactorTooltip() {
			@Override
			public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
				float opad = 10f;

				MapParams params = new MapParams();
				params.showSystem(sys);

				float w = tooltip.getWidthSoFar();
				float h = Math.round(w / 1.6f);
				params.positionToShowAllMarkersAndSystems(true, Math.min(w, h));

				UIPanelAPI map = tooltip.createSectorMap(w, h, params, sys.getNameWithLowercaseType());

				tooltip.addPara("The presence of stellar infrastructure like comm relays or sensor arrays " +
								"in your isolated " + colonies + " " + isOrAre + " attracting attention from the Nanites, the most impactful " +
								"of which is located in the " + sys.getNameWithLowercaseTypeShort() + ". " +
								"This increases the chance a Nanite integration fleet will appear in your " + colonies
								+ " to raid it.", 0f, Misc.getHighlightColor(),
						"comm relays",
						"sensor arrays");

				tooltip.addPara("%s should address, or at least reduce, these concerns.", opad,
						Misc.getHighlightColor(), "Disassembling the relays");

				tooltip.addCustom(map, opad);
			}
		};
	}

	public String getDesc() {
		return "Colony infrastructure";
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
					total += getProgressForInfra(system);
			}
		}
		return total;
	}

	protected int getProgressForInfra(StarSystemAPI system) {
		if (system == null) return 0;
		int total = 0;

		total += system.getEntitiesWithTag(Tags.COMM_RELAY).size() * 2;
		total += system.getEntitiesWithTag(Tags.SENSOR_ARRAY).size();

		return total;
	}

	public float getMagnitudeContribution(StarSystemAPI system) {
		if (getProgress() <= 0) return 0f;

		return (0.4f + 0.6f * intel.getMarketPresenceFactor(system)) * ContractorStaticVars.NANITE_MAX_MAG;
	}
}
