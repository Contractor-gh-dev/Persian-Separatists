package contractor.data.scripts.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;

import java.util.ArrayList;
import java.util.List;

public class SepWingSweepStats extends BaseShipSystemScript {
	private boolean sweep = false, doOnce = false, arm = true;
	private ShipAPI ship;
	private final List<WeaponAPI> decos = new ArrayList<>();


	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ship = (ShipAPI) stats.getEntity();
		if (ship == null || ship.isHulk())
			return;

		if (!doOnce)
			doOnce();

		if (state == State.COOLDOWN)
			arm = true;

		if (state == State.ACTIVE && arm) {
			arm = false;
			if (sweep) {
				sweep = false;
				stats.getMaxTurnRate().unmodify(id);
				stats.getTurnAcceleration().unmodify(id);
				stats.getMaxSpeed().unmodify(id);
				stats.getAcceleration().unmodify(id);
				stats.getDeceleration().unmodify(id);
			} else {
				sweep = true;
				stats.getMaxTurnRate().modifyMult(id, 1.5f);
				stats.getTurnAcceleration().modifyMult(id, 1.5f);
				stats.getMaxSpeed().modifyMult(id, 0.75f);
				stats.getAcceleration().modifyMult(id, 1.4f);
				stats.getDeceleration().modifyMult(id, 1.4f);
			}
		}

		if (!sweep) {
			for (WeaponAPI deco : decos) {
				float angle = deco.getCurrAngle();
				float face = ship.getFacing();
				face += deco.getSlot().getAngle();
				face = Misc.normalizeAngle(face);
				angle += MathUtils.getShortestRotation(angle, face) * 0.01f;
				angle = Misc.normalizeAngle(angle);
				deco.setCurrAngle(angle);
			}
		} else {
			for (WeaponAPI deco : decos) {
				float angle = deco.getCurrAngle();
				angle += MathUtils.getShortestRotation(angle, ship.getFacing()) * 0.01f;
				angle = Misc.normalizeAngle(angle);
				deco.setCurrAngle(angle);
			}
		}
	}

	private void doOnce() {
		doOnce = true;
		for (WeaponAPI wep : ship.getAllWeapons()) {
			if (wep.getSpec().hasTag("sep_wingsweep"))
				decos.add(wep);
		}
	}

	public boolean getMode() {
		return sweep;
	}
}
