package contractor.data.scripts.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import lunalib.lunaSettings.LunaSettings;
import org.lwjgl.input.Keyboard;
import org.magiclib.util.MagicSettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContractorStaticVars {
	public static final String CONTRACTOR_MODID = "contractor_separatists";
	public static final String CONTRACTOR_SEP_ID = "contractor_separatists", CONTRACTOR_DRG_ID = "contractor_duskgroup", CONTRACTOR_DRG_SURV_ID = "contractor_dusksurvivors", CONTRACTOR_FENARANS_ID = "contractor_fenarans", CONTRACTOR_NANITE_ID = "contractor_nanite";
	public static final String SEPARATIST_FACTION_LEAD = "sepFactionLeader", DUSKGROUP_FACTION_LEAD = "duskFactionLeader", DRG_BLACKSITE_BS_LEAD = "duskBlacksiteAlphaAdmin", FENAR_FACTION_LEAD = "fenarFactionLeader", DUSKGROUP_PSI_AI_LEADER = "duskPsiAI";
	public static final String MISSILE_COMMAND_DATAID = "missilecommand_player_data";
	public static final String KOIT_ID = "separatists_koit", APOPHIS_ID = "separatists_apophis", LYNCIS_ID = "separatists_lyncis", SERAPH_ID = "separatists_seraph", DUSK_ID = "duskgroup_planet";
	public static final String SEP_CONDITION_FRONTIER = "sep_frontier", SEP_CONDITION_REDICE = "red_ice", SEP_CONDITION_GEOTHERMAL = "sep_geothermal";
	public static final String CONTRACTOR_PSI_CORE_ID = "drg_psi_core", CONTRACTOR_BIOMETAL_ID = "drg_biometal_item";
	public static final String DRG_ROALA_PLANET_SYSTEM_KEY = "$drgROALA_planetSystem", DRG_ROALA_ARK_SYSTEM_KEY = "$drgROALA_arkSystem", DRG_ROALA_PLANET_KEY = "$drgROALA_planet", DRG_ROALA_QM_KEY = "$drgROALA_qm";
	public static final String CONTRACTOR_NANITE_HOME_SYSTEM = "contractor_nanite_system", CONTRACTOR_NANITE_NO_SPAWN = "theme_con_nanite_no_spawn", CONTRACTOR_NO_NANITE_REGEN = "contractor_no_nanite_regen";
	public static final Map<ShipAPI.HullSize, Integer> COMMAND_MISSILE_BASE = new HashMap<>();
	public static final List<String> NANITE_PROTECTED_TARGETS = new ArrayList<>();
	public static int COMMAND_KEY, ECCM_BONUS, MC_BONUS;
	public static float NANITE_MAX_MAG = 0.5f;
	public static int NANITE_PROGRESS_SCALE = 15;
	public static boolean STATUS_EFFECTS_ENABLED = true, FENARANS_ENABLED = false;
	public static float REPAIR_BEAM_RATIO = 0.2f;

	public static void loadValues() {
		if (Global.getSettings().getModManager().isModEnabled("lunalib")) {
			STATUS_EFFECTS_ENABLED = LunaSettings.getBoolean(CONTRACTOR_MODID, "sep_StatusEffects");

			if (LunaSettings.getBoolean(CONTRACTOR_MODID, "sep_FenarEnable") != null)
				FENARANS_ENABLED = LunaSettings.getBoolean(CONTRACTOR_MODID, "sep_FenarEnable");

			COMMAND_KEY = LunaSettings.getInt(CONTRACTOR_MODID, "sep_CommandKey");
			COMMAND_MISSILE_BASE.put(HullSize.FIGHTER, 2);
			COMMAND_MISSILE_BASE.put(HullSize.FRIGATE, LunaSettings.getInt(CONTRACTOR_MODID, "sep_CmdLimitFrig"));
			COMMAND_MISSILE_BASE.put(HullSize.DESTROYER, LunaSettings.getInt(CONTRACTOR_MODID, "sep_CmdLimitDest"));
			COMMAND_MISSILE_BASE.put(HullSize.CRUISER, LunaSettings.getInt(CONTRACTOR_MODID, "sep_CmdLimitCru"));
			COMMAND_MISSILE_BASE.put(HullSize.CAPITAL_SHIP, LunaSettings.getInt(CONTRACTOR_MODID, "sep_CmdLimitCap"));

			ECCM_BONUS = LunaSettings.getInt(CONTRACTOR_MODID, "sep_EccmBonus");
			MC_BONUS = LunaSettings.getInt(CONTRACTOR_MODID, "sep_McBonus");
			REPAIR_BEAM_RATIO = LunaSettings.getFloat(CONTRACTOR_MODID, "sep_RepairBeamLimit");

			Global.getSettings().setBoolean("enableSeparatistStart", LunaSettings.getBoolean(CONTRACTOR_MODID, "sep_SeparatistStart"));
		} else {
			STATUS_EFFECTS_ENABLED = MagicSettings.getBoolean(CONTRACTOR_MODID, "enable_status_effects");
			FENARANS_ENABLED = MagicSettings.getBoolean(CONTRACTOR_MODID, "enable_fenarans");

			COMMAND_KEY = Keyboard.getKeyIndex(MagicSettings.getString(CONTRACTOR_MODID, "command_key"));
			COMMAND_MISSILE_BASE.put(HullSize.FIGHTER, 2);
			COMMAND_MISSILE_BASE.put(HullSize.FRIGATE, MagicSettings.getInteger(CONTRACTOR_MODID, "command_missile_base_frigate"));
			COMMAND_MISSILE_BASE.put(HullSize.DESTROYER, MagicSettings.getInteger(CONTRACTOR_MODID, "command_missile_base_destroyer"));
			COMMAND_MISSILE_BASE.put(HullSize.CRUISER, MagicSettings.getInteger(CONTRACTOR_MODID, "command_missile_base_cruiser"));
			COMMAND_MISSILE_BASE.put(HullSize.CAPITAL_SHIP, MagicSettings.getInteger(CONTRACTOR_MODID, "command_missile_base_capital"));

			ECCM_BONUS = MagicSettings.getInteger(CONTRACTOR_MODID, "command_eccm_bonus");
			MC_BONUS = MagicSettings.getInteger(CONTRACTOR_MODID, "command_cic_bonus");
			REPAIR_BEAM_RATIO = MagicSettings.getFloat(CONTRACTOR_MODID, "repairbeam_ratio");

		}

		NANITE_MAX_MAG = MagicSettings.getFloat(CONTRACTOR_MODID, "nanite_max_mag");
		NANITE_PROGRESS_SCALE = MagicSettings.getInteger(CONTRACTOR_MODID, "nanite_progress_scale");

		NANITE_PROTECTED_TARGETS.clear();
		NANITE_PROTECTED_TARGETS.addAll(MagicSettings.getList(CONTRACTOR_MODID, "nanite_immune_list"));
	}
}
