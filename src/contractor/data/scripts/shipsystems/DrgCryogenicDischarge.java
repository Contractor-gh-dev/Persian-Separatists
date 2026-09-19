package contractor.data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class DrgCryogenicDischarge extends BaseShipSystemScript {
	private static final float SPEEDMOD = -50f;
	private static final Color coldGlow = new Color(175, 175, 255, 255);
	private static final Color wepGlow = new Color(125, 125, 255, 200);
	private boolean hasFired = false;

	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = null;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI) stats.getEntity();
		} else {
			return;
		}

		stats.getMaxSpeed().modifyPercent(id, SPEEDMOD);
		stats.getAcceleration().modifyPercent(id, SPEEDMOD);
		stats.getDeceleration().modifyPercent(id, SPEEDMOD);
		stats.getTurnAcceleration().modifyPercent(id, SPEEDMOD);

		List<WeaponAPI> sysWeps = new ArrayList<>();
		for (WeaponAPI wep : ship.getAllWeapons()) {
			if (wep.getId().equals("drg_cryogenicemitter")) {
				wep.setGlowAmount(effectLevel * 0.5f, wepGlow);
				sysWeps.add(wep);
			}
		}

		if (state == State.IN || state == State.ACTIVE) {

			Vector2f randomArea = new Vector2f(MathUtils.getRandomPointInCircle(ship.getLocation(), 160f));
			float size = (float) Math.random() * 64f + 32f;
			Global.getCombatEngine().addNebulaParticle(randomArea, ship.getVelocity(), size, 0.75f, 0.5f, 0.3f, 2f, coldGlow, true);
			for (WeaponAPI sysWep : sysWeps) {
				Vector2f randomVel = new Vector2f(Misc.getUnitVectorAtDegreeAngle(sysWep.getCurrAngle()));
				VectorUtils.resize(randomVel, (float) Math.random() * 32f + 64f, randomVel);
				randomVel = Vector2f.add(ship.getVelocity(), randomVel, randomVel);
				Global.getCombatEngine().addHitParticle(sysWep.getLocation(), randomVel, size * 0.4f, 1f, 0.7f, coldGlow);
			}
		}

		if (state == State.ACTIVE && !hasFired) {
			hasFired = true;
			for (WeaponAPI sysWep : sysWeps) {
				sysWep.setForceFireOneFrame(true);
			}
		}
	}

	public void unapply(MutableShipStatsAPI stats, String id) {
		ShipAPI ship = (ShipAPI) stats.getEntity();
		for (WeaponAPI wep : ship.getAllWeapons()) {
			if (wep.getId().equals("drg_cryogenicemitter")) {
				wep.setGlowAmount(0f, wepGlow);
			}
		}
		stats.getMaxSpeed().unmodify(id);
		stats.getAcceleration().unmodify(id);
		stats.getDeceleration().unmodify(id);
		stats.getTurnAcceleration().unmodify(id);
		hasFired = false;
	}

	public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
		if (system.isChargeup())
			return "CHARGING";
		return null;
	}
}
