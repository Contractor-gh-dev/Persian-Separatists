package contractor.data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.combat.threat.ConstructionSwarmSystemScript;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import contractor.data.scripts.plugins.NaniteRevivePlugin;

import java.awt.*;

public class NaniteHullmod extends BaseHullMod {
	public static final float AIM_BONUS = 0.75f;
	public static final float MISSILE_GUIDANCE_BONUS = 0.75f;
	public static final float EW_PENALTY_MULT = 0.5f;
	public static final float MODULE_DAMAGE_TAKEN_MULT = 0.75f;
	public static final float EMP_DAMAGE_TAKEN_MULT = 1.50f;

	public static final float MAX_REGEN_LEVEL = 1f;
	public static final float REGEN_RATE = 0.007f;
	public static final float SPAWN_TIME = 10f;
	public static final String SHIP_BEING_REVIVED = "ship_being_revived";

	@Override
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		ConstructionSwarmSystemScript.init();

		stats.getAutofireAimAccuracy().modifyFlat(id, AIM_BONUS);
		stats.getMissileGuidance().modifyFlat(id, MISSILE_GUIDANCE_BONUS);

		stats.getDynamic().getMod(Stats.ELECTRONIC_WARFARE_PENALTY_MOD).modifyMult(id, EW_PENALTY_MULT);

		stats.getEmpDamageTakenMult().modifyMult(id, EMP_DAMAGE_TAKEN_MULT);
		stats.getEngineDamageTakenMult().modifyMult(id, MODULE_DAMAGE_TAKEN_MULT);
		stats.getWeaponDamageTakenMult().modifyMult(id, MODULE_DAMAGE_TAKEN_MULT);
		stats.getDynamic().getMod(Stats.CAN_REPAIR_MODULES_UNDER_FIRE).modifyFlat(id, 1f);
	}

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		CombatEngineAPI engine = Global.getCombatEngine();
		if (ship.isAlive() && ship.getOriginalOwner() != 0) {
			engine.setCombatNotOverForAtLeast(SPAWN_TIME * 0.5f);
		}

		if (!ship.isHulk()) {
			doRepairs(ship, amount);

			String key = NaniteForges.DATAKEY + ship.getId();
			NaniteForges.NaniteForgesNpcData reloadData = (NaniteForges.NaniteForgesNpcData) engine.getCustomData().get(key);
			if (reloadData == null) {
				reloadData = new NaniteForges.NaniteForgesNpcData(ship);
				engine.getCustomData().put(key, reloadData);
			}
			reloadData.advance(ship, amount);
		} else if (ship.isHulk() && !ship.hasTag(SHIP_BEING_REVIVED))
			engine.addPlugin(new NaniteRevivePlugin(ship, SPAWN_TIME));
	}

	private void doRepairs(ShipAPI ship, float amount) {
		if (ship.getHullLevel() >= MAX_REGEN_LEVEL) return;
		if (ship.isHulk()) return;

		float maxHull = ship.getMaxHitpoints();
		float currHull = ship.getHitpoints();
		float maxPoints = maxHull * MAX_REGEN_LEVEL;
		float scale = (float) Math.min(1f, Math.log(ship.getMutableStats().getPeakCRDuration().computeEffective(ship.getHullSpec().getNoCRLossTime()) / ship.getTimeDeployedForCRReduction()));

		float repairAmount = maxHull * REGEN_RATE * amount * scale;

		if (repairAmount > maxPoints - currHull) repairAmount = maxPoints - currHull;

		if (repairAmount > 0)
			ship.setHitpoints(ship.getHitpoints() + repairAmount);
	}

	@Override
	public boolean shouldAddDescriptionToTooltip(HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
		return false;
	}

	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		float pad = 3f;
		float opad = 10f;
		Color h = Misc.getHighlightColor();
		Color bad = Misc.getNegativeHighlightColor();
		Color t = Misc.getTextColor();
		Color g = Misc.getGrayColor();

		tooltip.addPara("Nanite hulls have a number of shared properties.", pad);

		tooltip.addSectionHeading("Combat", Alignment.MID, opad);
		tooltip.addPara("Ship hull regenerates %s per second while ship has peak performance time remaining. The strength of regeneration weakens as peak time runs out.",
				opad, h, "" + (int) Math.round(REGEN_RATE * 100f) + "%");
		tooltip.addPara("All weapons that use and do not regain ammo will now regenerate some ammo every %s seconds.", opad, h,
				"60");
		tooltip.addPara("Target leading accuracy increased greatly for all weapons, including missiles. Effect "
				+ "of enemy ECM rating reduced by %s.", opad, h, "" + (int) Math.round(EW_PENALTY_MULT * 100f) + "%");
		tooltip.addPara("Weapon and engine damage taken is reduced by %s. However, EMP damage taken is increased by %s. In "
						+ "addition, repairs of damaged but functional weapons and engines can continue while they are under fire.",
				opad, h,
				(int) Math.round((1f - MODULE_DAMAGE_TAKEN_MULT) * 100f) + "%",
				(int) Math.round(Math.abs(1f - EMP_DAMAGE_TAKEN_MULT) * 100f) + "%");
	}

	public float getTooltipWidth() {
		return super.getTooltipWidth();
	}
}
