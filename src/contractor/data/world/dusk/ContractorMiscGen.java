package contractor.data.world.dusk;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.*;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.MiscellaneousThemeGenerator;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import contractor.data.scripts.util.ContractorStaticVars;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import static com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.addOrbitingEntities;
import static com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.computeSystemData;
import static contractor.data.scripts.campaign.fleets.DisposableNaniteFleetManager.DIST_2;
import static contractor.data.scripts.util.ContractorStaticVars.*;

public class ContractorMiscGen implements SectorGeneratorPlugin {
	public void generate(SectorAPI sector) {
		int i = 0;
		Vector2f location;
		boolean checkPass = false;
		do {
			boolean distPass = true;
			location = MathUtils.getRandomPointInCircle(new Vector2f(31000, -42000), 2500f);
			final List<StarSystemAPI> allSys = Global.getSector().getStarSystems();
			for (StarSystemAPI sys : allSys) {
				if (MathUtils.isWithinRange(location, sys.getLocation(), 1500f)) {
					distPass = false;
					break;
				}
			}
			checkPass = distPass;
			i++;
		} while (i < 200 && !checkPass);

		if (i == 200) {
			location = new Vector2f(31000, -42000);
			if (Global.getSettings().isDevMode())
				Global.getLogger(ContractorMiscGen.class).warn("Failed to find unoccupied location for Verdan system, falling back to default!");
		}

		StarSystemGenerator.CustomConstellationParams params = new StarSystemGenerator.CustomConstellationParams(StarAge.AVERAGE);
		params.location = new Vector2f(27500, -40000);
		params.maxStars = 4;
		params.minStars = 3;
		params.name = "Junius";
		params.systemTypes.add(StarSystemGenerator.StarSystemType.SINGLE);
		params.systemTypes.add(StarSystemGenerator.StarSystemType.BINARY_FAR);
		StarSystemGenerator generator = new StarSystemGenerator(params);
		Constellation constellation = generator.generate();


		StarSystemAPI system = sector.createStarSystem("Verdan");
		system.setAge(StarAge.YOUNG);
		system.getLocation().set(location.x, location.y);
		system.setEnteredByPlayer(false);
		system.setBaseName("Verdan");
		system.setBackgroundTextureFilename("graphics/backgrounds/background2.jpg");
		system.addTag(Tags.THEME_SPECIAL);
		system.addTag(CONTRACTOR_NANITE_NO_SPAWN);
		system.addTag(Tags.DO_NOT_RESPAWN_PLAYER_IN);
		system.setConstellation(constellation);
		constellation.getSystems().add(system);

		PlanetAPI star = system.initStar("separatists_verdan", "star_bluegreen_giant", 1000f, 475f);
		star.setName("Verdan");

		SectorEntityToken stableloc1 = system.addCustomEntity(null, null, "stable_location", Factions.NEUTRAL);
		stableloc1.setCircularOrbitPointingDown(star, 200, 3250, 180f);
		SectorEntityToken stableloc2 = system.addCustomEntity(null, null, "stable_location", Factions.NEUTRAL);
		stableloc2.setCircularOrbitPointingDown(star, 40, 5350, 260f);

		JumpPointAPI jumpPoint = Global.getFactory().createJumpPoint("verdan_ijp", "Inner Jump Point");
		OrbitAPI orbit = Global.getFactory().createCircularOrbit(star, 0, 1850, 100);
		jumpPoint.setOrbit(orbit);
		jumpPoint.setStandardWormholeToHyperspaceVisual();
		system.addEntity(jumpPoint);

		system.addTerrain(Terrain.MAGNETIC_FIELD, new MagneticFieldTerrainPlugin.MagneticFieldParams(128f, 2000f, star, 1900f, 2100f, new Color(120, 40, 250, 140), 0.25f, Color.green));

		addOrbitingEntities(system, star, star.getStarSystem().getAge(), 4, 6, 2500f, 0, true, false);

		List<PlanetAPI> list = system.getPlanets();
		List<SectorEntityToken> toRemove = new ArrayList<>();

		for (SectorEntityToken token : list) {
			if (token.getCircularOrbitRadius() > 8000f && token.getCircularOrbitRadius() < 9500f) {
				List<SectorEntityToken> listAll = system.getAllEntities();
				for (SectorEntityToken tokenAll : listAll) {
					if (tokenAll.getOrbitFocus() == token)
						toRemove.add(tokenAll);
				}
				toRemove.add(token);
			}
		}

		for (SectorEntityToken token : toRemove)
			system.removeEntity(token);
		toRemove.clear();

		PlanetAPI planet_one = system.addPlanet("separatists_verdan_planet_dawn", star, "Dawn", Planets.TUNDRA, 165, 115, 8900, 300);
		planet_one.getSpec().setTilt(5);
		planet_one.getSpec().setAtmosphereThickness(0.5f);
		planet_one.getSpec().setCloudColor(new Color(180, 220, 255, 200));
		planet_one.getSpec().setGlowColor(new Color(200, 225, 255, 200));
		planet_one.getSpec().setUseReverseLightForGlow(true);
		planet_one.applySpecChanges();

		planet_one.getMarket().setPrimaryEntity(planet_one);
		planet_one.getMarket().setPlanetConditionMarketOnly(true);
		planet_one.getMarket().setFactionId(Factions.NEUTRAL);
		planet_one.getMarket().setSurveyLevel(MarketAPI.SurveyLevel.NONE);
		planet_one.getMarket().addCondition(Conditions.COLD);
		planet_one.getMarket().addCondition(Conditions.HABITABLE);
		planet_one.getMarket().addCondition(Conditions.MILD_CLIMATE);
		planet_one.getMarket().addCondition(ContractorStaticVars.SEP_CONDITION_GEOTHERMAL);
		planet_one.getMarket().addCondition(Conditions.ORE_SPARSE);
		planet_one.getMarket().addCondition(Conditions.FARMLAND_BOUNTIFUL);
		planet_one.getMarket().addCondition(Conditions.ORGANICS_ABUNDANT);

		PlanetAPI planet_two = system.addPlanet("separatists_verdan_planet_dawn_moon", planet_one, "Sunrise", Planets.BARREN, 105, 60, 725, 120);
		planet_two.getSpec().setTilt(31);
		planet_two.getSpec().setAtmosphereThickness(0f);
		planet_two.getSpec().setUseReverseLightForGlow(false);
		planet_two.applySpecChanges();

		planet_two.getMarket().setPrimaryEntity(planet_two);
		planet_two.getMarket().setPlanetConditionMarketOnly(true);
		planet_two.getMarket().setFactionId(Factions.NEUTRAL);
		planet_two.getMarket().setSurveyLevel(MarketAPI.SurveyLevel.NONE);
		planet_two.getMarket().addCondition(Conditions.NO_ATMOSPHERE);
		planet_two.getMarket().addCondition(Conditions.LOW_GRAVITY);
		planet_two.getMarket().addCondition(Conditions.ORE_ABUNDANT);
		planet_two.getMarket().addCondition(Conditions.RARE_ORE_MODERATE);

		if (FENARANS_ENABLED) { //guaranteed station for event
			MiscellaneousThemeGenerator gen = new MiscellaneousThemeGenerator();
			BaseThemeGenerator.StarSystemData data = computeSystemData(system);
			WeightedRandomPicker<String> picker = new WeightedRandomPicker<>();
			picker.add(Entities.STATION_RESEARCH);
			gen.addResearchStations(data, 1f, 1, 1, picker);

			for (SectorEntityToken ent : system.getAllEntities()) {
				if (Objects.equals(ent.getCustomEntityType(), Entities.STATION_RESEARCH))
					ent.getMemoryWithoutUpdate().set("$con_fenar_station", true);
			}
		}

		system.autogenerateHyperspaceJumpPoints(true, true, true);
		clearHyperClouds(system);

		addSpeedwell(planet_one);
		initNaniteFaction(sector);
	}

	private void clearHyperClouds(StarSystemAPI system) {
		HyperspaceTerrainPlugin plugin = (HyperspaceTerrainPlugin) Misc.getHyperspaceTerrain().getPlugin();
		NebulaEditor editor = new NebulaEditor(plugin);
		float minRadius = plugin.getTileSize() * 2.25f;
		float radius = system.getMaxRadiusInHyperspace();
		editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius, 0, 360f);
		editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius, 0, 360f, 0.25f);
	}

	protected void addSpeedwell(PlanetAPI planet) {
		ShipRecoverySpecial.PerShipData ship = new ShipRecoverySpecial.PerShipData("sep_speedwell2_exp", ShipRecoverySpecial.ShipCondition.PRISTINE, 0f);
		ship.shipName = "Speedwell";
		DerelictShipEntityPlugin.DerelictShipData params = new DerelictShipEntityPlugin.DerelictShipData(ship, false);
		CustomCampaignEntityAPI entity = (CustomCampaignEntityAPI) BaseThemeGenerator.addSalvageEntity(planet.getContainingLocation(), Entities.WRECK, Factions.NEUTRAL, params);
		entity.getMemoryWithoutUpdate().set("$speedwellWreck", true);
		entity.setSensorProfile(1f);
		entity.setDiscoverable(true);

		Random random = StarSystemGenerator.random;
		float orbitRadius = planet.getRadius() + 200f;
		float orbitDays = orbitRadius / (10f + random.nextFloat() * 5f);
		entity.setCircularOrbit(planet, random.nextFloat() * 360f, orbitRadius, orbitDays);

		DerelictShipEntityPlugin dsep = (DerelictShipEntityPlugin) entity.getCustomPlugin();
		ShipRecoverySpecial.PerShipData copy = dsep.getData().ship.clone();
		copy.variant = Global.getSettings().getVariant(copy.variantId).clone();
		copy.variantId = null;
		copy.variant.addTag(Tags.SHIP_CAN_NOT_SCUTTLE);
		copy.nameAlwaysKnown = true;
		copy.pruneWeapons = false;

		ShipRecoverySpecial.ShipRecoverySpecialData data = new ShipRecoverySpecial.ShipRecoverySpecialData(null);
		data.notNowOptionExits = true;
		data.noDescriptionText = true;
		data.addShip(copy);

		Misc.setSalvageSpecial(entity, data);
	}

	protected void initNaniteFaction(SectorAPI sector) {
		FactionAPI nanites = sector.getFaction(CONTRACTOR_NANITE_ID);
		List<FactionAPI> allFactions = sector.getAllFactions();
		for (FactionAPI faction : allFactions) {
			if (faction == nanites)
				continue;
			nanites.setRelationship(faction.getId(), RepLevel.HOSTILE);
		}
	}

	public static void initNaniteFactionPost(SectorAPI sector) {
		List<StarSystemAPI> systems = sector.getStarSystems();
//        if (Global.getSector().getPersistentData().get(CONTRACTOR_NANITE_HOME_SYSTEM) != null) { //todo: remove this debug code for final update
//            StarSystemAPI home = (StarSystemAPI) Global.getSector().getPersistentData().get(CONTRACTOR_NANITE_HOME_SYSTEM);
//            for (StarSystemAPI system : systems) {
//                if (system.hasTag(Tags.SYSTEM_ALREADY_USED_FOR_STORY)
//                        || system.hasTag(Tags.THEME_CORE)
//                        || system.hasTag(Tags.THEME_REMNANT)
//                        || system.hasTag(Tags.THEME_SPECIAL)
//                        || system.hasTag(Tags.SYSTEM_ABYSSAL)
//                        || system.hasTag(CONTRACTOR_NANITE_NO_SPAWN))
//                    continue;
//                float dist = Misc.getDistanceLY(home.getLocation(), system.getLocation());
//                if (dist < DIST_2 + 5f) {
//                    if (!system.hasTag(CONTRACTOR_NANITE_ID))
//                        system.addTag(CONTRACTOR_NANITE_ID);
//                }
//            }
//            return;
//        }

		if (Global.getSector().getPersistentData().get(CONTRACTOR_NANITE_HOME_SYSTEM) != null)
			return;

		WeightedRandomPicker<StarSystemAPI> picker = new WeightedRandomPicker<>();
		for (StarSystemAPI system : systems) {
			if (system.hasTag(Tags.SYSTEM_ALREADY_USED_FOR_STORY)
					|| system.hasTag(Tags.THEME_CORE)
					|| system.hasTag(Tags.THEME_REMNANT)
					|| system.hasTag(Tags.THEME_SPECIAL)
					|| system.hasTag(Tags.SYSTEM_ABYSSAL)
					|| system.hasTag(CONTRACTOR_NANITE_NO_SPAWN))
				continue;
			picker.add(system, (float) Math.log(Misc.getDistanceLY(system.getLocation(), Global.getSector().getStarSystem(KOIT_ID).getLocation())));
		}
		StarSystemAPI picked = picker.pick();
		if (picked == null) {
			Global.getLogger(ContractorMiscGen.class).warn("FAILED TO SELECT NANITE HOME SYSTEM");
			return;
		} else if (Global.getSettings().isDevMode())
			Global.getLogger(ContractorMiscGen.class).info("NANITE HOME SYSTEM SET: " + picked.getBaseName());
		picked.addTag(CONTRACTOR_NANITE_HOME_SYSTEM);
		Global.getSector().getPersistentData().put(CONTRACTOR_NANITE_HOME_SYSTEM, picked);

		for (StarSystemAPI system : systems) {
			if (system.hasTag(Tags.SYSTEM_ALREADY_USED_FOR_STORY)
					|| system.hasTag(Tags.THEME_CORE)
					|| system.hasTag(Tags.THEME_REMNANT)
					|| system.hasTag(Tags.THEME_SPECIAL)
					|| system.hasTag(Tags.SYSTEM_ABYSSAL)
					|| system.hasTag(CONTRACTOR_NANITE_NO_SPAWN))
				continue;
			float dist = Misc.getDistanceLY(picked.getLocation(), system.getLocation());
			if (dist < DIST_2 + 5f) {
				system.addTag(CONTRACTOR_NANITE_ID);
			}
		}
	}
}
