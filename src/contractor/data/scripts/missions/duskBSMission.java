package contractor.data.scripts.missions;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.intel.deciv.DecivTracker;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithSearch;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantOfficerGeneratorPlugin;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import contractor.data.world.dusk.ContractorMiscGen;

import java.awt.*;
import java.util.List;

import static com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.random;
import static contractor.data.scripts.util.ContractorStaticVars.*;

public class duskBSMission extends HubMissionWithSearch implements FleetEventListener {
	public enum Stage {
		VISITDUSKSYS,
		VISITBLACKSITE,
		SEARCH,
		CONFRONT,
		COMPLETEKILL,
		FAILED,
	}

	protected SectorEntityToken duskPlanet;
	protected SectorEntityToken blackSite;
	protected SectorEntityToken rogueBlackSite;
	protected StarSystemAPI duskSys;
	protected StarSystemAPI blackSiteSys;
	protected StarSystemAPI blackSiteRogueSys;
	protected CampaignFleetAPI affairsFleet;
	protected PersonAPI rogueAdmin;

	@Override
	protected boolean create(MarketAPI createdAt, boolean barEvent) {
		setMissionId("dgBSM");
		setGiverIsPotentialContactOnSuccess(0f);
		setStoryMission();
		setNoAbandon();

		rogueAdmin = Global.getSector().getFaction(Factions.REMNANTS).createRandomPerson();
		rogueAdmin.setRankId("internalAffairs");
		rogueAdmin.setPostId("internalAffairs");
		rogueAdmin.setVoice(Voices.FAITHFUL);
		rogueAdmin.getMemoryWithoutUpdate().set("$dgBSMRogue", true);

		blackSiteSys = Global.getSector().getStarSystem(APOPHIS_ID);
		//if (Global.getSector().getStarSystem("Apophis").getEntityById("duskblacksite") == null) return false; //check for market destroy
		blackSite = blackSiteSys.getEntityById("duskblacksite");

		duskSys = Global.getSector().getStarSystem(KOIT_ID);
		duskPlanet = duskSys.getEntityById("duskgroup_planet");

		requireSystemInterestingAndNotUnsafeOrCore();
		preferSystemOnFringeOfSector();
		preferSystemUnexplored();
		preferSystemBlackHole();
		blackSiteRogueSys = pickSystem(true);
		if (blackSiteRogueSys == null) return false;

		PlanetAPI planet;
		List<BaseThemeGenerator.OrbitGap> gaps = BaseThemeGenerator.findGaps(blackSiteRogueSys.getCenter(), 7000, 16000, 800);
		float orbitRadius = 8000;
		if (!gaps.isEmpty()) {
			orbitRadius = (gaps.get(0).start + gaps.get(0).end) * 0.5f;
		}
		float orbitDays = orbitRadius / (20f + random.nextFloat() * 5f);
		float radius = 100f + random.nextFloat() * 50f;
		float angle = random.nextFloat() * 360f;
		String type = Planets.BARREN;
		String name = "Midnight";
		planet = blackSiteRogueSys.addPlanet("duskRoguePlanet", blackSiteRogueSys.getStar(), name, type, angle, radius, orbitRadius, orbitDays);

		if (planet == null) {
			if (Global.getSettings().isDevMode())
				Global.getLogger(ContractorMiscGen.class).warn("FAILED TO CREATE/FIND PLANET IN SYSTEM FOR duskBSM");
			return false;
		}

		//planet = blackSiteRogueSys.addPlanet("duskRoguePlanet", blackSiteRogueSys.getStar(), "Midnight", "duskfrozen", 200, 110, 8100, 420);
		planet.changeType("duskfrozen", random);
		planet.getMarket().getConditions().clear();
		planet.getMarket().addCondition(Conditions.VERY_COLD);
		planet.getMarket().addCondition(Conditions.DARK);
		planet.getMarket().addCondition("red_ice");
		planet.getMarket().addCondition(Conditions.ORE_MODERATE);
		planet.getMarket().addCondition(Conditions.RARE_ORE_SPARSE);
		planet.getMarket().addCondition(Conditions.VOLATILES_DIFFUSE);

		rogueBlackSite = blackSiteRogueSys.addCustomEntity("duskrogueblacksite", "Dusk Blacksite Omega", "duskblacksite", CONTRACTOR_DRG_ID);
		rogueBlackSite.setCustomDescriptionId("duskblacksite_desc");
		rogueBlackSite.setCircularOrbitPointingDown(blackSiteRogueSys.getEntityById("duskRoguePlanet"), 155f, 600f, 100f);
		rogueBlackSite.setInteractionImage("illustrations", "duskblacksite");
		rogueBlackSite.setSensorProfile(1f);
		rogueBlackSite.setDiscoverable(true);
		rogueBlackSite.setDiscoveryXP(4000f);
		rogueBlackSite.getMemoryWithoutUpdate().set(DecivTracker.NO_DECIV_KEY, true); //needed?

		FleetMemberAPI station = genStation(rogueBlackSite.getOrbit());
		station.setId("duskrogueblacksite_station");
		station.setShipName("Dusk Blacksite Omega");
		//station.setSpriteOverride("duskblacksite");

		beginEnteredLocationTrigger(duskSys, Stage.VISITDUSKSYS);
		triggerCreateFleet(FleetSize.LARGE, FleetQuality.VERY_HIGH, "contractor_duskgroup", "duskInternalAffairs", duskPlanet);
		triggerSetFleetOfficers(OfficerNum.MORE, OfficerQuality.HIGHER);
		triggerSetFleetCommander(Global.getSector().getFaction("contractor_duskgroup").createRandomPerson());
		triggerSetFleetFaction("contractor_duskgroup");
		triggerSetFleetMemoryValue(MemFlags.MEMORY_KEY_SOURCE_MARKET, "dusk_matador_market");
		triggerMakeFleetIgnoredByOtherFleets();
		triggerMakeFleetIgnoreOtherFleetsExceptPlayer();
		triggerFleetAllowLongPursuit();
		triggerSetFleetAlwaysPursue();
		triggerFleetNoAutoDespawn();
		triggerFleetNoJump();
		triggerPickLocationAroundEntity(duskPlanet, 10);
		triggerSpawnFleetAtPickedLocation();
		triggerMakeHostileAndAggressive();
		triggerSetFleetGenericHailPermanent("dgBSMHail");
		triggerOrderFleetInterceptPlayer();
		triggerFleetMakeImportantPermanent(null);
		triggerSetFleetMemoryValue("$dgBSMFleet", true);
		triggerSetFleetMissionRef("$dgBSM_ref");
		endTrigger();

		setStartingStage(Stage.VISITDUSKSYS);
		setSuccessStage(Stage.COMPLETEKILL);
		setFailureStage(Stage.FAILED);

		setStageOnGlobalFlag(Stage.VISITBLACKSITE, "$duskBSMFleetMet");
		setStageOnGlobalFlag(Stage.SEARCH, "$duskBSMBlacksiteComm");
		setStageOnGlobalFlag(Stage.CONFRONT, "$duskBSMConfront");
		setStageOnGlobalFlag(Stage.COMPLETEKILL, "$duskBSMKill");
		setStageOnGlobalFlag(Stage.FAILED, "$duskBSMQuit");

		if (!setGlobalReference("$dgBSM_ref")) return false;

		Global.getSoundPlayer().playUISound("ui_intel_log_update", 1, 1);
		return true;
	}

	public void setReward() {
		setCreditReward(CreditReward.VERY_HIGH);
	}

	protected void updateInteractionDataImpl() {
		set("$dgBSMRogue_Name", rogueAdmin.getNameString());
		set("$dgBSMRogue_ManWom", rogueAdmin.getManOrWoman());
		set("$dgBSMRogue_HeShe", rogueAdmin.getHeOrShe());
		set("$dgBSMRogue_HisHer", rogueAdmin.getHisOrHer());

		set("$dgBSMSysName", blackSiteRogueSys.getBaseName());
		set("$dgBSMSysDist", getDistanceLY(blackSiteRogueSys));
	}

	@Override
	public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, CampaignEventListener.FleetDespawnReason reason, Object param) {
	}

	@Override
	public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {
	}

	// description when selected in intel screen
	@Override
	public void addDescriptionForNonEndStage(TooltipMakerAPI info, float width, float height) {
		float opad = 10f;
		Color h = Misc.getHighlightColor();
		if (currentStage == Stage.VISITDUSKSYS) {
			info.addPara("I've received a mysterious message from the Dusk Research Group saying they will speak with me.", opad);
		} else if (currentStage == Stage.VISITBLACKSITE) {
			info.addPara("A DRG Internal Affairs fleet has asked me to visit the " +
					blackSiteSys.getNameWithLowercaseTypeShort() + " system and search the asteroid belt.", opad);
		} else if (currentStage == Stage.SEARCH) {
			info.addPara(Global.getSector().getImportantPeople().getPerson("duskBlacksiteAlphaAdmin").getNameString() + ", a DRG Internal Affairs Officer has asked " +
					"me to find and eliminate a rogue blacksite in the " + blackSiteRogueSys.getBaseName() + " system.", opad);
		}
	}

	// short description in popups and the intel entry
	@Override
	public boolean addNextStepText(TooltipMakerAPI info, Color tc, float pad) {
		Color h = Misc.getHighlightColor();
		if (currentStage == Stage.VISITDUSKSYS) {
			info.addPara("I've received a mysterious message from the Dusk Research Group.", tc, pad);
		} else if (currentStage == Stage.VISITBLACKSITE) {
			info.addPara("DRG has asked me to visit an asteroid belt in " + blackSiteSys.getNameWithLowercaseTypeShort() + ".", tc, pad);
		} else if (currentStage == Stage.SEARCH) {
			info.addPara("Find and eliminate a rogue blacksite in the " + blackSiteRogueSys.getBaseName() + " system.", tc, pad);
		}
		return false;
	}

	@Override
	public SectorEntityToken getMapLocation(SectorMapAPI map) {
		if (currentStage == Stage.VISITBLACKSITE) {
			return getMapLocationFor(blackSiteSys.getCenter());
		} else if (currentStage == Stage.SEARCH) {
			return getMapLocationFor(blackSiteRogueSys.getCenter());
		}
		return null;
	}

	@Override
	public String getBaseName() {
		return "An Internal Affair";
	}

	private FleetMemberAPI genStation(OrbitAPI orbit) {
		CampaignFleetAPI fleet = FleetFactoryV3.createEmptyFleet(CONTRACTOR_DRG_ID, FleetTypes.BATTLESTATION, null);

		FleetMemberAPI member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, "station2_hightech_Standard");
		fleet.getFleetData().addFleetMember(member);

		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_JUMP, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_ALLOW_DISENGAGE, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_FORCE_TRANSPONDER_OFF, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MISSION_IMPORTANT, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_IGNORED_BY_OTHER_FLEETS, true);
		//fleet.getMemoryWithoutUpdate().set("$dgBSMroguestation", true);
		fleet.addTag(Tags.NEUTRINO_HIGH);
		fleet.setCommander(rogueAdmin);

		fleet.setStationMode(true);

		blackSiteRogueSys.addEntity(fleet);

		fleet.clearAbilities();
		fleet.getDetectedRangeMod().modifyFlat("gen", 500f);

		fleet.setAI(null);

		fleet.setOrbit(orbit);

		String coreId = Commodities.ALPHA_CORE;

		AICoreOfficerPlugin plugin = Misc.getAICoreOfficerPlugin(coreId);
		PersonAPI commander = plugin.createPerson(coreId, fleet.getFaction().getId(), random);

		fleet.setCommander(commander);
		fleet.getFlagship().setCaptain(commander);
		fleet.setId("duskrogueblacksite_fleet");

		RemnantOfficerGeneratorPlugin.integrateAndAdaptCoreForAIFleet(fleet.getFlagship());
		RemnantOfficerGeneratorPlugin.addCommanderSkills(commander, fleet, null, 3, random);

		member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());

		return member;
	}
}