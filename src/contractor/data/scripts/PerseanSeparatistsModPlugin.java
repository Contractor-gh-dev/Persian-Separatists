package contractor.data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.econ.ResourceDepositsCondition;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.intel.events.HostileActivityEventIntel;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import com.fs.starfarer.api.impl.codex.CodexDataV2;
import com.fs.starfarer.api.impl.codex.CodexEntryPlugin;
import com.fs.starfarer.api.impl.codex.CodexEntryV2;

import contractor.data.codex.*;
import contractor.data.scripts.campaign.fleets.DisposableNaniteFleetManager;
import contractor.data.scripts.campaign.fleets.SDFSeparatists;
import contractor.data.scripts.campaign.fleets.SDFSeparatists2;
import contractor.data.scripts.campaign.intel.NaniteHostileActivityCauseInfra;
import contractor.data.scripts.campaign.intel.NaniteHostileActivityCauseSize;
import contractor.data.scripts.campaign.intel.NaniteHostileActivityCauseSystem;
import contractor.data.scripts.campaign.intel.NaniteHostileActivityFactor;
import contractor.data.scripts.characters.FastImportantPerson;
import contractor.data.scripts.plugins.ContractorCampaignPlugin;
import contractor.data.scripts.plugins.ContractorLunaListener;
import contractor.data.scripts.util.ContractorStaticVars;
import contractor.data.scripts.weapons.SepRepairBeamAI;
import contractor.data.scripts.weapons.shots.*;
import contractor.data.world.dusk.ContractorMiscGen;
import contractor.data.world.dusk.DuskGen;
import contractor.data.world.dusk.FenaranGen;
import contractor.data.world.dusk.SeparatistsGen;
import lunalib.lunaSettings.LunaSettings;

import java.util.ArrayList;

import static com.fs.starfarer.api.impl.codex.CodexDataV2.CAT_WEAPONS;
import static contractor.data.scripts.util.ContractorStaticVars.*;

public class PerseanSeparatistsModPlugin extends BaseModPlugin {
	static {
		ResourceDepositsCondition.COMMODITY.put("red_ice", "drg_crystals");
		ResourceDepositsCondition.MODIFIER.put("red_ice", 1);
		ResourceDepositsCondition.INDUSTRY.put("drg_crystals", Industries.MINING);
		ResourceDepositsCondition.BASE_MODIFIER.put("drg_crystals", -1);
	}

	@Override
	public void onApplicationLoad() {
		boolean hasLazyLib = Global.getSettings().getModManager().isModEnabled("lw_lazylib");
		boolean hasMagicLib = Global.getSettings().getModManager().isModEnabled("MagicLib");
		boolean hasLunaLib = Global.getSettings().getModManager().isModEnabled("lunalib");

		if (!hasLazyLib)
			throw new RuntimeException("Persean Separatists requires LazyLib!" + "\nGet it at https://fractalsoftworks.com/forum/index.php?topic=5444");
		if (!hasMagicLib)
			throw new RuntimeException("Persean Separatists requires MagicLib!" + "\nGet it at https://fractalsoftworks.com/forum/index.php?topic=25868");
		if (hasLunaLib)
			LunaSettings.addSettingsListener(new ContractorLunaListener());

		ContractorStaticVars.loadValues();
		Global.getSettings().getSimOpponentsDev().add("sep_station_small_mix");
		Global.getSettings().getSimOpponentsDev().add("sep_station_medium_mix");
		Global.getSettings().getSimOpponentsDev().add("sep_station_big_mix");
		Global.getSettings().resetCached();
	}

	public void onDevModeF8Reload() {
		ContractorStaticVars.loadValues();
	}

	@Override
	public void onNewGame() {
		boolean haveNexerelin = Global.getSettings().getModManager().isModEnabled("nexerelin");
		if (!haveNexerelin || !Global.getSector().getMemoryWithoutUpdate().getBoolean("$nex_randomSector")) {
			initDusk();
			initSeparatists();
			initMisc();
			if (FENARANS_ENABLED)
				initFenarans();
		}
	}

	public void onGameLoad(boolean newGame) {
		ContractorStaticVars.loadValues();
		SectorAPI sector = Global.getSector();

		boolean haveNexerelin = Global.getSettings().getModManager().isModEnabled("nexerelin");
		if (haveNexerelin && sector.getMemoryWithoutUpdate().getBoolean("$nex_randomSector")) {
			sector.registerPlugin(new ContractorCampaignPlugin());
			return;
		}

		if (!sector.hasScript(SDFSeparatists.class))
			sector.addScript(new SDFSeparatists());
		if (!sector.hasScript(SDFSeparatists2.class))
			sector.addScript(new SDFSeparatists2());
		if (!sector.hasScript(DisposableNaniteFleetManager.class))
			sector.addScript(new DisposableNaniteFleetManager());
		sector.registerPlugin(new ContractorCampaignPlugin());

		boolean hasDuskGroup = SharedData.getData().getPersonBountyEventData().isParticipating(CONTRACTOR_DRG_ID);
		boolean hasSeparatists = SharedData.getData().getPersonBountyEventData().isParticipating(CONTRACTOR_SEP_ID);
		boolean hasFenarans = sector.getMemoryWithoutUpdate().getBoolean("$FenaransGened");

		if (!hasDuskGroup)
			initDusk();
		if (!hasSeparatists) {
			initSeparatists();
			initMisc();
		}
		if (FENARANS_ENABLED && !hasFenarans)
			initFenarans();

		ContractorMiscGen.initNaniteFactionPost(sector);

		Global.getSector().getListenerManager().removeListenerOfClass(NaniteHostileActivityFactor.class);

		HostileActivityEventIntel HAEI = HostileActivityEventIntel.get();
		if (HAEI != null) {
			HAEI.removeActivityOfClass(NaniteHostileActivityFactor.class);

			NaniteHostileActivityFactor factor = new NaniteHostileActivityFactor(HAEI);
			HAEI.addActivity(factor, new NaniteHostileActivityCauseSystem(HAEI));
			HAEI.addActivity(factor, new NaniteHostileActivityCauseInfra(HAEI));
			HAEI.addActivity(factor, new NaniteHostileActivityCauseSize(HAEI));
		}

		MarketAPI duskHome = sector.getEconomy().getMarket("dusk_matador_market");
		MarketAPI sepHome = sector.getEconomy().getMarket("separatists_sedna_market");

		//ArrayList<String> list = new ArrayList<>(1);
		//list.add(Tags.CONTACT_SCIENCE);
		ArrayList<String> list2 = new ArrayList<>(1);
		list2.add(Skills.INDUSTRIAL_PLANNING);

		if (sector.getImportantPeople().getData(DRG_BLACKSITE_BS_LEAD) == null) {
			FastImportantPerson.createPerson(null, DRG_BLACKSITE_BS_LEAD, CONTRACTOR_DRG_ID, FullName.Gender.MALE, "internalAffairs", "internalAffairs",
					PersonImportance.VERY_HIGH, "Isaac", "Herne", null, Voices.VILLAIN, list2, false, 0, false, false);
		}

		if (sector.getImportantPeople().getData(DUSKGROUP_FACTION_LEAD) == null) {
			FastImportantPerson.createPerson(duskHome, DUSKGROUP_FACTION_LEAD, CONTRACTOR_DRG_ID, FullName.Gender.MALE, "factionLeader", "factionLeader",
					PersonImportance.VERY_HIGH, "Lorenz", "Stancliff", null, Voices.SCIENTIST, list2, true, 0, false, false);
		}

		if (sector.getImportantPeople().getData(SEPARATIST_FACTION_LEAD) == null) {
			FastImportantPerson.createPerson(sepHome, SEPARATIST_FACTION_LEAD, CONTRACTOR_SEP_ID, FullName.Gender.MALE, "factionLeader", "factionLeader",
					PersonImportance.VERY_HIGH, "Jose", "Arman", null, Voices.SOLDIER, list2, true, 0, false, false);
		}

		if (sector.getImportantPeople().getData(DUSKGROUP_PSI_AI_LEADER) == null)
			FastImportantPerson.createPerson(null, DUSKGROUP_PSI_AI_LEADER, CONTRACTOR_DRG_ID, FullName.Gender.MALE, "customResearchAi", "customResearchAi",
					PersonImportance.VERY_HIGH, "Octarine", "Intangible", null, Voices.SCIENTIST, null, false, 0, true, false);

		if (FENARANS_ENABLED) {
			MarketAPI fenarHome = sector.getStarSystem("Fenar").getEntityById("fenar_fenar").getMarket();
			if (sector.getImportantPeople().getData(FENAR_FACTION_LEAD) == null) {
				FastImportantPerson.createPerson(fenarHome, FENAR_FACTION_LEAD, CONTRACTOR_FENARANS_ID, FullName.Gender.MALE, "factionLeader", "factionLeader",
						PersonImportance.VERY_HIGH, "F", "L", null, Voices.FAITHFUL, list2, false, 0, false, false);
			}
		}
	}

	public PluginPick<MissileAIPlugin> pickMissileAI(MissileAPI missile, ShipAPI launchingShip) {
		return switch (missile.getProjectileSpecId()) {
			case "sep_type1_cruise_missile", "sep_type1_cruise_f_missile" ->
					new PluginPick<>(new SepTypeOneMissileAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC);
			case "sep_gator_missile", "sep_gator_missile_f" ->
					new PluginPick<>(new SepGatorMissileAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC);
			case "sep_vls_med_missile", "sep_bowe_torp", "sep_bowe_f_torp" ->
					new PluginPick<>(new SepVLSMissileAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC);
			case "sep_vls_med_dumb_missile" ->
					new PluginPick<>(new SepVLSDumbMissileAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC);
			case "sep_kessler_missile" ->
					new PluginPick<>(new KesslerMissileAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC);
			default -> null;
		};
	}

	public PluginPick<AutofireAIPlugin> pickWeaponAutofireAI(WeaponAPI weapon) {
		if (weapon.getId().equals("sep_repairbeam"))
			return new PluginPick<>(new SepRepairBeamAI(weapon), CampaignPlugin.PickPriority.MOD_SPECIFIC);
		return null;
	}

	public void onCodexDataGenerated() {
		CodexEntryPlugin commandMissile = new CodexCommandMissiles(CodexIds.COMMAND_MISSILES, "Command Missiles", CodexDataV2.getIcon(CodexIds.COMMAND_MISSILES));
		CodexEntryPlugin commandMissileMode = new CodexCommandMissilesModes(CodexIds.COMMAND_MISSILES_MODE, "Command Missile Modes", CodexDataV2.getIcon(CodexIds.COMMAND_MISSILES));
		CodexEntryPlugin status = new CodexEntryV2(CodexIds.CAT_STATUS, "Status Effects", CodexDataV2.getIcon(CodexIds.CAT_STATUS));
		CodexEntryPlugin statusCryo = new CodexCryogenic(CodexIds.STATUS_CRYOGENIC, "Cryogenic Freeze", CodexDataV2.getIcon(CodexIds.STATUS_CRYOGENIC));
		CodexEntryPlugin stausRad = new CodexRadiation(CodexIds.STATUS_RADIATION, "Radioactive Decay", CodexDataV2.getIcon(CodexIds.STATUS_RADIATION));
		CodexEntryPlugin statusJam = new CodexJammed(CodexIds.STATUS_JAMMED, "EWAR Jamming", CodexDataV2.getIcon(CodexIds.STATUS_JAMMED));
		CodexEntryPlugin statusEntropic = new CodexEntropic(CodexIds.STATUS_ENTROPIC, "Entropic Instability", CodexDataV2.getIcon(CodexIds.STATUS_ENTROPIC));
		CodexEntryPlugin statusElectric = new CodexElectric(CodexIds.STATUS_ELECTRIC, "Electrical Overload", CodexDataV2.getIcon(CodexIds.STATUS_ELECTRIC));
		CodexEntryPlugin statusNanite = new CodexNaniteCorruption(CodexIds.STATUS_NANITE, "Nanite Corruption", CodexDataV2.getIcon(CodexIds.STATUS_NANITE));

		CodexDataV2.ROOT.addChild(status);
		status.addChild(statusCryo);
		status.addChild(stausRad);
		status.addChild(statusJam);
		status.addChild(statusEntropic);
		status.addChild(statusElectric);
		status.addChild(statusNanite);
		CodexDataV2.getEntry(CAT_WEAPONS).addChild(commandMissile);
		CodexDataV2.getEntry(CAT_WEAPONS).addChild(commandMissileMode); //todo: figure out how to specify tech/manufacturer so they dont appear in all categories

		CodexDataV2.rebuildIdToEntryMap();

		CodexDataV2.makeRelated("codex_system_drg_entanglement", "codex_hullmod_entangled_mod");
		CodexDataV2.makeRelated(CodexIds.STATUS_CRYOGENIC, "codex_system_drg_cryogenicdischarge");
		CodexDataV2.makeRelated(CodexIds.STATUS_CRYOGENIC, "codex_weapon_drg_cryobeam");
		CodexDataV2.makeRelated(CodexIds.STATUS_RADIATION, "codex_weapon_sep_dirty_nuke");
		CodexDataV2.makeRelated(CodexIds.STATUS_RADIATION, "codex_weapon_sep_dirty_nuke_payload");
		CodexDataV2.makeRelated(CodexIds.STATUS_JAMMED, "codex_weapon_sep_eclipse");
		CodexDataV2.makeRelated(CodexIds.STATUS_ENTROPIC, "codex_system_drg_entropic_lash");
		//CodexDataV2.makeRelated(CodexIds.STATUS_JAMMED, "codex_weapon_sep_eclipse_fighter");
		CodexDataV2.makeRelated(CodexIds.STATUS_ELECTRIC, "codex_weapon_drg_thunder");
		CodexDataV2.makeRelated(CodexIds.STATUS_ELECTRIC, "codex_hullmod_distributed_fire_control");
		CodexDataV2.makeRelated(CodexIds.STATUS_ELECTRIC, "codex_system_sep_interdictor");
		CodexDataV2.makeRelated(CodexIds.STATUS_ELECTRIC, "codex_system_sep_interdictor_pulse");
		CodexDataV2.makeRelated(CodexIds.STATUS_ELECTRIC, "codex_system_sep_interdictor_station");
		CodexDataV2.makeRelated("codex_weapon_sep_dirty_nuke", "codex_weapon_sep_dirty_nuke_payload");
		CodexDataV2.makeRelated(CodexIds.COMMAND_MISSILES, "codex_hullmod_eccm");
		CodexDataV2.makeRelated(CodexIds.COMMAND_MISSILES, "codex_hullmod_contractor_missilecommand");
		CodexDataV2.makeRelated(CodexIds.COMMAND_MISSILES, "codex_weapon_sep_vls_small");
		CodexDataV2.makeRelated(CodexIds.COMMAND_MISSILES, "codex_weapon_sep_vls_med");
		CodexDataV2.makeRelated(CodexIds.COMMAND_MISSILES, "codex_weapon_sep_bowe");
		CodexDataV2.makeRelated(CodexIds.COMMAND_MISSILES, "codex_weapon_sep_bowe_rack");
		CodexDataV2.makeRelated(CodexIds.COMMAND_MISSILES, CodexIds.COMMAND_MISSILES_MODE);
		CodexDataV2.makeRelated(CodexIds.COMMAND_MISSILES_MODE, "codex_hullmod_contractor_missilecommand");
		CodexDataV2.makeRelated(CodexIds.STATUS_NANITE, "codex_weapon_nanite_corrupter");
		CodexDataV2.makeRelated(CodexIds.STATUS_NANITE, "codex_hullmod_contractor_bioarmor");
		CodexDataV2.makeRelated(CodexIds.STATUS_NANITE, "codex_hullmod_contractor_repairnanites");
		CodexDataV2.makeRelated(CodexIds.STATUS_NANITE, "codex_hullmod_contractor_naniteforges");

	}

	private static void initDusk() {
		new DuskGen().generate(Global.getSector());
	}

	private static void initSeparatists() {
		new SeparatistsGen().generate(Global.getSector());
	}

	private static void initMisc() {
		new ContractorMiscGen().generate(Global.getSector());
	}

	private static void initFenarans() {
		new FenaranGen().generate(Global.getSector());
	}
}
