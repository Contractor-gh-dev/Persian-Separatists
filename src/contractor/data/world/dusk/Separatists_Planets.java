package contractor.data.world.dusk;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.campaign.OrbitAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.*;
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.util.Misc;
import contractor.data.scripts.rulecmd.SepFenarPlanetMissionCMD;
import contractor.data.scripts.util.ContractorUtils;
import illustratedEntities.memory.ImageDataMemory;

import static contractor.data.scripts.util.ContractorStaticVars.*;

public class Separatists_Planets {

	public static void generate(SectorAPI sector) {
		boolean hasIllustrated = Global.getSettings().getModManager().isModEnabled("illustrated_entities");

		StarSystemAPI koit_system = sector.getStarSystem(KOIT_ID);
		StarSystemAPI apophis_system = sector.getStarSystem(APOPHIS_ID);
		StarSystemAPI lyncis_system = sector.createStarSystem(LYNCIS_ID);
		lyncis_system.setOptionalUniqueId(LYNCIS_ID);
		StarSystemAPI seraph_system = sector.createStarSystem(SERAPH_ID);
		seraph_system.setOptionalUniqueId(SERAPH_ID);
		lyncis_system.setBaseName("Lyncis");
		lyncis_system.getLocation().set(-4000, -24500);
		lyncis_system.setBackgroundTextureFilename("graphics/backgrounds/background1.jpg");
		lyncis_system.setAge(StarAge.AVERAGE);
		lyncis_system.setEnteredByPlayer(true);
		lyncis_system.addTag(Tags.THEME_CORE);
		seraph_system.setBaseName("Seraph");
		seraph_system.getLocation().set(-3600, -31000);
		seraph_system.setBackgroundTextureFilename("graphics/backgrounds/background4.jpg");
		seraph_system.setAge(StarAge.OLD);
		seraph_system.setEnteredByPlayer(false);
		Constellation dusk_constellation = ContractorUtils.quickConstellation(Constellation.ConstellationType.NORMAL, StarAge.AVERAGE, "Lynx");
		dusk_constellation.getSystems().add(koit_system);
		dusk_constellation.getSystems().add(lyncis_system);
		dusk_constellation.getSystems().add(seraph_system);
		dusk_constellation.getSystems().add(apophis_system);
		koit_system.setConstellation(dusk_constellation);
		lyncis_system.setConstellation(dusk_constellation);
		seraph_system.setConstellation(dusk_constellation);
		apophis_system.setConstellation(dusk_constellation);

		PlanetAPI dusk_koit_sun = koit_system.getStar();
		PlanetAPI dusk_lyncis_sun = lyncis_system.initStar("Lyncis", StarTypes.BLUE_GIANT, 1100f, 575f); // MAKE SUN
		PlanetAPI dusk_seraph_sun = seraph_system.initStar("Seraph", StarTypes.ORANGE, 650f, 450f);

		PlanetAPI separatists_planet = koit_system.addPlanet("separatists_planet", dusk_koit_sun, "Sedna", Planets.TUNDRA, 100, 110, 4500, 210);
		separatists_planet.getSpec().setTilt(20);
		separatists_planet.getSpec().setCloudColor(new Color(225, 223, 230, 150));
		separatists_planet.getSpec().setGlowColor(new Color(205, 215, 255, 155));
		separatists_planet.getSpec().setUseReverseLightForGlow(true);
		separatists_planet.setFaction(CONTRACTOR_SEP_ID);
		separatists_planet.setCustomDescriptionId("separatists_sedna_desc");
		separatists_planet.applySpecChanges();
		if (hasIllustrated)
			illustratedEntities.helper.ImageHandler.setImage(separatists_planet, ImageDataMemory.getInstance().get(907), false);

		PlanetAPI separatists_planet_b = koit_system.addPlanet("separatists_planet_b", dusk_koit_sun, "New Umbra", Planets.BARREN_DESERT, 0, 130, 2400, 150);
		separatists_planet_b.getSpec().setTilt(0);
		separatists_planet_b.setName("New Umbra");
		separatists_planet_b.getSpec().setCloudColor(new Color(90, 80, 65, 100));
		separatists_planet_b.getSpec().setGlowColor(new Color(100, 55, 25, 155));
		separatists_planet_b.getSpec().setUseReverseLightForGlow(true);
		separatists_planet_b.setFaction(CONTRACTOR_SEP_ID);
		separatists_planet_b.setCustomDescriptionId("separatists_umbra_desc");
		separatists_planet_b.applySpecChanges();
		if (hasIllustrated)
			illustratedEntities.helper.ImageHandler.setImage(separatists_planet_b, ImageDataMemory.getInstance().get(1102), false);

		PlanetAPI lyncis_planet_a = lyncis_system.addPlanet("lyncis_planet_a", dusk_lyncis_sun, "Iapidi", Planets.BARREN_CASTIRON, 170, 120, 2900, 175);
		lyncis_planet_a.getSpec().setTilt(50);
		lyncis_planet_a.getSpec().setGlowColor(new Color(120, 40, 250, 140));
		lyncis_planet_a.getSpec().setUseReverseLightForGlow(false);
		lyncis_planet_a.setFaction(CONTRACTOR_SEP_ID);
		lyncis_planet_a.setCustomDescriptionId("separatists_iapidi_desc");
		lyncis_planet_a.applySpecChanges();
		if (hasIllustrated)
			illustratedEntities.helper.ImageHandler.setImage(lyncis_planet_a, ImageDataMemory.getInstance().get(505), false);

		PlanetAPI lyncis_planet_b = lyncis_system.addPlanet("lyncis_planet_b", dusk_lyncis_sun, "Nushagak", Planets.PLANET_LAVA_MINOR, 100, 105, 4300, 225);
		lyncis_planet_b.getSpec().setTilt(0);
		lyncis_planet_b.getSpec().setGlowColor(Color.LIGHT_GRAY);
		lyncis_planet_b.getSpec().setUseReverseLightForGlow(true);
		lyncis_planet_b.applySpecChanges();

		PlanetAPI lyncis_planet_c = lyncis_system.addPlanet("lyncis_planet_c", dusk_lyncis_sun, "Diada", Planets.IRRADIATED, 290, 115, 5550, 295);
		lyncis_planet_c.getSpec().setTilt(10);
		lyncis_planet_c.getSpec().setGlowColor(Color.ORANGE);
		lyncis_planet_c.getSpec().setUseReverseLightForGlow(true);
		lyncis_planet_c.applySpecChanges();

		PlanetAPI seraph_planet_a = seraph_system.addPlanet("seraph_planet_a", dusk_seraph_sun, "Tehom", Planets.PLANET_WATER, 220, 110, 2850, 180);
		seraph_planet_a.getSpec().setTilt(0);
		seraph_planet_a.getSpec().setCloudColor(new Color(150, 150, 190, 155));
		seraph_planet_a.getSpec().setGlowColor(new Color(20, 100, 220, 155));
		seraph_planet_a.getSpec().setUseReverseLightForGlow(true);
		seraph_planet_a.setFaction(CONTRACTOR_SEP_ID);
		seraph_planet_a.setCustomDescriptionId("separatists_tehom_desc");
		seraph_planet_a.applySpecChanges();
		if (hasIllustrated)
			illustratedEntities.helper.ImageHandler.setImage(seraph_planet_a, ImageDataMemory.getInstance().get(1226), false);

		PlanetAPI seraph_planet_b = seraph_system.addPlanet("seraph_planet_b", dusk_seraph_sun, "Berouth", Planets.ICE_GIANT, 40, 250, 6500, 260);
		seraph_planet_b.getSpec().setTilt(5);
		seraph_planet_b.getSpec().setGlowColor(Color.BLUE);
		seraph_planet_b.getSpec().setUseReverseLightForGlow(true);
		seraph_planet_b.applySpecChanges();

		MarketAPI separatists_sedna_market = Global.getFactory().createMarket("separatists_sedna_market", separatists_planet.getName(), 5);
		separatists_sedna_market.setPrimaryEntity(separatists_planet);
		separatists_sedna_market.setFactionId(CONTRACTOR_SEP_ID);
		separatists_sedna_market.setName("Sedna");
		separatists_sedna_market.addCondition(Conditions.POPULATION_5);
		separatists_sedna_market.addCondition(Conditions.HABITABLE);
		separatists_sedna_market.addCondition(Conditions.COLD);
		separatists_sedna_market.addCondition(Conditions.EXTREME_WEATHER);
		separatists_sedna_market.addCondition(Conditions.ORE_MODERATE);
		separatists_sedna_market.addCondition(Conditions.ORGANICS_COMMON);
		separatists_sedna_market.addCondition(Conditions.FARMLAND_POOR);
		separatists_sedna_market.addIndustry(Industries.SPACEPORT);
		separatists_sedna_market.addIndustry(Industries.MINING);
		separatists_sedna_market.addIndustry(Industries.FARMING);
		separatists_sedna_market.addIndustry(Industries.WAYSTATION);
		separatists_sedna_market.addIndustry(Industries.POPULATION);
		separatists_sedna_market.addIndustry(Industries.LIGHTINDUSTRY);
		separatists_sedna_market.addIndustry(Industries.HEAVYBATTERIES);
		separatists_sedna_market.addIndustry("sep_orbitalstation_medium");
		separatists_sedna_market.addIndustry(Industries.PATROLHQ);
		separatists_sedna_market.addSubmarket(Submarkets.SUBMARKET_OPEN);
		separatists_sedna_market.addSubmarket(Submarkets.SUBMARKET_BLACK);
		separatists_sedna_market.addSubmarket(Submarkets.SUBMARKET_STORAGE);
		separatists_sedna_market.addSubmarket(Submarkets.GENERIC_MILITARY);
		separatists_sedna_market.getTariff().setBaseValue(0.25f);
		Misc.makeStoryCritical(separatists_sedna_market, SepFenarPlanetMissionCMD.id);

		MarketAPI separatists_new_umbra_market = Global.getFactory().createMarket("separatists_new_umbra_market", separatists_planet_b.getName(), 4);
		separatists_new_umbra_market.setPrimaryEntity(separatists_planet_b);
		separatists_new_umbra_market.setFactionId(CONTRACTOR_SEP_ID);
		separatists_new_umbra_market.setName("New Umbra");
		separatists_new_umbra_market.addCondition(Conditions.POPULATION_4);
		separatists_new_umbra_market.addCondition(Conditions.VERY_HOT);
		separatists_new_umbra_market.addCondition(Conditions.DESERT);
		separatists_new_umbra_market.addCondition(Conditions.ORE_ABUNDANT);
		separatists_new_umbra_market.addCondition(Conditions.RARE_ORE_SPARSE);
		separatists_new_umbra_market.addCondition(Conditions.RURAL_POLITY);
		separatists_new_umbra_market.addCondition(Conditions.THIN_ATMOSPHERE);
		separatists_new_umbra_market.addIndustry(Industries.SPACEPORT);
		separatists_new_umbra_market.addIndustry(Industries.REFINING);
		separatists_new_umbra_market.addIndustry(Industries.POPULATION);
		separatists_new_umbra_market.addIndustry(Industries.HEAVYBATTERIES);
		separatists_new_umbra_market.addIndustry(Industries.MILITARYBASE);
		separatists_new_umbra_market.addIndustry("sep_orbitalstation_small");
		separatists_new_umbra_market.addSubmarket(Submarkets.SUBMARKET_OPEN);
		separatists_new_umbra_market.addSubmarket(Submarkets.SUBMARKET_BLACK);
		separatists_new_umbra_market.addSubmarket(Submarkets.SUBMARKET_STORAGE);
		separatists_new_umbra_market.addSubmarket(Submarkets.GENERIC_MILITARY);
		separatists_new_umbra_market.getTariff().setBaseValue(0.25f);

		MarketAPI separatists_iapidi_market = Global.getFactory().createMarket("separatists_iapidi_market", lyncis_planet_a.getName(), 4);
		separatists_iapidi_market.setPrimaryEntity(lyncis_planet_a);
		separatists_iapidi_market.setFactionId(CONTRACTOR_SEP_ID);
		separatists_iapidi_market.setName("Iapidi");
		separatists_iapidi_market.addCondition(Conditions.POPULATION_4);
		separatists_iapidi_market.addCondition(Conditions.NO_ATMOSPHERE);
		separatists_iapidi_market.addCondition(Conditions.VERY_HOT);
		separatists_iapidi_market.addCondition(Conditions.LOW_GRAVITY);
		separatists_iapidi_market.addCondition(Conditions.ORE_RICH);
		separatists_iapidi_market.addCondition(Conditions.RARE_ORE_RICH);
		separatists_iapidi_market.addCondition(Conditions.RURAL_POLITY);
		separatists_iapidi_market.addIndustry(Industries.MINING);
		separatists_iapidi_market.addIndustry(Industries.SPACEPORT);
		separatists_iapidi_market.addIndustry("sep_orbitalstation_small");
		separatists_iapidi_market.addIndustry(Industries.POPULATION);
		separatists_iapidi_market.addIndustry(Industries.HEAVYBATTERIES);
		separatists_iapidi_market.addIndustry(Industries.ORBITALWORKS, new ArrayList<>(List.of(Items.CORRUPTED_NANOFORGE)));
		separatists_iapidi_market.addIndustry(Industries.PATROLHQ);
		separatists_iapidi_market.addSubmarket(Submarkets.SUBMARKET_OPEN);
		separatists_iapidi_market.addSubmarket(Submarkets.SUBMARKET_BLACK);
		separatists_iapidi_market.addSubmarket(Submarkets.SUBMARKET_STORAGE);
		separatists_iapidi_market.addSubmarket(Submarkets.GENERIC_MILITARY);
		separatists_iapidi_market.getTariff().setBaseValue(0.25f);

		MarketAPI separatists_tehom_market = Global.getFactory().createMarket("separatists_tehom_market", seraph_planet_a.getName(), 3);
		separatists_tehom_market.setPrimaryEntity(seraph_planet_a);
		separatists_tehom_market.setFactionId(CONTRACTOR_SEP_ID);
		separatists_tehom_market.setName("Tehom");
		separatists_tehom_market.addCondition(Conditions.POPULATION_3);
		separatists_tehom_market.addCondition(Conditions.HABITABLE);
		separatists_tehom_market.addCondition(Conditions.WATER_SURFACE);
		separatists_tehom_market.addCondition(Conditions.OUTPOST);
		separatists_tehom_market.addCondition(Conditions.RURAL_POLITY);
		separatists_tehom_market.addCondition(Conditions.MILD_CLIMATE);
		separatists_tehom_market.addCondition(Conditions.CLOSED_IMMIGRATION);
		separatists_tehom_market.addCondition(Conditions.ORE_SPARSE);
		separatists_tehom_market.addCondition(Conditions.FARMLAND_RICH);
		separatists_tehom_market.addCondition(Conditions.ORGANICS_ABUNDANT);
		separatists_tehom_market.addCondition(Conditions.RUINS_WIDESPREAD);
		separatists_tehom_market.addIndustry("duskworksite");
		separatists_tehom_market.addIndustry("duskstardock");
		separatists_tehom_market.addIndustry("sep_orbitalstation_small");
		separatists_tehom_market.addIndustry(Industries.TECHMINING);
		separatists_tehom_market.addSubmarket(Submarkets.SUBMARKET_OPEN);
		separatists_tehom_market.getTariff().setBaseValue(0.25f);

		lyncis_planet_b.getMarket().setPrimaryEntity(lyncis_planet_b);
		lyncis_planet_b.getMarket().setFactionId(Factions.NEUTRAL);
		lyncis_planet_b.getMarket().setPlanetConditionMarketOnly(true);
		lyncis_planet_b.getMarket().addCondition(Conditions.THIN_ATMOSPHERE);
		lyncis_planet_b.getMarket().addCondition(Conditions.HOT);
		lyncis_planet_b.getMarket().addCondition(Conditions.HIGH_GRAVITY);
		lyncis_planet_b.getMarket().addCondition(Conditions.RARE_ORE_RICH);
		lyncis_planet_b.getMarket().addCondition(Conditions.ORE_RICH);

		lyncis_planet_c.getMarket().setPrimaryEntity(lyncis_planet_c);
		lyncis_planet_c.getMarket().setFactionId(Factions.NEUTRAL);
		lyncis_planet_c.getMarket().setPlanetConditionMarketOnly(true);
		lyncis_planet_c.getMarket().addCondition(Conditions.IRRADIATED);
		lyncis_planet_c.getMarket().addCondition(Conditions.COLD);
		lyncis_planet_c.getMarket().addCondition(Conditions.POOR_LIGHT);
		lyncis_planet_c.getMarket().addCondition(Conditions.RUINS_SCATTERED);
		lyncis_planet_c.getMarket().addCondition(Conditions.ORE_ABUNDANT);

		separatists_sedna_market.setSurveyLevel(MarketAPI.SurveyLevel.FULL);
		separatists_new_umbra_market.setSurveyLevel(MarketAPI.SurveyLevel.FULL);
		separatists_iapidi_market.setSurveyLevel(MarketAPI.SurveyLevel.FULL);
		separatists_tehom_market.setSurveyLevel(MarketAPI.SurveyLevel.FULL);

		separatists_planet.setMarket(separatists_sedna_market);
		separatists_planet_b.setMarket(separatists_new_umbra_market);
		lyncis_planet_a.setMarket(separatists_iapidi_market);
		seraph_planet_a.setMarket(separatists_tehom_market);

		sector.getEconomy().addMarket(separatists_sedna_market, false);
		sector.getEconomy().addMarket(separatists_new_umbra_market, false);
		sector.getEconomy().addMarket(separatists_iapidi_market, false);
		sector.getEconomy().addMarket(separatists_tehom_market, false);

		addPostEconMarketConditions(separatists_sedna_market, SEP_CONDITION_FRONTIER);
		addPostEconMarketConditions(separatists_new_umbra_market, SEP_CONDITION_FRONTIER);
		addPostEconMarketConditions(separatists_iapidi_market, SEP_CONDITION_FRONTIER);
		addPostEconMarketConditions(separatists_tehom_market, SEP_CONDITION_FRONTIER);

		SectorEntityToken separatists_relay = koit_system.addCustomEntity("separatists_relay1", "Sedna Relay", "comm_relay_makeshift", CONTRACTOR_SEP_ID);
		separatists_relay.setCircularOrbit(dusk_koit_sun, 150, 4100, 210);

		SectorEntityToken separatists_relay2 = seraph_system.addCustomEntity("separatists_relay2", "Tehom Relay", "comm_relay_makeshift", CONTRACTOR_SEP_ID);
		separatists_relay2.setCircularOrbit(dusk_seraph_sun, 110, 2850, 200);

		SectorEntityToken separatists_relay3 = lyncis_system.addCustomEntity("separatists_relay3", "Lyncis Relay", "comm_relay_makeshift", CONTRACTOR_SEP_ID);
		separatists_relay3.setCircularOrbit(dusk_lyncis_sun, 200, 3650, 210);

		//SectorEntityToken stableloc1 = lyncis_system.addCustomEntity(null, null, "stable_location", Factions.NEUTRAL);
		//stableloc1.setCircularOrbitPointingDown(dusk_lyncis_sun, 200, 3650, 210f);
		SectorEntityToken stableloc2 = lyncis_system.addCustomEntity(null, null, "stable_location", Factions.NEUTRAL);
		stableloc2.setCircularOrbitPointingDown(dusk_lyncis_sun, 40, 5650, 305f);
		//SectorEntityToken stableloc3 = seraph_system.addCustomEntity(null, null, "stable_location", Factions.NEUTRAL);
		//stableloc3.setCircularOrbitPointingDown(dusk_seraph_sun, 110, 2850, 200f);

		seraph_system.addRingBand(seraph_planet_b, "misc", "rings_ice0", 256f, 1, Color.white, 128f, 400, 160f, "ring", "Ice Ring");
		seraph_system.addRingBand(dusk_seraph_sun, "misc", "rings_ice0", 256f, 3, Color.white, 256f, 4500, 300f, "ring", "Ice Ring");
		seraph_system.addRingBand(dusk_seraph_sun, "misc", "rings_ice0", 256f, 5, Color.white, 256f, 4750, 300f, "ring", "Ice Ring");
		seraph_system.addRingBand(dusk_seraph_sun, "misc", "rings_ice0", 256f, 4, Color.white, 256f, 5000, 300f, "ring", "Ice Ring");

		SectorEntityToken sep_plan_b_mag = koit_system.addTerrain(Terrain.MAGNETIC_FIELD, new MagneticFieldTerrainPlugin.MagneticFieldParams(256f, 275f, separatists_planet_b, 150f, 400f, new Color(120, 40, 250, 140), 0.25f)); //works sortof
		SectorEntityToken lyncis_plan_a_mag = lyncis_system.addTerrain(Terrain.MAGNETIC_FIELD, new MagneticFieldTerrainPlugin.MagneticFieldParams(256f, 275f, lyncis_planet_a, 150f, 400f, new Color(120, 40, 250, 140), 10f));
		sep_plan_b_mag.setCircularOrbit(separatists_planet_b, 0, 0, 100);
		lyncis_plan_a_mag.setCircularOrbit(lyncis_planet_a, 0, 0, 100);

		JumpPointAPI jumpPoint = Global.getFactory().createJumpPoint("lyncis_ijp", "Inner Jump Point");
		OrbitAPI orbit = Global.getFactory().createCircularOrbit(dusk_lyncis_sun, 0, 1850, 100);
		jumpPoint.setOrbit(orbit);
		jumpPoint.setStandardWormholeToHyperspaceVisual();
		lyncis_system.addEntity(jumpPoint);

		JumpPointAPI jumpPoint2 = Global.getFactory().createJumpPoint("Seraph_fjp", "Fringe Jump Point");
		OrbitAPI orbit2 = Global.getFactory().createCircularOrbit(dusk_seraph_sun, 0, 7250, 200);
		jumpPoint2.setOrbit(orbit2);
		jumpPoint2.setStandardWormholeToHyperspaceVisual();
		seraph_system.addEntity(jumpPoint2);


		lyncis_system.autogenerateHyperspaceJumpPoints(true, false, false);
		seraph_system.autogenerateHyperspaceJumpPoints(true, false, true);

		clearHyperClouds(lyncis_system);
		clearHyperClouds(seraph_system);
	}

	private static void addPostEconMarketConditions(MarketAPI market, String condition) {
		market.addCondition(condition);
	}

	private static void clearHyperClouds(StarSystemAPI system) {
		HyperspaceTerrainPlugin plugin = (HyperspaceTerrainPlugin) Misc.getHyperspaceTerrain().getPlugin();
		NebulaEditor editor = new NebulaEditor(plugin);
		float minRadius = plugin.getTileSize() * 2.25f;  //clean radius multi
		float radius = system.getMaxRadiusInHyperspace();
		editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius, 0, 360f); //begone evil clouds
		editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius, 0, 360f, 0.25f);
	}
}
