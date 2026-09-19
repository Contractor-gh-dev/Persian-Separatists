package contractor.data.scripts.missions;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.Script;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.missions.DelayedFleetEncounter;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithSearch;
import com.fs.starfarer.api.impl.campaign.missions.hub.ReqMode;
import com.fs.starfarer.api.impl.campaign.procgen.NameGenData;
import com.fs.starfarer.api.impl.campaign.procgen.PlanetConditionGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.ProcgenUsedNames;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import contractor.data.scripts.rulecmd.DrgROALAMissionCMD;
import contractor.data.world.dusk.ContractorMiscGen;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.getLocations;
import static contractor.data.scripts.util.ContractorStaticVars.*;

public class DrgROALAMission extends HubMissionWithSearch implements FleetEventListener {
	public enum Stage {
		GETEQUIPMENT,
		BOUNTY,
		BOUNTYKILLED,
		SEARCH4ARK,
		SEARCH4PLANET,
		DECIDE,
		COMPLETE,
		FAILED
	}

	protected StarSystemAPI planetSystem, arkSystem;
	protected PlanetAPI planet;
	protected FleetSize fleetSize;
	protected String fleetFaction, fleetType;
	protected FleetQuality fleetQuality;

	@Override
	public void acceptImpl(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
		Global.getSoundPlayer().playUISound("ui_intel_log_update", 1, 1);
	}

	public static StarSystemAPI getPlanetSystem() {
		return (StarSystemAPI) Global.getSector().getPersistentData().get(DRG_ROALA_PLANET_SYSTEM_KEY);
	}

	public static StarSystemAPI getArkSystem() {
		return (StarSystemAPI) Global.getSector().getPersistentData().get(DRG_ROALA_ARK_SYSTEM_KEY);
	}

	public static StarSystemAPI getPlanet() {
		return (StarSystemAPI) Global.getSector().getPersistentData().get(DRG_ROALA_PLANET_SYSTEM_KEY);
	}

	public static SectorEntityToken getQMPrimary() {
		return ((PersonAPI) Global.getSector().getPersistentData().get(DRG_ROALA_QM_KEY)).getMarket().getPrimaryEntity();
	}

	@Override
	public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, CampaignEventListener.FleetDespawnReason reason, Object param) {
	}

	@Override
	public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {
	}

	@Override
	protected boolean create(MarketAPI createdAt, boolean barEvent) {
		setGiverFaction(CONTRACTOR_DRG_ID);
		setGiverImportance(PersonImportance.HIGH);
		setGiverPost(Ranks.POST_MERCHANT);
		setGiverRank(Ranks.SPACE_CHIEF);
		setGiverVoice(Voices.SCIENTIST);
		setGiverTags(Tags.CONTACT_SCIENCE);
		findOrCreateGiver(createdAt, true, false);

		PersonAPI person = getPerson();
		if (person == null) return false;

		setMissionId(DrgROALAMissionCMD.id);
		setGiverIsPotentialContactOnSuccess();
		setStoryMission();
		setNoAbandon();

		if (!setGlobalReference("$drgROALA_ref", "$drgROALA_inprog")) return false;

		PersonAPI survivorAdmin = Global.getSector().getFaction(CONTRACTOR_DRG_SURV_ID).createRandomPerson();
		survivorAdmin.setRankId("factionLeader");
		survivorAdmin.setPostId("factionLeader");
		survivorAdmin.setVoice(Voices.SCIENTIST);

		preferSystemOnFringeOfSector();
		preferSystemUnexplored();
		preferSystemHasNumPlanets(4);
		preferSystemHasNumPlanets(3);
		preferSystemHasNumPlanets(2);
		planetSystem = pickSystem(true);
		if (planetSystem == null)
			return false;
		planetSystem.addTag(Tags.SYSTEM_ALREADY_USED_FOR_STORY);
		Global.getSector().getPersistentData().put(DRG_ROALA_PLANET_SYSTEM_KEY, planetSystem);
		Global.getSector().getMemoryWithoutUpdate().set(DRG_ROALA_PLANET_SYSTEM_KEY, planetSystem.getId());

		requireSystemIs(planetSystem);
		requirePlanetConditions(ReqMode.ALL, Conditions.HABITABLE);
		requirePlanetUnpopulated();
		planet = pickPlanet(true);
		if (planet == null) {
			List<BaseThemeGenerator.OrbitGap> gaps = BaseThemeGenerator.findGaps(planetSystem.getCenter(), 3000, 10000, 800);
			float orbitRadius = 3500;
			if (!gaps.isEmpty()) {
				orbitRadius = (gaps.get(0).start + gaps.get(0).end) * 0.5f;
			}
			float orbitDays = orbitRadius / (20f + genRandom.nextFloat() * 5f);
			float radius = 100f + genRandom.nextFloat() * 50f;
			float angle = genRandom.nextFloat() * 360f;
			String type = Planets.BARREN;
			ProcgenUsedNames.NamePick namePick = ProcgenUsedNames.pickName(NameGenData.TAG_PLANET, null, null);
			String name = namePick.nameWithRomanSuffixIfAny;
			planet = planetSystem.addPlanet("drgROALA_planet", planetSystem.getStar(), name, type, angle, radius, orbitRadius, orbitDays);

			if (planet == null) {
				if (Global.getSettings().isDevMode())
					Global.getLogger(ContractorMiscGen.class).warn("FAILED TO CREATE/FIND PLANET IN SYSTEM FOR drgROALA");
				return false;
			}
		}
		Global.getSector().getPersistentData().put(DRG_ROALA_PLANET_KEY, planet);
		Global.getSector().getMemoryWithoutUpdate().set(DRG_ROALA_PLANET_KEY, planet.getId());

		preferSystemOnFringeOfSector();
		preferSystemUnexplored();
		requireSystemNot(planetSystem);
		requireSystemWithinRangeOf(planetSystem.getLocation(), 10f);
		arkSystem = pickSystem(true);
		if (arkSystem == null)
			return false;
		arkSystem.addTag(Tags.SYSTEM_ALREADY_USED_FOR_STORY);
		Global.getSector().getPersistentData().put(DRG_ROALA_ARK_SYSTEM_KEY, arkSystem);
		Global.getSector().getMemoryWithoutUpdate().set(DRG_ROALA_ARK_SYSTEM_KEY, arkSystem.getId());

		planet.changeType(Planets.PLANET_WATER, genRandom);
		planet.addTag(Tags.NOT_RANDOM_MISSION_TARGET);
		planet.getMarket().getConditions().clear();
		PlanetConditionGenerator.generateConditionsForPlanet(null, planet, planetSystem.getAge());
		planet.getMarket().removeCondition(Conditions.DECIVILIZED);
		planet.getMarket().removeCondition(Conditions.DECIVILIZED_SUBPOP);
		planet.getMarket().removeCondition(Conditions.RUINS_EXTENSIVE);
		planet.getMarket().removeCondition(Conditions.RUINS_SCATTERED);
		planet.getMarket().removeCondition(Conditions.RUINS_VAST);
		planet.getMarket().removeCondition(Conditions.RUINS_WIDESPREAD);
		planet.getMarket().removeCondition(Conditions.INIMICAL_BIOSPHERE);
		planet.getMarket().removeCondition(Conditions.EXTREME_WEATHER);
		planet.getMarket().removeCondition(Conditions.HABITABLE);

		planet.getMarket().addCondition(Conditions.RURAL_POLITY);
		planet.getMarket().addCondition(Conditions.HABITABLE);

		setRepPenaltyFaction(0f);
		setRepPenaltyPerson(0f);

		setStartingStage(Stage.GETEQUIPMENT);
		setSuccessStage(Stage.COMPLETE);
		setFailureStage(Stage.FAILED);

		setStageOnGlobalFlag(Stage.BOUNTY, "$drgROALA_agreeBounty");
		setStageOnGlobalFlag(Stage.BOUNTYKILLED, "$drgROALA_bountyKilled");
		setStageOnGlobalFlag(Stage.SEARCH4ARK, "$drgROALA_gotGear");
		setStageOnGlobalFlag(Stage.SEARCH4PLANET, "$drgROALA_foundArk");
		setStageOnGlobalFlag(Stage.DECIDE, "$drgROALA_foundPlanet");
		setStageOnGlobalFlag(Stage.COMPLETE, "$drgROALA_complete");
		setStageOnGlobalFlag(Stage.FAILED, "$drgROALA_failed");

		beginEnteredLocationTrigger(Global.getSector().getStarSystem("valhalla"), Stage.BOUNTY);
		triggerCreateFleet(FleetSize.SMALL, FleetQuality.DEFAULT, Factions.HEGEMONY, FleetTypes.PATROL_MEDIUM, Global.getSector().getStarSystem("valhalla"));
		triggerSpawnFleetNear(Global.getSector().getStarSystem("valhalla").getEntityById("ragnar_complex"), null, "$drgROALA_ref");
		triggerFleetSetName("Inspector Fleet");
		triggerFleetSetPatrolActionText("Idle");
		triggerFleetNoAutoDespawn();
		triggerFleetNoJump();
		triggerMakeFleetIgnoredByOtherFleets();
		triggerMakeFleetIgnoreOtherFleetsExceptPlayer();
		triggerOrderFleetPatrol(Global.getSector().getStarSystem("valhalla").getEntityById("ragnar_complex"));
		triggerFleetMakeImportant("$drgROALA_bounty", Stage.BOUNTY);
		triggerMakeFleetGoAwayAfterDefeat();
		triggerFleetAddDefeatTrigger("drgROALABountyDestroyed");
		endTrigger();

		beginStageTrigger(Stage.SEARCH4ARK);
		triggerRunScriptAfterDelay(0f, new spawnArk());
		endTrigger();

		return true;
	}

	//description when selected in intel screen
	@Override
	public void addDescriptionForNonEndStage(TooltipMakerAPI info, float width, float height) {
		float opad = 10f;
		Color h = Misc.getHighlightColor();
		if (currentStage == Stage.GETEQUIPMENT) {
			info.addPara("A DRG scientist has asked me to acquire special equipment for an expedition to a lost Ark vessel. I should talk to quarter or portmasters to get the items I need.", opad);
		} else if (currentStage == Stage.BOUNTY) {
			info.addPara("The Tri-Tachyon quartermaster needs me to perform a hitjob on a Hegemony inspector in the Valhalla system before I receive the gear. \n\nThe target should be near the Ragnar Complex station.", opad);
		} else if (currentStage == Stage.BOUNTYKILLED) {
			info.addPara("I need to return to the Tri-Tachyon quartermaster to collect the equipment and data.", opad);
		} else if (currentStage == Stage.SEARCH4ARK) {
			info.addPara("I need to travel to the " + arkSystem.getBaseName() + "system and look for the Ark.", opad);
		} else if (currentStage == Stage.SEARCH4PLANET) {
			info.addPara("The Ark is mostly empty and clues point to the " + planetSystem.getBaseName() + "system.", opad);
		} else if (currentStage == Stage.DECIDE) {
			info.addPara("The Ark survivors has asked me not to reveal their existence, but the DRG will want to know. I have to make a decision.", opad);
		}
	}

	//SHORT description in popups and the intel entry
	@Override
	public boolean addNextStepText(TooltipMakerAPI info, Color tc, float pad) {
		Color h = Misc.getHighlightColor();
		if (currentStage == Stage.GETEQUIPMENT) {
			info.addPara("A DRG scientist has asked me to acquire special equipment for the expedition.", tc, pad);
		} else if (currentStage == Stage.BOUNTY) {
			info.addPara("The Tri-Tachyon QM needs me to perform a hitjob on a Hegemony inspector.", tc, pad);
		} else if (currentStage == Stage.BOUNTYKILLED) {
			info.addPara("Return to Tri-Tachyon QM to collect heavy equipment.", tc, pad);
		} else if (currentStage == Stage.SEARCH4ARK) {
			info.addPara("Look for the Ark in the " + arkSystem.getBaseName() + "system.", tc, pad);
		} else if (currentStage == Stage.SEARCH4PLANET) {
			info.addPara("Follow up clues leading to the " + planetSystem.getBaseName() + "system.", tc, pad);
		} else if (currentStage == Stage.DECIDE) {
			info.addPara("Decide the survivors fate.", tc, pad);
		}
		return false;
	}

	@Override
	public SectorEntityToken getMapLocation(SectorMapAPI map, Object currentStage) {
		if (currentStage == Stage.BOUNTY) {
			return Global.getSector().getStarSystem("valhalla").getEntityById("ragnar_complex");
		} else if (currentStage == Stage.BOUNTYKILLED) {
			return getQMPrimary();
		} else if (currentStage == Stage.SEARCH4ARK) {
			return getArkSystem().getCenter();
		} else if (currentStage == Stage.SEARCH4PLANET) {
			return getPlanetSystem().getCenter();
		} else if (currentStage == Stage.DECIDE) {
			return getPlanet().getCenter();
		}
		return super.getMapLocation(map, currentStage);
	}

	@Override
	public String getBaseName() {
		return "Raiders of a Lost Ark";
	}

	protected void setFleetFrac(FleetSize size) {
		fleetSize = size;
	}

	protected void setFleetFaction(String faction, String type) {
		fleetFaction = faction;
		fleetType = type;
	}

	protected void setFleetQuality(FleetQuality qual) {
		fleetQuality = qual;
	}

	@Override
	public boolean callEvent(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
		String action = params.get(0).getString(memoryMap);
		switch (action) {
			case "prepfleet" -> {
				if (fleetFaction == null || fleetQuality == null || fleetSize == null || fleetType == null)
					assignFleet();

				DelayedFleetEncounter e = new DelayedFleetEncounter(genRandom, getMissionId());
				e.setDelay(10f);
				e.setEncounterFromSomewhereInSystem();
				e.beginCreate();
				e.triggerCreateFleet(fleetSize, fleetQuality, fleetFaction, fleetType, arkSystem);
				e.setRepFactionChangesTiny();
				e.triggerSetAdjustStrengthBasedOnQuality(true, getQuality());
				e.triggerSetFleetCommander(Global.getSector().getFaction(fleetFaction).createRandomPerson());
				e.triggerMakeFleetIgnoredByOtherFleets();
				e.triggerMakeFleetIgnoreOtherFleetsExceptPlayer();
				e.triggerFleetAllowLongPursuit();
				e.triggerSetFleetAlwaysPursue();
				e.triggerFleetNoAutoDespawn();
				e.triggerMakeHostileAndAggressive();
				e.triggerSetFleetGenericHailPermanent("$drgROALA_fleetHail");
				e.triggerOrderFleetInterceptPlayer();
				e.triggerFleetMakeImportantPermanent(null);
				e.triggerSetFleetMemoryValue("$drgROALA_fleet", true);
				e.triggerSetFleetMissionRef("$drgROALA_ref");
				e.endCreate();
				return true;
			}
			case "rememberqm" -> {
				PersonAPI ttqm = dialog.getInteractionTarget().getActivePerson();
				Global.getSector().getPersistentData().put(DRG_ROALA_QM_KEY, ttqm);
				Global.getSector().getMemoryWithoutUpdate().set(DRG_ROALA_QM_KEY, ttqm.getId());
				return true;
			}
		}
		return super.callEvent(ruleId, dialog, params, memoryMap);
	}

	private void assignFleet() {
		float fpFrac = ((Global.getSettings().getBattleSize() / 2f) / Global.getSector().getPlayerFleet().getFleetPoints()) * 0.9f;
		FleetSize size = FleetSize.LARGER;
		String faction, type;
		FleetQuality qual;
		String fleetSet = Global.getSector().getPlayerPerson().getMemoryWithoutUpdate().get("$drgROALA_fleet").toString();
		switch (fleetSet) {
			case "pirate" -> {
				faction = Factions.PIRATES;
				type = FleetTypes.RAIDER;
				qual = FleetQuality.DEFAULT;
			}
			case "heg" -> {
				faction = Factions.HEGEMONY;
				type = FleetTypes.INSPECTION_FLEET;
				qual = FleetQuality.HIGHER;
				fpFrac *= 0.9f;
			}
			case "tri" -> {
				faction = Factions.TRITACHYON;
				type = FleetTypes.MERC_BOUNTY_HUNTER;
				qual = FleetQuality.SMOD_1;
				fpFrac *= 0.7f;
			}
			case "path" -> {
				faction = Factions.LUDDIC_PATH;
				type = FleetTypes.PATROL_MEDIUM;
				qual = FleetQuality.LOWER;
				fpFrac *= 1.2f;
			}
			case "luddic" -> {
				faction = Factions.LUDDIC_CHURCH;
				type = FleetTypes.PATROL_MEDIUM;
				qual = FleetQuality.LOWER;
				fpFrac *= 1.2f;
			}
			default -> {
				Global.getLogger(ContractorMiscGen.class).warn("FAILED SPECIFICATION FOR FLEET $player.drgROALA_fleet; USING FALLBACK");
				faction = Factions.PIRATES;
				type = FleetTypes.RAIDER;
				qual = FleetQuality.DEFAULT;
			}
		}
		for (FleetSize s : FleetSize.values()) {
			if (s.maxFPFraction < fpFrac)
				continue;
			size = s;
			break;
		}
		setFleetQuality(qual);
		setFleetFrac(size);
		setFleetFaction(faction, type);
	}

	protected class spawnArk implements Script {
		@Override
		public void run() {
			if (arkSystem == null)
				arkSystem = (StarSystemAPI) Global.getSector().getPersistentData().get(DRG_ROALA_ARK_SYSTEM_KEY);
			if (planet == null) {
				planet = (PlanetAPI) Global.getSector().getPersistentData().get(DRG_ROALA_PLANET_KEY);
				planetSystem = planet.getStarSystem();
			}
			LinkedHashMap<BaseThemeGenerator.LocationType, Float> weights = new LinkedHashMap<BaseThemeGenerator.LocationType, Float>();
			weights.put(BaseThemeGenerator.LocationType.PLANET_ORBIT, 10f);
			weights.put(BaseThemeGenerator.LocationType.JUMP_ORBIT, 1f);
			weights.put(BaseThemeGenerator.LocationType.NEAR_STAR, 1f);
			weights.put(BaseThemeGenerator.LocationType.OUTER_SYSTEM, 5f);
			weights.put(BaseThemeGenerator.LocationType.IN_ASTEROID_BELT, 15f);
			weights.put(BaseThemeGenerator.LocationType.IN_RING, 15f);
			weights.put(BaseThemeGenerator.LocationType.IN_ASTEROID_FIELD, 15f);
			weights.put(BaseThemeGenerator.LocationType.STAR_ORBIT, 1f);
			weights.put(BaseThemeGenerator.LocationType.IN_SMALL_NEBULA, 1f);
			weights.put(BaseThemeGenerator.LocationType.L_POINT, 1f);
			WeightedRandomPicker<BaseThemeGenerator.EntityLocation> locs = getLocations(genRandom, arkSystem, 100f, weights);
			BaseThemeGenerator.AddedEntity ark = BaseThemeGenerator.addEntity(genRandom, arkSystem, locs, "drg_arkship", CONTRACTOR_DRG_ID);
			ark.entity.setCustomDescriptionId("drg_arkdesc");
			ark.entity.getMemoryWithoutUpdate().set("$drgROALA_ark", 1);

			SectorEntityToken magField = arkSystem.addTerrain("magnetic_field", new MagneticFieldTerrainPlugin.MagneticFieldParams(256f, 275f, ark.entity, 100f, 200f, new Color(210, 40, 170, 140), 0.25f));
			magField.setCircularOrbit(ark.entity, 0f, 0f, 100f);
			weights.clear();
			locs.clear();

			SectorEntityToken station = planetSystem.addCustomEntity("drgROALA_station", "Makeshift Station", Entities.MAKESHIFT_STATION, Factions.DERELICT);
			station.setCircularOrbit(planet, 120f, 150f, 80f);
		}
	}
}
