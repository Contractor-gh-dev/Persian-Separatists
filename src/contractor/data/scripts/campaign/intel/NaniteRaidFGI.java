package contractor.data.scripts.campaign.intel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.intel.group.*;
import com.fs.starfarer.api.impl.campaign.missions.FleetCreatorMission;
import com.fs.starfarer.api.impl.campaign.missions.hub.BaseHubMission;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import contractor.data.scripts.campaign.fleets.NaniteFIDConfig;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD.*;

public class NaniteRaidFGI extends GenericRaidFGI {
	private static final List<String> VALID_TARGETS = new ArrayList<>();
	static {
		VALID_TARGETS.add(Industries.TECHMINING);
		VALID_TARGETS.add(Industries.REFINING);
		VALID_TARGETS.add(Industries.SPACEPORT);
		VALID_TARGETS.add(Industries.MEGAPORT);
		VALID_TARGETS.add(Industries.HEAVYINDUSTRY);
		VALID_TARGETS.add(Industries.ORBITALWORKS);
		VALID_TARGETS.add(Industries.WAYSTATION);
		VALID_TARGETS.add(Industries.TAG_STATION);
	}

	private static List<String> DES1_VARIANTS = new ArrayList<>();

	public NaniteRaidFGI(GenericRaidParams params) {
		super(params);
	}

	@Override
	protected void initActions() {
		setFaction(params.factionId);
		waitAction = new FGWaitAction(params.source.getPrimaryEntity(), params.prepDays,
				"");
		addAction(waitAction, PREPARE_ACTION);

		raidAction = createPayloadAction();

		travelAction = new FGTravelAction(params.source.getPrimaryEntity(),
				raidAction.getWhere().getCenter());

		addAction(travelAction, TRAVEL_ACTION);
		addAction(raidAction, PAYLOAD_ACTION);

		SectorEntityToken returnWhere = params.source.getPrimaryEntity();
		if (returnWhere.getStarSystem() != null) {
			returnWhere = returnWhere.getStarSystem().getCenter();
		}
		returnAction = new FGTravelAction(raidAction.getWhere().getCenter(),
				params.source.getPrimaryEntity());
		returnAction.setTravelText("");
		addAction(returnAction, RETURN_ACTION);

		origin = params.source.getPrimaryEntity();

		int total = 0;
		for (Integer i : params.fleetSizes) total += i;
		createRoute(params.factionId, total, params.fleetSizes.size(), null, params);
	}

	protected CampaignFleetAPI createFleet(int size, float damage) {
		Vector2f loc = origin.getLocation();

		FleetCreatorMission m = new FleetCreatorMission(getRandom());

		preConfigureFleet(size, m);

		m.beginFleet();

		String factionId = getFleetCreationFactionOverride(size);
		if (factionId == null) factionId = params.factionId;

		m.createFleet(params.style, size, factionId, loc);
		m.triggerSetFleetFaction(params.factionId);

		m.setFleetSource(params.source);
		setFleetCreatorQualityFromRoute(m);
		m.setFleetDamageTaken(damage);

		m.triggerSetWarFleet();

		if (params.makeFleetsHostile) {
			for (MarketAPI market : params.raidParams.allowedTargets) {
				m.triggerMakeHostileToFaction(market.getFactionId());
			}
			m.triggerMakeHostile();
		}

		m.triggerMakeNoRepImpact();
		m.triggerMakeAlwaysSpreadTOffHostility();

		configureFleet(size, m);

		CampaignFleetAPI fleet = m.createFleet();
		if (fleet != null) {
			configureFleet(size, fleet);
		}

		return fleet;
	}

	protected void configureFleet(int size, CampaignFleetAPI fleet) {
		WeightedRandomPicker<String> picker = new WeightedRandomPicker<>();

		for (FleetMemberAPI curr : fleet.getFleetData().getMembersListCopy()) {
			curr.getRepairTracker().setCR(curr.getRepairTracker().getMaxCR());
			curr.setPersonalityOverride(Personalities.RECKLESS);

			List<String> variants = Global.getSettings().getHullIdToVariantListMap().get(curr.getHullSpec().getBaseHullId());
			picker.addAll(variants);
			String pick = picker.pick();
			curr.setVariant(Global.getSettings().getVariant(pick), true, true);
			picker.clear();
		}

		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_FLEET_DO_NOT_GET_SIDETRACKED, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_INTERACTION_DIALOG_CONFIG_OVERRIDE_GEN, new NaniteFIDConfig());
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
	}

	protected void addNonUpdateBulletPoints(TooltipMakerAPI info, Color tc, Object param, ListInfoMode mode, float initPad) {
		Color h = Misc.getHighlightColor();
		Color s = raidAction.getSystemNameHighlightColor();
		FGAction curr = getCurrentAction();
		StarSystemAPI system = raidAction.getWhere();
		String forces = getForcesNoun();

		float untilDeployment = getETAUntil(PREPARE_ACTION);
		float untilDeparture = getETAUntil(TRAVEL_ACTION);
		float untilRaid = getETAUntil(PAYLOAD_ACTION);
		float untilReturn = getETAUntil(RETURN_ACTION, true);
		if (!isEnding()) {
			if (mode == ListInfoMode.MESSAGES || getElapsed() <= 0f) { // initial notification only, not updates
				addTargetingBulletPoint(info, tc, param, mode, initPad);
				initPad = 0f;
			}
			if (untilDeployment > 0) {
				addETABulletPoints(null, null, false, untilDeployment, ETAType.DEPLOYMENT, info, tc, initPad);
				initPad = 0f;
			} else if (untilDeparture > 0) {
				addETABulletPoints(null, null, false, untilDeparture, ETAType.DEPARTURE, info, tc, initPad);
				initPad = 0f;
			}
			if (untilRaid > 0 && getSource().getContainingLocation() != system) {
				addETABulletPoints(system.getNameWithLowercaseTypeShort(), s, false, untilRaid, ETAType.ARRIVING,
						info, tc, initPad);
				initPad = 0f;
			}
			if (untilReturn > 0 && RETURN_ACTION.equals(curr.getId()) && getSource().getContainingLocation() != system &&
					mode != ListInfoMode.INTEL) {
				StarSystemAPI from = getSource().getStarSystem();

				addETABulletPoints(from.getNameWithLowercaseTypeShort(), null, false, untilReturn, ETAType.RETURNING,
						info, tc, initPad);
				initPad = 0f;
			}
			if ((mode == ListInfoMode.INTEL || mode == ListInfoMode.MAP_TOOLTIP)
					&& curr != null && curr.getId().equals(PAYLOAD_ACTION)) {
				LabelAPI label = info.addPara("Harvesting in the " + system.getNameWithLowercaseTypeShort(), tc, initPad);
				label.setHighlightColors(s);
				label.setHighlight(system.getNameWithNoType());
				initPad = 0f;
			}
		}

		if (mode != ListInfoMode.IN_DESC && isEnding()) {
			if (!isSucceeded()) {
				if (!isAborted() && !isFailed()) {
					info.addPara("The " + forces + " have failed to integrate with their targets", tc, initPad);
				} else {
					if (isFailedButNotDefeated()) {
						info.addPara("The " + forces + " have failed to integrate with their targets", tc, initPad);
					} else {
						info.addPara("The " + forces + " have been defeated and lose cohesion", tc, initPad);
					}
				}
			}
		}
	}

	protected void addUpdateBulletPoints(TooltipMakerAPI info, Color tc, Object param, ListInfoMode mode, float initPad) {
		StarSystemAPI system = raidAction.getWhere();
		String forces = getForcesNoun();
		String noun = getNoun();
		Color s = raidAction.getSystemNameHighlightColor();
		//Color h = Misc.getHighlightColor();
		if (ABORT_UPDATE.equals(param)) {
			if (isInPreLaunchDelay()) {
				info.addPara("The " + noun + " was disrupted in the initial stages", tc, initPad);
			} else {
				info.addPara("The " + forces + " have been defeated and lose cohesion", tc, initPad);
			}
		} else if (FLEET_LAUNCH_UPDATE.equals(param)) {
			float untilDeparture = getETAUntil(TRAVEL_ACTION);
			float untilRaid = getETAUntil(PAYLOAD_ACTION);
			info.addPara("Fleet deployment in progress", tc, initPad);
			initPad = 0f;
			if (untilDeparture > 0) {
				addETABulletPoints(null, null, false, untilDeparture, ETAType.DEPARTURE, info, tc, initPad);
			}
			if (untilRaid > 0 && getSource().getContainingLocation() != system) {
				addETABulletPoints(system.getNameWithLowercaseTypeShort(), s, false, untilRaid, ETAType.ARRIVING,
						info, tc, initPad);
			}
		} else if (PREPARE_ACTION.equals(param)) {
			float untilRaid = getETAUntil(PAYLOAD_ACTION);
			addETABulletPoints(system.getNameWithLowercaseTypeShort(), s, true, untilRaid, ETAType.ARRIVING,
					info, tc, initPad);
		} else if (TRAVEL_ACTION.equals(param)) {
			addArrivedBulletPoint(system.getNameWithLowercaseTypeShort(), s, info, tc, initPad);
		} else if (PAYLOAD_ACTION.equals(param)) {
			if (isSucceeded()) {
				info.addPara("The " + forces + " are withdrawing", tc, initPad);
			} else {
				if (isAborted()) {
					info.addPara("The " + forces + " have been defeated and lose cohesion", tc, initPad);
				} else {
					info.addPara("The " + forces + " have failed to integrate with their targets", tc, initPad);
				}
			}
		}
	}

	protected void addAssessmentSection(TooltipMakerAPI info, float width, float height, float opad) {
		Color h = Misc.getHighlightColor();

		FactionAPI faction = getFaction();

		List<MarketAPI> targets = params.raidParams.allowedTargets;

		String noun = getNoun();
		if (!isEnding() && !isSucceeded() && !isFailed()) {
			info.addSectionHeading("Assessment",
					faction.getBaseUIColor(), faction.getDarkUIColor(), Alignment.MID, opad);
			if (targets.isEmpty()) {
				info.addPara("There are no colonies for the " + noun + " to target in the system.", opad);
			} else {
				StarSystemAPI system = raidAction.getWhere();

				String forces = getForcesNoun();

				boolean potentialDanger = addStrengthDesc(info, opad, system, forces,
						"the " + noun + " is unlikely to find success",
						"the outcome of the " + noun + " is uncertain",
						"the " + noun + " is likely to find success");

				if (potentialDanger) {
					String safe = "should be safe from the " + noun;
					String risk = "are at risk of being raided and losing stability:";
					String highlight = "losing stability:";
					if (params.raidParams.bombardment == BombardType.SATURATION) {
						risk = "are at risk of suffering a saturation bombardment resulting in catastrophic damage:";
						highlight = "catastrophic damage";
					} else if (params.raidParams.bombardment == BombardType.TACTICAL) {
						risk = "are at risk of being harvested and having their industrial infrastructure destroyed:";
						highlight = "industrial infrastructure destroyed";
					} else if (!params.raidParams.disrupt.isEmpty()) {
						risk = "are at risk of being harvested and having their industrial infrastructure destroyed";
						highlight = "industrial infrastructure destroyed";
					}
					if (getAssessmentRiskStringOverride() != null) {
						risk = getAssessmentRiskStringOverride();
					}
					if (getAssessmentRiskStringHighlightOverride() != null) {
						highlight = getAssessmentRiskStringHighlightOverride();
					}
					showMarketsInDanger(info, opad, width, system, targets,
							safe, risk, highlight);
				}
			}
			addPostAssessmentSection(info, width, height, opad);
		}
	}

	protected void addPayloadActionStatus(TooltipMakerAPI info, float width, float height, float opad) {
		StarSystemAPI to = raidAction.getWhere();
		info.addPara("Harvesting in the " + to.getNameWithLowercaseTypeShort() + ".", opad);
	}

	protected void addStatusSection(TooltipMakerAPI info, float width, float height, float opad) {
		FGAction curr = getCurrentAction();
		boolean showStatus = curr != null || isEnding() || isSucceeded();

		if (showStatus) {
			String noun = getNoun();
			String forces = getForcesNoun();
			info.addSectionHeading("Status",
					faction.getBaseUIColor(), faction.getDarkUIColor(), Alignment.MID, opad);
			if (isEnding() && !isSucceeded()) {
				if (isFailed() || isAborted()) {
					if (isFailedButNotDefeated()) {
						info.addPara("The " + forces + " are withdrawing.", opad);
					} else {
						info.addPara("The " + forces + " have been defeated and any "
								+ "remaining ships have lost cohesion.", opad);
					}
				} else {
					info.addPara("The " + forces + " are withdrawing.", opad);
				}
			} else if (isEnding() || isSucceeded()) {
				info.addPara("The " + noun + " was successful and the " + forces + " are withdrawing.", opad);
			} else if (curr != null) {
				StarSystemAPI to = raidAction.getWhere();
				if (isInPreLaunchDelay()) {
					if (getSource().getMarket() != null) {
						BaseHubMission.addStandardMarketDesc("The " + noun + " is in the initial phase at",
								getSource().getMarket(), info, opad);
					}
				} else if (PREPARE_ACTION.equals(curr.getId())) {
					if (getSource().getMarket() != null) {
						BaseHubMission.addStandardMarketDesc("Massing together at the edge of the system, ",
								getSource().getMarket(), info, opad);
					} else {
						info.addPara("Massing together at the edge of the system, " + getSource().getName() + ".", opad);
					}
				} else if (TRAVEL_ACTION.equals(curr.getId())) {
					if (getSource().getMarket() == null) {
						info.addPara("Traveling to the " +
								to.getNameWithLowercaseTypeShort() + ".", opad);
					} else {
						info.addPara("Traveling from " + getSource().getMarket().getName() + " to the " +
								to.getNameWithLowercaseTypeShort() + ".", opad);
					}
				} else if (RETURN_ACTION.equals(curr.getId())) {
					if (getSource().getMarket() == null) {
						info.addPara("Departing to deep space.", opad);
					} else {
						info.addPara("Departing to " + getSource().getMarket().getName() + ".", opad);
					}
				} else if (PAYLOAD_ACTION.equals(curr.getId())) {
					addPayloadActionStatus(info, width, height, opad);
				}
			}
		}
	}

	public boolean hasCustomRaidAction() {
		return true;
	}

	public void doCustomRaidAction(CampaignFleetAPI fleet, MarketAPI market, float raidStr) {
		WeightedRandomPicker<String> picker = new WeightedRandomPicker<>();

		for (Industry industry : market.getIndustries()) {
			for (String id : VALID_TARGETS)
				if (Objects.equals(industry.getId(), id))
					picker.add(id);
		}

		Industry target = market.getIndustry(picker.pick());

		StatBonus defenderBase = new StatBonus();

		StatBonus defender = market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD);
		String increasedDefensesKey = "core_addedDefStr";
		float added = getDefenderIncreaseValue(market);
		if (added > 0) {
			defender.modifyFlat(increasedDefensesKey, added, "Increased defender preparedness");
		}
		float defenderStr = (int) Math.round(defender.computeEffective(defenderBase.computeEffective(0f)));
		defender.unmodifyFlat(increasedDefensesKey);

		boolean canDisrupt = true;
		float raidMult = raidStr / Math.max(1f, (raidStr + defenderStr));
		raidMult = Math.round(raidMult * 100f) / 100f;

		if (raidMult < DISRUPTION_THRESHOLD) {
			canDisrupt = false;
		}
		if (!canDisrupt) return;

		applyDefenderIncreaseFromRaid(market);

		String reason = faction.getDisplayName() + " attack";

		applyRaidStabiltyPenalty(market, reason, raidMult);
		//Misc.setFlagWithReason(market.getMemoryWithoutUpdate(), MemFlags.RECENTLY_RAIDED,
		//		faction.getId(), true, 30f);
		//Misc.setRaidedTimestamp(market);

		if (target != null) {
			if (target.getSpec().hasTag(Industries.TAG_STATION)) {
				if (target.isDisrupted())
					market.removeIndustry(target.getId(), null, false);
				else
					target.setDisrupted(raidMult * 60f);
			} else
				market.removeIndustry(target.getId(), null, false);
		}
	}
}




