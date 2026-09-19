package contractor.data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import contractor.data.scripts.weapons.shots.EntangledAI;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;

public class EntangledMod extends BaseHullMod {
	public static class EntangledModData {
		private final IntervalUtil tracker = new IntervalUtil(0.2f, 0.3f);
		private final IntervalUtil trackerJitter = new IntervalUtil(0.45f, 0.55f);
		public String parentID = "";
		private boolean dissipate = false;
	}

	private static final Color JITTER_COLOR = new Color(145, 2, 105, 45);
	private static final Color JITTER_UNDER_COLOR = new Color(175, 60, 120, 150);
	private static final Color ARC_COLOR = new Color(175, 60, 160, 255);
	private static final String DATAKEY = "EntangledKey_";
	private static final float DAMAMT = 20f;
	private static final float EMPAMT = 60f;
	private static final float VENTBONUS = 150f;

	public void advanceInCombat(ShipAPI ship, float amount) {
		if (!ship.isAlive()) {
			if (ship.isHulk()) {
				Global.getCombatEngine().removeEntity(ship);
			}
			return;
		}

		ship.setJitter(this, JITTER_COLOR, 1f, 3, 0, 10f);
		ship.setJitterUnder(this, JITTER_UNDER_COLOR, 1f, 25, 0f, 15f);

		WeaponAPI weapon = null;
		for (WeaponAPI w : ship.getAllWeapons())
			if (w.getSlot().getId().equals("ENT01")) //required slot id on hull
				weapon = w;

		CombatEngineAPI engine = Global.getCombatEngine();
		String key = DATAKEY + ship.getId();

		EntangledModData data = (EntangledModData) engine.getCustomData().get(key);
		if (data == null) {
			data = new EntangledModData();
			engine.getCustomData().put(key, data);
		}

		Vector2f zeroed = new Vector2f(0f, 0f);
		Vector2f spriteSize = new Vector2f(Global.getSettings().getSprite("systems", "drg_hydra_sprite").getWidth(), Global.getSettings().getSprite("systems", "drg_hydra_sprite").getHeight());
		float arcAngleJitter = ship.getFacing() - 90f;

		data.trackerJitter.advance(amount);
		if (data.trackerJitter.intervalElapsed())
			MagicRender.battlespace(Global.getSettings().getSprite("systems", "drg_hydra_sprite"), ship.getLocation(), zeroed, spriteSize, zeroed, arcAngleJitter, 0, JITTER_UNDER_COLOR, true, 0.1f, 0.1f, 1f);

		float arcAngle = 0f;
		if (!data.parentID.isEmpty()) {
			for (ShipAPI engineShip : engine.getShips())
				if (engineShip.getId().equals(data.parentID)) {
					arcAngle = VectorUtils.getAngle(ship.getLocation(), engineShip.getLocation());
				}
		}
		weapon.setFacing(arcAngle); //crash if no slot

		if (data.parentID.isEmpty())
			return;

		data.tracker.advance(amount);
		if (data.tracker.intervalElapsed()) {
			float angleRandomed = (weapon.getCurrAngle() + ((float) (Math.random() - 0.5f) * 75f));
			if (angleRandomed > 360f)
				angleRandomed -= 360f;
			else if (angleRandomed < 0f)
				angleRandomed += 360f;

			MissileAPI missile = (MissileAPI) engine.spawnProjectile(ship, weapon, "drg_entangled_spark", weapon.getFirePoint(0), angleRandomed, null);
			missile.setWeaponSpec(weapon.getId());
			missile.setMissileAI(new EntangledAI(missile, data.parentID));
			missile.getActiveLayers().remove(CombatEngineLayers.FF_INDICATORS_LAYER);

			if (ship.getFluxLevel() > 0.5f && !data.dissipate) {
				data.dissipate = true;
				ShipAPI parent = null;

				for (ShipAPI engineShip : engine.getShips())
					if (engineShip.getId().equals(data.parentID))
						parent = engineShip;

				ship.getMutableStats().getFluxDissipation().modifyFlat(DATAKEY, VENTBONUS);
				Global.getCombatEngine().spawnEmpArc(ship, ship.getLocation(), ship, parent, DamageType.ENERGY, DAMAMT, EMPAMT, 2300f,
						"system_emp_emitter_impact", 35f, JITTER_UNDER_COLOR, ARC_COLOR);
			} else if (data.dissipate) {
				data.dissipate = false;
				ship.getMutableStats().getFluxDissipation().unmodifyFlat(DATAKEY);
			}
		}
	}

	public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
		if (index == 0) return "" + (int) Math.round(VENTBONUS);
		if (index == 1) return (int) Math.round(50f) + "%";
		if (index == 2) return "damaging emp arcs";

		return null;
	}
}
