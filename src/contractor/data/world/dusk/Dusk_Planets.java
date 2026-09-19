package contractor.data.world.dusk;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.intel.deciv.DecivTracker;
import com.fs.starfarer.api.impl.campaign.procgen.*;
import com.fs.starfarer.api.impl.campaign.terrain.AsteroidFieldTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.BaseRingTerrain;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.util.Misc;
import illustratedEntities.memory.ImageDataMemory;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import static contractor.data.scripts.util.ContractorStaticVars.*;

public class Dusk_Planets {

	public static void generate(SectorAPI sector) {
		StarSystemAPI koit_system = sector.createStarSystem(KOIT_ID);
		koit_system.setOptionalUniqueId(KOIT_ID);
		koit_system.setBaseName("Koit");
		koit_system.getLocation().set(-3000, -22500);
		koit_system.setBackgroundTextureFilename("graphics/backgrounds/background5.jpg");
		koit_system.setAge(StarAge.AVERAGE);
		koit_system.setEnteredByPlayer(true);
		koit_system.addTag(Tags.THEME_CORE);

		StarSystemAPI apophis_system = sector.createStarSystem(APOPHIS_ID);
		apophis_system.setOptionalUniqueId(APOPHIS_ID);
		apophis_system.setBaseName("Apophis");
		apophis_system.getLocation().set(-2650, -26750);
		apophis_system.setBackgroundTextureFilename("graphics/backgrounds/background2.jpg");
		koit_system.setAge(StarAge.OLD);
		koit_system.setEnteredByPlayer(false);

		PlanetAPI dusk_koit_sun = koit_system.initStar("Koit", StarTypes.YELLOW, 800f, 450f); // MAKE SUN
		PlanetAPI dusk_apophis_sun = apophis_system.initStar("Apophis", StarTypes.BROWN_DWARF, 250f, 350f);

		PlanetAPI duskgroup_planet = koit_system.addPlanet(DUSK_ID, dusk_koit_sun, "Dusk", "duskfrozen", 200, 110, 8300, 425);
		duskgroup_planet.getSpec().setTilt(10);
		duskgroup_planet.setCustomDescriptionId("duskplanetdesc");
		duskgroup_planet.getSpec().setCloudColor(new Color(35, 23, 44, 100));
		duskgroup_planet.getSpec().setGlowColor(new Color(255, 235, 205, 155));
		duskgroup_planet.getSpec().setUseReverseLightForGlow(true);
		duskgroup_planet.setFaction(CONTRACTOR_DRG_ID);
		duskgroup_planet.applySpecChanges();
		if (Global.getSettings().getModManager().isModEnabled("illustrated_entities"))
			illustratedEntities.helper.ImageHandler.setImage(duskgroup_planet, ImageDataMemory.getInstance().get(1313), false);

		PlanetAPI duskgroup_planet_b = koit_system.addPlanet("duskgroup_planet_b", dusk_koit_sun, "Taygete", Planets.BARREN, 300, 110, 12000, 500);
		duskgroup_planet_b.getSpec().setTilt(42);
		duskgroup_planet_b.getSpec().setGlowColor(new Color(150, 150, 150, 155));
		duskgroup_planet_b.getSpec().setUseReverseLightForGlow(false);
		duskgroup_planet_b.applySpecChanges();

		apophis_system.addPlanet("apophis_planet_a", dusk_apophis_sun, "Jekkad", "lava", 350, 120, 1800, 1750);
		apophis_system.addPlanet("apophis_planet_b", dusk_apophis_sun, "Voit", "toxic", 350, 130, 3350, 250);
		apophis_system.addPlanet("apophis_planet_c", dusk_apophis_sun, "Luto", "rocky_ice", 350, 60, 5650, 300);
		PlanetAPI apophis_planet_d = apophis_system.addPlanet("apophis_planet_d", dusk_apophis_sun, "Rostom", Planets.GAS_GIANT, 350, 275, 9400, 375);
		apophis_system.setEnteredByPlayer(true);

		SectorEntityToken duskBlacksiteStation = apophis_system.addCustomEntity("duskblacksite", "Dusk Blacksite Alpha", "duskblacksite", CONTRACTOR_DRG_ID);
		duskBlacksiteStation.setCustomDescriptionId("duskblacksite_desc");
		duskBlacksiteStation.setCircularOrbitPointingDown(dusk_apophis_sun, 80f, 5625f, 300f);
		duskBlacksiteStation.setInteractionImage("illustrations", "duskblacksite");
		duskBlacksiteStation.setSensorProfile(300f);
		duskBlacksiteStation.setDiscoverable(true);
		duskBlacksiteStation.setDiscoveryXP(2000f);
		duskBlacksiteStation.getMemoryWithoutUpdate().set(DecivTracker.NO_DECIV_KEY, true); //needed?
		duskBlacksiteStation.getMemoryWithoutUpdate().set("$dusk_blacksite_market", true);

		CampaignFleetAPI stationFleet = genStation(duskBlacksiteStation.getOrbit(), apophis_system, duskBlacksiteStation.getMarket());
		stationFleet.setNoFactionInName(true);
		stationFleet.getMemoryWithoutUpdate().set(MemFlags.STATION_MARKET, duskBlacksiteStation.getMarket());
		stationFleet.setCircularOrbit(duskBlacksiteStation, 0, 0, 100);
		stationFleet.setHidden(true);

		MarketAPI dusk_matador_market = Global.getFactory().createMarket("dusk_matador_market", duskgroup_planet.getName(), 5);
		dusk_matador_market.setPrimaryEntity(duskgroup_planet);
		dusk_matador_market.setFactionId(CONTRACTOR_DRG_ID);
		dusk_matador_market.setName("Dusk");
		dusk_matador_market.addCondition(Conditions.POPULATION_5);
		dusk_matador_market.addCondition(Conditions.VERY_COLD);
		dusk_matador_market.addCondition(SEP_CONDITION_REDICE);
		dusk_matador_market.addCondition(Conditions.ORE_SPARSE);
		dusk_matador_market.addCondition(Conditions.VOLATILES_ABUNDANT);
		dusk_matador_market.addCondition(Conditions.INDUSTRIAL_POLITY);
		dusk_matador_market.addIndustry(Industries.MEGAPORT);
		dusk_matador_market.getIndustry(Industries.MEGAPORT).setAICoreId(Commodities.GAMMA_CORE);
		dusk_matador_market.addIndustry(Industries.ORBITALWORKS, new ArrayList<>(List.of(Items.PRISTINE_NANOFORGE)));
		dusk_matador_market.addIndustry(Industries.MINING);
		dusk_matador_market.addIndustry(Industries.WAYSTATION);
		dusk_matador_market.addIndustry(Industries.POPULATION);
		dusk_matador_market.addIndustry(Industries.HIGHCOMMAND);
		dusk_matador_market.getIndustry(Industries.HIGHCOMMAND).setAICoreId(Commodities.GAMMA_CORE);
		dusk_matador_market.addIndustry(Industries.HEAVYBATTERIES, new ArrayList<>(List.of(Items.DRONE_REPLICATOR)));
		dusk_matador_market.addIndustry(Industries.STARFORTRESS_HIGH);
		dusk_matador_market.getIndustry(Industries.STARFORTRESS_HIGH).setAICoreId(Commodities.ALPHA_CORE);
		dusk_matador_market.addSubmarket(Submarkets.SUBMARKET_OPEN);
		dusk_matador_market.addSubmarket(Submarkets.SUBMARKET_BLACK);
		dusk_matador_market.addSubmarket(Submarkets.SUBMARKET_STORAGE);
		dusk_matador_market.addSubmarket(Submarkets.GENERIC_MILITARY);
		dusk_matador_market.getTariff().setBaseValue(0.3f);
		dusk_matador_market.setSurveyLevel(MarketAPI.SurveyLevel.FULL);
		duskgroup_planet.setMarket(dusk_matador_market);

		duskgroup_planet_b.getMarket().setPrimaryEntity(duskgroup_planet_b);
		duskgroup_planet_b.getMarket().setFactionId(Factions.NEUTRAL);
		duskgroup_planet_b.getMarket().setPlanetConditionMarketOnly(true);
		duskgroup_planet_b.getMarket().addCondition(Conditions.NO_ATMOSPHERE);
		duskgroup_planet_b.getMarket().addCondition(Conditions.VERY_COLD);
		duskgroup_planet_b.getMarket().addCondition(Conditions.POOR_LIGHT);
		duskgroup_planet_b.getMarket().addCondition(Conditions.LOW_GRAVITY);
		duskgroup_planet_b.getMarket().addCondition(Conditions.ORE_MODERATE);
		duskgroup_planet_b.getMarket().addCondition(Conditions.RARE_ORE_SPARSE);
		duskgroup_planet_b.getMarket().setSurveyLevel(MarketAPI.SurveyLevel.FULL);

		sector.getEconomy().addMarket(dusk_matador_market, false);
		//sector.getEconomy().addMarket(dusk_blacksite_market,false);

		SectorEntityToken drg_relay = koit_system.addCustomEntity("duskgroup_relay", "DRG Relay", "comm_relay", CONTRACTOR_DRG_ID);
		drg_relay.setCircularOrbit(dusk_koit_sun, 280, 6900, 300);

		SectorEntityToken stableloc2 = koit_system.addCustomEntity(null, null, "stable_location", Factions.NEUTRAL);
		stableloc2.setCircularOrbitPointingDown(dusk_koit_sun, 40, 7000, 305);

		koit_system.addAsteroidBelt(dusk_koit_sun, 340, 3250, 160, 360, 400);
		koit_system.addRingBand(dusk_koit_sun, "misc", "rings_dust0", 256f, 2, Color.white, 128f, 3270, 370f, "ring", "Dust Ring");
		koit_system.addRingBand(dusk_koit_sun, "misc", "rings_asteroids0", 256f, 1, Color.white, 128f, 3280, 380f, "ring", "Dust Ring");
		koit_system.addRingBand(dusk_koit_sun, "misc", "rings_dust0", 256f, 4, Color.white, 128f, 3290, 390f, "ring", "Dust Ring");

		koit_system.addAsteroidBelt(dusk_koit_sun, 850, 5700, 256, 440, 470);
		koit_system.addRingBand(dusk_koit_sun, "misc", "rings_dust0", 256f, 1, Color.white, 256f, 5670, 450f, "ring", "Dust Ring");
		koit_system.addRingBand(dusk_koit_sun, "misc", "rings_asteroids0", 256f, 2, Color.white, 256f, 5700, 460f, "ring", "Dust Ring");
		koit_system.addRingBand(dusk_koit_sun, "misc", "rings_dust0", 256f, 5, Color.white, 256f, 5730, 470f, "ring", "Dust Ring");

		koit_system.addAsteroidBelt(dusk_koit_sun, 1200, 9950, 300, 850, 900);
		koit_system.addRingBand(dusk_koit_sun, "misc", "rings_dust0", 256f, 3, Color.white, 128f, 10090, 860f, "ring", "Dust Ring");
		koit_system.addRingBand(dusk_koit_sun, "misc", "rings_asteroids0", 256f, 3, Color.white, 128f, 10100, 870f, "ring", "Dust Ring");
		koit_system.addRingBand(dusk_koit_sun, "misc", "rings_dust0", 256f, 6, Color.white, 128f, 10110, 880f, "ring", "Dust Ring");

		SectorEntityToken asteroidFieldOne = koit_system.addTerrain(Terrain.ASTEROID_FIELD, new AsteroidFieldTerrainPlugin.AsteroidFieldParams(900f, 900f, 70, 80, 4f, 16f, null));
		asteroidFieldOne.setCircularOrbit(dusk_koit_sun, 100, 8000, 425);
		SectorEntityToken asteroidFieldTwo = koit_system.addTerrain(Terrain.ASTEROID_FIELD, new AsteroidFieldTerrainPlugin.AsteroidFieldParams(550f, 550f, 50, 60, 4f, 16f, null));
		asteroidFieldTwo.setCircularOrbit(dusk_koit_sun, 320, 4300, 210);
		SectorEntityToken asteroidFieldThree = koit_system.addTerrain(Terrain.ASTEROID_FIELD, new AsteroidFieldTerrainPlugin.AsteroidFieldParams(450f, 450f, 45, 60, 4f, 16f, null));
		asteroidFieldThree.setCircularOrbit(dusk_koit_sun, 220, 2200, 150);
		SectorEntityToken asteroidFieldFour = koit_system.addTerrain(Terrain.ASTEROID_FIELD, new AsteroidFieldTerrainPlugin.AsteroidFieldParams(950f, 950f, 70, 80, 4f, 16f, null));
		asteroidFieldFour.setCircularOrbit(dusk_koit_sun, 330, 7950, 425);

		koit_system.addRingBand(dusk_koit_sun, "misc", "rings_dust0", 256f, 5, Color.white, 128f, 3200, 360f, "ring", "Dust Ring");
		koit_system.addRingBand(dusk_koit_sun, "misc", "rings_dust0", 256f, 7, Color.white, 128f, 3210, 370f, "ring", "Dust Ring");
		koit_system.addRingBand(dusk_koit_sun, "misc", "rings_dust0", 256f, 10, Color.white, 128f, 3220, 380f, "ring", "Dust Ring");

		koit_system.addRingBand(duskgroup_planet, "misc", "rings_ice0", 256f, 5, Color.white, 96f, 400, 160f, "ring", "Ice Ring");
		koit_system.addRingBand(duskgroup_planet, "misc", "rings_ice0", 256f, 7, Color.white, 96f, 410, 170f, "ring", "Ice Ring");
		koit_system.addRingBand(duskgroup_planet, "misc", "rings_ice0", 256f, 10, Color.white, 96f, 420, 180f, "ring", "Ice Ring");

		koit_system.addRingBand(dusk_koit_sun, "misc", "rings_dust0", 256f, 5, Color.lightGray, 256f, 9900, 560f, "ring", "Dust Ring");
		koit_system.addRingBand(dusk_koit_sun, "misc", "rings_dust0", 256f, 7, Color.lightGray, 256f, 9910, 560f, "ring", "Dust Ring");
		koit_system.addRingBand(dusk_koit_sun, "misc", "rings_dust0", 256f, 10, Color.lightGray, 256f, 9920, 560f, "ring", "Dust Ring");

		koit_system.addRingBand(duskgroup_planet_b, "misc", "rings_ice0", 256f, 3, Color.white, 128f, 400, 160f, "ring", "Ice Ring");
		koit_system.addRingBand(duskgroup_planet_b, "misc", "rings_ice0", 256f, 6, Color.white, 128f, 420, 170f, "ring", "Ice Ring");
		koit_system.addRingBand(duskgroup_planet_b, "misc", "rings_ice0", 256f, 9, Color.white, 128f, 440, 180f, "ring", "Ice Ring");


		koit_system.addAsteroidBelt(dusk_koit_sun, 100, 5300, 256, 150, 250, Terrain.ASTEROID_BELT, null);
		koit_system.addAsteroidBelt(dusk_koit_sun, 100, 5700, 256, 150, 250, Terrain.ASTEROID_BELT, null);

		koit_system.addAsteroidBelt(dusk_koit_sun, 100, 6150, 128, 200, 300, Terrain.ASTEROID_BELT, null);
		koit_system.addAsteroidBelt(dusk_koit_sun, 100, 6450, 188, 200, 300, Terrain.ASTEROID_BELT, null);
		koit_system.addAsteroidBelt(dusk_koit_sun, 100, 6675, 256, 200, 300, Terrain.ASTEROID_BELT, null);

		koit_system.addRingBand(dusk_koit_sun, "misc", "rings_dust0", 256f, 0, Color.white, 256f, 5200, 100f);
		koit_system.addRingBand(dusk_koit_sun, "misc", "rings_dust0", 256f, 1, Color.white, 256f, 5400, 120f);
		koit_system.addRingBand(dusk_koit_sun, "misc", "rings_dust0", 256f, 2, Color.white, 256f, 5600, 150f);
		koit_system.addRingBand(dusk_koit_sun, "misc", "rings_dust0", 256f, 1, Color.white, 256f, 5800, 100f);

		// add one ring that covers all of the above
		SectorEntityToken ring = koit_system.addTerrain(Terrain.RING, new BaseRingTerrain.RingParams(600 + 256, 5500, null, "King's Men"));
		ring.setCircularOrbit(dusk_koit_sun, 0, 0, 120);

		koit_system.addRingBand(dusk_koit_sun, "misc", "rings_dust0", 256f, 0, Color.white, 256f, 6000, 100f);
		koit_system.addRingBand(dusk_koit_sun, "misc", "rings_dust0", 256f, 1, Color.white, 256f, 6100, 140f);
		koit_system.addRingBand(dusk_koit_sun, "misc", "rings_dust0", 256f, 2, Color.white, 256f, 6200, 180f);

		// add one ring that covers all of the above
		ring = koit_system.addTerrain(Terrain.RING, new BaseRingTerrain.RingParams(200 + 256, 6100, null, "King's Men"));
		ring.setCircularOrbit(dusk_koit_sun, 0, 0, 120);

		koit_system.addRingBand(dusk_koit_sun, "misc", "rings_dust0", 256f, 3, Color.white, 256f, 6300, 160f);
		koit_system.addRingBand(dusk_koit_sun, "misc", "rings_dust0", 256f, 2, Color.white, 256f, 6400, 200f);
		koit_system.addRingBand(dusk_koit_sun, "misc", "rings_dust0", 256f, 1, Color.white, 256f, 6500, 240f);

		koit_system.addRingBand(dusk_koit_sun, "misc", "rings_ice0", 256f, 0, Color.white, 256f, 6500, 120f);
		koit_system.addRingBand(dusk_koit_sun, "misc", "rings_ice0", 256f, 2, Color.white, 256f, 6600, 160f);
		koit_system.addRingBand(dusk_koit_sun, "misc", "rings_ice0", 256f, 1, Color.white, 256f, 6700, 180f);
		koit_system.addRingBand(dusk_koit_sun, "misc", "rings_ice0", 256f, 2, Color.white, 256f, 6800, 200f);

		// add one ring that covers all of the above
		ring = koit_system.addTerrain(Terrain.RING, new BaseRingTerrain.RingParams(300 + 256, 6650, null, "King's Men"));
		ring.setCircularOrbit(dusk_koit_sun, 0, 0, 120);


		apophis_system.addAsteroidBelt(dusk_apophis_sun, 800, 5200, 500, 250, 275);
		apophis_system.addAsteroidBelt(dusk_apophis_sun, 850, 6050, 500, 275, 300);
		apophis_system.addAsteroidBelt(apophis_planet_d, 125, 600, 400, 150, 200);
		apophis_system.addRingBand(dusk_apophis_sun, "misc", "rings_dust0", 256f, 2, Color.white, 256f, 5000, 300f, "ring", "Dust Ring");
		apophis_system.addRingBand(dusk_apophis_sun, "misc", "rings_asteroids0", 256f, 1, Color.white, 256f, 5200, 300f, "ring", "Dust Ring");
		apophis_system.addRingBand(dusk_apophis_sun, "misc", "rings_dust0", 256f, 1, Color.white, 256f, 5400, 300f, "ring", "Dust Ring");
		apophis_system.addRingBand(dusk_apophis_sun, "misc", "rings_dust0", 256f, 3, Color.white, 256f, 5850, 300f, "ring", "Dust Ring");
		apophis_system.addRingBand(dusk_apophis_sun, "misc", "rings_asteroids0", 256f, 4, Color.white, 256f, 6050, 300f, "ring", "Dust Ring");
		apophis_system.addRingBand(dusk_apophis_sun, "misc", "rings_dust0", 256f, 4, Color.white, 256f, 6250, 300f, "ring", "Dust Ring");

		JumpPointAPI jumpPoint2 = Global.getFactory().createJumpPoint("dusk_ijp", "Inner Jump Point");
		OrbitAPI orbit2 = Global.getFactory().createCircularOrbit(dusk_koit_sun, 90, 1850, 125);
		jumpPoint2.setOrbit(orbit2);
		jumpPoint2.setStandardWormholeToHyperspaceVisual();
		koit_system.addEntity(jumpPoint2);

		koit_system.autogenerateHyperspaceJumpPoints(true, true, false);
		apophis_system.autogenerateHyperspaceJumpPoints(true, true, true);

		clearHyperClouds(koit_system);
		clearHyperClouds(apophis_system);
	}

	private static void clearHyperClouds(StarSystemAPI system) {
		HyperspaceTerrainPlugin plugin = (HyperspaceTerrainPlugin) Misc.getHyperspaceTerrain().getPlugin();
		NebulaEditor editor = new NebulaEditor(plugin);
		float minRadius = plugin.getTileSize() * 2.25f;
		float radius = system.getMaxRadiusInHyperspace();
		editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius, 0, 360f); //begone evil clouds
		editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius, 0, 360f, 0.25f);
	}

	private static CampaignFleetAPI genStation(OrbitAPI orbit, StarSystemAPI system, MarketAPI market) {
		CampaignFleetAPI fleet = FleetFactoryV3.createEmptyFleet(CONTRACTOR_DRG_ID, FleetTypes.BATTLESTATION, market);

		FleetMemberAPI member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, "station3_hightech_Standard");
		fleet.getFleetData().addFleetMember(member);

		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_JUMP, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_ALLOW_DISENGAGE, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_FORCE_TRANSPONDER_OFF, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_IGNORED_BY_OTHER_FLEETS, true);
		//fleet.getMemoryWithoutUpdate().set("$dusk_blacksite_market", true);
		fleet.addTag(Tags.NEUTRINO_HIGH);
		PersonAPI commander = Global.getSector().getImportantPeople().getPerson(DRG_BLACKSITE_BS_LEAD);
		fleet.setCommander(commander);
		fleet.setStationMode(true);
		system.addEntity(fleet);
		fleet.clearAbilities();
		fleet.getDetectedRangeMod().modifyFlat("gen", 10f);
		fleet.setAI(null);
		fleet.setOrbit(orbit);
		fleet.getFlagship().setCaptain(commander);
		member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());

		return fleet;
	}
}
