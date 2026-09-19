package contractor.data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import contractor.data.scripts.plugins.PlayerMissileCommand;
import org.lazywizard.lazylib.MathUtils;

import java.awt.*;
import java.util.HashMap;

import static contractor.data.scripts.util.ContractorStaticVars.MC_BONUS;
import static contractor.data.scripts.util.ContractorStaticVars.MISSILE_COMMAND_DATAID;

public class MissileCommand extends BaseHullMod {
	private static final float LMISSILE_COST_MOD = 20f;
	private static final float MMISSILE_COST_MOD = 30f;
	private static final float SMISSILE_COST_MOD = 40f;
	private static final float MISSILE_ROF_MOD = 25f;
	private static float pulse = 0.8f;

	private static final HashMap<ShipAPI.HullSize, Float> SIGHT_BONUS = new HashMap<>(); static {
		SIGHT_BONUS.put(ShipAPI.HullSize.FRIGATE, 500f);
		SIGHT_BONUS.put(ShipAPI.HullSize.DESTROYER, 1000f);
		SIGHT_BONUS.put(ShipAPI.HullSize.CRUISER, 1500f);
		SIGHT_BONUS.put(ShipAPI.HullSize.CAPITAL_SHIP, 2000f);
	}

	public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getDynamic().getMod(Stats.LARGE_MISSILE_MOD).modifyPercent(id, -LMISSILE_COST_MOD);
		stats.getDynamic().getMod(Stats.MEDIUM_MISSILE_MOD).modifyPercent(id, -MMISSILE_COST_MOD);
		stats.getDynamic().getMod(Stats.SMALL_MISSILE_MOD).modifyPercent(id, -SMISSILE_COST_MOD);

		stats.getSightRadiusMod().modifyFlat(id, SIGHT_BONUS.get(hullSize));

		stats.getMissileRoFMult().modifyPercent(id, MISSILE_ROF_MOD);
	}

	public void applyEffectsAfterShipAddedToCombatEngine(ShipAPI ship, String id) {
		CombatEngineAPI engine = Global.getCombatEngine();
		if (!engine.hasPluginOfClass(PlayerMissileCommand.class))
			engine.addPlugin(new PlayerMissileCommand());
	}

	public boolean affectsOPCosts() {
		return true;
	}

	public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
		return false;
	}

	public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		float opad = 10f;

		Color h = Misc.getHighlightColor();
		Color b = Misc.getNegativeHighlightColor();

		tooltip.addPara("Ship has a Missile Combat Information Center to control, and necessary supply infrastructure to support, missile weapons platforms. " +
						"OP cost of missile weapons reduced by %s/%s/%s per mount size and missile weapons fire %s faster.", opad, h,
				Math.round(SMISSILE_COST_MOD) + "%", Math.round(MMISSILE_COST_MOD) + "%", Math.round(LMISSILE_COST_MOD) + "%", Math.round(MISSILE_ROF_MOD) + "%");
		tooltip.addPara("Also grants %s additional command missile bandwidth, and gives the player ship the ability to select missile guidance mode.", opad, h,
				MC_BONUS + "");
	}

	public void advanceInCombat(ShipAPI ship, float amount) {
		CombatEngineAPI engine = Global.getCombatEngine();
		if (!engine.isEntityInPlay(ship))
			return;
		WeaponAPI dish = null;
		int mode = 0;

		if (ship == engine.getPlayerShip() && engine.getCustomData().get(MISSILE_COMMAND_DATAID) != null)
			mode = (((PlayerMissileCommand.PlayerMissileCommandData) engine.getCustomData().get(MISSILE_COMMAND_DATAID)).systemMode);

		for (WeaponAPI wep : ship.getAllWeapons())
			if (wep.getSpec().getWeaponId().equals("sep_command_dish"))
				dish = wep;

		if (dish != null) {
			float face = dish.getCurrAngle();
			if (mode == 6) {
				face = face + 15f * amount * MathUtils.getShortestRotation(face, Misc.getAngleInDegrees(dish.getLocation(), ship.getMouseTarget()));
				pulse += amount;
				if (pulse > 0.95f)
					dish.setForceFireOneFrame(true);
				if (pulse > 1)
					pulse = 0;
			} else {
				face = face + amount * 180f;
			}
			face = Misc.normalizeAngle(face);
			dish.setFacing(face);
		}
	}
}
