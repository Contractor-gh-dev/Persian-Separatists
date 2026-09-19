package contractor.data.world.dusk;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.impl.MusicPlayerPluginImpl;
import com.fs.starfarer.api.impl.campaign.CoreLifecyclePluginImpl;
import com.fs.starfarer.api.impl.campaign.WarningBeaconEntityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;

import static com.fs.starfarer.api.impl.campaign.procgen.themes.MiscellaneousThemeGenerator.makeDiscoverable;
import static contractor.data.scripts.util.ContractorStaticVars.*;

public class FenaranGen implements SectorGeneratorPlugin {
	public static final Color ABYSS_AMBIENT_LIGHT_COLOR = new Color(100, 100, 100, 255);
	public static final String NASCENT_WELL_KEY = "$PlanetFenar_well";

	@Override
	public void generate(SectorAPI sector) {
		genSystem(sector);
		initFactionRelationships(sector);
		Global.getSector().getMemoryWithoutUpdate().set("$FenaransGened", true);
	}

	public static void initFactionRelationships(SectorAPI sector) {
		FactionAPI contractor_fenarans = sector.getFaction(CONTRACTOR_FENARANS_ID);
		FactionAPI player = sector.getFaction(Factions.PLAYER);
		FactionAPI hegemony = sector.getFaction(Factions.HEGEMONY);
		FactionAPI tritachyon = sector.getFaction(Factions.TRITACHYON);
		FactionAPI pirates = sector.getFaction(Factions.PIRATES);
		FactionAPI independent = sector.getFaction(Factions.INDEPENDENT);
		FactionAPI church = sector.getFaction(Factions.LUDDIC_CHURCH);
		FactionAPI path = sector.getFaction(Factions.LUDDIC_PATH);
		FactionAPI kol = sector.getFaction(Factions.KOL);
		FactionAPI diktat = sector.getFaction(Factions.DIKTAT);
		FactionAPI persean = sector.getFaction(Factions.PERSEAN);
		FactionAPI guard = sector.getFaction(Factions.LIONS_GUARD);
		FactionAPI remnants = sector.getFaction(Factions.REMNANTS);
		FactionAPI derelicts = sector.getFaction(Factions.DERELICT);

		contractor_fenarans.setRelationship(player.getId(), RepLevel.SUSPICIOUS);
		contractor_fenarans.setRelationship(hegemony.getId(), RepLevel.INHOSPITABLE);
		contractor_fenarans.setRelationship(tritachyon.getId(), RepLevel.SUSPICIOUS);
		contractor_fenarans.setRelationship(pirates.getId(), RepLevel.HOSTILE);
		contractor_fenarans.setRelationship(independent.getId(), RepLevel.SUSPICIOUS);
		contractor_fenarans.setRelationship(persean.getId(), RepLevel.SUSPICIOUS);
		contractor_fenarans.setRelationship(church.getId(), RepLevel.HOSTILE);
		contractor_fenarans.setRelationship(path.getId(), RepLevel.VENGEFUL);
		contractor_fenarans.setRelationship(kol.getId(), RepLevel.VENGEFUL);
		contractor_fenarans.setRelationship(diktat.getId(), RepLevel.HOSTILE);
		contractor_fenarans.setRelationship(guard.getId(), RepLevel.HOSTILE);
		contractor_fenarans.setRelationship(remnants.getId(), RepLevel.HOSTILE);
		contractor_fenarans.setRelationship(derelicts.getId(), RepLevel.HOSTILE);

		//modded factions
		contractor_fenarans.setRelationship(CONTRACTOR_SEP_ID, RepLevel.WELCOMING);
		contractor_fenarans.setRelationship(CONTRACTOR_DRG_ID, RepLevel.FAVORABLE);

		contractor_fenarans.setRelationship("SCY", RepLevel.SUSPICIOUS);
		contractor_fenarans.setRelationship("shadow_industry", RepLevel.SUSPICIOUS);
		contractor_fenarans.setRelationship("syndicate_asp", RepLevel.SUSPICIOUS);

		contractor_fenarans.setRelationship("citadeldefenders", RepLevel.SUSPICIOUS);
		contractor_fenarans.setRelationship("tiandong", RepLevel.SUSPICIOUS);
		contractor_fenarans.setRelationship("metelson", RepLevel.SUSPICIOUS);
		contractor_fenarans.setRelationship("Coalition", RepLevel.NEUTRAL);

		contractor_fenarans.setRelationship("sun_ice", RepLevel.SUSPICIOUS);
		contractor_fenarans.setRelationship("pn_colony", RepLevel.SUSPICIOUS);
		contractor_fenarans.setRelationship("neutrinocorp", RepLevel.SUSPICIOUS);
		contractor_fenarans.setRelationship("blackrock_driveyards", RepLevel.SUSPICIOUS);

		contractor_fenarans.setRelationship("dassault_mikoyan", RepLevel.SUSPICIOUS);
		contractor_fenarans.setRelationship("interstellarimperium", RepLevel.INHOSPITABLE);
		contractor_fenarans.setRelationship("apex_design", RepLevel.SUSPICIOUS);

		contractor_fenarans.setRelationship("pack", RepLevel.INHOSPITABLE);
		contractor_fenarans.setRelationship("6eme_bureau", RepLevel.INHOSPITABLE);

		contractor_fenarans.setRelationship("diableavionics", RepLevel.HOSTILE);
		contractor_fenarans.setRelationship("maystar_federationte", RepLevel.HOSTILE);
		contractor_fenarans.setRelationship("pirateAnar", RepLevel.HOSTILE);
		contractor_fenarans.setRelationship("sun_ici", RepLevel.HOSTILE);
		contractor_fenarans.setRelationship("junk_pirates", RepLevel.HOSTILE);
		contractor_fenarans.setRelationship("exigency", RepLevel.HOSTILE);
		contractor_fenarans.setRelationship("exipirated", RepLevel.HOSTILE);
		contractor_fenarans.setRelationship("cabal", RepLevel.HOSTILE);
		contractor_fenarans.setRelationship("the_deserter", RepLevel.HOSTILE);
		contractor_fenarans.setRelationship("blade_breakers", RepLevel.HOSTILE);

		contractor_fenarans.setRelationship("crystanite", RepLevel.VENGEFUL);
		contractor_fenarans.setRelationship("new_galactic_order", RepLevel.VENGEFUL);
		contractor_fenarans.setRelationship("explorer_society", RepLevel.VENGEFUL);

		contractor_fenarans.setRelationship("noir", RepLevel.SUSPICIOUS);
		contractor_fenarans.setRelationship("Lte", RepLevel.SUSPICIOUS);
		contractor_fenarans.setRelationship("GKSec", RepLevel.SUSPICIOUS);
		contractor_fenarans.setRelationship("gmda", RepLevel.SUSPICIOUS);
		contractor_fenarans.setRelationship("oculus", RepLevel.SUSPICIOUS);
		contractor_fenarans.setRelationship("nomads", RepLevel.SUSPICIOUS);
		contractor_fenarans.setRelationship("thulelegacy", RepLevel.SUSPICIOUS);
		contractor_fenarans.setRelationship("infected", RepLevel.SUSPICIOUS);
		contractor_fenarans.setRelationship("star_federation", RepLevel.NEUTRAL);
	}

	private static void genSystem(SectorAPI sector) {
		StarSystemAPI system = sector.createStarSystem("Fenar");
		system.setAge(StarAge.OLD);
		system.getLocation().set(40000, -45000);
		system.setEnteredByPlayer(false);
		system.setName("Fenar");
		system.setBackgroundTextureFilename("graphics/backgrounds/background4dim.jpg");
		system.setType(StarSystemGenerator.StarSystemType.DEEP_SPACE);
		system.addTag(Tags.THEME_HIDDEN);
		system.addTag(Tags.THEME_SPECIAL);
		system.addTag(Tags.DO_NOT_RESPAWN_PLAYER_IN);
		system.addTag(Tags.SYSTEM_ALREADY_USED_FOR_STORY);
		system.getMemoryWithoutUpdate().set(MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY, "music_campaign_abyssal");

		SectorEntityToken center = system.initNonStarCenter();

		system.setLightColor(ABYSS_AMBIENT_LIGHT_COLOR); // light color in entire system, affects all entities
		center.addTag(Tags.AMBIENT_LS);

		String name = Misc.genEntityCatalogId(Misc.CatalogEntryType.GIANT);
		PlanetAPI giant = system.addPlanet("fenar_ice_giant", null, name, Planets.ICE_GIANT, 0, 450, 0, 0);
		giant.getMarket().addCondition(Conditions.DENSE_ATMOSPHERE);
		giant.getMarket().addCondition(Conditions.COLD);
		giant.getMarket().addCondition(Conditions.DARK);
		giant.getMarket().addCondition(Conditions.VOLATILES_TRACE);
		giant.getMarket().addCondition(Conditions.HIGH_GRAVITY);
		giant.setOrbit(null);
		giant.setLocation(0, 0);

		CustomCampaignEntityAPI beacon = system.addCustomEntity(null, null, Entities.WARNING_BEACON, Factions.NEUTRAL);
		beacon.setCircularOrbitPointingDown(giant, 0, 1000, 60);

		beacon.getMemoryWithoutUpdate().set("$planetFenarBeacon", true);
		beacon.getMemoryWithoutUpdate().set(WarningBeaconEntityPlugin.PING_ID_KEY, Pings.WARNING_BEACON1);
		beacon.getMemoryWithoutUpdate().set(WarningBeaconEntityPlugin.PING_FREQ_KEY, 2f);
		beacon.getMemoryWithoutUpdate().set(WarningBeaconEntityPlugin.PING_COLOR_KEY, new Color(200, 5, 250, 150));
		beacon.getMemoryWithoutUpdate().set(WarningBeaconEntityPlugin.GLOW_COLOR_KEY, new Color(100, 0, 255, 150));

		PlanetAPI fenar = system.addPlanet("fenar_fenar", giant, "Fenar", Planets.TUNDRA, 40, 130, 4900, 275);
		fenar.getMarket().addCondition(Conditions.COLD);
		fenar.getMarket().addCondition(Conditions.DARK);
		fenar.getMarket().addCondition(Conditions.ORE_MODERATE);
		fenar.getMarket().addCondition(Conditions.RARE_ORE_SPARSE);
		fenar.getMarket().addCondition(Conditions.HABITABLE);
		fenar.getMarket().addCondition(Conditions.RUINS_WIDESPREAD);
		fenar.getMarket().addCondition(Conditions.ORGANICS_TRACE);
		fenar.getMarket().addCondition(Conditions.SOLAR_ARRAY);
		fenar.getMarket().addCondition(Conditions.OUTPOST);
		fenar.getMarket().addCondition(Conditions.FRONTIER);
		fenar.getMemoryWithoutUpdate().set("$planetFenar", true);
		fenar.getMarket().getMemoryWithoutUpdate().set("$ruinsExplored", true);

		CoreLifecyclePluginImpl.addRuinsJunk(fenar);

		String faction = Factions.NEUTRAL;
		float period = fenar.getCircularOrbitPeriod();
		float angle = fenar.getCircularOrbitAngle();
		float radius = 270f + fenar.getRadius();
		float xp = 200f, profile = 1000f;
		SectorEntityToken mirror1 = system.addCustomEntity(null, "Stellar Mirror Alpha", Entities.STELLAR_MIRROR, faction);
		SectorEntityToken mirror2 = system.addCustomEntity(null, "Stellar Mirror Beta", Entities.STELLAR_MIRROR, faction);
		SectorEntityToken mirror3 = system.addCustomEntity(null, "Stellar Mirror Gamma", Entities.STELLAR_MIRROR, faction);
		SectorEntityToken mirror4 = system.addCustomEntity(null, "Stellar Mirror Delta", Entities.STELLAR_MIRROR, faction);
		SectorEntityToken mirror5 = system.addCustomEntity(null, "Stellar Mirror Epsilon", Entities.STELLAR_MIRROR, faction);
		mirror1.setCircularOrbitPointingDown(fenar, angle - 40, radius, period);
		mirror2.setCircularOrbitPointingDown(fenar, angle - 20, radius, period);
		mirror3.setCircularOrbitPointingDown(fenar, angle, radius, period);
		mirror4.setCircularOrbitPointingDown(fenar, angle + 20, radius, period);
		mirror5.setCircularOrbitPointingDown(fenar, angle + 40, radius, period);
		makeDiscoverable(mirror1, xp, profile);
		makeDiscoverable(mirror2, xp, profile);
		makeDiscoverable(mirror3, xp, profile);
		makeDiscoverable(mirror4, xp, profile);
		makeDiscoverable(mirror5, xp, profile);
		mirror1.getMemoryWithoutUpdate().set("$planetFenarMirror", true);
		mirror2.getMemoryWithoutUpdate().set("$planetFenarMirror", true);
		mirror3.getMemoryWithoutUpdate().set("$planetFenarMirror", true);
		mirror4.getMemoryWithoutUpdate().set("$planetFenarMirror", true);
		mirror5.getMemoryWithoutUpdate().set("$planetFenarMirror", true);

		system.generateAnchorIfNeeded();

		NascentGravityWellAPI well = Global.getSector().createNascentGravityWell(beacon, 50f);
		well.addTag(Tags.NO_ENTITY_TOOLTIP);
		well.setColorOverride(new Color(125, 50, 255));
		Global.getSector().getHyperspace().addEntity(well);
		well.autoUpdateHyperLocationBasedOnInSystemEntityAtRadius(beacon, 0);

		Global.getSector().getMemoryWithoutUpdate().set(NASCENT_WELL_KEY, well);

		HyperspaceTerrainPlugin plugin = (HyperspaceTerrainPlugin) Misc.getHyperspaceTerrain().getPlugin();
		NebulaEditor editor = new NebulaEditor(plugin);
		editor.clearArc(system.getLocation().x, system.getLocation().y, 0, 160, 0, 360f);
	}
}
